package ai.gameclaw.tools.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ApiHallucinationDetector {

    private static final Logger log = LoggerFactory.getLogger(ApiHallucinationDetector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern CSHARP_API_PATTERN = Pattern.compile(
            "\\b(UnityEngine|UnityEditor|System)\\.([A-Za-z0-9_.]+)\\b"
    );
    private static final Pattern CPP_API_PATTERN = Pattern.compile(
            "\\b(U[A-Z][A-Za-z0-9_]+|A[A-Z][A-Za-z0-9_]+|F[A-Z][A-Za-z0-9_]+|GEngine|GetWorld)\\b"
    );
    private static final Pattern GDSCRIPT_API_PATTERN = Pattern.compile(
            "\\b(get_node|get_tree|get_parent|queue_free|add_child|remove_child|connect|disconnect|" +
                    "emit_signal|call_deferred|is_instance_valid|ResourceLoader|SceneTree|Engine|Input|" +
                    "InputMap|PhysicsServer|RenderingServer|AudioServer|NavigationServer|" +
                    "TranslationServer|DisplayServer|OS|ProjectSettings|Time|JSON|DirAccess|" +
                    "FileAccess|RegEx|StreamPeer|TCP|UDP|WebSocket|HTTPClient|HTTPRequest)\\b"
    );

    private final Map<Engine, Set<String>> apiIndex = new ConcurrentHashMap<>();
    private final Map<Engine, List<ApiEntry>> apiEntries = new ConcurrentHashMap<>();
    private final Path skillsDir;
    private final MeterRegistry meterRegistry;

    public ApiHallucinationDetector(@Value("${agent.workspace:}") Resource workspace,
                                     @Autowired(required = false) MeterRegistry meterRegistry) throws IOException {
        this.skillsDir = workspace.getFilePath().resolve("game-skills");
        this.meterRegistry = meterRegistry;
        Files.createDirectories(skillsDir);
        loadAllIndexes();
    }

    public DetectionResult detect(Engine engine, String code) {
        Set<String> usedApis = extractApis(engine, code);
        Set<String> knownApis = apiIndex.getOrDefault(engine, Set.of());

        Set<String> unknown = new LinkedHashSet<>();
        for (String api : usedApis) {
            if (!isKnownApi(knownApis, api)) {
                unknown.add(api);
            }
        }

        if (!unknown.isEmpty()) {
            log.warn("[HallucinationDetector] Engine={}, unknown APIs: {}", engine, unknown);
            if (meterRegistry != null) {
                Counter.builder("hallucination_count")
                        .tag("engine", engine.name())
                        .register(meterRegistry)
                        .increment(unknown.size());
            }
        }

        return new DetectionResult(unknown, usedApis.size());
    }

    public String queryApi(Engine engine, String query) {
        List<ApiEntry> entries = apiEntries.getOrDefault(engine, List.of());
        if (entries.isEmpty()) {
            return "未找到 " + engine.getDisplayName() + " 的API索引，请先添加API Skills包";
        }

        String lowerQuery = query.toLowerCase();
        List<ApiEntry> matches = new ArrayList<>();
        for (ApiEntry entry : entries) {
            String fqnLower = entry.fqn.toLowerCase();
            if (fqnLower.contains(lowerQuery) || lowerQuery.contains(entry.kind.toLowerCase())) {
                matches.add(entry);
            }
        }

        if (matches.isEmpty()) {
            String[] words = lowerQuery.split("\\s+");
            for (ApiEntry entry : entries) {
                for (String word : words) {
                    if (word.length() > 2 && entry.fqn.toLowerCase().contains(word)) {
                        matches.add(entry);
                        break;
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            return "未找到匹配 '" + query + "' 的API，请尝试更具体的关键词";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(engine.getDisplayName()).append(" API 查询结果 (").append(matches.size()).append(" 项):\n\n");
        int limit = Math.min(matches.size(), 10);
        for (int i = 0; i < limit; i++) {
            ApiEntry e = matches.get(i);
            sb.append("- ").append(e.fqn).append(" (").append(e.kind).append(")");
            if (e.since != null) sb.append(" [since ").append(e.since).append("]");
            if (e.docUrl != null) sb.append("\n  文档: ").append(e.docUrl);
            sb.append("\n");
        }
        if (matches.size() > limit) {
            sb.append("\n...还有 ").append(matches.size() - limit).append(" 项结果");
        }
        return sb.toString();
    }

    private Set<String> extractApis(Engine engine, String code) {
        Set<String> apis = new LinkedHashSet<>();
        Pattern pattern = switch (engine) {
            case UNITY -> CSHARP_API_PATTERN;
            case UNREAL -> CPP_API_PATTERN;
            case GODOT -> GDSCRIPT_API_PATTERN;
        };
        Matcher matcher = pattern.matcher(code);
        while (matcher.find()) {
            apis.add(matcher.group());
        }
        return apis;
    }

    private boolean isKnownApi(Set<String> knownApis, String api) {
        if (knownApis.contains(api)) return true;
        for (String known : knownApis) {
            if (api.startsWith(known) || known.startsWith(api)) return true;
        }
        return false;
    }

    private void loadAllIndexes() {
        for (Engine engine : Engine.values()) {
            String dirName = engine.name().toLowerCase();
            Path engineDir = skillsDir.resolve(dirName);
            if (!Files.isDirectory(engineDir)) continue;

            Set<String> fqns = new HashSet<>();
            List<ApiEntry> entries = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(engineDir, "*api-index*.json")) {
                for (Path indexFile : stream) {
                    loadIndexFile(indexFile, fqns, entries);
                }
            } catch (IOException e) {
                log.warn("[HallucinationDetector] Failed to load index for {}: {}", engine, e.getMessage());
            }

            apiIndex.put(engine, fqns);
            apiEntries.put(engine, entries);
            log.info("[HallucinationDetector] Loaded {} APIs for {}", fqns.size(), engine);
        }
    }

    private void loadIndexFile(Path file, Set<String> fqns, List<ApiEntry> entries) throws IOException {
        JsonNode root = MAPPER.readTree(Files.readAllBytes(file));
        JsonNode apis = root.get("apis");
        if (apis == null || !apis.isArray()) return;

        for (JsonNode api : apis) {
            String fqn = api.path("fqn").asText("");
            if (!fqn.isEmpty()) {
                fqns.add(fqn);
                entries.add(new ApiEntry(
                        fqn,
                        api.path("kind").asText("method"),
                        api.path("since").asText(null),
                        api.path("doc-url").asText(null)
                ));
            }
        }
    }

    public record DetectionResult(Set<String> unknownApis, int totalApis) {}
    record ApiEntry(String fqn, String kind, String since, String docUrl) {}
}
