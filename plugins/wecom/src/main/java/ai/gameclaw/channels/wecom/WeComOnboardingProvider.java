package ai.gameclaw.channels.wecom;

import ai.gameclaw.configuration.ConfigurationManager;
import ai.gameclaw.onboarding.OnboardingProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(54)
public class WeComOnboardingProvider implements OnboardingProvider {

    @Override
    public String getStepId() {
        return "wecom";
    }

    @Override
    public String getStepTitle() {
        return "企业微信";
    }

    @Override
    public String getTemplatePath() {
        return "onboarding/steps/wecom";
    }

    @Override
    public void prepareModel(Map<String, Object> session, Map<String, Object> model) {
        model.put("wecomCorpId", session.getOrDefault("wecomCorpId", ""));
        model.put("wecomAgentId", session.getOrDefault("wecomAgentId", ""));
        model.put("wecomSecret", session.getOrDefault("wecomSecret", ""));
        model.put("wecomToken", session.getOrDefault("wecomToken", ""));
        model.put("wecomEncodingAesKey", session.getOrDefault("wecomEncodingAesKey", ""));
    }

    @Override
    public String processStep(Map<String, String> formParams, Map<String, Object> session) {
        String corpId = formParams.get("wecomCorpId");
        String agentId = formParams.get("wecomAgentId");
        String secret = formParams.get("wecomSecret");
        if (corpId == null || corpId.isBlank() || agentId == null || agentId.isBlank()
                || secret == null || secret.isBlank()) {
            return "Corp ID, Agent ID and Secret are required";
        }
        session.put("wecomCorpId", corpId);
        session.put("wecomAgentId", agentId);
        session.put("wecomSecret", secret);
        session.put("wecomToken", formParams.getOrDefault("wecomToken", ""));
        session.put("wecomEncodingAesKey", formParams.getOrDefault("wecomEncodingAesKey", ""));
        return null;
    }

    @Override
    public void saveConfiguration(Map<String, Object> session, ConfigurationManager configurationManager) throws Exception {
        String corpId = (String) session.get("wecomCorpId");
        if (corpId == null) return;

        configurationManager.updateProperty("agent.channels.wecom.corp-id", corpId);
        configurationManager.updateProperty("agent.channels.wecom.agent-id", (String) session.get("wecomAgentId"));
        configurationManager.updateProperty("agent.channels.wecom.secret", (String) session.get("wecomSecret"));

        String token = (String) session.get("wecomToken");
        if (token != null && !token.isBlank()) {
            configurationManager.updateProperty("agent.channels.wecom.token", token);
        }
        String encodingAesKey = (String) session.get("wecomEncodingAesKey");
        if (encodingAesKey != null && !encodingAesKey.isBlank()) {
            configurationManager.updateProperty("agent.channels.wecom.encoding-aes-key", encodingAesKey);
        }
    }

    @Override
    public boolean isOptional() {
        return true;
    }
}
