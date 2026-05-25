package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GitHubCopilotOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "github-copilot"; }
    @Override public String getLabel() { return "GitHub Copilot"; }
    @Override public String slogan() { return "GitHub Models / Copilot via OpenAI-compatible API (requires GitHub PAT)."; }
    @Override public String defaultModel() { return "gpt-4o"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://models.inference.ai.azure.com"); }
}
