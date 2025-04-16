package wavesDRSN.p2p_messenger_backend.webrtc;

import gRPC.v1.Signaling.*;
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

    public void handleCandidates(IceCandidatesMessage message) {
        try {
            logger.debug("Handling {} ICE candidates from {} to {}",
                message.getCandidatesCount(), message.getSender(), message.getReceiver());

            sessionManager.getSession(message.getReceiver()).ifPresentOrElse(
                receiver -> {
                    logger.info("Forwarding ICE candidates from {} to {}",
                        message.getSender(), message.getReceiver());
                    receiver.sendIceCandidates(message);
                    logger.debug("ICE candidates forwarded");
                },
                () -> logger.warn("Receiver {} not found for ICE candidates from {}",
                    message.getReceiver(), message.getSender())
            );

        } catch (Exception e) {
            logger.error("ICE candidates handling failed: {}", e.getMessage(), e);
        }
    }
}
