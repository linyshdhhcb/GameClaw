package ai.gameclaw.governance;

import java.util.List;

public interface BusinessRule {
    String name();
    List<RuleViolation> validate(Object output);
}
