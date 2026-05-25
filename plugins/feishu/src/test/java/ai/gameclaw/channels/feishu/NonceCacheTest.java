package ai.gameclaw.channels.feishu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NonceCacheTest {

    private NonceCache nonceCache;

    @BeforeEach
    void setUp() {
        nonceCache = new NonceCache();
    }

    @Test
    void acquireNewNonce() {
        assertThat(nonceCache.acquire("nonce1")).isTrue();
    }

    @Test
    void rejectReplayNonce() {
        nonceCache.acquire("nonce1");
        assertThat(nonceCache.acquire("nonce1")).isFalse();
    }

    @Test
    void rejectNullNonce() {
        assertThat(nonceCache.acquire(null)).isFalse();
    }

    @Test
    void rejectBlankNonce() {
        assertThat(nonceCache.acquire("")).isFalse();
    }

    @Test
    void differentNoncesAreIndependent() {
        assertThat(nonceCache.acquire("nonce1")).isTrue();
        assertThat(nonceCache.acquire("nonce2")).isTrue();
    }
}
