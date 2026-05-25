package ai.gameclaw.providers.compat;

import org.springframework.stereotype.Component;

@Component
public class OpenAIAgentOnboardingProvider extends OpenAICompatibleProvider {

    @Override
    public String getId() {
        return "openai";
    }

    @Override
    public String getLabel() {
        return "OpenAI";
    }

    @Override
    public String slogan() {
        return "Uses OpenAI API key for ChatGPT as an agent.";
    }

    @Override
    public String defaultModel() {
        return "gpt-5.4";
    }
}
