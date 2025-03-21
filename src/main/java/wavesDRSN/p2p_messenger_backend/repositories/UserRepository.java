package wavesDRSN.p2p_messenger_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import wavesDRSN.p2p_messenger_backend.entities.UserEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username); // поиск по уникальному имени юзера

    List<UserEntity> findByPublicName(String publicName); // поиск по публ имени

    List<UserEntity> findByIsOnline(boolean isOnline); // поиск пользователей по статусу

    List<UserEntity> findByCreatedAtAfter(LocalDateTime date); // поиск пользователей, зарегистрированных после указанной даты

    List<UserEntity> findByLastActiveAtBefore(LocalDateTime date); // {опционально} поиск юзеров, которые не заходили в сеть после указаной даты
    // например пользователь неактивен 2 недели


}
