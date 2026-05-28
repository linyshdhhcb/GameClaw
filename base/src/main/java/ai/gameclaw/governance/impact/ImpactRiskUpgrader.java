package ai.gameclaw.governance.impact;

import ai.gameclaw.security.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class ImpactRiskUpgrader {

    private final ImpactThresholds thresholds;

    public ImpactRiskUpgrader(ImpactThresholds thresholds) {
        this.thresholds = thresholds;
    }

    public RiskLevel upgradeRisk(RiskLevel currentRisk, ImpactReport report) {
        if (currentRisk == RiskLevel.L5_PRODUCTION) {
            return currentRisk;
        }

        if (report.affectedFiles() >= thresholds.getHighImpactFileCount()) {
            return RiskLevel.L5_PRODUCTION;
        }

        if (report.affectedFiles() >= thresholds.getMediumImpactFileCount()) {
            RiskLevel[] levels = RiskLevel.values();
            int nextOrdinal = Math.min(currentRisk.ordinal() + 1, levels.length - 1);
            return levels[nextOrdinal];
        }

        return currentRisk;
    }
}
