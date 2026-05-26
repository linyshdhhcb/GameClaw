package ai.gameclaw.skills;

public record ClawHubSearchResult(
        String name,
        String description,
        String latestVersion,
        long downloads
) {
}
