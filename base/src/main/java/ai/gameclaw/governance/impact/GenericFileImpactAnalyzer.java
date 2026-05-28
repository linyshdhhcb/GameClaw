package ai.gameclaw.governance.impact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class GenericFileImpactAnalyzer implements ImpactAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(GenericFileImpactAnalyzer.class);
    private static final int MAX_SCAN_FILES = 1000;
    private static final List<String> TEXT_EXTENSIONS = List.of(
            ".cs", ".gd", ".py", ".js", ".ts", ".lua", ".rb", ".go", ".rs", ".cpp", ".c", ".h", ".hpp"
    );

    @Override
    public ImpactReport analyze(Path targetFile, Path projectRoot) {
        String fileName = extractBaseName(targetFile);
        String extension = extractExtension(targetFile);
        if (fileName.isEmpty()) {
            return ImpactReport.empty(targetFile.toString());
        }

        List<String> searchTerms = buildSearchTerms(fileName, extension);
        List<String> affected = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(projectRoot, FileVisitOption.FOLLOW_LINKS)) {
            List<Path> candidateFiles = walk
                    .filter(p -> !isExcludedDirectory(p))
                    .filter(p -> !p.equals(targetFile))
                    .filter(p -> isTextFile(p))
                    .limit(MAX_SCAN_FILES)
                    .toList();

            for (Path candidate : candidateFiles) {
                if (containsAnyTerm(candidate, searchTerms)) {
                    affected.add(candidate.toString());
                }
            }
        } catch (IOException e) {
            log.warn("[Impact:Generic] Failed to scan project root: {}", e.getMessage());
        }

        return ImpactReport.of(targetFile.toString(), affected, "generic");
    }

    @Override
    public boolean supports(Path file) {
        String name = file.toString();
        return TEXT_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private List<String> buildSearchTerms(String fileName, String extension) {
        List<String> terms = new ArrayList<>();
        terms.add(fileName);

        if (".cs".equals(extension)) {
            terms.add(fileName);
        } else if (".gd".equals(extension)) {
            terms.add("preload(\"" + fileName);
            terms.add("load(\"" + fileName);
        }

        return terms;
    }

    private boolean containsAnyTerm(Path file, List<String> searchTerms) {
        try {
            String content = Files.readString(file);
            for (String term : searchTerms) {
                if (content.contains(term)) {
                    return true;
                }
            }
        } catch (IOException e) {
            log.debug("[Impact:Generic] Failed to read {}: {}", file, e.getMessage());
        }
        return false;
    }

    private String extractBaseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String extractExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot) : "";
    }

    private boolean isTextFile(Path path) {
        String name = path.toString();
        return TEXT_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private boolean isExcludedDirectory(Path path) {
        String str = path.toString();
        return str.contains("target") || str.contains(".git");
    }
}
