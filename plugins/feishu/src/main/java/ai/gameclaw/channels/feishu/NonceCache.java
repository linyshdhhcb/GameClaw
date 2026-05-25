package ai.gameclaw.channels.feishu;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

public class NonceCache {

    private final Cache<String, Boolean> cache;

    public NonceCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }

    public boolean acquire(String nonce) {
        if (nonce == null || nonce.isBlank()) return false;
        Boolean existing = cache.getIfPresent(nonce);
        if (existing != null) return false;
        cache.put(nonce, Boolean.TRUE);
        return true;
    }
}
