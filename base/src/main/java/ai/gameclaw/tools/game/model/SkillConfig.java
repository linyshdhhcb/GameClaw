package ai.gameclaw.tools.game.model;

import jakarta.validation.constraints.*;

public record SkillConfig(
        @Min(1) int id,
        @NotBlank @Size(max = 32) String name,
        @NotBlank String type,
        @Min(1) @Max(100) int level,
        @Min(0) int damage,
        @Min(0) int cooldown,
        @Min(0) int range,
        @NotBlank String description
) {}
