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
import java.util.stream.Collectors;

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
        // Attempt to gracefully close all active sessions
        sessions.values().forEach(UserSession::close);
    }

    public void createSession(String username, String userKey,
                            StreamObserver<UserConnectionResponse> observer) {
        if (username == null || username.trim().isEmpty() || userKey == null || userKey.trim().isEmpty()) {
            logger.warn("Attempted to create session with invalid username or userKey.");
            if (observer != null) {
                observer.onError(new IllegalArgumentException("Username and UserKey cannot be empty."));
            }
            return;
        }
        UserSession session = new UserSession(username, userKey, observer);
        sessions.put(username, session);
        keyToUserMap.put(userKey, username);
        broadcastUsersList();
        logger.info("Created session for {} with key {}", username, userKey);
    }

    // Удаление сессии
    public void removeSession(String username) {
        if (username == null) return;
        UserSession session = sessions.remove(username);
        if (session != null) {
            // Remove all keys associated with this username
            keyToUserMap.entrySet().removeIf(entry -> username.equals(entry.getValue()));
            try {
                session.close();
            } catch (Exception e) {
                logger.error("Error closing session for {}", username, e);
            }
            broadcastUsersList();
            logger.info("Removed session for {}", username);
        } else {
            logger.warn("Attempted to remove non-existent session for {}", username);
        }
    }

    public String getUsernameByKey(String key) {
        if (validateKey(key)) return keyToUserMap.get(key);
        return null;
    }

    public boolean validateKey(String userKey) {
        return userKey != null && keyToUserMap.containsKey(userKey);
    }

    public Optional<UserSession> getSession(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(sessions.get(username));
    }

    public Optional<UserSession> getSessionByKey(String userKey) {
        if (userKey == null) return Optional.empty();
        return Optional.ofNullable(keyToUserMap.get(userKey))
            .flatMap(username -> Optional.ofNullable(sessions.get(username)));
    }

    public void registerSdpObserver(String userId, StreamObserver<SDPExchange> observer) {
        if (userId == null || observer == null) {
            logger.warn("Attempt to register SDP observer with null userId or observer.");
            return;
        }
        logger.debug("Registering SDP observer for {}", userId);
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.setSdpObserver(observer);
            logger.info("SDP observer registered for {}", userId);
        } else {
            logger.warn("Attempt to register SDP observer for non-existent user session: {}", userId);
             // It's important that the client handles the case where registration might fail
            try {
                observer.onError(new IllegalStateException("User session not found, cannot register SDP observer."));
            } catch (Exception e) {
                logger.warn("Error sending onError to SDP observer for non-existent user {}: {}", userId, e.getMessage());
            }
        }
    }

    public void registerIceObserver(String userId, StreamObserver<ICEExchange> observer) {
        if (userId == null || observer == null) {
            logger.warn("Attempt to register ICE observer with null userId or observer.");
            return;
        }
        logger.debug("Registering ICE observer for {}", userId);
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.setIceObserver(observer);
            logger.info("ICE observer registered for {}", userId);
        } else {
            logger.warn("Attempt to register ICE observer for non-existent user session: {}", userId);
            try {
                observer.onError(new IllegalStateException("User session not found, cannot register ICE observer."));
            } catch (Exception e) {
                logger.warn("Error sending onError to ICE observer for non-existent user {}: {}", userId, e.getMessage());
            }
        }
    }

    public void removeSdpObserver(String userId) {
        if (userId == null) return;
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.setSdpObserver(null);
            logger.info("SDP observer removed for {}", userId);
        }
    }

    public void removeIceObserver(String userId) {
        if (userId == null) return;
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.setIceObserver(null);
            logger.info("ICE observer removed for {}", userId);
        }
    }

    public void broadcastUsersList() {
        UsersList usersList = UsersList.newBuilder()
            .addAllUsers(sessions.keySet().stream()
                .map(name -> User.newBuilder().setName(name).build())
                .collect(Collectors.toList())) // Use Collectors.toList() for compatibility
            .build();

        sessions.values().forEach(session -> {
            try {
                // Ensure session still has an active observer
                if (session.getObserver() != null) {
                    session.sendResponse(UserConnectionResponse.newBuilder()
                        .setUsersList(usersList)
                        .build());
                }
            } catch (Exception e) {
                // This can happen if the client disconnected abruptly
                logger.warn("Error broadcasting users list to {}: {}. Might be disconnected.", session.getUsername(), e.getMessage());
                // Optionally, consider cleaning up this session if sending fails repeatedly
            }
        });
    }

    public void updateLastActive(String username) {
        if (username == null) return;
        UserSession session = sessions.get(username);
        if (session != null) {
            session.updateLastActive();
        }
    }

    private void checkInactiveSessions() {
        logger.debug("Checking inactive sessions. Current session count: {}", sessions.size());
        Instant now = Instant.now();
        List<String> inactiveUsernames = new ArrayList<>();

        sessions.forEach((username, session) -> {
            if (Duration.between(session.getLastActive(), now).toMinutes() > 5) { // 5 minutes inactivity
                inactiveUsernames.add(username);
            }
        });

        if (!inactiveUsernames.isEmpty()) {
            inactiveUsernames.forEach(username -> {
                logger.info("Removing inactive session due to timeout: {}", username);
                removeSession(username); // This will also trigger close and broadcast
            });
            // broadcastUsersList() is called within removeSession, so not needed here again
        }
        logger.debug("Finished checking inactive sessions. Session count now: {}", sessions.size());
    }

    // These methods are not used in this iteration for direct SDP/ICE forwarding
    // as the new logic will be in SDPProcessor and IceCandidateHandler
    // public void forwardSDP(String receiver, SessionDescription sdp) {
    //     UserSession session = sessions.get(receiver);
    //     if (session != null) {
    //         session.sendSDP(sdp);
    //     }
    // }

    // public void forwardIceCandidates(String receiver, IceCandidatesMessage candidates) {
    //     UserSession session = sessions.get(receiver);
    //     if (session != null) {
    //         // session.sendIceCandidates(candidates); // Assuming this was the intent
    //     }
    // }
}
