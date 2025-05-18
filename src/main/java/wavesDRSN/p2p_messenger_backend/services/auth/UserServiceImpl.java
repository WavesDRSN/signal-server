package wavesDRSN.p2p_messenger_backend.services.auth;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wavesDRSN.p2p_messenger_backend.dto.UserDTO;
import wavesDRSN.p2p_messenger_backend.entities.UserEntity;
import wavesDRSN.p2p_messenger_backend.exceptions.UsernameAlreadyExistsException;
import wavesDRSN.p2p_messenger_backend.repositories.UserRepository;
import wavesDRSN.p2p_messenger_backend.utils.UserMapper;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public boolean updateFcmToken(String userId, String fcmToken) {
        try {
            UserEntity user = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            user.setFcmToken(fcmToken);
            userRepository.save(user);
            log.info("Updated FCM token for user: {}", userId);
            return true;

        } catch (EntityNotFoundException ex) {
            log.warn("Attempt to update token for non-existent user: {}", userId);
            return false;
        } catch (Exception e) {
            log.error("Error updating FCM token for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public UserDTO registerUser(String username, byte[] publicKey) {
        log.info("Попытка зарегистрировать пользователя: {}", username);
        if (userRepository.existsByUsername(username)) {
            log.info("Ошибка регистрации: Имя пользователя {} уже существует.", username);
            throw new UsernameAlreadyExistsException("Имя пользователя '" + username + "' уже занято.");
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(username);
        newUser.setPublicKey(publicKey);

        UserEntity savedUser = userRepository.save(newUser);

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserByUsername(String username) {
        Optional<UserEntity> user = userRepository.findByUsername(username);
        return user.map(userMapper::toDto);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
