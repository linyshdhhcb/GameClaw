package ai.gameclaw.skills;

import org.springframework.context.ApplicationEvent;

public class SkillsChangedEvent extends ApplicationEvent {

    public SkillsChangedEvent(Object source) {
        super(source);
    }
}
