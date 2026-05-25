package ai.gameclaw.security;

public enum Role {
    PLANNER("游戏策划"),
    PROGRAMMER("游戏程序员"),
    DATA_ANALYST("数据分析师"),
    OPERATIONS("游戏运营"),
    QA("QA/测试"),
    TA("技术美术"),
    DEVOPS("DevOps/运维"),
    PROJECT_MANAGER("项目经理"),
    ADMIN("管理员"),
    PLATFORM_ADMIN("平台管理员");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
