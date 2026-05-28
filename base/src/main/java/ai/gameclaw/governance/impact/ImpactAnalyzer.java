package ai.gameclaw.governance.impact;

import java.nio.file.Path;

public interface ImpactAnalyzer {
    ImpactReport analyze(Path targetFile, Path projectRoot);
    boolean supports(Path file);
}
