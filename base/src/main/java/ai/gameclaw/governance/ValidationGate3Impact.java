package ai.gameclaw.governance;

import ai.gameclaw.governance.impact.CompositeImpactAnalyzer;
import ai.gameclaw.governance.impact.ImpactReport;
import ai.gameclaw.governance.impact.ImpactRiskUpgrader;
import ai.gameclaw.governance.impact.ImpactThresholds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Order(3)
@ConditionalOnBean(CompositeImpactAnalyzer.class)
public class ValidationGate3Impact implements ValidationGate {

    private static final Logger log = LoggerFactory.getLogger(ValidationGate3Impact.class);

    private final CompositeImpactAnalyzer analyzer;
    private final ImpactRiskUpgrader riskUpgrader;
    private final ImpactThresholds thresholds;

    public ValidationGate3Impact(CompositeImpactAnalyzer analyzer,
                                 ImpactRiskUpgrader riskUpgrader,
                                 ImpactThresholds thresholds) {
        this.analyzer = analyzer;
        this.riskUpgrader = riskUpgrader;
        this.thresholds = thresholds;
    }

    @Override
    public String name() {
        return "impact";
    }

    @Override
    public ValidationResult validate(Object output, Class<?> expectedType) {
        Path targetFile = extractFilePath(output);
        if (targetFile == null) {
            return ValidationResult.ok(output);
        }

        Path workspace = Path.of(".").toAbsolutePath().normalize();
        ImpactReport report = analyzer.analyze(targetFile, workspace);

        log.debug("[Gate3:Impact] {} files affected by {}", report.affectedFiles(), targetFile);

        if (report.affectedFiles() >= thresholds.getHighImpactFileCount()) {
            List<String> violations = new ArrayList<>(report.affectedPaths());
            return ValidationResult.fail("IMPACT_HIGH_RISK",
                    "Impact analysis: " + report.affectedFiles() + " files affected, requires manual approval",
                    violations);
        }

        return ValidationResult.ok(output);
    }

    @SuppressWarnings("unchecked")
    private Path extractFilePath(Object output) {
        if (output instanceof Map<?, ?> map) {
            Object value = map.get("targetFile");
            if (value == null) value = map.get("file");
            if (value == null) value = map.get("path");
            if (value != null) {
                return Path.of(value.toString());
            }
        }
        if (output instanceof String s) {
            try {
                Path p = Path.of(s);
                if (p.getFileName() != null && s.contains(".") || s.contains("/") || s.contains("\\")) {
                    return p;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
