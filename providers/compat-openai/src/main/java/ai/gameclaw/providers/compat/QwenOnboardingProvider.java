package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class QwenOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "qwen"; }
    @Override public String getLabel() { return "Qwen (DashScope)"; }
    @Override public String slogan() { return "阿里通义千问 DashScope / Qwen-Max / Qwen-Plus 系列（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "qwen-max-latest"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://dashscope.aliyuncs.com/compatible-mode/v1"); }
}
