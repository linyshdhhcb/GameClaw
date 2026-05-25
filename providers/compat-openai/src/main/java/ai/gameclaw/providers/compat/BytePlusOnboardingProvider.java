package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BytePlusOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "byteplus"; }
    @Override public String getLabel() { return "BytePlus ModelArk"; }
    @Override public String slogan() { return "BytePlus 海外 ModelArk Skylark / Doubao 模型（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "skylark-pro-32k"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://ark.ap-southeast.bytepluses.com/api/v3"); }
}
