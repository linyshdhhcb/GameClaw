package ai.gameclaw.tools.game.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record MonsterConfig(
        @Min(1) int id,
        @NotBlank @Size(max = 32) String name,
        @Min(1) @Max(100) int level,
        @Min(1) @Max(1_000_000) int hp,
        @Min(1) @Max(100_000) int attack,
        @Min(0) @Max(100_000) int defense,
        @DecimalMin("0.0") @DecimalMax("1.0") double dropRate,
        @NotEmpty List<@Valid SkillRef> skills,
        String description
) {}
