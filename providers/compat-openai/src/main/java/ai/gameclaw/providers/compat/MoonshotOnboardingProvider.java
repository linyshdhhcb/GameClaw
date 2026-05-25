package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MoonshotOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "moonshot"; }
    @Override public String getLabel() { return "Moonshot (Kimi)"; }
    @Override public String slogan() { return "月之暗面 Kimi / Moonshot-V2 长上下文模型（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "moonshot-v1-128k"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api.moonshot.cn/v1"); }
}
