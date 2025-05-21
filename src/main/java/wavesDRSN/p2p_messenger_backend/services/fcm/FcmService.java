package wavesDRSN.p2p_messenger_backend.services.fcm;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class FcmService {
    private final FirebaseMessaging firebaseMessaging;

    public FcmService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public String sendToToken(String token, String title, String body, Map<String, String> data)
        throws FirebaseMessagingException {

        Message message = buildMessage(title, body, data).setToken(token).build();
        return firebaseMessaging.send(message);
    }

    public String sendToTopic(String topic, String title, String body, Map<String, String> data)
        throws FirebaseMessagingException {

        Message message = buildMessage(title, body, data).setTopic(topic).build();
        return firebaseMessaging.send(message);
    }

    private Message.Builder buildMessage(String title, String body, Map<String, String> data) {
        return Message.builder()
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .putAllData(data);
    }
}