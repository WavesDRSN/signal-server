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

import java.util.HashMap;
import java.util.Map;
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
            String messageId = null;

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
                    String tokenSuffix = targetIdentifier.length() > 5 ? targetIdentifier.substring(targetIdentifier.length() - 5) : targetIdentifier;
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
                case TARGET_NOT_SET: // Explicitly handle TARGET_NOT_SET
                default: // Catches TARGET_NOT_SET and any future unhandled cases
                    log.warn("No target (token or topic) specified or unknown target in SendNotification request. TargetCase: {}, EventType: '{}'", targetCase, eventType);
                    responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("No target (token or topic) specified, or unknown target type for notification.")
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
                 e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT)) { // Check for specific error codes
                log.info("FCM token for target '{}', EventType '{}' seems invalid ({}). Attempting to remove it via UserService.",
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

    @Override
    public void notifyServer(
            MessageEvent request,
            StreamObserver<NotificationResponse> responseObserver
    ) {
        String senderId = request.getSenderId();
        String receiverId = request.getReceiverId();

        log.info("NotifyServer called: sender={}, receiver={}", senderId, receiverId);

        try {
            // Получаем FCM токен получателя
            String receiverFcmToken = userService.getFcmTokenByUserId(receiverId);

            if (receiverFcmToken == null || receiverFcmToken.trim().isEmpty()) {
                log.warn("No FCM token found for user: {}", receiverId);

                NotificationResponse response = NotificationResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("No FCM token found for receiver")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Подготавливаем данные для уведомления
            Map<String, String> notificationData = new HashMap<>();
            notificationData.put("sender_id", senderId);
            notificationData.put("receiver_id", receiverId);
            notificationData.put("action", "initiate_connection"); // Для инициации SDP соединения
            notificationData.put("message", "У вас новое сообщение");


            // Отправляем FCM уведомление
            String messageId = fcmService.sendDataOnlyNotificationToToken(
                    receiverFcmToken,
                    "new_message",
                    notificationData
            );

            log.info("Successfully sent pending message notification. Sender: {}, Receiver: {}, FCM Message ID: {}",
                    senderId, receiverId, messageId);

            NotificationResponse response = NotificationResponse.newBuilder()
                    .setSuccess(true)
                    .setMessageId(messageId != null ? messageId : "")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for pending message notification. Sender: {}, Receiver: {}. Error: {}",
                    senderId, receiverId, e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        } catch (FirebaseMessagingException e) {
            log.error("FCM error while sending pending message notification. Sender: {}, Receiver: {}. ErrorCode: {}, Message: {}",
                    senderId, receiverId, e.getErrorCode(), e.getMessage(), e);

            // Если токен недействителен, удаляем его
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                    e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                try {
                    String receiverFcmToken = userService.getFcmTokenByUserId(receiverId);
                    if (receiverFcmToken != null) {
                        userService.removeFcmToken(receiverFcmToken);
                        log.info("Removed invalid FCM token for user: {}", receiverId);
                    }
                } catch (Exception removeEx) {
                    log.error("Failed to remove invalid FCM token for user {}: {}", receiverId, removeEx.getMessage());
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
            log.error("Error during pending message notification send. Sender: {}, Receiver: {}: {}",
                    senderId, receiverId, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error during notification send: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in notifyServer. Sender: {}, Receiver: {}: {}",
                    senderId, receiverId, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected internal error during notification sending.")
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}