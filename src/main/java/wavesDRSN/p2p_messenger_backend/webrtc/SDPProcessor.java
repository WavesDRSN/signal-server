package wavesDRSN.p2p_messenger_backend.webrtc;

import gRPC.v1.SessionDescription;
import org.springframework.stereotype.Component;

@Component
public class SDPProcessor {
    public void processOffer(SessionDescription offer) {
        // Логика создания SDP answer
    }

    public void processAnswer(SessionDescription answer) {
        // Логика применения SDP answer
    }
}
