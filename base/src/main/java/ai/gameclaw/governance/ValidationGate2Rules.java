package ai.gameclaw.governance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
@ConditionalOnBean(BusinessRule.class)
public class ValidationGate2Rules implements ValidationGate {

    private static final Logger log = LoggerFactory.getLogger(ValidationGate2Rules.class);

    private final List<BusinessRule> rules;

    public ValidationGate2Rules(List<BusinessRule> rules) {
        this.rules = rules;
    }

    @Override
    public String name() {
        return "rules";
    }

    @Override
    public ValidationResult validate(Object output, Class<?> expectedType) {
        List<String> allViolations = new ArrayList<>();

        for (BusinessRule rule : rules) {
            List<RuleViolation> violations = rule.validate(output);
            for (RuleViolation v : violations) {
                String entry = "[%s] %s: %s".formatted(v.ruleName(), v.field(), v.message());
                allViolations.add(entry);
                log.debug("[Gate2:Rules] Violation: {}", entry);
            }
        }

        if (!allViolations.isEmpty()) {
            log.debug("[Gate2:Rules] {} violation(s) detected by {} rule(s)", allViolations.size(), rules.size());
            return ValidationResult.fail("RULES_VIOLATION",
                    "Business rules validation failed with " + allViolations.size() + " violation(s)",
                    allViolations);
        }

        log.debug("[Gate2:Rules] All {} rule(s) passed", rules.size());
        return ValidationResult.ok(output);
    }
}
