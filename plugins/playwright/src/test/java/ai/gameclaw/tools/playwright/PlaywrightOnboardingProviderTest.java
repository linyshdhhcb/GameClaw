package ai.gameclaw.tools.playwright;

import ai.gameclaw.configuration.ConfigurationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static ai.gameclaw.tools.playwright.PlaywrightOnboardingProvider.SESSION_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaywrightOnboardingProviderTest {

    @Mock
    Environment environment;

    @Mock
    ConfigurationManager configurationManager;

    @Test
    void stepMetadataIsCorrect() {
        PlaywrightOnboardingProvider provider = new PlaywrightOnboardingProvider(environment);

        assertThat(provider.getStepId()).isEqualTo("playwright");
        assertThat(provider.getStepTitle()).isEqualTo("Browser Automation");
        assertThat(provider.getTemplatePath()).isEqualTo("onboarding/steps/playwright");
        assertThat(provider.isOptional()).isTrue();
    }

    @Test
    void processStepStoresEnabledTrueWhenCheckboxPresent() {
        PlaywrightOnboardingProvider provider = new PlaywrightOnboardingProvider(environment);
        Map<String, Object> session = new HashMap<>();

        String result = provider.processStep(Map.of("playwrightEnabled", "on"), session);

        assertThat(result).isNull();
        assertThat(session).containsEntry(SESSION_ENABLED, "true");
    }

    @Test
    void processStepStoresEnabledFalseWhenCheckboxAbsent() {
        PlaywrightOnboardingProvider provider = new PlaywrightOnboardingProvider(environment);
        Map<String, Object> session = new HashMap<>();

        String result = provider.processStep(Map.of(), session);

        assertThat(result).isNull();
        assertThat(session).containsEntry(SESSION_ENABLED, "false");
    }

    @Test
    void prepareModelUsesFallbackFromEnvironment() {
        when(environment.getProperty("agent.tools.playwright.enabled", "false")).thenReturn("true");
        PlaywrightOnboardingProvider provider = new PlaywrightOnboardingProvider(environment);
        Map<String, Object> model = new HashMap<>();

        provider.prepareModel(Map.of(), model);

        assertThat(model).containsEntry("playwrightEnabled", true);
    }

    @Test
    void prepareModelDefaultsToFalse() {
        when(environment.getProperty("agent.tools.playwright.enabled", "false")).thenReturn("false");
        PlaywrightOnboardingProvider provider = new PlaywrightOnboardingProvider(environment);
        Map<String, Object> model = new HashMap<>();

        provider.prepareModel(Map.of(), model);

        assertThat(model).containsEntry("playwrightEnabled", false);
    }

    @Test
    void prepareModelPrefersSessionOverEnvironment() {
        when(environment.getProperty("agent.tools.playwright.enabled", "false")).thenReturn("false");
        PlaywrightOnboardingProvider provider = new PlaywrightOnboardingProvider(environment);
        Map<String, Object> session = Map.of(SESSION_ENABLED, "true");
        Map<String, Object> model = new HashMap<>();

        provider.prepareModel(session, model);

        assertThat(model).containsEntry("playwrightEnabled", true);
    }

    @Test
    void saveConfigurationWritesEnabledTrueWhenEnabled() throws IOException {
        PlaywrightOnboardingProvider provider = new PlaywrightOnboardingProvider(environment);
        Map<String, Object> session = Map.of(SESSION_ENABLED, "true");

        provider.saveConfiguration(session, configurationManager);

        verify(configurationManager).updateProperties(Map.of("agent.tools.playwright.enabled", true));
    }

    @Test
    void saveConfigurationDoesNothingWhenDisabled() throws IOException {
        PlaywrightOnboardingProvider provider = new PlaywrightOnboardingProvider(environment);
        Map<String, Object> session = Map.of(SESSION_ENABLED, "false");

        provider.saveConfiguration(session, configurationManager);

        verifyNoInteractions(configurationManager);
    }

    @Test
    void saveConfigurationDoesNothingWhenAbsentFromSession() throws IOException {
        PlaywrightOnboardingProvider provider = new PlaywrightOnboardingProvider(environment);

        provider.saveConfiguration(Map.of(), configurationManager);

        verifyNoInteractions(configurationManager);
    }
}
