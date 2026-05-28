package ai.gameclaw.governance;

import ai.gameclaw.governance.impact.CompositeImpactAnalyzer;
import ai.gameclaw.governance.impact.ImpactReport;
import ai.gameclaw.governance.impact.ImpactRiskUpgrader;
import ai.gameclaw.governance.impact.ImpactThresholds;
import ai.gameclaw.security.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationGate3ImpactTest {

    @Mock
    private CompositeImpactAnalyzer analyzer;

    @Mock
    private ImpactRiskUpgrader riskUpgrader;

    private ImpactThresholds thresholds;

    private ValidationGate3Impact gate;

    @BeforeEach
    void setUp() {
        thresholds = new ImpactThresholds();
        gate = new ValidationGate3Impact(analyzer, riskUpgrader, thresholds);
    }

    @Test
    void nameReturnsImpact() {
        assertThat(gate.name()).isEqualTo("impact");
    }

    @Test
    void passesWhenNoTargetFile() {
        Map<String, Object> output = Map.of("action", "read", "data", "value");
        ValidationResult result = gate.validate(output, Map.class);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void passesWhenLowImpact() {
        Path targetFile = Path.of("/project/src/Service.java");
        Map<String, Object> output = Map.of("targetFile", targetFile.toString());

        when(analyzer.analyze(any(), any())).thenReturn(
                ImpactReport.of("Service.java", List.of("Caller.java"), "java"));

        ValidationResult result = gate.validate(output, Map.class);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void failsWhenHighImpact() {
        Path targetFile = Path.of("/project/src/CoreService.java");
        Map<String, Object> output = Map.of("targetFile", targetFile.toString());

        List<String> affectedPaths = IntStream.range(0, 12).mapToObj(i -> "file" + i + ".java").toList();
        when(analyzer.analyze(any(), any())).thenReturn(
                ImpactReport.of("CoreService.java", affectedPaths, "java"));

        ValidationResult result = gate.validate(output, Map.class);
        assertThat(result.valid()).isFalse();
        assertThat(result.code()).isEqualTo("IMPACT_HIGH_RISK");
    }

    @Test
    void extractsTargetFileFromMap() {
        Path targetFile = Path.of("/project/src/Player.java");
        Map<String, Object> output = Map.of("targetFile", targetFile.toString());

        when(analyzer.analyze(any(), any())).thenReturn(
                ImpactReport.of("Player.java", List.of("Ref.java"), "java"));

        ValidationResult result = gate.validate(output, Map.class);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void extractsTargetFileFromPathKey() {
        Path targetFile = Path.of("/project/src/Enemy.cs");
        Map<String, Object> output = Map.of("path", targetFile.toString());

        when(analyzer.analyze(any(), any())).thenReturn(
                ImpactReport.of("Enemy.cs", List.of("Ref.cs"), "generic"));

        ValidationResult result = gate.validate(output, Map.class);
        assertThat(result.valid()).isTrue();
    }
}
