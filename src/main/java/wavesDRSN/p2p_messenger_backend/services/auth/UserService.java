package wavesDRSN.p2p_messenger_backend.services.auth;

import org.springframework.stereotype.Service;
import wavesDRSN.p2p_messenger_backend.auth.model.User;

import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public void registerUser(String nickname, PublicKey publicKey) {
        users.put(nickname, new User(nickname, publicKey));
    }

    public Optional<User> getUser(String nickname) {
        return Optional.ofNullable(users.get(nickname));
    }

    public boolean existsByNickname(String nickname) {
        return users.containsKey(nickname);
    }
}
