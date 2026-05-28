package ai.gameclaw.governance.impact;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gameclaw.governance.impact")
public class ImpactThresholds {
    private int highImpactFileCount = 10;
    private int mediumImpactFileCount = 5;
    private int maxScanFiles = 1000;

    public int getHighImpactFileCount() { return highImpactFileCount; }
    public void setHighImpactFileCount(int v) { this.highImpactFileCount = v; }
    public int getMediumImpactFileCount() { return mediumImpactFileCount; }
    public void setMediumImpactFileCount(int v) { this.mediumImpactFileCount = v; }
    public int getMaxScanFiles() { return maxScanFiles; }
    public void setMaxScanFiles(int v) { this.maxScanFiles = v; }
}
