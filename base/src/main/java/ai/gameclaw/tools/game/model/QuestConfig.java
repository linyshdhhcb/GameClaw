package ai.gameclaw.tools.game.model;

import jakarta.validation.constraints.*;

import java.util.List;

public record QuestConfig(
        @Min(1) int id,
        @NotBlank @Size(max = 64) String name,
        @NotBlank String type,
        @Min(1) int minLevel,
        @NotBlank String objective,
        @NotEmpty List<@NotBlank String> rewards,
        @NotBlank String description
) {}
