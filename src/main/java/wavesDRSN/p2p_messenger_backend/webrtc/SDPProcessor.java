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

    public void processSDP(SessionDescription sdp) {
        try {
            logger.debug("Attempting to process SDP for {}", sdp.getReceiver());

            sessionManager.getSession(sdp.getReceiver()).ifPresentOrElse(
                receiver -> {
                    logger.info("Forwarding SDP from {} to {}", sdp.getSender(), sdp.getReceiver());
                    receiver.sendSDP(sdp);
                    logger.debug("SDP forwarded successfully");
                },
                () -> logger.warn("Receiver {} not found for SDP from {}",
                    sdp.getReceiver(), sdp.getSender())
            );

        } catch (Exception e) {
            logger.error("SDP processing failed: {}", e.getMessage(), e);
        }
    }
}
