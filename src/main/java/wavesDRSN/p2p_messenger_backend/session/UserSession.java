package wavesDRSN.p2p_messenger_backend.session;

import gRPC.v1.Signaling.*;
import io.grpc.stub.StreamObserver;
import lombok.Getter;

import java.time.Instant;

@Getter
public class UserSession {
    private final String username;
    private final StreamObserver<UserConnectionResponse> observer;
    private volatile Instant lastActive;
    private StreamObserver<SessionDescription> sdpObserver;
    private StreamObserver<IceCandidatesMessage> iceObserver;

    public UserSession(String username, StreamObserver<UserConnectionResponse> observer) {
        this.username = username;
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

    public void setSdpObserver(StreamObserver<SessionDescription> sdpObserver) {
        this.sdpObserver = sdpObserver;
    }

    public void setIceObserver(StreamObserver<IceCandidatesMessage> iceObserver) {
        this.iceObserver = iceObserver;
    }

    public void sendSDP(SessionDescription sdp) {
        if (sdpObserver != null) {
            sdpObserver.onNext(sdp);
        }
    }

    public void sendIceCandidates(IceCandidatesMessage candidates) {
        if (iceObserver != null) {
            iceObserver.onNext(candidates);
        }
    }

    public void close() {
        observer.onCompleted();
        if (sdpObserver != null) sdpObserver.onCompleted();
        if (iceObserver != null) iceObserver.onCompleted();
    }

}