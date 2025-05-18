package wavesDRSN.p2p_messenger_backend.services;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FcmService {

    public void sendNotification(String token, String title, String body, Map<String, String> data)
        throws FirebaseMessagingException {

        Message message = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .putAllData(data)
            .build();

        FirebaseMessaging.getInstance().sendAsync(message);
    }

    public void sendToTopic(String topic, String title, String body)
        throws FirebaseMessagingException {

        Message message = Message.builder()
            .setTopic(topic)
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .build();

        FirebaseMessaging.getInstance().sendAsync(message);
    }
}