package wavesDRSN.p2p_messenger_backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {
    // порог времени неактивности
    private static final long TIMEOUT_MILLIS = 60000;

    // хранение инфы о соединениях, ключ - имя юзера, значение - сессия соединения
    private final Map<String, OnlineUserSession> onlineUsers = new HashMap<>();

    @Override
    public void registerOnlineUser(String username, Object connection) {
        OnlineUserSession session = new OnlineUserSession(connection, System.currentTimeMillis());
        onlineUsers.put(username, session);
    }

    @Override
    public Optional<Object> getConnection(String username) {
        return Optional.ofNullable(onlineUsers.get(username))
                .map(session -> session.connection);
    }

    @Override
    public void removeOnlineUser(String username) {
        onlineUsers.remove(username);
    }

    @Override
    public void refreshKeepAlive(String username) {
        OnlineUserSession session = onlineUsers.get(username);
        if (session != null) {
            session.lastKeepAlive = System.currentTimeMillis();
        }
    }

    /**
     * С помощью Scheduled Spring автоматически запускает проверку онлайн-сессии
     * и удаляет те, для которых не получен keepAlive в течение TIMEOUT_MILLIS
     */
    @Scheduled(fixedDelay = 30000)
    public void removeStaleSessions() {
        long now = System.currentTimeMillis();
        onlineUsers.entrySet().removeIf(entry -> now - entry.getValue().lastKeepAlive > TIMEOUT_MILLIS);
    }

    /**
     * Класс для хранения объекта соединения и времени последнего keepAlive
     */
    private static class OnlineUserSession {
        private final Object connection;
        private volatile long lastKeepAlive;

        public OnlineUserSession(Object connection, long lastKeepAlive) {
            this.connection = connection;
            this.lastKeepAlive = lastKeepAlive;
        }
    }
}
