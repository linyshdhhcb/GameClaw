package ai.gameclaw.governance.impact;

import java.util.List;

public record ImpactReport(
    int affectedFiles,
    List<String> affectedPaths,
    String targetFile,
    String analysisType
) {
    public static ImpactReport empty(String targetFile) {
        return new ImpactReport(0, List.of(), targetFile, "none");
    }

    public static ImpactReport of(String targetFile, List<String> affectedPaths, String analysisType) {
        return new ImpactReport(affectedPaths.size(), affectedPaths, targetFile, analysisType);
    }
}
