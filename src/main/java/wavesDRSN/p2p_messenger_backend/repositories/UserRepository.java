package wavesDRSN.p2p_messenger_backend.repositories;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<SecurityProperties.User, UUID> {

}
