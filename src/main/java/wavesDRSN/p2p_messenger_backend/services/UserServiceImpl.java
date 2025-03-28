package wavesDRSN.p2p_messenger_backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wavesDRSN.p2p_messenger_backend.entities.UserEntity;
import wavesDRSN.p2p_messenger_backend.repositories.UserRepository;

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
    public UserEntity updateProfile(UUID id, String newPublicName, String newBio) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setPublicName(newPublicName);
        user.setBio(newBio);

        return userRepository.save(user);
    }

    @Override
    public List<UserEntity> getByPublicName(String publicName) {
        return userRepository.findByPublicName(publicName);
    }

}
