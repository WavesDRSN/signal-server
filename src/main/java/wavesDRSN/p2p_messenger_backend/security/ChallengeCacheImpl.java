package wavesDRSN.p2p_messenger_backend.security;

import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ChallengeCacheImpl implements ChallengeCache {
    @Override
    public void putChallenge(String challengeId, byte[] challenge, Duration TTL) {
        //TODO: заглушка
    }

    @Override
    public byte[] getAndRemove(String challengeId) {
        return null; //TODO: заглушка
    }

}
