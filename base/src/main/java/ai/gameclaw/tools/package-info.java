@org.springframework.modulith.ApplicationModule(
    displayName = "Tools",
    allowedDependencies = {"agent::llm", "configuration", "tasks", "onboarding", "governance", "observability", "security"}
)
package ai.gameclaw.tools;
