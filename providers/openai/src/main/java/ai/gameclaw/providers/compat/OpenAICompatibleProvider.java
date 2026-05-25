package ai.gameclaw.providers.compat;

import ai.gameclaw.onboarding.AgentOnboardingProvider;

import java.util.Optional;

public abstract class OpenAICompatibleProvider implements AgentOnboardingProvider {

    @Override
    public String backendId() {
        return "openai";
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }
}
