package wavesDRSN.p2p_messenger_backend.session;

import gRPC.v1.IceCandidatesMessage;
import gRPC.v1.SessionDescription;
import gRPC.v1.User;
import gRPC.v1.UserConnectionResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class UserSessionManager {
    private final ConcurrentMap<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init() {
        // Проверка неактивных сессий каждую минуту, сделано от балды
        scheduler.scheduleAtFixedRate(this::checkInactiveSessions, 1, 1, TimeUnit.MINUTES);
    }

    // Создание новой сессии
    public UserSession createSession(String username, StreamObserver<UserConnectionResponse> observer) {
        UserSession session = new UserSession(username, observer);
        sessions.put(username, session);
        broadcastUsersList();
        return session;
    }

    // Удаление сессии
    public void removeSession(String username) {
        sessions.remove(username);
        broadcastUsersList();
    }

    // Рассылка обновленного списка пользователей
    private void broadcastUsersList() {
        List<User> users = sessions.keySet().stream()
                .map(name -> User.newBuilder().setName(name).build())
                .collect(Collectors.toList());

        sessions.forEach((name, session) -> session.sendUsersList(users));
    }

    // Проверка активности
    private void checkInactiveSessions() {
        sessions.values().removeIf(session -> !session.isAlive());
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
