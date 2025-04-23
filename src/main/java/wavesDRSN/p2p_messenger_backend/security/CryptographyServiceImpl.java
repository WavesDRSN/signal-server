package wavesDRSN.p2p_messenger_backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

@Slf4j
@Service
public class CryptographyServiceImpl implements CryptographyService {

    @Override
    public PublicKey parsePublicKey(byte[] publicKeyDerBytes) throws InvalidKeySpecException {
        try {
            // объект для восстановления ключа из байтов
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
            // превращает массив байт publicKeyDerBytes в объект X509EncodedKeySpec
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyDerBytes);
            // восстанавливает объект PublicKey
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            String errorMessage = "Критическая ошибка конфигурации криптографии: Провайдер BC или алгоритм Ed25519 недоступен.";
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        } catch (InvalidKeySpecException e) {
            // Если формат ключа неверен - пробрасываем стандартное исключение
            log.warn("Ошибка парсинга публичного ключа: неверный формат или данные. Length: {}", publicKeyDerBytes.length, e);
            throw e; // Пробрасываем дальше
        }
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
            // сравниваем предоставленную подпись с вычисленной и возвращаем true/false
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            // Ошибки конфигурации - критично
            String errorMessage = "Критическая ошибка конфигурации криптографии: Провайдер BC или алгоритм Ed25519 недоступен.";
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        } catch (InvalidKeyException e) {
            // Предоставленный PublicKey не подходит для проверки этим алгоритмом (маловероятно, если parsePublicKey отработал)
            log.error("Ошибка проверки подписи: Невалидный публичный ключ предоставлен для initVerify.", e);
            return false; // Считаем подпись неверной
        } catch (SignatureException e) {
            // Ошибка в процессе проверки: либо формат signatureBytes некорректен, либо подпись просто неверна математически
            log.warn("Ошибка проверки подписи: Неверный формат подписи или подпись не соответствует данным/ключу.", e);
            return false; // Считаем подпись неверной
        }
    }
}
