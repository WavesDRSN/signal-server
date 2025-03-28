package wavesDRSN.p2p_messenger_backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import wavesDRSN.p2p_messenger_backend.utils.OnlineUserSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {
    // порог времени неактивности
    @Value("${app.session.timeout-millis}")
    private long timeoutMillis;

    // хранение инфы о соединениях, ключ - имя юзера, значение - сессия соединения
    private final Map<String, OnlineUserSession> onlineUsers = new HashMap<>();

    @Override
    public void registerOnlineUser(String username, Object connection) {
        OnlineUserSession session = new OnlineUserSession(connection, System.currentTimeMillis());
        onlineUsers.put(username, session);
        // TODO:
    }

    @Override
    public Optional<Object> getConnection(String username) {
        return Optional.ofNullable(onlineUsers.get(username))
                .map(OnlineUserSession::getConnection);
    }

    @Override
    public void removeOnlineUser(String username) {
        onlineUsers.remove(username);
    }

    @Override
    public void refreshKeepAlive(String username) {
        OnlineUserSession session = onlineUsers.get(username);
        if (session != null) {
            session.setLastKeepAlive(System.currentTimeMillis());
        }
        else {
            onlineUsers.remove(username);
        }
    }

    /**
     * С помощью Scheduled Spring автоматически запускает проверку онлайн-сессии
     * и удаляет те, для которых не получен keepAlive в течение TIMEOUT_MILLIS
     */
    @Scheduled(fixedDelayString = "#{${app.session.timeout-millis} / 2}") // 30 сек
    public void removeStaleSessions() {
        long now = System.currentTimeMillis();
        onlineUsers.entrySet().removeIf(entry -> now - entry.getValue().getLastKeepAlive() > timeoutMillis);
    }


    /* Для тестов раскомить

    // В классе OnlineUserServiceImpl:
    public Map<String, OnlineUserSession> getOnlineUsers() {
        return onlineUsers;
    }

    public OnlineUserSession getSession(String username) {
        return onlineUsers.get(username);
    }

    */
}
