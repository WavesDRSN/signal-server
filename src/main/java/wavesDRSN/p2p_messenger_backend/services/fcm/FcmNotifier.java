package wavesDRSN.p2p_messenger_backend.services.fcm;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class FcmNotifier {

    public void sendDirectNotification(String fcmToken,
                                      String title,
                                      String body,
                                      Map<String, String> data) {
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .build();

            FirebaseMessaging.getInstance().sendAsync(message);
        } catch (Exception e) {

            System.err.println("FCM error: " + e);
        }
    }
}