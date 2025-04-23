package wavesDRSN.p2p_messenger_backend.services.auth;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wavesDRSN.p2p_messenger_backend.security.CryptographyServiceImpl;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

@Getter
@Service
public class ChallengeService {
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final int challengeLength = 32;
    private final CryptographyServiceImpl cryptoService;

    @Autowired
    public ChallengeService(CryptographyServiceImpl cryptoService) {
        this.cryptoService = cryptoService;
    }


        public record Challenge(String id, byte[] bytes) {
    }

    public Challenge generateChallenge(String username) {
        byte[] challengeBytes = new byte[challengeLength];
        random.nextBytes(challengeBytes);

        String challengeId = UUID.randomUUID().toString();
        Challenge challenge = new Challenge(challengeId, challengeBytes);

        challenges.put(challengeId, challenge);
        return challenge;
    }

    public void validateChallenge(String challengeId, byte[] signature, PublicKey publicKey) {
        Challenge challenge = Optional.ofNullable(challenges.remove(challengeId))
            .orElseThrow(() -> new IllegalArgumentException("Invalid challenge ID"));

        if (!cryptoService.verifySignature(publicKey, challenge.bytes(), signature)) {
            throw new SecurityException("Invalid signature");
        }
    }
}
