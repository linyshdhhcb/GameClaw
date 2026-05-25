package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class XAIOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "xai"; }
    @Override public String getLabel() { return "xAI Grok"; }
    @Override public String slogan() { return "xAI Grok-4 / Grok-Code via OpenAI-compatible API."; }
    @Override public String defaultModel() { return "grok-4-latest"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api.x.ai/v1"); }
}
