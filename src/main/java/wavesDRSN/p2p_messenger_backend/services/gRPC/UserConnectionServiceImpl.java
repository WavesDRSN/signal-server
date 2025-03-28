package wavesDRSN.p2p_messenger_backend.services.gRPC;

import gRPC.v1.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import wavesDRSN.p2p_messenger_backend.session.UserSession;
import wavesDRSN.p2p_messenger_backend.session.UserSessionManager;

@GrpcService
public class UserConnectionServiceImpl extends UserConnectionGrpc.UserConnectionImplBase {
    private final UserSessionManager sessionManager;

    @Autowired
    public UserConnectionServiceImpl(UserSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    // Двусторонний потоковый метод для списка пользователей
    @Override
    public StreamObserver<UserConnectionRequest> loadUsersList(
            StreamObserver<UserConnectionResponse> responseObserver) {
        return new StreamObserver<>() {
            private UserSession session;

            @Override
            public void onNext(UserConnectionRequest request) {
                if (request.hasInitialRequest()) {
                    // Первичное подключение
                    InitialUserConnectionRequest initial = request.getInitialRequest();
                    session = sessionManager.createSession(initial.getName(), responseObserver);
                } else if (request.hasStillAlive()) {
                    // Heartbeat
                    session.updateLastActive();
                }
            }

            @Override
            public void onError(Throwable t) {
                sessionManager.removeSession(session.getUsername());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
                sessionManager.removeSession(session.getUsername());
            }
        };
    }

    // Отправка ICE-кандидатов (двусторонний поток)
    @Override
    public StreamObserver<IceCandidatesMessage> sendIceCandidates(
            StreamObserver<IceCandidatesMessage> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(IceCandidatesMessage message) {
                // Пересылка кандидатов получателю
                sessionManager.forwardIceCandidates(message.getReceiver(), message);
                responseObserver.onNext(message); // Эхо-ответ (или обработка)
            }

            @Override
            public void onError(Throwable t) {
                // Логирование ошибки
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    // Обмен SDP
    @Override
    public void exchangeSDP(SessionDescription request,
                            StreamObserver<SessionDescription> responseObserver) {
        sessionManager.forwardSDP(request.getReceiver(), request);
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }

    // Отключение пользователя
    @Override
    public void userDisconnect(DisconnectRequest request,
                               StreamObserver<DisconnectResponse> responseObserver) {
        sessionManager.removeSession(request.getName());
        responseObserver.onNext(DisconnectResponse.newBuilder()
                .setText("User " + request.getName() + " disconnected").build());
        responseObserver.onCompleted();
    }
}
