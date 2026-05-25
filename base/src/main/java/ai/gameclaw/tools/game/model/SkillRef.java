package ai.gameclaw.tools.game.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record SkillRef(
        @Min(1) int id,
        @DecimalMin("0.0") @DecimalMax("1.0") double probability
) {}
