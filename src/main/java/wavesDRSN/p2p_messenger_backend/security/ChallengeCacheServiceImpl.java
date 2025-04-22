package wavesDRSN.p2p_messenger_backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChallengeCacheServiceImpl implements ChallengeCacheService {
    private final Cache<String, byte[]> cacheStore;

    // добавляет запись в кэш
    @Override
    public void putChallenge(String challengeId, byte[] challenge) {
        if (!StringUtils.hasText(challengeId)) {
            throw new IllegalArgumentException("challengeId не может быть пустым");
        }

        if (challenge == null || challenge.length == 0) {
            throw new IllegalArgumentException("Массив байт challenge не может быть null или пустым для ID: \" + challengeId)");
        }

        cacheStore.put(challengeId, challenge);
        // log.debug("Challenge сохранен для ID: {}", challengeId); // опциональное логирование
    }

    // получает и удаляет challenge из кэша по ID
    @Override
    public byte[] getAndRemove(String challengeId) {
        byte[] challenge = cacheStore.getIfPresent(challengeId);

        if (challenge == null) {
            throw new IllegalArgumentException("Challenge с ID " + challengeId + " не найден в кэше");
        }

        cacheStore.invalidate(challengeId);

        return challenge;
    }

}
