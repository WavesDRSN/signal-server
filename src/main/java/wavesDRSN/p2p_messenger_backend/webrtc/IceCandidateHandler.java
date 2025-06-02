package wavesDRSN.p2p_messenger_backend.webrtc;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.protobuf.util.JsonFormat;
import gRPC.v1.Signaling.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import wavesDRSN.p2p_messenger_backend.dto.UserDTO; // Необходимо импортировать UserDTO
import wavesDRSN.p2p_messenger_backend.services.auth.UserService;
import wavesDRSN.p2p_messenger_backend.services.fcm.FcmService;
import wavesDRSN.p2p_messenger_backend.session.UserSession;
import wavesDRSN.p2p_messenger_backend.session.UserSessionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
public class IceCandidateHandler {
    private final UserSessionManager sessionManager;
    private final FcmService fcmService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(IceCandidateHandler.class);

    public IceCandidateHandler(UserSessionManager sessionManager, FcmService fcmService, UserService userService) {
        this.sessionManager = sessionManager;
        this.fcmService = fcmService;
        this.userService = userService;
    }

    public void handleCandidates(String senderUserKey, IceCandidatesMessage candidates) {
        try {
            Optional<UserSession> senderSessionOpt = sessionManager.getSessionByKey(senderUserKey);
            if (senderSessionOpt.isEmpty()) {
                logger.warn("Sender session not found for key: {}. Cannot process ICE candidates.", senderUserKey);
                return;
            }
            UserSession senderSession = senderSessionOpt.get();
            String senderUsername = senderSession.getUsername();
            String receiverUsername = candidates.getReceiver();

            Optional<UserSession> receiverSessionOpt = sessionManager.getSession(receiverUsername);

            if (receiverSessionOpt.isPresent() && receiverSessionOpt.get().hasActiveIceObserver()) {
                UserSession receiverSession = receiverSessionOpt.get();
                receiverSession.sendIceCandidates(candidates);
                logger.info("Forwarded ICE candidates from {} to {} via active stream.",
                        senderUsername, receiverUsername);
            } else {
                if (receiverSessionOpt.isPresent()) {
                    logger.info("Receiver {} is online but ICE observer is not active. Attempting FCM notification for ICE candidates.", receiverUsername);
                } else {
                    logger.info("Receiver {} is offline. Attempting FCM notification for ICE candidates.", receiverUsername);
                }

                Optional<UserDTO> receiverUserDtoOpt = userService.getUserByUsername(receiverUsername);
                // Предполагаем, что UserDTO имеет метод getFcmToken()
                Optional<String> receiverFcmTokenOpt = receiverUserDtoOpt.flatMap(dto -> Optional.ofNullable(dto.getFcmToken()));


                if (receiverFcmTokenOpt.isPresent() && !receiverFcmTokenOpt.get().isBlank()) {
                    String receiverFcmToken = receiverFcmTokenOpt.get();
                    Map<String, String> dataPayload = new HashMap<>();
                    try {
                        String candidatesJson = JsonFormat.printer().print(candidates);
                        dataPayload.put("iceCandidates", candidatesJson);
                    } catch (Exception e) {
                        logger.error("Could not format ICE candidates to JSON for FCM: {}", e.getMessage());
                        dataPayload.put("iceCandidatesCount", String.valueOf(candidates.getCandidatesCount()));
                        dataPayload.put("errorFormatting", "true");
                    }
                    dataPayload.put("sender", senderUsername);
                    String eventType = "ice_exchange";

                    try {
                        String tokenSuffix = receiverFcmToken.length() > 5 ? receiverFcmToken.substring(receiverFcmToken.length() - 5) : receiverFcmToken;
                        logger.info("Sending FCM ICE candidates notification to {} (token ending with ...{}). Sender: {}",
                                receiverUsername, tokenSuffix, senderUsername);

                        fcmService.sendDataOnlyNotificationToToken(receiverFcmToken, eventType, dataPayload);

                        logger.info("FCM ICE candidates notification successfully initiated for {}.", receiverUsername);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid argument for FCM send (ICE). Receiver: {}, Error: {}", receiverUsername, e.getMessage());
                    } catch (FirebaseMessagingException e) {
                        logger.error("Failed to send FCM ICE candidates notification to {}: ErrorCode: {}, Message: {}",
                                receiverUsername, e.getErrorCode(), e.getMessage(), e);
                        if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                                e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                            logger.warn("FCM token for {} seems invalid ({}). Attempting to remove.", receiverUsername, e.getMessagingErrorCode());
                            userService.removeFcmToken(receiverFcmToken);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        logger.error("FCM send async operation for ICE candidates to {} failed: {}",
                                receiverUsername, e.getMessage(), e);
                    }
                } else {
                    logger.warn("No active FCM token found for receiver {}. Cannot send push notification for ICE candidates.",
                            receiverUsername);
                }
            }
        } catch (Exception e) {
            logger.error("ICE candidates processing failed for sender key {}: {}", senderUserKey, e.getMessage(), e);
        }
    }
}