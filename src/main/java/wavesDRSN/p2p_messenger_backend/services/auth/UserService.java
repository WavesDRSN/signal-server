package wavesDRSN.p2p_messenger_backend.services.auth;

import org.springframework.stereotype.Service;
import wavesDRSN.p2p_messenger_backend.dto.UserDTO;
import wavesDRSN.p2p_messenger_backend.exceptions.UsernameAlreadyExistsException;

import java.util.Optional;

@Service
public interface UserService {
    UserDTO registerUser(String username, byte[] publicKey) throws UsernameAlreadyExistsException;

    Optional<UserDTO> getUserByUsername(String username);

    boolean existsByUsername(String username);

    boolean updateFcmToken(String userId, String fcmToken);
}
