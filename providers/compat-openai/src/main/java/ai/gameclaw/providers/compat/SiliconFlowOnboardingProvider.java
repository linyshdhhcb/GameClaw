package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SiliconFlowOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "siliconflow"; }
    @Override public String getLabel() { return "SiliconFlow (硅基流动)"; }
    @Override public String slogan() { return "硅基流动 SiliconCloud 开源模型聚合（OpenAI 兼容，Qwen / DeepSeek / GLM 等）."; }
    @Override public String defaultModel() { return "Qwen/Qwen2.5-72B-Instruct"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api.siliconflow.cn/v1"); }
}
