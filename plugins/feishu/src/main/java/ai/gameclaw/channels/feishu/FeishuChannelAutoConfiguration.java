package ai.gameclaw.channels.feishu;

import ai.gameclaw.agent.Agent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

@AutoConfiguration
@ConditionalOnProperty(prefix = "agent.channels.feishu", name = {"app-id", "app-secret"})
public class FeishuChannelAutoConfiguration {

    @Bean
    public FeishuApiClient feishuApiClient(
            @org.springframework.beans.factory.annotation.Value("${agent.channels.feishu.app-id}") String appId,
            @org.springframework.beans.factory.annotation.Value("${agent.channels.feishu.app-secret}") String appSecret) {
        return new FeishuApiClient(appId, appSecret);
    }

    @Bean
    public FeishuTenantRegistry feishuTenantRegistry(JdbcTemplate jdbc) {
        return new FeishuTenantRegistry(jdbc);
    }

    @Bean
    public NonceCache nonceCache() {
        return new NonceCache();
    }

    @Bean
    public SlashCommandRouter slashCommandRouter() {
        return new SlashCommandRouter(Map.of(
                "/design", (args, event) -> "[Design Tool] 收到策划指令: " + args,
                "/query", (args, event) -> "[Query Tool] 收到查询指令: " + args,
                "/review", (args, event) -> "[Review Tool] 收到审查指令: " + args
        ));
    }

    @Bean
    public FeishuChannel feishuChannel(ai.gameclaw.channels.ChannelRegistry channelRegistry,
                                       Agent agent,
                                       FeishuApiClient apiClient,
                                       FeishuTenantRegistry tenantRegistry,
                                       SlashCommandRouter slashCommandRouter) {
        return new FeishuChannel(channelRegistry, agent, apiClient, tenantRegistry, slashCommandRouter);
    }

    @Bean
    public FeishuEventController feishuEventController(
            @org.springframework.beans.factory.annotation.Value("${agent.channels.feishu.verification-token:}") String verificationToken,
            @org.springframework.beans.factory.annotation.Value("${agent.channels.feishu.encrypt-key:}") String encryptKey,
            NonceCache nonceCache,
            FeishuChannel feishuChannel) {
        return new FeishuEventController(verificationToken, encryptKey, nonceCache, feishuChannel);
    }

    @Bean
    public FeishuOnboardingProvider feishuOnboardingProvider() {
        return new FeishuOnboardingProvider();
    }
}
