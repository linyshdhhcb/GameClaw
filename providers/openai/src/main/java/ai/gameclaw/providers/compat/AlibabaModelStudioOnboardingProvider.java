package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AlibabaModelStudioOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "alibaba-model-studio"; }
    @Override public String getLabel() { return "Alibaba Model Studio"; }
    @Override public String slogan() { return "阿里云百炼 Model Studio 海外站（OpenAI 兼容，Qwen / Llama 等）."; }
    @Override public String defaultModel() { return "qwen-max-latest"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://dashscope-intl.aliyuncs.com/compatible-mode/v1"); }
}
