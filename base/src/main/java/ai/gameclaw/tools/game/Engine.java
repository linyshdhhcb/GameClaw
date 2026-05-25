package ai.gameclaw.tools.game;

public enum Engine {
    UNITY("Unity", "C#"),
    UNREAL("Unreal", "C++"),
    GODOT("Godot", "GDScript");

    private final String displayName;
    private final String language;

    Engine(String displayName, String language) {
        this.displayName = displayName;
        this.language = language;
    }

    public String getDisplayName() { return displayName; }
    public String getLanguage() { return language; }

    public static Engine fromString(String value) {
        for (Engine e : values()) {
            if (e.name().equalsIgnoreCase(value) || e.displayName.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return UNITY;
    }
}
