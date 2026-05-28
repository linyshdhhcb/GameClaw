package ai.gameclaw.channels.slack;

import ai.gameclaw.configuration.ConfigurationManager;
import ai.gameclaw.onboarding.OnboardingProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(53)
public class SlackOnboardingProvider implements OnboardingProvider {

    static final String SESSION_BOT_TOKEN = "onboarding.slack.bot-token";
    static final String SESSION_APP_TOKEN = "onboarding.slack.app-token";
    static final String SESSION_ALLOWED_USER = "onboarding.slack.allowed-user";

    private static final String BOT_TOKEN_PROPERTY = "agent.channels.slack.token";
    private static final String APP_TOKEN_PROPERTY = "agent.channels.slack.app-token";
    private static final String ALLOWED_USER_PROPERTY = "agent.channels.slack.allowed-user";

    @Override
    public String getStepId() {
        return "slack";
    }

    @Override
    public String getStepTitle() {
        return "Slack";
    }

    @Override
    public String getTemplatePath() {
        return "onboarding/steps/slack";
    }

    @Override
    public void prepareModel(Map<String, Object> session, Map<String, Object> model) {
        model.put("slackBotToken", session.getOrDefault(SESSION_BOT_TOKEN, ""));
        model.put("slackAppToken", session.getOrDefault(SESSION_APP_TOKEN, ""));
        model.put("slackAllowedUser", session.getOrDefault(SESSION_ALLOWED_USER, ""));
    }

    @Override
    public String processStep(Map<String, String> formParams, Map<String, Object> session) {
        String botToken = formParams.getOrDefault("slackBotToken", "").trim();
        String appToken = formParams.getOrDefault("slackAppToken", "").trim();
        if (!botToken.startsWith("xoxb-")) {
            return "Bot Token must start with xoxb-";
        }
        if (!appToken.startsWith("xapp-")) {
            return "App Token must start with xapp-";
        }
        session.put(SESSION_BOT_TOKEN, botToken);
        session.put(SESSION_APP_TOKEN, appToken);
        session.put(SESSION_ALLOWED_USER, formParams.getOrDefault("slackAllowedUser", "").trim());
        return null;
    }

    @Override
    public void saveConfiguration(Map<String, Object> session, ConfigurationManager configurationManager) throws Exception {
        String botToken = (String) session.get(SESSION_BOT_TOKEN);
        String appToken = (String) session.get(SESSION_APP_TOKEN);
        String allowedUser = (String) session.get(SESSION_ALLOWED_USER);
        if (botToken == null) return;

        configurationManager.updateProperty(BOT_TOKEN_PROPERTY, botToken);
        configurationManager.updateProperty(APP_TOKEN_PROPERTY, appToken);
        if (allowedUser != null && !allowedUser.isBlank()) {
            configurationManager.updateProperty(ALLOWED_USER_PROPERTY, allowedUser);
        }
    }

    @Override
    public boolean isOptional() {
        return true;
    }
}
