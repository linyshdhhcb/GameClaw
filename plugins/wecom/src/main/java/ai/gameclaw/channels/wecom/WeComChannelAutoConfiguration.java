package ai.gameclaw.channels.wecom;

import ai.gameclaw.agent.Agent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ConditionalOnProperty(prefix = "agent.channels.wecom", name = {"corp-id", "agent-id", "secret", "token", "encoding-aes-key"})
public class WeComChannelAutoConfiguration {

    @Bean
    public WeComApiClient weComApiClient(
            @Value("${agent.channels.wecom.corp-id}") String corpId,
            @Value("${agent.channels.wecom.agent-id}") String agentId,
            @Value("${agent.channels.wecom.secret}") String secret) {
        return new WeComApiClient(corpId, agentId, secret);
    }

    @Bean
    public WeComCrypto weComCrypto(
            @Value("${agent.channels.wecom.token}") String token,
            @Value("${agent.channels.wecom.encoding-aes-key}") String encodingAesKey,
            @Value("${agent.channels.wecom.corp-id}") String corpId) {
        return new WeComCrypto(token, encodingAesKey, corpId);
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public WeComTenantRegistry weComTenantRegistry(JdbcTemplate jdbc) {
        return new WeComTenantRegistry(jdbc);
    }

    @Bean
    public WeComChannel weComChannel(ai.gameclaw.channels.ChannelRegistry channelRegistry,
                                     Agent agent,
                                     WeComApiClient apiClient,
                                     WeComTenantRegistry tenantRegistry) {
        return new WeComChannel(channelRegistry, agent, apiClient, tenantRegistry);
    }

    @Bean
    @Order(54)
    public WeComOnboardingProvider weComOnboardingProvider() {
        return new WeComOnboardingProvider();
    }
}
