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
     * Обновляет профиль
     * @param id UUID пользователя
     * @param newPublicName новое публичное имя пользователя
     * @param newBio новое описание пользователя
     * @return обновленный объект UserEntity
     */
    UserEntity updateProfile(UUID id, String newPublicName, String newBio);

    /**
     * Поиск пользователей по публичному имени
     * @param publicName публичное имя
     * @return лист пользователей с таким именем или пусто
     */
    List<UserEntity> getByPublicName(String publicName);
}
