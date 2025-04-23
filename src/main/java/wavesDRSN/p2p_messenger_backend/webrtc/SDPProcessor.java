package wavesDRSN.p2p_messenger_backend.webrtc;

import gRPC.v1.Signaling.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import wavesDRSN.p2p_messenger_backend.session.UserSessionManager;

@Component
public class SDPProcessor {
    private final UserSessionManager sessionManager;
    private static final Logger logger = LoggerFactory.getLogger(SDPProcessor.class);

    public SDPProcessor(UserSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void processSDP(String senderKey, SessionDescription sdp) {
        try {
            sessionManager.getSessionByKey(senderKey).ifPresent(sender -> {
                sessionManager.getSession(sdp.getReceiver()).ifPresentOrElse(
                    receiver -> {
                        receiver.sendSDP(sdp);
                        logger.info("Forwarded SDP from {} to {}",
                            sender.getUsername(), receiver.getUsername());
                    },
                    () -> logger.warn("Receiver {} not found for SDP from {}",
                        sdp.getReceiver(), sender.getUsername())
                );
            });
        } catch (Exception e) {
            logger.error("SDP processing failed: {}", e.getMessage(), e);
        }
    }
}
