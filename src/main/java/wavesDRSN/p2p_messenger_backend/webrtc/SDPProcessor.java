package wavesDRSN.p2p_messenger_backend.webrtc;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
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
public class SDPProcessor {
    private final UserSessionManager sessionManager;
    private final FcmService fcmService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(SDPProcessor.class);

    public SDPProcessor(UserSessionManager sessionManager, FcmService fcmService, UserService userService) {
        this.sessionManager = sessionManager;
        this.fcmService = fcmService;
        this.userService = userService;
    }

    public void processSDP(String senderUserKey, SessionDescription sdp) {
        try {
            Optional<UserSession> senderSessionOpt = sessionManager.getSessionByKey(senderUserKey);
            if (senderSessionOpt.isEmpty()) {
                logger.warn("Sender session not found for key: {}. Cannot process SDP.", senderUserKey);
                return;
            }
            UserSession senderSession = senderSessionOpt.get();
            String senderUsername = senderSession.getUsername();
            String receiverUsername = sdp.getReceiver();

            Optional<UserSession> receiverSessionOpt = sessionManager.getSession(receiverUsername);

            if (receiverSessionOpt.isPresent() && receiverSessionOpt.get().hasActiveSdpObserver()) {
                UserSession receiverSession = receiverSessionOpt.get();
                receiverSession.sendSDP(sdp);
                logger.info("Forwarded SDP from {} ({}) to {} via active stream.",
                    senderUsername, sdp.getType(), receiverUsername);
            } else {
                if (receiverSessionOpt.isPresent()) {
                    logger.info("Receiver {} is online but SDP observer is not active. Attempting FCM notification for SDP {}.",
                        receiverUsername, sdp.getType());
                } else {
                    logger.info("Receiver {} is offline. Attempting FCM notification for SDP {}.",
                        receiverUsername, sdp.getType());
                }

                Optional<UserDTO> receiverUserDtoOpt = userService.getUserByUsername(receiverUsername);
                // Предполагаем, что UserDTO имеет метод getFcmToken()
                Optional<String> receiverFcmTokenOpt = receiverUserDtoOpt.flatMap(dto -> Optional.ofNullable(dto.getFcmToken()));


                if (receiverFcmTokenOpt.isPresent() && !receiverFcmTokenOpt.get().isBlank()) {
                    String receiverFcmToken = receiverFcmTokenOpt.get();
                    Map<String, String> dataPayload = new HashMap<>();
                    dataPayload.put("sdp", sdp.getSdp());
                    dataPayload.put("type", sdp.getType());
                    dataPayload.put("sender", senderUsername);

                    String eventType = "sdp_offer"; // или "sdp_offer", "sdp_answer"

                    try {
                        String tokenSuffix = receiverFcmToken.length() > 5 ? receiverFcmToken.substring(receiverFcmToken.length() - 5) : receiverFcmToken;
                        logger.info("Sending FCM SDP {} notification to {} (token ending with ...{}). Sender: {}",
                                sdp.getType(), receiverUsername, tokenSuffix, senderUsername);

                        fcmService.sendDataOnlyNotificationToToken(receiverFcmToken, eventType, dataPayload);

                        logger.info("FCM SDP {} notification successfully initiated for {}.", sdp.getType(), receiverUsername);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid argument for FCM send (SDP {}). Receiver: {}, Error: {}", sdp.getType(), receiverUsername, e.getMessage());
                    } catch (FirebaseMessagingException e) {
                        logger.error("Failed to send FCM SDP {} notification to {}: ErrorCode: {}, Message: {}",
                                sdp.getType(), receiverUsername, e.getErrorCode(), e.getMessage(), e);
                        if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                            e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                            logger.warn("FCM token for {} seems invalid ({}). Attempting to remove.", receiverUsername, e.getMessagingErrorCode());
                            userService.removeFcmToken(receiverFcmToken);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        logger.error("FCM send async operation for SDP {} to {} failed: {}",
                                sdp.getType(), receiverUsername, e.getMessage(), e);
                    }
                } else {
                    logger.warn("No active FCM token found for receiver {}. Cannot send push notification for SDP {}.",
                        receiverUsername, sdp.getType());
                }
            }
        } catch (Exception e) {
            logger.error("SDP processing failed for SDP from key {}: {}", senderUserKey, e.getMessage(), e);
        }
    }
}
