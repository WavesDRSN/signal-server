package wavesDRSN.p2p_messenger_backend.security;

import java.time.Duration;

public interface ChallengeCache  {
    /**
     *  Сохраняет challenge с TTL
     * @param challengeId идентификатор
     * @param challenge байты challenge
     * @param TTL время жизни
     */
    void putChallenge(String challengeId, byte[] challenge, Duration TTL);

    /**
     * Получает и удаляет challenge по идентификатору
     * @param challengeId идентификатор
     * @return байты или null
     */
    byte[] getAndRemove(String challengeId);
}
