package ai.gameclaw.skills;

import org.springframework.context.ApplicationEvent;

public class SkillInstalledEvent extends ApplicationEvent {

    private final String skillName;
    private final String version;
    private final java.nio.file.Path installPath;

    public SkillInstalledEvent(Object source, String skillName, String version, java.nio.file.Path installPath) {
        super(source);
        this.skillName = skillName;
        this.version = version;
        this.installPath = installPath;
    }

    public String getSkillName() {
        return skillName;
    }

    public String getVersion() {
        return version;
    }

    public java.nio.file.Path getInstallPath() {
        return installPath;
    }
}
