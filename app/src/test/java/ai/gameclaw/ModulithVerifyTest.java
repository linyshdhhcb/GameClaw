package ai.gameclaw;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ModulithVerifyTest {

    @Test
    void verifyModulithModules() {
        var modules = ApplicationModules.of(GameClawApplication.class);

        assertThat(modules).isNotNull();
        assertThat(modules.stream().toList()).isNotEmpty();

        modules.forEach(module -> {
            assertThat(module.getDisplayName()).isNotBlank();
        });
    }

    @Test
    void verifyNoModuleViolations() {
        var modules = ApplicationModules.of(GameClawApplication.class);

        modules.verify();
    }
}
