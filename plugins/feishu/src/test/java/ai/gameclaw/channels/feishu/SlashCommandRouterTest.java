package ai.gameclaw.channels.feishu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SlashCommandRouterTest {

    private SlashCommandRouter router;

    @BeforeEach
    void setUp() {
        router = new SlashCommandRouter(Map.of(
                "/design", (args, event) -> "Design: " + args,
                "/query", (args, event) -> "Query: " + args,
                "/review", (args, event) -> "Review: " + args
        ));
    }

    @Test
    void routeKnownCommand() {
        FeishuEvent event = new FeishuEvent("tk1", "chat1", "user1", "/design 5 monsters", null, "text", "msg1");
        String result = router.route("/design 5 monsters", event);
        assertThat(result).isEqualTo("Design: 5 monsters");
    }

    @Test
    void routeUnknownCommand() {
        String result = router.route("/unknown", null);
        assertThat(result).contains("未知命令");
    }

    @Test
    void routeCommandWithNoArgs() {
        String result = router.route("/query", null);
        assertThat(result).isEqualTo("Query: ");
    }
}
