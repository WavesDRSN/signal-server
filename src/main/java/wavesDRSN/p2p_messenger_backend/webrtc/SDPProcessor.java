package wavesDRSN.p2p_messenger_backend.webrtc;

import gRPC.v1.SessionDescription;
import org.springframework.stereotype.Component;
import wavesDRSN.p2p_messenger_backend.session.UserSessionManager;

@Component
public class SDPProcessor {
    private final UserSessionManager sessionManager;

    public SDPProcessor(UserSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void processSDP(SessionDescription sdp) {
        sessionManager.getSession(sdp.getReceiver()).ifPresent(receiver ->
            receiver.sendSDP(sdp)
        );
    }
}
