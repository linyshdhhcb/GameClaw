package ai.gameclaw.governance.rollback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FilesystemRestorer {

    private static final Logger log = LoggerFactory.getLogger(FilesystemRestorer.class);

    public void restore(String filePath, String originalContent) {
        try {
            Path path = Path.of(filePath).normalize();
            Files.writeString(path, originalContent);
            log.info("[FilesystemRestorer] Restored file: {}", path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore file: " + filePath, e);
        }
    }
}
