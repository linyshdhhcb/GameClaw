package ai.gameclaw.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class PromptSanitizer {

    private static final Logger log = LoggerFactory.getLogger(PromptSanitizer.class);

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+instructions"),
            Pattern.compile("(?i)forget\\s+(all\\s+)?previous\\s+instructions"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)jailbreak"),
            Pattern.compile("(?i)DAN\\s+mode"),
            Pattern.compile("(?i)developer\\s+mode"),
            Pattern.compile("(?i)\\[INST\\].*\\[/INST\\]", Pattern.DOTALL)
    );

    private static final Pattern SYSTEM_ROLE_INJECTION = Pattern.compile(
            "(?i)(role|system)\\s*:\\s*(system|assistant|admin)", Pattern.DOTALL);

    public String sanitize(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return prompt;
        }
        String sanitized = prompt;
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                log.warn("[PromptSanitizer] Detected potential injection pattern: {}", pattern.pattern());
                sanitized = pattern.matcher(sanitized).replaceAll("[FILTERED]");
            }
        }
        if (SYSTEM_ROLE_INJECTION.matcher(sanitized).find()) {
            log.warn("[PromptSanitizer] Detected system role injection attempt");
            sanitized = SYSTEM_ROLE_INJECTION.matcher(sanitized).replaceAll("[FILTERED]");
        }
        return sanitized;
    }

    public boolean isSuspicious(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return false;
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                return true;
            }
        }
        return SYSTEM_ROLE_INJECTION.matcher(prompt).find();
    }
}
