package ai.gameclaw.compat.openclaw;

public interface OpenClawTool {

    String name();

    String description();

    String execute(String input);
}
