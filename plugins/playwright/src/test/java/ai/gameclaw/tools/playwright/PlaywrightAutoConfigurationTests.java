package ai.gameclaw.tools.playwright;

import ai.gameclaw.tools.AutoDiscoveredTool;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PlaywrightAutoConfigurationTests {

    @Test
    void playwrightBeanIsCreatedWhenEnabledPropertyIsTrue() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlaywrightAutoConfiguration.class))
                .withPropertyValues("agent.tools.playwright.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(AutoDiscoveredTool.class);
                    Map<String, AutoDiscoveredTool> autoDiscoveredTools = context.getBeansOfType(AutoDiscoveredTool.class);
                    Assertions.assertThat(autoDiscoveredTools)
                            .hasSize(1)
                            .containsKey("autoDiscoveredPlaywrightBrowserTool");
                });
    }

    @Test
    void playwrightBeanIsAbsentWhenEnabledPropertyIsMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlaywrightAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(AutoDiscoveredTool.class));
    }

    @Test
    void playwrightBeanIsAbsentWhenEnabledPropertyIsFalse() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlaywrightAutoConfiguration.class))
                .withPropertyValues("agent.tools.playwright.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AutoDiscoveredTool.class));
    }
}
