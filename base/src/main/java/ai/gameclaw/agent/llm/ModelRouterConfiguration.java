package ai.gameclaw.agent.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ModelRouterProperties.class)
public class ModelRouterConfiguration {
}
