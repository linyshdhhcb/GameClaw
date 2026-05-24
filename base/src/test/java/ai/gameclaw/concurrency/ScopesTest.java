package ai.gameclaw.concurrency;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopesTest {

    @Test
    void raceReturnsFirstSuccessfulResult() throws Exception {
        String result = Scopes.race(List.of(
                () -> "fast",
                () -> { Thread.sleep(500); return "slow"; }
        ));

        assertThat(result).isEqualTo("fast");
    }

    @Test
    void raceThrowsWhenAllFail() {
        assertThatThrownBy(() -> Scopes.race(List.of(
                () -> { throw new RuntimeException("fail-a"); },
                () -> { throw new RuntimeException("fail-b"); }
        ))).isInstanceOf(Exception.class);
    }

    @Test
    void allCollectsAllSuccessfulResults() throws Exception {
        List<String> results = Scopes.all(List.of(
                () -> "a",
                () -> "b",
                () -> "c"
        ));

        assertThat(results).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void allThrowsWhenAnyFails() {
        assertThatThrownBy(() -> Scopes.all(List.of(
                () -> "ok",
                () -> { throw new RuntimeException("fail"); }
        ))).isInstanceOf(Exception.class);
    }
}
