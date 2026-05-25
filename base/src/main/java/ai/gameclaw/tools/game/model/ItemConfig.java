package ai.gameclaw.tools.game.model;

import jakarta.validation.constraints.*;

public record ItemConfig(
        @Min(1) int id,
        @NotBlank @Size(max = 32) String name,
        @NotBlank String type,
        @NotBlank String rarity,
        @Min(0) int value,
        @NotBlank String description
) {}
