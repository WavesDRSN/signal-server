package wavesDRSN.p2p_messenger_backend.services.auth;

import wavesDRSN.p2p_messenger_backend.dto.UserDTO;
import wavesDRSN.p2p_messenger_backend.exceptions.UsernameAlreadyExistsException;

import java.util.Optional;

public interface UserService {

    /**
     * Регистрирует нового пользователя в системе.
     *
     * @param username Желаемое имя пользователя.
     * @param publicKey Публичный ключ пользователя в виде массива байт.
     * @return UserDTO с данными зарегистрированного пользователя.
     * @throws UsernameAlreadyExistsException если имя пользователя уже занято.
     */
    UserDTO registerUser(String username, byte[] publicKey) throws UsernameAlreadyExistsException;

    /**
     * Ищет пользователя по его имени.
     *
     * @param username Имя пользователя.
     * @return Optional с UserDTO, если пользователь найден, иначе Optional.empty().
     */
    Optional<UserDTO> getUserByUsername(String username);

    /**
     * Ищет пользователя по его уникальному идентификатору (ID).
     *
     * @param userId Уникальный идентификатор пользователя (в виде строки).
     * @return Optional с UserDTO, если пользователь найден, иначе Optional.empty().
     */
    Optional<UserDTO> getUserById(String userId); // <-- ДОБАВЛЕН

    /**
     * Проверяет, существует ли пользователь с указанным именем.
     *
     * @param username Имя пользователя для проверки.
     * @return true, если пользователь существует, иначе false.
     */
    boolean existsByUsername(String username);

    /**
     * Обновляет или устанавливает FCM токен для указанного пользователя.
     *
     * @param userId Уникальный идентификатор пользователя (в виде строки).
     * @param fcmToken Новый FCM токен.
     * @return true, если токен был успешно обновлен, иначе false.
     */
    boolean updateFcmToken(String userId, String fcmToken);

    /**
     * Удаляет (очищает) указанный FCM токен у пользователя, которому он принадлежит.
     * Используется для удаления невалидных токенов.
     *
     * @param fcmToken FCM токен, который нужно удалить.
     */
    void removeFcmToken(String fcmToken);

    String getFcmTokenByUsername(String username);
}
