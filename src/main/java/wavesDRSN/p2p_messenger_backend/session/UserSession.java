package wavesDRSN.p2p_messenger_backend.session;

import gRPC.v1.SessionDescription;
import gRPC.v1.User;
import gRPC.v1.UserConnectionResponse;
import gRPC.v1.UsersList;
import io.grpc.stub.StreamObserver;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class UserSession {
    @Getter
    private final String username;
    private final StreamObserver<UserConnectionResponse> responseObserver;
    private volatile Instant lastActive;
    private Duration keepAliveInterval; // TODO не забыть про настройку интервала

    public UserSession(String username, StreamObserver<UserConnectionResponse> observer) {
        this.username = username;
        this.responseObserver = observer;
        this.lastActive = Instant.now();
    }

    // Отправка списка пользователей
    public void sendUsersList(List<User> users) {
        UsersList list = UsersList.newBuilder().addAllUsers(users).build();
        UserConnectionResponse response = UserConnectionResponse.newBuilder()
                .setUsersList(list)
                .build();
        responseObserver.onNext(response);
    }

    // Отправка SDP предложения/ответа
    public void sendSDP(SessionDescription sdp) {
        UserConnectionResponse response = UserConnectionResponse.newBuilder()
                .setSessionDescription(sdp)
                .build();
        responseObserver.onNext(response);
    }

    // Обновление времени последней активности
    public void updateLastActive() {
        this.lastActive = Instant.now();
    }

    // Проверка активности
    public boolean isAlive() {
        return Duration.between(lastActive, Instant.now()).compareTo(keepAliveInterval) < 0;
    }
}
