package wavesDRSN.p2p_messenger_backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

@Slf4j
@Service
public class CryptographyServiceImpl implements CryptographyService {

    @Override
    public PublicKey parsePublicKey(byte[] publicKeyDerBytes) {
        try {
            // объект для восстановления ключа из байтов
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
            // превращает массив байт publicKeyDerBytes в объект X509EncodedKeySpec
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyDerBytes);
            // восстанавливает объект PublicKey
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            log.error("Алгоритм Ed25519 не поддерживается: {}", e.getMessage(), e);
        } catch (NoSuchProviderException e) {
            log.error("Провайдер Bouncy Castle не найден: {}", e.getMessage(), e);
        } catch (InvalidKeySpecException e) {
            log.error("Неверная спецификация ключа: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка при загрузке ключа: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signatureBytes) {
        try {
            // объект для крипто операций
            Signature signature = Signature.getInstance("Ed25519", "BC"); // объект умеет
            // проверка подписи
            signature.initVerify(publicKey);
            // передаем оригинальные данные cache из CacheChallenge
            signature.update(data);
            // сравниваем предоставленную подпись с вычисленной и возвращаем ture/false
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Алгоритм Ed25519 не поддерживается: {}", e.getMessage(), e);
        } catch (NoSuchProviderException e) {
            log.error("Провайдер Bouncy Castle не найден: {}", e.getMessage(), e);
        } catch (InvalidKeyException e) {
            log.error("Недопустимый публичный ключ: {}", e.getMessage(), e);
        } catch (SignatureException e) {
            log.error("Ошибка при обработке подписи: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Непредвиденная ошибка: {}", e.getMessage(), e);
        }
        return false;
    }
}
