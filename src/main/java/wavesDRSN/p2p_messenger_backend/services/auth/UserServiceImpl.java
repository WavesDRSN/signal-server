package wavesDRSN.p2p_messenger_backend.services.auth;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public boolean updateFcmToken(String userId, String fcmToken) {
        Long userEntityId;
        try {
            userEntityId = Long.valueOf(userId);
        } catch (NumberFormatException e) {
            log.warn("Invalid userId format passed to updateFcmToken: {}. Must be a Long.", userId);
            return false;
        }

        log.info("Attempting to update FCM token for user ID: {}", userEntityId);
        try {
            UserEntity user = userRepository.findById(userEntityId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userEntityId));

            if (fcmToken.equals(user.getFcmToken())) {
                log.info("FCM token for user ID: {} is already up to date. No changes made.", userEntityId);
                return true;
            }

            // Проверка, не занят ли этот токен уже другим пользователем
            Optional<UserEntity> existingUserWithToken = userRepository.findByFcmToken(fcmToken);
            if (existingUserWithToken.isPresent() && !existingUserWithToken.get().getId().equals(userEntityId)) {
                log.warn("Attempt to assign FCM token '{}' to user ID {} which is already in use by user ID {}",
                         fcmToken.substring(Math.max(0, fcmToken.length()-5)), userEntityId, existingUserWithToken.get().getId());
                // Если токен занят, обновление не удастся из-за unique constraint.
            }

            user.setFcmToken(fcmToken);
            userRepository.save(user);
            log.info("Successfully updated FCM token for user ID: {}", userEntityId);
            return true;

        } catch (EntityNotFoundException ex) {
            log.warn("Attempt to update FCM token for non-existent user ID: {}. Message: {}", userEntityId, ex.getMessage());
            return false;
        } catch (DataIntegrityViolationException e) {
            // Это сработает, если unique constraint на fcm_token в базе данных не даст сохранить дубликат
            String tokenSuffix = fcmToken.length() > 5 ? fcmToken.substring(fcmToken.length() - 5) : fcmToken;
            log.error("Error updating FCM token for user ID {}: Data integrity violation (FCM token ending with ...{} likely already exists for another user). Message: {}",
                      userEntityId, tokenSuffix, e.getMessage());
            return false;
        }
        catch (Exception e) {
            log.error("Unexpected error updating FCM token for user ID {}: {}", userEntityId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public UserDTO registerUser(String username, byte[] publicKey) {
        log.info("Attempting to register user with username: '{}'", username);
        if (userRepository.existsByUsername(username)) {
            log.warn("Registration failed: Username '{}' already exists.", username);
            throw new UsernameAlreadyExistsException("Username '" + username + "' is already taken.");
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(username);
        newUser.setPublicKey(publicKey);

        UserEntity savedUser = userRepository.save(newUser);
        log.info("User '{}' (ID: {}) registered successfully.", savedUser.getUsername(), savedUser.getId());
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserByUsername(String username) {
        log.debug("Fetching user by username: '{}'", username);
        Optional<UserEntity> userEntity = userRepository.findByUsername(username);
        if (userEntity.isEmpty()) {
            log.debug("User not found with username: '{}'", username);
        }
        return userEntity.map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserById(String userId) {
        Long userEntityId;
        try {
            userEntityId = Long.valueOf(userId);
        } catch (NumberFormatException e) {
            log.warn("Invalid userId format requested: {}", userId);
            return Optional.empty();
        }
        log.debug("Fetching user by ID: '{}'", userEntityId);
        Optional<UserEntity> userEntity = userRepository.findById(userEntityId);
        if (userEntity.isEmpty()) {
            log.debug("User not found with ID: '{}'", userEntityId);
        }
        return userEntity.map(userMapper::toDto);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        log.debug("Checking existence of username: '{}'", username);
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional
    public void removeFcmToken(String fcmToken) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            log.warn("Attempt to remove a null or empty FCM token.");
            return;
        }
        String tokenSuffix = fcmToken.length() > 5 ? fcmToken.substring(fcmToken.length() - 5) : fcmToken;
        log.info("Attempting to find and remove FCM token ending with: ...{}", tokenSuffix);

        // Используем findByFcmToken, который мы добавили в UserRepository
        Optional<UserEntity> userOpt = userRepository.findByFcmToken(fcmToken);

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            log.info("Found user ID {} (username: '{}') associated with FCM token ending ...{}. Setting token to null.",
                     user.getId(), user.getUsername(), tokenSuffix);
            user.setFcmToken(null); // Устанавливаем токен в null
            userRepository.save(user); // Сохраняем изменения
            log.info("Successfully removed (nulled) FCM token for user ID {} (username: '{}').", user.getId(), user.getUsername());
        } else {
            log.info("No user found with FCM token ending ...{}. No action taken.", tokenSuffix);
        }
    }

    @Override
    @Transactional
    public String getFcmTokenByUsername(String username){
        if (username == null || username.trim().isEmpty()) {
            log.warn("Attempt to get FCM token with null or empty userId");
            return null;
        }


        log.debug("Attempting to get FCM token for username: {}", username);

        try {
            Optional<UserEntity> userEntity = userRepository.findByUsername(username);

            if (userEntity.isEmpty()) {
                log.debug("User not found with username: {}. Cannot retrieve FCM token.", username);
                return null;
            }

            UserEntity user = userEntity.get();
            String fcmToken = user.getFcmToken();

            if (fcmToken == null || fcmToken.trim().isEmpty()) {
                log.debug("User {} has no FCM token registered.",
                        username);
                return null;
            }

            String tokenSuffix = fcmToken.length() > 5 ? fcmToken.substring(fcmToken.length() - 5) : fcmToken;
            log.debug("Successfully retrieved FCM token for user {}. Token ends with: ...{}",
                    username, tokenSuffix);

            return fcmToken;

        } catch (Exception e) {
            log.error("Unexpected error retrieving FCM token for user {}: {}", username, e.getMessage(), e);
            return null;
        }
    }
}
