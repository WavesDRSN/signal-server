package wavesDRSN.p2p_messenger_backend.security;

public interface ChallengeCache  {
    /**
     *  Сохраняет challenge с TTL
     * @param challengeId идентификатор
     * @param challenge байты challenge
     */
    void putChallenge(String challengeId, byte[] challenge);

    /**
     * Получает и удаляет challenge по идентификатору
     * @param challengeId идентификатор
     * @return байты или null
     */
    byte[] getAndRemove(String challengeId);
}
