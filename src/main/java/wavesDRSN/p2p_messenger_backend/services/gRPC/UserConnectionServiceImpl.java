package wavesDRSN.p2p_messenger_backend.services.gRPC;

import com.google.protobuf.Duration;
import gRPC.v1.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import wavesDRSN.p2p_messenger_backend.session.UserSessionManager;
import wavesDRSN.p2p_messenger_backend.webrtc.IceCandidateHandler;
import wavesDRSN.p2p_messenger_backend.webrtc.SDPProcessor;

@GrpcService
public class UserConnectionServiceImpl extends UserConnectionGrpc.UserConnectionImplBase {

    private static final Logger logger = LoggerFactory.getLogger(UserConnectionServiceImpl.class);
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
                try {
                    logger.debug("Received SDP from {} to {} (type: {})",
                        sdp.getSender(), sdp.getReceiver(), sdp.getType());

                    if (userId == null) {
                        userId = sdp.getSender();
                        sessionManager.registerSdpObserver(userId, responseObserver);
                        logger.info("Registered SDP observer for user: {}", userId);
                    }

                    logger.debug("Processing SDP for {}", sdp.getReceiver());
                    sdpProcessor.processSDP(sdp);
                    logger.debug("SDP processed successfully");

                } catch (Exception e) {
                    logger.error("Error processing SDP from {}: {}", sdp.getSender(), e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("SDP stream error for {}: {}", userId, t.getMessage(), t);
                cleanup();
            }

            @Override
            public void onCompleted() {
                logger.info("SDP stream completed for {}", userId);
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
                try {
                    logger.debug("Received {} ICE candidates from {} to {}",
                        message.getCandidatesCount(), message.getSender(), message.getReceiver());

                    if (userId == null) {
                        userId = message.getSender();
                        sessionManager.registerIceObserver(userId, responseObserver);
                        logger.info("Registered ICE observer for user: {}", userId);
                    }

                    logger.debug("Forwarding ICE candidates to {}", message.getReceiver());
                    iceHandler.handleCandidates(message);
                    logger.debug("ICE candidates forwarded successfully");

                } catch (Exception e) {
                    logger.error("Error processing ICE candidates from {}: {}",
                        message.getSender(), e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("ICE stream error for {}: {}", userId, t.getMessage(), t);
                cleanup();
            }

            @Override
            public void onCompleted() {
                logger.info("ICE stream completed for {}", userId);
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
