package ai.gameclaw.governance.impact;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactThresholdsTest {

    @Test
    void defaultValues() {
        ImpactThresholds thresholds = new ImpactThresholds();
        assertThat(thresholds.getHighImpactFileCount()).isEqualTo(10);
        assertThat(thresholds.getMediumImpactFileCount()).isEqualTo(5);
        assertThat(thresholds.getMaxScanFiles()).isEqualTo(1000);
    }

    @Test
    void settersWork() {
        ImpactThresholds thresholds = new ImpactThresholds();
        thresholds.setHighImpactFileCount(20);
        thresholds.setMediumImpactFileCount(8);
        thresholds.setMaxScanFiles(500);
        assertThat(thresholds.getHighImpactFileCount()).isEqualTo(20);
        assertThat(thresholds.getMediumImpactFileCount()).isEqualTo(8);
        assertThat(thresholds.getMaxScanFiles()).isEqualTo(500);
    }
}
