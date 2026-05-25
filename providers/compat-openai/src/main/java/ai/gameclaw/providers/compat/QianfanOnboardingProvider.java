package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class QianfanOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "qianfan"; }
    @Override public String getLabel() { return "Qianfan (Baidu)"; }
    @Override public String slogan() { return "百度千帆 / ERNIE 系列大模型（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "ernie-4.5-turbo-128k"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://qianfan.baidubce.com/v2"); }
}
