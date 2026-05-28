package ai.gameclaw.governance.impact;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
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
public class JavaImpactAnalyzer implements ImpactAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(JavaImpactAnalyzer.class);
    private static final int MAX_SCAN_FILES = 1000;

    @Override
    public ImpactReport analyze(Path targetFile, Path projectRoot) {
        List<String> classNames = extractClassNames(targetFile);
        if (classNames.isEmpty()) {
            return ImpactReport.empty(targetFile.toString());
        }

        List<String> callers = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectRoot, FileVisitOption.FOLLOW_LINKS)) {
            List<Path> javaFiles = walk
                    .filter(p -> !isExcludedDirectory(p))
                    .filter(p -> p.toString().endsWith(".java"))
                    .limit(MAX_SCAN_FILES)
                    .toList();

            for (Path javaFile : javaFiles) {
                if (javaFile.equals(targetFile)) {
                    continue;
                }
                if (referencesAnyClass(javaFile, classNames)) {
                    callers.add(javaFile.toString());
                }
            }
        } catch (IOException e) {
            log.warn("[Impact:Java] Failed to scan project root: {}", e.getMessage());
        }

        return ImpactReport.of(targetFile.toString(), callers, "java");
    }

    @Override
    public boolean supports(Path file) {
        return file.toString().endsWith(".java");
    }

    private List<String> extractClassNames(Path targetFile) {
        List<String> names = new ArrayList<>();
        try {
            var cu = StaticJavaParser.parse(targetFile);
            for (TypeDeclaration<?> type : cu.findAll(TypeDeclaration.class)) {
                if (type.isPublic() && type instanceof ClassOrInterfaceDeclaration) {
                    names.add(type.getNameAsString());
                }
            }
        } catch (Exception e) {
            log.warn("[Impact:Java] Failed to parse {}: {}", targetFile, e.getMessage());
        }
        return names;
    }

    private boolean referencesAnyClass(Path javaFile, List<String> classNames) {
        try {
            String content = Files.readString(javaFile);
            for (String className : classNames) {
                if (content.contains(className)) {
                    return true;
                }
            }
        } catch (IOException e) {
            log.debug("[Impact:Java] Failed to read {}: {}", javaFile, e.getMessage());
        }
        return false;
    }

    private boolean isExcludedDirectory(Path path) {
        String str = path.toString();
        return str.contains("target") || str.contains(".git");
    }
}
