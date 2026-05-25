package ai.gameclaw.tools.game.model;

public record GrowthCurvePoint(
        int level,
        int hp,
        int attack,
        int defense,
        double expRequired
) {}
