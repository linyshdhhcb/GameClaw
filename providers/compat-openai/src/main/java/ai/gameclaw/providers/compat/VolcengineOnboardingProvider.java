package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VolcengineOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "volcengine"; }
    @Override public String getLabel() { return "Volcengine (火山引擎)"; }
    @Override public String slogan() { return "字节火山引擎 Ark / 豆包 Doubao 大模型（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "doubao-1.5-pro-32k"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://ark.cn-beijing.volces.com/api/v3"); }
}
