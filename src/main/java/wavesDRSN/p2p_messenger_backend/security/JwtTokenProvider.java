package wavesDRSN.p2p_messenger_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys; // Используем Keys для безопасного создания ключа
import io.jsonwebtoken.security.SignatureException; // Конкретные исключения
import jakarta.annotation.PostConstruct; // Для инициализации ключа
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component; // Используем @Component, так как это утилитарный класс

import javax.crypto.SecretKey; // Используем SecretKey
import java.util.Date;

@Component // Заменяем @Service на @Component для утилитарного бина
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecretString; // Берем секрет как строку

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    private SecretKey jwtSecretKey; // Храним секрет как SecretKey

    @PostConstruct // Метод выполнится после создания бина и внедрения зависимостей
    protected void init() {
        // Преобразуем строку секрета в безопасный ключ SecretKey
        // Убедитесь, что ваш секрет достаточно длинный для HS512 (минимум 64 байта в Base64)
        jwtSecretKey = Keys.hmacShaKeyFor(jwtSecretString.getBytes());
        log.info("JWT Secret Key initialized.");
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512) // Используем SecretKey
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey) // Используем SecretKey
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey) // Используем SecretKey
                    .build()
                    .parseClaimsJws(authToken);
            return true;
            // Ловим конкретные исключения для логирования и отладки
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            // Возникает, если токен null, пустой или содержит только пробелы
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}