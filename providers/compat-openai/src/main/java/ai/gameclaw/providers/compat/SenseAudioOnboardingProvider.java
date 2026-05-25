package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SenseAudioOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "senseaudio"; }
    @Override public String getLabel() { return "SenseNova (商汤)"; }
    @Override public String slogan() { return "商汤日日新 SenseNova / SenseAudio 音频大模型（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "SenseChat-5"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api.sensenova.cn/compatible-mode/v1"); }
}
