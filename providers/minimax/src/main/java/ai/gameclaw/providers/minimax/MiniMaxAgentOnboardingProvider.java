package ai.gameclaw.providers.minimax;

import ai.gameclaw.onboarding.AgentOnboardingProvider;
import org.springframework.stereotype.Component;

@Component
public class MiniMaxAgentOnboardingProvider implements AgentOnboardingProvider {

    @Override
    public String getId() {
        return "minimax";
    }

    @Override
    public String getLabel() {
        return "MiniMax";
    }

    @Override
    public String slogan() {
        return "Uses MiniMax API key for MiniMax-Text / abab as an agent.";
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public String defaultModel() {
        return "MiniMax-Text-01";
    }
}
