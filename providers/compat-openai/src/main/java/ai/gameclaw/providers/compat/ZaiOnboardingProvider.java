package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ZaiOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "zai"; }
    @Override public String getLabel() { return "Z.AI (智谱 GLM)"; }
    @Override public String slogan() { return "智谱 BigModel GLM-4.5 / GLM-4-Plus 系列（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "glm-4.5"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://open.bigmodel.cn/api/paas/v4"); }
}
