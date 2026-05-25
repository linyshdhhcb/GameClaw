package ai.gameclaw.providers.deepseek;

import ai.gameclaw.onboarding.AgentOnboardingProvider;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekAgentOnboardingProvider implements AgentOnboardingProvider {

    @Override
    public String getId() {
        return "deepseek";
    }

    @Override
    public String getLabel() {
        return "DeepSeek";
    }

    @Override
    public String slogan() {
        return "Uses DeepSeek API key for DeepSeek-V3 / DeepSeek-R1 as an agent.";
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public String defaultModel() {
        return "deepseek-chat";
    }
}
