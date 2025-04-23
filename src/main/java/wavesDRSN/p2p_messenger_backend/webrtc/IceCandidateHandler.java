package wavesDRSN.p2p_messenger_backend.webrtc;

import gRPC.v1.IceCandidatesMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import wavesDRSN.p2p_messenger_backend.session.UserSessionManager;

@Component
public class IceCandidateHandler {
    private final UserSessionManager sessionManager;

    public IceCandidateHandler(UserSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    private static final Logger logger = LoggerFactory.getLogger(IceCandidateHandler.class);

    public void handleCandidates(String senderKey, IceCandidatesMessage candidates) {
        try {
            sessionManager.getSessionByKey(senderKey).ifPresent(sender -> {
                sessionManager.getSession(candidates.getReceiver()).ifPresentOrElse(
                    receiver -> {
                        receiver.sendIceCandidates(candidates);
                        logger.info("Forwarded ICE candidates from {} to {}",
                            sender.getUsername(), receiver.getUsername());
                    },
                    () -> logger.warn("Receiver {} not found for ICE from {}",
                        candidates.getReceiver(), sender.getUsername())
                );
            });
        } catch (Exception e) {
            logger.error("ICE processing failed: {}", e.getMessage(), e);
        }
    }
}
