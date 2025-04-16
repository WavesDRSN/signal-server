package wavesDRSN.p2p_messenger_backend.auth.model;

import lombok.Getter;

import java.time.Instant;

@Getter
public class NicknameReservation {
    private final String nickname;
    private final String token;
    private final Instant expiresAt;

    public NicknameReservation(String nickname, String token, Instant expiresAt) {
        this.nickname = nickname;
        this.token = token;
        this.expiresAt = expiresAt;
    }

}
