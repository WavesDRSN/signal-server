package wavesDRSN.p2p_messenger_backend.services.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import wavesDRSN.p2p_messenger_backend.auth.model.NicknameReservation;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class NicknameReservationService {
    private final Map<String, NicknameReservation> reservations = new ConcurrentHashMap<>();
    private final byte reservationTimeoutMinutes = 1;

    public NicknameReservation reserve(String nickname) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(reservationTimeoutMinutes * 60);

        NicknameReservation reservation = new NicknameReservation(
            nickname, token, expiresAt
        );

        reservations.put(token, reservation);
        return reservation;
    }

    public Optional<NicknameReservation> validateToken(String token) {
        return Optional.ofNullable(reservations.get(token))
            .filter(r -> r.getExpiresAt().isAfter(Instant.now()));
    }

    public void removeReservation(String token) {
        reservations.remove(token);
    }

    public boolean existsByNickname(String nickname) {
        return reservations.values().stream()
                .anyMatch(r -> r.getNickname().equals(nickname) &&
                        r.getExpiresAt().isAfter(Instant.now()));
    }

    @Scheduled(fixedRate = 5 * 60 * 1000) // Каждые 5 минут
    public void cleanupExpiredReservations() {
        Instant now = Instant.now();
        reservations.values().removeIf(r -> r.getExpiresAt().isBefore(now));
    }
}
