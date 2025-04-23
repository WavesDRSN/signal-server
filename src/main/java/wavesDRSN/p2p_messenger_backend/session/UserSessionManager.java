package wavesDRSN.p2p_messenger_backend.session;

import gRPC.v1.Signaling.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Component
public class UserSessionManager {
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> keyToUserMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Logger logger = LoggerFactory.getLogger(UserSessionManager.class);


    @PostConstruct
    public void init() {
        // Проверка неактивных сессий каждую минуту, сделано от балды
        scheduler.scheduleAtFixedRate(this::checkInactiveSessions, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

    public void createSession(String username, String userKey,
                            StreamObserver<UserConnectionResponse> observer) {
        UserSession session = new UserSession(username, userKey, observer);
        sessions.put(username, session);
        keyToUserMap.put(userKey, username);
        broadcastUsersList();
        logger.info("Created session for {} with key {}", username, userKey);
    }

    // Удаление сессии
    public void removeSession(String username) {
        UserSession session = sessions.remove(username);
        if (session != null) {
            keyToUserMap.values().removeIf(v -> v.equals(username));
            session.close();
            broadcastUsersList();
            logger.info("Removed session for {}", username);
        }
    }

    public String getUsernameByKey(String key) {
        if (validateKey(key)) return keyToUserMap.get(key);
        return null;
    }

    public boolean validateKey(String userKey) {
        return keyToUserMap.containsKey(userKey);
    }

    public Optional<UserSession> getSession(String username) {
        return Optional.ofNullable(sessions.get(username));
    }

    public Optional<UserSession> getSessionByKey(String userKey) {
        return Optional.ofNullable(keyToUserMap.get(userKey))
            .map(sessions::get);
    }

    public void registerSdpObserver(String userId, StreamObserver<SDPExchange> observer) {
        logger.debug("Registering SDP observer for {}", userId);
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.setSdpObserver(observer);
            logger.info("SDP observer registered for {}", userId);
        } else {
            logger.warn("Attempt to register SDP observer for non-existent user: {}", userId);
        }
    }

    public void registerIceObserver(String userId, StreamObserver<ICEExchange> observer) {
        logger.debug("Registering ICE observer for {}", userId);
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.setIceObserver(observer);
            logger.info("ICE observer registered for {}", userId);
        } else {
            logger.warn("Attempt to register ICE observer for non-existent user: {}", userId);
        }
    }

    public void removeSdpObserver(String userId) {
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.setSdpObserver(null);
        }
    }

    public void removeIceObserver(String userId) {
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.setIceObserver(null);
        }
    }

    public void broadcastUsersList() {
        UsersList usersList = UsersList.newBuilder()
            .addAllUsers(sessions.keySet().stream()
                .map(name -> User.newBuilder().setName(name).build())
                .toList())
            .build();

        sessions.values().forEach(session -> {
            try {
                session.sendResponse(UserConnectionResponse.newBuilder()
                    .setUsersList(usersList)
                    .build());
            } catch (Exception e) {
                logger.error("Error broadcasting to {}: {}", session.getUsername(), e.getMessage());
            }
        });
    }

    public void updateLastActive(String username) {
        UserSession session = sessions.get(username);
        if (session != null) {
            session.updateLastActive();
        }
    }

    private void checkInactiveSessions() {
        logger.debug("Checking inactive sessions");
        Instant now = Instant.now();
        sessions.values().removeIf(session -> {
            boolean isInactive = Duration.between(session.getLastActive(), now).toMinutes() > 5;
            if (isInactive) {
                logger.info("Removing inactive session: {}", session.getUsername());
                keyToUserMap.values().removeIf(v -> v.equals(session.getUsername()));
            }
            return isInactive;
        });
        broadcastUsersList();
    }

    // Отправка SDP получателю
    public void forwardSDP(String receiver, SessionDescription sdp) {
        UserSession session = sessions.get(receiver);
        if (session != null) {
            session.sendSDP(sdp);
        }
    }

    // Обработка ICE-кандидатов
    public void forwardIceCandidates(String receiver, IceCandidatesMessage candidates) {
        UserSession session = sessions.get(receiver);
        if (session != null) {
            //TODO Логика отправки кандидатов через StreamObserver
        }
    }
}
