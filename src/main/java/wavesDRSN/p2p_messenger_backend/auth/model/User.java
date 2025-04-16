package wavesDRSN.p2p_messenger_backend.auth.model;

import lombok.Getter;

import java.security.PublicKey;

public record User(String nickname, PublicKey publicKey) {
}
