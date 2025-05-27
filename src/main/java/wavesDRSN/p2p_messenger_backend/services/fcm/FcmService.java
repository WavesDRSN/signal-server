package wavesDRSN.p2p_messenger_backend.services.fcm;

import com.google.firebase.messaging.*; // FirebaseMessaging, Message, AndroidConfig, etc.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);
    private final FirebaseMessaging firebaseMessaging;

    public FcmService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public String sendDataOnlyNotificationToToken(String fcmToken, String eventType, Map<String, String> data)
        throws FirebaseMessagingException, ExecutionException, InterruptedException {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            log.warn("Attempt to send FCM message to an empty or null token. EventType: {}", eventType);
            throw new IllegalArgumentException("FCM token cannot be null or empty.");
        }
        String tokenSuffix = fcmToken.substring(Math.max(0, fcmToken.length() - 5));
        log.info("Attempting to send FCM data-only message for Android. Token ends with: ...{}, EventType: '{}', Data keys: {}",
                 tokenSuffix, eventType, data != null ? data.keySet() : "null");

        Message message = buildAndroidDataOnlyMessage(eventType, data)
            .setToken(fcmToken)
            .build();

        try {
            String messageId = firebaseMessaging.sendAsync(message).get();
            log.info("Successfully sent FCM message for Android to token ending with ...{}. EventType: '{}', Message ID: {}",
                     tokenSuffix, eventType, messageId);
            return messageId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("FCM send operation for Android was interrupted for token ending with ...{}, EventType: '{}'", tokenSuffix, eventType, e);
            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseMessagingException) {
                throw (FirebaseMessagingException) e.getCause();
            }
            log.error("FCM send operation for Android failed with ExecutionException for token ending with ...{}, EventType: '{}'", tokenSuffix, eventType, e);
            throw e;
        }
    }

    public String sendDataOnlyNotificationToTopic(String topic, String eventType, Map<String, String> data)
        throws FirebaseMessagingException, ExecutionException, InterruptedException {
        if (topic == null || topic.trim().isEmpty()) {
            log.warn("Attempt to send FCM message to an empty or null topic for Android. EventType: {}", eventType);
            throw new IllegalArgumentException("Topic cannot be null or empty.");
        }
        log.info("Attempting to send FCM data-only message to Android topic: '{}', EventType: '{}', Data keys: {}",
                 topic, eventType, data != null ? data.keySet() : "null");

        Message message = buildAndroidDataOnlyMessage(eventType, data)
            .setTopic(topic)
            .build();

        try {
            String messageId = firebaseMessaging.sendAsync(message).get();
            log.info("Successfully sent FCM message to Android topic: '{}'. EventType: '{}', Message ID: {}", topic, eventType, messageId);
            return messageId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("FCM send operation for Android was interrupted for topic: '{}', EventType: '{}'", topic, eventType, e);
            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseMessagingException) {
                throw (FirebaseMessagingException) e.getCause();
            }
            log.error("FCM send operation for Android failed with ExecutionException for topic: '{}', EventType: '{}'", topic, eventType, e);
            throw e;
        }
    }

    // Метод для сборки сообщения, специфичный для Android data-only
    private Message.Builder buildAndroidDataOnlyMessage(String eventType, Map<String, String> data) {
        Message.Builder messageBuilder = Message.builder();
        if (data != null) {
            messageBuilder.putAllData(data);
        }

        if (eventType != null && !eventType.trim().isEmpty()) {
          messageBuilder.putData("event_type", eventType);
        } else {
          log.warn("Building Android FCM message without an event_type. Setting to 'unknown'.");
          messageBuilder.putData("event_type", "unknown");
        }

        messageBuilder
            .setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH) // Для немедленной доставки data-only сообщений
                .setTtl(Duration.ofDays(1).toMillis())    // Время жизни сообщения, например, 1 день
                // Можно добавить другие специфичные для Android параметры, если нужно:
                // .setRestrictedPackageName("your.package.name")
                // .setCollapseKey("your_collapse_key")
                .build());

        log.debug("Built Android FCM data-only message builder. EventType: '{}', Data keys: {}", eventType, data != null ? data.keySet() : "null");
        return messageBuilder;
    }
}