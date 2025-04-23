package wavesDRSN.p2p_messenger_backend.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Value("${cache.challenge.ttlMinutes:2}")
    private int challengeTtlMinutes;

    @Value("${cache.challenge.maxSize:10000}")
    private int challengeMaxSize;

    @Bean
    public Cache<String, byte[]> challengeCacheStore() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(challengeTtlMinutes)) // TTL
                .maximumSize(challengeMaxSize) // ограничение кол-ва записей в кэше
                .recordStats() // сбор статистики
                .build();
    }
}
