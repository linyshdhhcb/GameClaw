package ai.gameclaw.governance.impact;

import ai.gameclaw.security.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactRiskUpgraderTest {

    private ImpactRiskUpgrader upgrader;

    @BeforeEach
    void setUp() {
        ImpactThresholds thresholds = new ImpactThresholds();
        upgrader = new ImpactRiskUpgrader(thresholds);
    }

    @Test
    void noUpgradeWhenBelowThresholds() {
        ImpactReport report = ImpactReport.of("src/A.java", java.util.List.of("1", "2", "3"), "java");
        RiskLevel result = upgrader.upgradeRisk(RiskLevel.L2_SANDBOX_WRITE, report);
        assertThat(result).isEqualTo(RiskLevel.L2_SANDBOX_WRITE);
    }

    @Test
    void upgradeOneLevelWhenMediumImpact() {
        ImpactReport report = ImpactReport.of("src/A.java",
                java.util.stream.IntStream.range(0, 7).mapToObj(i -> "f" + i).toList(), "java");
        RiskLevel result = upgrader.upgradeRisk(RiskLevel.L2_SANDBOX_WRITE, report);
        assertThat(result).isEqualTo(RiskLevel.L3_PROJECT_WRITE);
    }

    @Test
    void upgradeToL5WhenHighImpact() {
        ImpactReport report = ImpactReport.of("src/A.java",
                java.util.stream.IntStream.range(0, 10).mapToObj(i -> "f" + i).toList(), "java");
        RiskLevel result = upgrader.upgradeRisk(RiskLevel.L2_SANDBOX_WRITE, report);
        assertThat(result).isEqualTo(RiskLevel.L5_PRODUCTION);
    }

    @Test
    void noUpgradeWhenAlreadyL5() {
        ImpactReport report = ImpactReport.of("src/A.java",
                java.util.stream.IntStream.range(0, 15).mapToObj(i -> "f" + i).toList(), "java");
        RiskLevel result = upgrader.upgradeRisk(RiskLevel.L5_PRODUCTION, report);
        assertThat(result).isEqualTo(RiskLevel.L5_PRODUCTION);
    }

    @Test
    void mediumImpactCapsAtL5() {
        ImpactReport report = ImpactReport.of("src/A.java",
                java.util.stream.IntStream.range(0, 7).mapToObj(i -> "f" + i).toList(), "java");
        RiskLevel result = upgrader.upgradeRisk(RiskLevel.L4_DB_WRITE, report);
        assertThat(result).isEqualTo(RiskLevel.L5_PRODUCTION);
    }

    @Test
    void zeroAffectedFilesNoUpgrade() {
        ImpactReport report = ImpactReport.of("src/A.java", java.util.List.of(), "java");
        RiskLevel result = upgrader.upgradeRisk(RiskLevel.L2_SANDBOX_WRITE, report);
        assertThat(result).isEqualTo(RiskLevel.L2_SANDBOX_WRITE);
    }
}
