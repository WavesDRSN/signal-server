package wavesDRSN.p2p_messenger_backend.security;

import java.security.*;
import java.security.spec.InvalidKeySpecException;

public interface CryptographyService {
    /**
     * Преобразовывает сырые байты публ ключа из формата X.509 SPKI DER в PublicKey
     * @param publicKeyDerBytes массив байт публичного ключа
     * @return объект PublicKey, с которым может работать java и Bouncy Castle
     */
    PublicKey parsePublicKey(byte[] publicKeyDerBytes) throws InvalidKeySpecException;

    /**
     * Проверяет, правда ли данные были подписаны приватным ключом подходящим под публичный ключ из БД
     * @param publicKey из первого метода
     * @param data оригинальное задание challenge
     * @param signatureBytes отдельное док-во, присланное клиентом с помощью приватного ключа, data и Ed25519
     * @return ДА/НЕТ
     */
    boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signatureBytes);
}