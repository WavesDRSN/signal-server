package wavesDRSN.p2p_messenger_backend.session;

import gRPC.v1.Signaling.*;
import io.grpc.stub.StreamObserver;
import lombok.Getter;

import java.time.Instant;

@Getter
public class UserSession {
    private final String username;
    private final StreamObserver<UserConnectionResponse> observer;
    private final String userKey;
    private volatile Instant lastActive;
    private StreamObserver<SDPExchange> sdpObserver;
    private StreamObserver<ICEExchange> iceObserver;

    public UserSession(String username, String userKey, 
                      StreamObserver<UserConnectionResponse> observer) {
        this.username = username;
        this.userKey = userKey;
        this.observer = observer;
        this.lastActive = Instant.now();
    }

    public void sendResponse(UserConnectionResponse response) {
        if (observer != null) {
            try {
                observer.onNext(response);
            } catch (Exception e) {
                // Handle exceptions, e.g., if the stream is already closed
                System.err.println("Error sending response to " + username + ": " + e.getMessage());
            }
        }
    }

    // Обновление времени последней активности
    public void updateLastActive() {
        this.lastActive = Instant.now();
    }

    public Instant getLastActive() {
        return lastActive;
    }

    public void setSdpObserver(StreamObserver<SDPExchange> sdpObserver) {
        this.sdpObserver = sdpObserver;
    }

    public boolean hasActiveSdpObserver() {
        return this.sdpObserver != null;
    }

    public void setIceObserver(StreamObserver<ICEExchange> iceObserver) {
        this.iceObserver = iceObserver;
    }

    public boolean hasActiveIceObserver() {
        return this.iceObserver != null;
    }

    public void sendSDP(SessionDescription sdp) {
        if (sdpObserver != null) {
            try {
                sdpObserver.onNext(SDPExchange.newBuilder().setSessionDescription(sdp).build());
            } catch (Exception e) {
                System.err.println("Error sending SDP to " + username + ": " + e.getMessage());
                // Optionally, mark sdpObserver as null or handle error
            }
        }
    }

    public void sendIceCandidates(IceCandidatesMessage candidates) {
        if (iceObserver != null) {
            try {
                iceObserver.onNext(ICEExchange.newBuilder().setIceCandidates(candidates).build());
            } catch (Exception e) {
                System.err.println("Error sending ICE to " + username + ": " + e.getMessage());
                // Optionally, mark iceObserver as null or handle error
            }
        }
    }

    public void close() {
        if (observer != null) {
            try {
                observer.onCompleted();
            } catch (Exception e) {
                System.err.println("Error completing main observer for " + username + ": " + e.getMessage());
            }
        }
        if (sdpObserver != null) {
            try {
                sdpObserver.onCompleted();
            } catch (Exception e) {
                System.err.println("Error completing SDP observer for " + username + ": " + e.getMessage());
            }
        }
        if (iceObserver != null) {
            try {
                iceObserver.onCompleted();
            } catch (Exception e) {
                System.err.println("Error completing ICE observer for " + username + ": " + e.getMessage());
            }
        }
    }

}