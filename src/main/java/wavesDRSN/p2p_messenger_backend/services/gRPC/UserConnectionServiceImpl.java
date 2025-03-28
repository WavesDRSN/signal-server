package wavesDRSN.p2p_messenger_backend.services.gRPC;

import com.google.protobuf.Duration;
import gRPC.v1.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import wavesDRSN.p2p_messenger_backend.session.UserSessionManager;
import wavesDRSN.p2p_messenger_backend.webrtc.IceCandidateHandler;
import wavesDRSN.p2p_messenger_backend.webrtc.SDPProcessor;

@GrpcService
public class UserConnectionServiceImpl extends UserConnectionGrpc.UserConnectionImplBase {

    private final UserSessionManager sessionManager;
    private final SDPProcessor sdpProcessor;
    private final IceCandidateHandler iceHandler;

    @Autowired
    public UserConnectionServiceImpl(UserSessionManager sessionManager,
                                    SDPProcessor sdpProcessor,
                                    IceCandidateHandler iceHandler) {
        this.sessionManager = sessionManager;
        this.sdpProcessor = sdpProcessor;
        this.iceHandler = iceHandler;
    }

    @Override
    public StreamObserver<UserConnectionRequest> loadUsersList(
            StreamObserver<UserConnectionResponse> responseObserver) {
        return new StreamObserver<>() {
            private String username;

            @Override
            public void onNext(UserConnectionRequest request) {
                if (request.hasInitialRequest()) {
                    handleInitialConnection(request.getInitialRequest());
                } else if (request.hasStillAlive()) {
                    handleKeepAlive();
                }
            }

            private void handleInitialConnection(InitialUserConnectionRequest initialRequest) {
                username = initialRequest.getName();
                sessionManager.createSession(username, responseObserver);

                // Исправленный блок построения ответа
                Duration keepAliveDuration = Duration.newBuilder()
                    .setSeconds(30)
                    .build();

                InitialUserConnectionResponse initialResponse = InitialUserConnectionResponse.newBuilder()
                    .setUserKeepAliveInterval(keepAliveDuration)
                    .build();

                UserConnectionResponse response = UserConnectionResponse.newBuilder()
                    .setInitialResponse(initialResponse)
                    .build();

                responseObserver.onNext(response);
                sessionManager.broadcastUsersList();
            }

            private void handleKeepAlive() {
                if (username != null) {
                    sessionManager.updateLastActive(username);
                }
            }

            @Override
            public void onError(Throwable t) {
                cleanup();
            }

            @Override
            public void onCompleted() {
                cleanup();
                responseObserver.onCompleted();
            }

            private void cleanup() {
                if (username != null) {
                    sessionManager.removeSession(username);
                }
            }
        };
    }

    @Override
    public void userDisconnect(DisconnectRequest request,
                              StreamObserver<DisconnectResponse> responseObserver) {
        sessionManager.removeSession(request.getName());
        responseObserver.onNext(DisconnectResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<SessionDescription> exchangeSDP(
            StreamObserver<SessionDescription> responseObserver) {
        return new StreamObserver<>() {
            private String userId;

            @Override
            public void onNext(SessionDescription sdp) {
                if (userId == null) {
                    userId = sdp.getSender();
                    sessionManager.registerSdpObserver(userId, responseObserver);
                }
                sdpProcessor.processSDP(sdp);
            }

            @Override
            public void onError(Throwable t) {
                cleanup();
            }

            @Override
            public void onCompleted() {
                cleanup();
                responseObserver.onCompleted();
            }

            private void cleanup() {
                if (userId != null) {
                    sessionManager.removeSdpObserver(userId);
                }
            }
        };
    }

    @Override
    public StreamObserver<IceCandidatesMessage> sendIceCandidates(
            StreamObserver<IceCandidatesMessage> responseObserver) {
        return new StreamObserver<>() {
            private String userId;

            @Override
            public void onNext(IceCandidatesMessage message) {
                if (userId == null) {
                    userId = message.getSender();
                    sessionManager.registerIceObserver(userId, responseObserver);
                }
                iceHandler.handleCandidates(message);
            }

            @Override
            public void onError(Throwable t) {
                cleanup();
            }

            @Override
            public void onCompleted() {
                cleanup();
                responseObserver.onCompleted();
            }

            private void cleanup() {
                if (userId != null) {
                    sessionManager.removeIceObserver(userId);
                }
            }
        };
    }
}
