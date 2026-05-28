package ai.gameclaw.governance.impact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@EnableConfigurationProperties(ImpactThresholds.class)
public class CompositeImpactAnalyzer implements ImpactAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(CompositeImpactAnalyzer.class);

    private final List<ImpactAnalyzer> analyzers;

    public CompositeImpactAnalyzer(List<ImpactAnalyzer> analyzers) {
        this.analyzers = analyzers;
    }

    @Override
    public ImpactReport analyze(Path targetFile, Path projectRoot) {
        for (ImpactAnalyzer analyzer : analyzers) {
            if (analyzer.supports(targetFile) && !(analyzer instanceof CompositeImpactAnalyzer)) {
                log.debug("[Impact:Composite] Using {} for {}", analyzer.getClass().getSimpleName(), targetFile);
                return analyzer.analyze(targetFile, projectRoot);
            }
        }
        return ImpactReport.empty(targetFile.toString());
    }

    @Override
    public boolean supports(Path file) {
        return true;
    }
}
