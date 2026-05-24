package ai.gameclaw.skills;

public interface SkillRegistry {

    void registerSkill(SkillDefinition skill);

    SkillDefinition getSkill(String name);

    java.util.List<SkillDefinition> getAllSkills();
}
