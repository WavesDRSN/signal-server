package wavesDRSN.p2p_messenger_backend.security;

import java.time.Duration;

public interface ChallengeCache  {
    void putChallenge(String challengeId, byte[] challenge, Duration TTL);

    byte[] getAndRemove(String challengeId);
}
