package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class XiaomiOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "xiaomi"; }
    @Override public String getLabel() { return "Xiaomi MiLM"; }
    @Override public String slogan() { return "小米 MiLM 大模型开放平台（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "mimo-7b-rl"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api.xiaomi.com/v1"); }
}
