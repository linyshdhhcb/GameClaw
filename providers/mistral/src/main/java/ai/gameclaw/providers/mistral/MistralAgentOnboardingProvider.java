package ai.gameclaw.providers.mistral;

import ai.gameclaw.onboarding.AgentOnboardingProvider;
import org.springframework.stereotype.Component;

@Component
public class MistralAgentOnboardingProvider implements AgentOnboardingProvider {

    @Override
    public String getId() {
        return "mistral-ai";
    }

    @Override
    public String getLabel() {
        return "Mistral AI";
    }

    @Override
    public String slogan() {
        return "Uses Mistral AI API key for Mistral Large / Codestral as an agent.";
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public String defaultModel() {
        return "mistral-large-latest";
    }
}
