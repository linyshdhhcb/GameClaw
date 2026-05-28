package ai.gameclaw.channels.slack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlackSlashCommandRouterTest {

    private SlackSlashCommandRouter router;

    @BeforeEach
    void setUp() {
        router = new SlackSlashCommandRouter();
    }

    @Test
    void routeDesignCommand() {
        SlackSlashCommandRouter.RoutedCommand result = router.route("design 5 monsters");
        assertThat(result.command()).isEqualTo("design");
        assertThat(result.args()).isEqualTo("5 monsters");
    }

    @Test
    void routeQueryCommand() {
        SlackSlashCommandRouter.RoutedCommand result = router.route("query game mechanics");
        assertThat(result.command()).isEqualTo("query");
        assertThat(result.args()).isEqualTo("game mechanics");
    }

    @Test
    void routeCodeCommand() {
        SlackSlashCommandRouter.RoutedCommand result = router.route("code player movement");
        assertThat(result.command()).isEqualTo("code");
        assertThat(result.args()).isEqualTo("player movement");
    }

    @Test
    void routeTestCommand() {
        SlackSlashCommandRouter.RoutedCommand result = router.route("test combat system");
        assertThat(result.command()).isEqualTo("test");
        assertThat(result.args()).isEqualTo("combat system");
    }

    @Test
    void routeHelpCommand() {
        SlackSlashCommandRouter.RoutedCommand result = router.route("help");
        assertThat(result.command()).isEqualTo("help");
        assertThat(result.args()).isEqualTo("");
    }

    @Test
    void routeCommandWithNoArgs() {
        SlackSlashCommandRouter.RoutedCommand result = router.route("design");
        assertThat(result.command()).isEqualTo("design");
        assertThat(result.args()).isEqualTo("");
    }

    @Test
    void routeEmptyInputFallsBackToHelp() {
        SlackSlashCommandRouter.RoutedCommand result = router.route("");
        assertThat(result.command()).isEqualTo("help");
        assertThat(result.args()).isEqualTo("");
    }

    @Test
    void routeNullInputFallsBackToHelp() {
        SlackSlashCommandRouter.RoutedCommand result = router.route(null);
        assertThat(result.command()).isEqualTo("help");
        assertThat(result.args()).isEqualTo("");
    }

    @Test
    void routeUnknownCommandFallsBackToHelp() {
        SlackSlashCommandRouter.RoutedCommand result = router.route("deploy production");
        assertThat(result.command()).isEqualTo("help");
        assertThat(result.args()).isEqualTo("deploy production");
    }

    @Test
    void routeCommandIsCaseInsensitive() {
        SlackSlashCommandRouter.RoutedCommand result = router.route("Design something");
        assertThat(result.command()).isEqualTo("design");
        assertThat(result.args()).isEqualTo("something");
    }
}
