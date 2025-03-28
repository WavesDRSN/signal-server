package wavesDRSN.p2p_messenger_backend.webrtc;

import gRPC.v1.IceCandidatesMessage;
import org.springframework.stereotype.Component;
import wavesDRSN.p2p_messenger_backend.session.UserSessionManager;

@Component
public class IceCandidateHandler {
    private final UserSessionManager sessionManager;

    public IceCandidateHandler(UserSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void handleCandidates(IceCandidatesMessage message) {
        sessionManager.getSession(message.getReceiver()).ifPresent(receiver ->
            receiver.sendIceCandidates(message)
        );
    }
}
