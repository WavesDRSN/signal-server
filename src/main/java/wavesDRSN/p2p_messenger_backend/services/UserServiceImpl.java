package wavesDRSN.p2p_messenger_backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wavesDRSN.p2p_messenger_backend.entities.UserEntity;
import wavesDRSN.p2p_messenger_backend.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Optional<UserEntity> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<UserEntity> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserEntity registerUser(String username, String password, String publicName) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        UserEntity user = UserEntity.builder()
                .username(username)
                .password(password)
                .publicName(publicName)
                .bio("")
                .isOnline(false)
                .createdAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    @Override
    public UserEntity updateProfile(UUID id, String newPublicName, String newBio) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setPublicName(newPublicName);
        user.setBio(newBio);

        // user.setLastActiveAt(LocalDateTime.now()); - обновление активности
        return userRepository.save(user);
    }

    @Override
    public void changePassword(UUID id, String oldPassword, String newPassword) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (!user.getPassword().equals(oldPassword)) { // НЕОБХОДИМО изменить на ?хэш?
            throw new IllegalArgumentException("Указан неверный пароль");
        }

        user.setPassword(newPassword);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Пользователь не найден.");
        }
        userRepository.deleteById(id);
    }
}
