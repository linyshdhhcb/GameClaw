package ai.gameclaw.governance;

import java.util.List;

public class ValidationGateException extends RuntimeException {

    private final String code;
    private final List<String> violations;
    private final int attempts;

    public ValidationGateException(String code, String message, List<String> violations, int attempts) {
        super(message);
        this.code = code;
        this.violations = violations;
        this.attempts = attempts;
    }

    public String code() {
        return code;
    }

    public List<String> violations() {
        return violations;
    }

    public int attempts() {
        return attempts;
    }
}
