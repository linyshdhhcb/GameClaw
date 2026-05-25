package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GroqOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "groq"; }
    @Override public String getLabel() { return "Groq"; }
    @Override public String slogan() { return "Ultra-fast LPU inference (Llama / Mixtral / Gemma) via OpenAI-compatible API."; }
    @Override public String defaultModel() { return "llama-3.3-70b-versatile"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api.groq.com/openai/v1"); }
}
