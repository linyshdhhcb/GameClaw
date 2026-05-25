package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OpenRouterOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "openrouter"; }
    @Override public String getLabel() { return "OpenRouter"; }
    @Override public String slogan() { return "Unified gateway to 300+ LLMs (Claude / GPT / Llama / etc.) via OpenAI-compatible API."; }
    @Override public String defaultModel() { return "anthropic/claude-sonnet-4-6"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://openrouter.ai/api/v1"); }
}
