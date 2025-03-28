package wavesDRSN.p2p_messenger_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import wavesDRSN.p2p_messenger_backend.entities.UserEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username); // поиск по уникальному имени юзера

    Optional<UserEntity> findById(UUID id);

    List<UserEntity> findByPublicName(String publicName); // поиск по публ имени
}
