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

    // Ключи для кастомных клеймов
    private static final String USER_ID_CLAIM = "userId";

    @PostConstruct // Метод выполнится после создания бина и внедрения зависимостей
    protected void init() {
        // Преобразуем строку секрета в безопасный ключ SecretKey
        // Убедитесь, что ваш секрет достаточно длинный для HS512 (минимум 64 байта в Base64)
        // Например, сгенерировать можно так:
        // SecureRandom random = new SecureRandom(); byte[] keyBytes = new byte[64]; random.nextBytes(keyBytes); String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        // И потом эту строку вставить в application.properties
        if (jwtSecretString == null || jwtSecretString.trim().isEmpty()) {
            log.error("JWT secret string is not configured in application properties (jwt.secret). Cannot initialize JWT provider.");
            throw new IllegalStateException("JWT secret is not configured.");
        }
        try {
            jwtSecretKey = Keys.hmacShaKeyFor(jwtSecretString.getBytes());
            log.info("JWT Secret Key initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize JWT Secret Key. Ensure the secret string is a valid Base64 encoded string for HMAC-SHA keys or a sufficiently long raw string: {}", e.getMessage());
            throw new IllegalStateException("Failed to initialize JWT Secret Key", e);
        }
    }

    /**
     * Генерирует JWT токен для пользователя.
     *
     * @param username Имя пользователя. Будет установлено как 'subject' (sub) в токене.
     * @param userId   Уникальный идентификатор пользователя (в виде строки). Будет добавлен как кастомный клейм.
     * @return Сгенерированный JWT токен.
     */
    public String generateToken(String username, String userId) { // <-- ИЗМЕНЕНА СИГНАТУРА
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username) // Устанавливаем username как основной subject
                .claim(USER_ID_CLAIM, userId) // Добавляем userId как кастомный клейм <-- ДОБАВЛЕН КЛЕЙМ
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

    /**
     * Извлекает идентификатор пользователя (userId) из кастомного клейма JWT токена.
     *
     * @param token JWT токен.
     * @return Идентификатор пользователя (userId) в виде строки, или null если клейм отсутствует.
     */
    public String getUserIdFromToken(String token) { // <-- ДОБАВЛЕН МЕТОД
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get(USER_ID_CLAIM, String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to parse JWT token or get userId claim: {}", e.getMessage());
            return null;
        }
    }


    /**
     * Проверяет валидность JWT токена.
     *
     * @param authToken Токен для проверки.
     * @return true, если токен валиден, иначе false.
     */
    public boolean validateToken(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            log.warn("Attempted to validate a null or empty token.");
            return false;
        }
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
            // Это может произойти, если authToken не является валидным JWS (например, не 3 части, разделенные точками)
            log.error("JWT claims string is invalid or empty: {}", ex.getMessage());
        }
        return false;
    }
}