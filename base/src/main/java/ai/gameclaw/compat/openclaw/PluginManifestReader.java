package ai.gameclaw.compat.openclaw;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class PluginManifestReader {

    private static final Logger log = LoggerFactory.getLogger(PluginManifestReader.class);
    private static final String MANIFEST_ENTRY = "plugin.json";

    private final ObjectMapper objectMapper;

    public PluginManifestReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PluginManifest readFromJar(Path pluginJar) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(pluginJar, getClass().getClassLoader())) {
            Path manifestPath = fs.getPath(MANIFEST_ENTRY);
            if (Files.notExists(manifestPath)) {
                throw new IOException("plugin.json not found in " + pluginJar);
            }
            String json = Files.readString(manifestPath);
            return objectMapper.readValue(json, PluginManifest.class);
        }
    }

    public PluginManifest readFromDirectory(Path pluginDir) throws IOException {
        Path manifestPath = pluginDir.resolve(MANIFEST_ENTRY);
        if (Files.notExists(manifestPath)) {
            throw new IOException("plugin.json not found in " + pluginDir);
        }
        String json = Files.readString(manifestPath);
        return objectMapper.readValue(json, PluginManifest.class);
    }
}
