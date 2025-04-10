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
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC"); // создает ключ из байт
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyDerBytes); // для распознавания ключа
            return keyFactory.generatePublic(keySpec); // возвращает готовый PublicKey
        } catch (NoSuchAlgorithmException e) {
            log.error("Алгоритм не поддерживается: {}", e.getMessage());
        } catch (NoSuchProviderException e) {
            log.error("Провайдер не найден: {}", e.getMessage());
        } catch (InvalidKeySpecException e) {
            log.error("Неверная спецификация ключа: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка при загрузке ключа: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance("Ed25519", "BC");
            signature.initVerify(publicKey);
            signature.update(data);
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
