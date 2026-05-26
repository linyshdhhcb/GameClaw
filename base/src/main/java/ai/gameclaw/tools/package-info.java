@org.springframework.modulith.ApplicationModule(
    displayName = "Tools",
    allowedDependencies = {"agent::llm", "configuration", "tasks", "onboarding", "governance", "observability", "security", "security::pii"}
)
package ai.gameclaw.tools;
