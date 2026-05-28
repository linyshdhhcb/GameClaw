package ai.gameclaw.governance.impact;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactReportTest {

    @Test
    void emptyReturnsZeroAffectedFiles() {
        ImpactReport report = ImpactReport.empty("src/A.java");
        assertThat(report.affectedFiles()).isZero();
        assertThat(report.affectedPaths()).isEmpty();
        assertThat(report.targetFile()).isEqualTo("src/A.java");
        assertThat(report.analysisType()).isEqualTo("none");
    }

    @Test
    void ofReturnsCorrectCount() {
        List<String> paths = List.of("src/A.java", "src/B.java");
        ImpactReport report = ImpactReport.of("src/Main.java", paths, "java");
        assertThat(report.affectedFiles()).isEqualTo(2);
        assertThat(report.affectedPaths()).containsExactly("src/A.java", "src/B.java");
        assertThat(report.targetFile()).isEqualTo("src/Main.java");
        assertThat(report.analysisType()).isEqualTo("java");
    }

    @Test
    void ofWithEmptyPaths() {
        ImpactReport report = ImpactReport.of("src/X.java", List.of(), "generic");
        assertThat(report.affectedFiles()).isZero();
        assertThat(report.affectedPaths()).isEmpty();
    }
}
