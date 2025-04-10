package wavesDRSN.p2p_messenger_backend.services;

import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

@Service
public class CryptographyServiceImpl implements CryptographyService {

    @Override
    public PublicKey parsePublicKey(byte[] publicKeyDerBytes) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC"); // создает ключ из байт
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyDerBytes); // для распознавания ключа
            return keyFactory.generatePublic(keySpec); // возвращает готовый PublicKey
        }
        catch (NoSuchAlgorithmException e) {
            System.err.println("Алгоритм не поддерживается" + e.getMessage());
        }
        catch (NoSuchProviderException e) {
            System.err.println("Провайдер не найден" + e.getMessage());
        }
        catch (InvalidKeySpecException e) {
            System.err.println("Неверная спецификация ключа" + e.getMessage());
        }
        catch (Exception e) {
            System.err.println("Произошла непредвиденная ошибка при загрузке ключа" + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signatureBytes) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        try {
            Signature signature = Signature.getInstance("Ed25519", "BC");
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signatureBytes);
        }
        catch (NoSuchAlgorithmException e) {
            System.err.println("Алгоритм не поддерживается" + e.getMessage());
        }
        catch (NoSuchProviderException e) {
            System.err.println("Провайдер не найден" + e.getMessage());
        }
        catch (InvalidKeyException e) {
            System.err.println("Недопустимый публичный ключ" + e.getMessage());
        }
        catch (SignatureException e) {
            System.err.println("Ошибка при обработке подписи" + e.getMessage());
        }
        catch (Exception e) {
            System.err.println("Непредвиденная ошибка" + e.getMessage());
        }
        return false;
    }
}
