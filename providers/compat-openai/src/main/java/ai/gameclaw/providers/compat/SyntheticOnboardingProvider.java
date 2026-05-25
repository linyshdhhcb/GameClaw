package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SyntheticOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "synthetic"; }
    @Override public String getLabel() { return "Synthetic"; }
    @Override public String slogan() { return "Synthetic 开源模型托管平台（OpenAI 兼容，Llama / Qwen / DeepSeek 等）."; }
    @Override public String defaultModel() { return "hf:meta-llama/Llama-3.3-70B-Instruct"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api.synthetic.new/v1"); }
}
