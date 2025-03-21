package wavesDRSN.p2p_messenger_backend.services;

import org.springframework.stereotype.Service;
import wavesDRSN.p2p_messenger_backend.entities.UserEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public interface UserService {
    /**
     * Находит пользователя по уник имени юзера
     * @param username имя пользователя для поиска
     * @return optional, содержащий найденного пользователя или пустой,
     */
    Optional<UserEntity> getUserByUsername(String username);

    /**
     * Находит пользователя по айди
     * @param id айди пользователя
     * @return  optional содержащий найденного пользователя или пустой
     */
    Optional<UserEntity> getUserById(UUID id);

    /**
     * Возвращает всех пользователей
     * @return список всех пользователей
     */
    List<UserEntity> getAllUsers();

    /**
     * Регистрирует пользователя
     * @param username уникальное имя пользователя
     * @param password пароль пользователя
     * @param publicName публичное имя пользователя
     * @return новый объект UserEntity
     */
    UserEntity registerUser(String username, String password, String publicName);

    /**
     * Обновляет профиль
     * @param id UUID пользователя
     * @param newPublicName новое публичное имя пользователя
     * @param newBio новое описание пользователя
     * @return обновленный объект UserEntity
     */
    UserEntity updateProfile(UUID id, String newPublicName, String newBio);

    /**
     * Меняет пароль
     * @param id UUID пользователя
     * @param oldPassword старый пароль
     * @param newPassword новый пароль, на который пользователь меняет старый
     */
    void changePassword(UUID id, String oldPassword, String newPassword);

    /**
    * Удаляет пользователя
    * @param id UUID пользователя
    */
    void deleteUser(UUID id);
}
