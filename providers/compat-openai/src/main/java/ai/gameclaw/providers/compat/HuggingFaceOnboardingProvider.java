package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HuggingFaceOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "huggingface"; }
    @Override public String getLabel() { return "Hugging Face"; }
    @Override public String slogan() { return "Hugging Face Inference Endpoints / Serverless API (OpenAI-compatible)."; }
    @Override public String defaultModel() { return "meta-llama/Llama-3.3-70B-Instruct"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api-inference.huggingface.co/v1"); }
}
