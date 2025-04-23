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
        observer.onNext(response);
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

    public void setIceObserver(StreamObserver<ICEExchange> iceObserver) {
        this.iceObserver = iceObserver;
    }

    public void sendSDP(SessionDescription sdp) {
        if (sdpObserver != null) {
            sdpObserver.onNext(SDPExchange.newBuilder().setSessionDescription(sdp).build());
        }
    }

    public void sendIceCandidates(IceCandidatesMessage candidates) {
        if (iceObserver != null) {
            iceObserver.onNext(ICEExchange.newBuilder().setIceCandidates(candidates).build());
        }
    }

    public void close() {
        observer.onCompleted();
        if (sdpObserver != null) sdpObserver.onCompleted();
        if (iceObserver != null) iceObserver.onCompleted();
    }

}