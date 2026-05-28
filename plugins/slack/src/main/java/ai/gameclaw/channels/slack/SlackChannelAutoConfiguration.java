package ai.gameclaw.channels.slack;

import ai.gameclaw.agent.Agent;
import ai.gameclaw.channels.ChannelRegistry;
import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ConditionalOnProperty(prefix = "agent.channels.slack", name = {"token", "app-token"})
public class SlackChannelAutoConfiguration {

    @Bean
    public App slackApp(
            @Value("${agent.channels.slack.token}") String botToken,
            @Value("${agent.channels.slack.app-token}") String appToken) {
        App app = new App();
        app.config().setSingleTeamBotToken(botToken);
        return app;
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public SlackTenantRegistry slackTenantRegistry(JdbcTemplate jdbc) {
        return new SlackTenantRegistry(jdbc);
    }

    @Bean
    public SlackChannel slackChannel(ChannelRegistry channelRegistry,
                                     Agent agent,
                                     App slackApp,
                                     SlackTenantRegistry tenantRegistry,
                                     @Value("${agent.channels.slack.allowed-user:}") String allowedUser) {
        return new SlackChannel(channelRegistry, agent, slackApp, tenantRegistry, allowedUser);
    }

    @Bean(destroyMethod = "close")
    public SocketModeApp socketModeApp(App slackApp,
                                       @Value("${agent.channels.slack.app-token}") String appToken) throws Exception {
        SocketModeApp socketModeApp = new SocketModeApp(appToken, slackApp);
        socketModeApp.startAsync();
        return socketModeApp;
    }

    @Bean
    public SlackOnboardingProvider slackOnboardingProvider() {
        return new SlackOnboardingProvider();
    }
}
