package wavesDRSN.p2p_messenger_backend.services.gRPC;

import com.google.protobuf.Duration;
import gRPC.v1.Signaling.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import wavesDRSN.p2p_messenger_backend.session.UserSessionManager;
import wavesDRSN.p2p_messenger_backend.webrtc.IceCandidateHandler;
import wavesDRSN.p2p_messenger_backend.webrtc.SDPProcessor;

import java.util.UUID;

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
        return new StreamObserver<UserConnectionRequest>() {
            private String username;
            private String userKey;

            @Override
            public void onNext(UserConnectionRequest request) {
                try {
                    if (request.hasInitialRequest()) {
                        handleInitialConnection(request.getInitialRequest());
                    } else if (request.hasStillAlive()) {
                        handleKeepAlive();
                    }
                } catch (Exception e) {
                    logger.error("Error processing UserConnectionRequest: {}", e.getMessage(), e);
                }
            }

            private void handleInitialConnection(InitialUserConnectionRequest initialRequest) {
                username = initialRequest.getName();
                userKey = UUID.randomUUID().toString();
                sessionManager.createSession(username, userKey, responseObserver);

                // Исправленный блок построения ответа
                Duration keepAliveDuration = Duration.newBuilder()
                    .setSeconds(30)
                    .build();

                InitialUserConnectionResponse initialResponse = InitialUserConnectionResponse.newBuilder()
                    .setUserKeepAliveInterval(keepAliveDuration)
                    .setUserKey(userKey)
                    .build();

                UserConnectionResponse response = UserConnectionResponse.newBuilder()
                    .setInitialResponse(initialResponse)
                    .build();

                responseObserver.onNext(response);
                sessionManager.broadcastUsersList();
                logger.info("New user session created: {} with key: {}", username, userKey);
            }

            private void handleKeepAlive() {
                if (username != null) {
                    sessionManager.updateLastActive(username);
                    logger.debug("Keep-alive received from {}", username);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("User connection stream error: {}", t.getMessage());
                cleanup();
            }

            @Override
            public void onCompleted() {
                logger.info("User connection stream completed");
                cleanup();
                responseObserver.onCompleted();
            }

            private void cleanup() {
                if (username != null) {
                    sessionManager.removeSession(username);
                    logger.info("Session cleaned up for {}", username);
                }
            }
        };
    }

    @Override
    public void userDisconnect(DisconnectRequest request,
                              StreamObserver<DisconnectResponse> responseObserver) {
        try {
            sessionManager.removeSession(request.getName());
            responseObserver.onNext(DisconnectResponse.newBuilder()
                .setText("Disconnected: " + request.getName())
                .build());
            responseObserver.onCompleted();
            logger.info("User disconnected: {}", request.getName());
        } catch (Exception e) {
            logger.error("Disconnect error: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public StreamObserver<SDPExchange> exchangeSDP(
            StreamObserver<SDPExchange> responseObserver) {
        return new StreamObserver<SDPExchange>() {
            private String userKey;
            private String userName;
            private boolean authenticated = false;

            @Override
            public void onNext(SDPExchange exchange) {
                try {
                    if (exchange.hasInitialRequest()) {
                        handleInitialRequest(exchange.getInitialRequest());
                    } else if (exchange.hasSessionDescription()) {
                        handleSessionDescription(exchange.getSessionDescription());
                    }
                } catch (Exception e) {
                    logger.error("SDP exchange error: {}", e.getMessage(), e);
                }
            }

            private void handleInitialRequest(SDPStreamInitialRequest request) {
                if (sessionManager.validateKey(request.getKey())) {
                    userKey = request.getKey();
                    authenticated = true;
                    responseObserver.onNext(SDPExchange.newBuilder()
                        .setInitialResponse(SDPStreamInitialResponse.newBuilder()
                            .setApproved(true)
                            .build())
                        .build());
                    sessionManager.registerSdpObserver(sessionManager.getUsernameByKey(userKey), responseObserver);
                    logger.info("SDP stream authenticated for key: {}", userKey);
                } else {
                    responseObserver.onNext(SDPExchange.newBuilder()
                        .setInitialResponse(SDPStreamInitialResponse.newBuilder()
                            .setApproved(false)
                            .build())
                        .build());
                    logger.warn("Invalid SDP stream key: {}", request.getKey());
                }
            }

            private void handleSessionDescription(SessionDescription sdp) {
                if (authenticated) {
                    sdpProcessor.processSDP(userKey, sdp);
                    logger.debug("Processed SDP from {}", userKey);
                } else {
                    logger.warn("Unauthorized SDP attempt from key: {}", userKey);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("SDP stream error: {}", t.getMessage());
                cleanup();
            }

            @Override
            public void onCompleted() {
                logger.info("SDP stream completed");
                cleanup();
                responseObserver.onCompleted();
            }

            private void cleanup() {
                authenticated = false;
                userKey = null;
            }
        };
    }

    @Override
    public StreamObserver<ICEExchange> sendIceCandidates(
            StreamObserver<ICEExchange> responseObserver) {
        return new StreamObserver<ICEExchange>() {
            private String userKey;
            private boolean authenticated = false;

            @Override
            public void onNext(ICEExchange exchange) {
                try {
                    if (exchange.hasInitialRequest()) {
                        handleInitialRequest(exchange.getInitialRequest());
                    } else if (exchange.hasIceCandidates()) {
                        handleIceCandidates(exchange.getIceCandidates());
                    }
                } catch (Exception e) {
                    logger.error("ICE exchange error: {}", e.getMessage(), e);
                }
            }

            private void handleInitialRequest(ICEStreamInitialRequest request) {
                if (sessionManager.validateKey(request.getKey())) {
                    userKey = request.getKey();
                    authenticated = true;
                    responseObserver.onNext(ICEExchange.newBuilder()
                        .setInitialResponse(ICEStreamInitialResponse.newBuilder()
                            .setApproved(true)
                            .build())
                        .build());
                    sessionManager.registerIceObserver(sessionManager.getUsernameByKey(userKey), responseObserver);
                    logger.info("ICE stream authenticated for key: {}", userKey);
                } else {
                    responseObserver.onNext(ICEExchange.newBuilder()
                        .setInitialResponse(ICEStreamInitialResponse.newBuilder()
                            .setApproved(false)
                            .build())
                        .build());
                    logger.warn("Invalid ICE stream key: {}", request.getKey());
                }
            }

            private void handleIceCandidates(IceCandidatesMessage candidates) {
                if (authenticated) {
                    iceHandler.handleCandidates(userKey, candidates);
                    logger.debug("Processed ICE candidates from {}", userKey);
                } else {
                    logger.warn("Unauthorized ICE attempt from key: {}", userKey);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("ICE stream error: {}", t.getMessage());
                cleanup();
            }

            @Override
            public void onCompleted() {
                logger.info("ICE stream completed");
                cleanup();
                responseObserver.onCompleted();
            }

            private void cleanup() {
                authenticated = false;
                userKey = null;
            }
        };
    }
}
