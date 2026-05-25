package ai.gameclaw.governance;

import java.util.List;

public record ValidationResult(
        boolean valid,
        Object parsed,
        String code,
        String message,
        List<String> violations
) {

    public static ValidationResult ok(Object parsed) {
        return new ValidationResult(true, parsed, null, null, List.of());
    }

    public static ValidationResult fail(String code, String message) {
        return new ValidationResult(false, null, code, message, List.of());
    }

    public static ValidationResult fail(String code, String message, List<String> violations) {
        return new ValidationResult(false, null, code, message, violations);
    }
}
