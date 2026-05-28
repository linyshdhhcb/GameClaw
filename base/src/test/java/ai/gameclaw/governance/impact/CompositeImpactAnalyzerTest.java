package ai.gameclaw.governance.impact;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeImpactAnalyzerTest {

    @Test
    void delegatesToSupportedAnalyzer() {
        ImpactAnalyzer javaAnalyzer = mock(ImpactAnalyzer.class);
        ImpactAnalyzer genericAnalyzer = mock(ImpactAnalyzer.class);

        Path javaFile = Path.of("Service.java");
        when(javaAnalyzer.supports(javaFile)).thenReturn(true);
        when(javaAnalyzer.analyze(javaFile, Path.of("/project")))
                .thenReturn(ImpactReport.of("Service.java", List.of("A.java", "B.java", "C.java"), "java"));

        when(genericAnalyzer.supports(javaFile)).thenReturn(false);

        CompositeImpactAnalyzer composite = new CompositeImpactAnalyzer(List.of(javaAnalyzer, genericAnalyzer));
        ImpactReport report = composite.analyze(javaFile, Path.of("/project"));

        assertThat(report.affectedFiles()).isEqualTo(3);
    }

    @Test
    void returnsEmptyWhenNoAnalyzerSupports() {
        ImpactAnalyzer analyzer1 = mock(ImpactAnalyzer.class);
        ImpactAnalyzer analyzer2 = mock(ImpactAnalyzer.class);

        Path unknownFile = Path.of("data.csv");
        when(analyzer1.supports(unknownFile)).thenReturn(false);
        when(analyzer2.supports(unknownFile)).thenReturn(false);

        CompositeImpactAnalyzer composite = new CompositeImpactAnalyzer(List.of(analyzer1, analyzer2));
        ImpactReport report = composite.analyze(unknownFile, Path.of("/project"));

        assertThat(report.affectedFiles()).isZero();
    }

    @Test
    void prefersFirstMatchingAnalyzer() {
        ImpactAnalyzer first = mock(ImpactAnalyzer.class);
        ImpactAnalyzer second = mock(ImpactAnalyzer.class);

        Path file = Path.of("Config.cs");
        when(first.supports(file)).thenReturn(true);
        when(first.analyze(file, Path.of("/project")))
                .thenReturn(ImpactReport.of("Config.cs", List.of("Ref.cs"), "generic"));

        when(second.supports(file)).thenReturn(true);
        when(second.analyze(file, Path.of("/project")))
                .thenReturn(ImpactReport.of("Config.cs", List.of("A.cs", "B.cs", "C.cs", "D.cs", "E.cs"), "generic"));

        CompositeImpactAnalyzer composite = new CompositeImpactAnalyzer(List.of(first, second));
        ImpactReport report = composite.analyze(file, Path.of("/project"));

        assertThat(report.affectedFiles()).isEqualTo(1);
    }
}
