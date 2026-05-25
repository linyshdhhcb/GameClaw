package ai.gameclaw.channels.feishu;

import ai.gameclaw.configuration.ConfigurationManager;
import ai.gameclaw.onboarding.OnboardingProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(52)
public class FeishuOnboardingProvider implements OnboardingProvider {

    @Override
    public String getStepId() {
        return "feishu";
    }

    @Override
    public String getStepTitle() {
        return "Feishu Bot";
    }

    @Override
    public String getTemplatePath() {
        return "onboarding/steps/feishu";
    }

    @Override
    public void prepareModel(Map<String, Object> session, Map<String, Object> model) {
        model.put("feishuAppId", session.getOrDefault("feishuAppId", ""));
        model.put("feishuAppSecret", session.getOrDefault("feishuAppSecret", ""));
        model.put("feishuVerificationToken", session.getOrDefault("feishuVerificationToken", ""));
    }

    @Override
    public String processStep(Map<String, String> formParams, Map<String, Object> session) {
        String appId = formParams.get("feishuAppId");
        String appSecret = formParams.get("feishuAppSecret");
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            return "App ID and App Secret are required";
        }
        session.put("feishuAppId", appId);
        session.put("feishuAppSecret", appSecret);
        session.put("feishuVerificationToken", formParams.getOrDefault("feishuVerificationToken", ""));
        return null;
    }

    @Override
    public void saveConfiguration(Map<String, Object> session, ConfigurationManager configurationManager) throws Exception {
        String appId = (String) session.get("feishuAppId");
        String appSecret = (String) session.get("feishuAppSecret");
        String verificationToken = (String) session.get("feishuVerificationToken");
        if (appId == null) return;

        configurationManager.updateProperty("agent.channels.feishu.app-id", appId);
        configurationManager.updateProperty("agent.channels.feishu.app-secret", appSecret);
        if (verificationToken != null && !verificationToken.isBlank()) {
            configurationManager.updateProperty("agent.channels.feishu.verification-token", verificationToken);
        }
    }

    @Override
    public boolean isOptional() {
        return true;
    }
}
