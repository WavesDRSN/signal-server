package wavesDRSN.p2p_messenger_backend.services.fcm;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import gRPC.v1.Notification.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wavesDRSN.p2p_messenger_backend.services.auth.UserService;

import java.util.concurrent.ExecutionException;

@GrpcService
public class NotificationServiceImpl extends NotificationServiceGrpc.NotificationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final FcmService fcmService;
    private final UserService userService;

    public NotificationServiceImpl(FcmService fcmService, UserService userService) {
        this.fcmService = fcmService;
        this.userService = userService;
    }

    @Override
    public void sendNotification(
            NotificationRequest request,
            StreamObserver<NotificationResponse> responseObserver
    ) {
        String targetIdentifier = "";
        NotificationRequest.TargetCase targetCase = request.getTargetCase();

        String eventType = request.getPayload().getEventType();
        if (eventType == null || eventType.trim().isEmpty()) {
            log.warn("SendNotification request received with empty event_type. Setting to 'generic_notification'.");
            eventType = "generic_notification";
        }

        try {
            String messageId;

            switch (targetCase) {
                case FCM_TOKEN:
                    targetIdentifier = request.getFcmToken();
                    if (targetIdentifier == null || targetIdentifier.trim().isEmpty()) {
                        log.warn("FCM token is missing in SendNotification request for eventType: '{}'", eventType);
                        responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription("FCM token must be provided for token-based notification.")
                            .asRuntimeException());
                        return;
                    }
                    String tokenSuffix = targetIdentifier.substring(Math.max(0, targetIdentifier.length() - 5));
                    log.info("Processing SendNotification to FCM Token. EventType: '{}', Token ends with: ...{}",
                             eventType, tokenSuffix);
                    messageId = fcmService.sendDataOnlyNotificationToToken(
                        targetIdentifier,
                        eventType,
                        request.getPayload().getDataMap()
                    );
                    break;

                case TOPIC:
                    targetIdentifier = request.getTopic();
                     if (targetIdentifier == null || targetIdentifier.trim().isEmpty()) {
                        log.warn("Topic is missing in SendNotification request for eventType: '{}'", eventType);
                        responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription("Topic must be provided for topic-based notification.")
                            .asRuntimeException());
                        return;
                    }
                    log.info("Processing SendNotification to Topic. EventType: '{}', Topic: '{}'", eventType, targetIdentifier);
                    messageId = fcmService.sendDataOnlyNotificationToTopic(
                        targetIdentifier,
                        eventType,
                        request.getPayload().getDataMap()
                    );
                    break;

                default:
                    log.warn("No target (token or topic) specified in SendNotification request. EventType: '{}'", eventType);
                    responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("No target (token or topic) specified for notification.")
                        .asRuntimeException());
                    return;
            }

            log.info("Successfully initiated FCM send. Target: '{}', EventType: '{}', Returned Message ID: {}",
                     targetIdentifier, eventType, messageId);
            NotificationResponse response = NotificationResponse.newBuilder()
                .setSuccess(true)
                .setMessageId(messageId != null ? messageId : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for FCM send operation. Target: '{}', EventType: '{}'. Error: {}",
                      targetIdentifier, eventType, e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .withCause(e)
                .asRuntimeException());
        }
        catch (FirebaseMessagingException e) {
            log.error("FCM error while sending notification. Target: '{}', EventType: '{}'. ErrorCode: {}, Message: {}",
                      targetIdentifier, eventType, e.getErrorCode(), e.getMessage(), e);

            if (targetCase == NotificationRequest.TargetCase.FCM_TOKEN &&
                targetIdentifier != null && !targetIdentifier.isEmpty() &&
                (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                 e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT)) {
                log.info("FCM token for target '{}', EventType '{}' seems invalid ({}). Attempting to remove it.",
                         targetIdentifier, eventType, e.getMessagingErrorCode());
                 try {
                    userService.removeFcmToken(targetIdentifier);
                 } catch (Exception removeEx) {
                    log.error("Failed to remove invalid FCM token '{}' after send error: {}", targetIdentifier, removeEx.getMessage(), removeEx);
                 }
            }

            responseObserver.onError(Status.INTERNAL
                .withDescription("FCM error: " + e.getErrorCode() + " - " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Error during FCM send async operation. Target: '{}', EventType: '{}': {}",
                      targetIdentifier, eventType, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal error during FCM send operation: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
        }
        catch (Exception e) {
            log.error("Unexpected internal error while sending notification. Target: '{}', EventType: '{}': {}",
                      targetIdentifier, eventType, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Unexpected internal error during notification sending.")
                .withCause(e)
                .asRuntimeException());
        }
    }
}