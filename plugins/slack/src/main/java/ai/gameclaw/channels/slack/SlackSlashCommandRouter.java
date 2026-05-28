package ai.gameclaw.channels.slack;

import java.util.Set;

public class SlackSlashCommandRouter {

    private static final Set<String> VALID_COMMANDS = Set.of("design", "query", "code", "test", "help");

    public RoutedCommand route(String text) {
        if (text == null || text.isBlank()) {
            return new RoutedCommand("help", "");
        }
        String[] parts = text.trim().split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        if (!VALID_COMMANDS.contains(command)) {
            return new RoutedCommand("help", text);
        }
        return new RoutedCommand(command, args);
    }

    public record RoutedCommand(String command, String args) {}
}
