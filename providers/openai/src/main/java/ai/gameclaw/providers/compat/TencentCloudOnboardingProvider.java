package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TencentCloudOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "tencent-cloud"; }
    @Override public String getLabel() { return "Tencent Cloud (混元)"; }
    @Override public String slogan() { return "腾讯云混元 Hunyuan / 知识引擎大模型（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "hunyuan-turbos-latest"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api.hunyuan.cloud.tencent.com/v1"); }
}
