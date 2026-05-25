package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StepFunOnboardingProvider extends OpenAICompatibleProvider {
    @Override public String getId() { return "stepfun"; }
    @Override public String getLabel() { return "StepFun (阶跃星辰)"; }
    @Override public String slogan() { return "阶跃星辰 Step-2 / Step-1V 多模态大模型（OpenAI 兼容）."; }
    @Override public String defaultModel() { return "step-2-16k"; }
    @Override public Optional<String> baseUrl() { return Optional.of("https://api.stepfun.com/v1"); }
}
