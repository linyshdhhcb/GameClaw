package ai.gameclaw.governance;

public interface ValidationGate {

    String name();

    ValidationResult validate(Object output, Class<?> expectedType);
}
