package ai.gameclaw.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ConcurrencyArchitectureTest {

    private static JavaClasses gameclawClasses;

    @BeforeAll
    static void importClasses() {
        gameclawClasses = new ClassFileImporter()
                .importPackages("ai.gameclaw");
    }

    @Test
    void noRawStartVirtualThread() {
        noClasses()
                .should().callMethod(Thread.class, "startVirtualThread", Runnable.class)
                .because("禁止裸 Thread.startVirtualThread，应使用 StructuredTaskScope 以保证 ScopedValue 上下文传播")
                .check(gameclawClasses);
    }

    @Test
    void noRawThreadStartInConcurrencyPackage() {
        noClasses()
                .that().resideInAPackage("..concurrency..")
                .should().callMethod(Thread.class, "start")
                .because("concurrency 包禁止直接调用 Thread.start，应使用 StructuredTaskScope")
                .check(gameclawClasses);
    }
}
