package ai.gameclaw.cli;

import ai.gameclaw.skills.ClawHubClient;
import ai.gameclaw.skills.ClawHubSearchResult;
import ai.gameclaw.skills.GameClawSkillsLoader;
import ai.gameclaw.skills.SkillDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "skill", description = "Manage GameClaw skills",
        subcommands = {SkillCommand.InstallCommand.class, SkillCommand.SearchCommand.class,
                SkillCommand.UpdateCommand.class, SkillCommand.ListCommand.class})
@Component
public class SkillCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(SkillCommand.class);

    private final ObjectProvider<ClawHubClient> clawHubClientProvider;
    private final ObjectProvider<GameClawSkillsLoader> skillsLoaderProvider;

    public SkillCommand(ObjectProvider<ClawHubClient> clawHubClientProvider,
                        ObjectProvider<GameClawSkillsLoader> skillsLoaderProvider) {
        this.clawHubClientProvider = clawHubClientProvider;
        this.skillsLoaderProvider = skillsLoaderProvider;
    }

    @Override
    public Integer call() {
        log.info("Usage: gameclaw skill [install|search|update|list]");
        return 0;
    }

    @Command(name = "install", description = "Install a skill from ClawHub")
    static class InstallCommand implements Callable<Integer> {

        private final ObjectProvider<ClawHubClient> clawHubClientProvider;
        private final ObjectProvider<GameClawSkillsLoader> skillsLoaderProvider;

        @Parameters(index = "0", description = "Skill name (e.g. unity-scripting-api)")
        String skillName;

        @Option(names = {"-v", "--version"}, description = "Version (default: latest)")
        String version;

        InstallCommand(ObjectProvider<ClawHubClient> clawHubClientProvider,
                       ObjectProvider<GameClawSkillsLoader> skillsLoaderProvider) {
            this.clawHubClientProvider = clawHubClientProvider;
            this.skillsLoaderProvider = skillsLoaderProvider;
        }

        @Override
        public Integer call() {
            var client = clawHubClientProvider.getIfAvailable();
            if (client == null) {
                log.error("ClawHub client not available. Set gameclaw.clawhub.enabled=true in application.yaml");
                return 1;
            }
            try {
                var path = client.install(skillName, version);
                log.info("Skill '{}' installed successfully at: {}", skillName, path);
                var loader = skillsLoaderProvider.getIfAvailable();
                if (loader != null) {
                    loader.reload();
                    log.info("Skills reloaded");
                }
                return 0;
            } catch (Exception e) {
                log.error("Failed to install skill '{}': {}", skillName, e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "search", description = "Search ClawHub for skills")
    static class SearchCommand implements Callable<Integer> {

        private final ObjectProvider<ClawHubClient> clawHubClientProvider;

        @Parameters(index = "0", description = "Search query")
        String query;

        SearchCommand(ObjectProvider<ClawHubClient> clawHubClientProvider) {
            this.clawHubClientProvider = clawHubClientProvider;
        }

        @Override
        public Integer call() {
            var client = clawHubClientProvider.getIfAvailable();
            if (client == null) {
                log.error("ClawHub client not available. Set gameclaw.clawhub.enabled=true in application.yaml");
                return 1;
            }
            try {
                List<ClawHubSearchResult> results = client.search(query);
                if (results.isEmpty()) {
                    log.info("No skills found for query: {}", query);
                    return 0;
                }
                log.info("Found {} skill(s):", results.size());
                for (var r : results) {
                    log.info("  {} (v{}) - {} [{} downloads]", r.name(), r.latestVersion(),
                            r.description(), r.downloads());
                }
                return 0;
            } catch (Exception e) {
                log.error("Search failed: {}", e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "update", description = "Update installed skills")
    static class UpdateCommand implements Callable<Integer> {

        private final ObjectProvider<ClawHubClient> clawHubClientProvider;

        @Option(names = {"--all"}, description = "Update all installed skills")
        boolean updateAll;

        @Parameters(index = "0", arity = "0..1", description = "Skill name to update")
        String skillName;

        UpdateCommand(ObjectProvider<ClawHubClient> clawHubClientProvider) {
            this.clawHubClientProvider = clawHubClientProvider;
        }

        @Override
        public Integer call() {
            var client = clawHubClientProvider.getIfAvailable();
            if (client == null) {
                log.error("ClawHub client not available. Set gameclaw.clawhub.enabled=true in application.yaml");
                return 1;
            }
            try {
                if (updateAll) {
                    client.updateAll();
                    log.info("All skills updated");
                } else if (skillName != null) {
                    client.update(skillName);
                    log.info("Skill '{}' updated", skillName);
                } else {
                    log.error("Specify a skill name or use --all");
                    return 1;
                }
                return 0;
            } catch (Exception e) {
                log.error("Update failed: {}", e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "list", description = "List installed skills")
    static class ListCommand implements Callable<Integer> {

        private final ObjectProvider<GameClawSkillsLoader> skillsLoaderProvider;

        @Option(names = {"--installed"}, description = "Show only installed skills")
        boolean installedOnly;

        ListCommand(ObjectProvider<GameClawSkillsLoader> skillsLoaderProvider) {
            this.skillsLoaderProvider = skillsLoaderProvider;
        }

        @Override
        public Integer call() {
            var loader = skillsLoaderProvider.getIfAvailable();
            if (loader == null) {
                log.error("Skills loader not available");
                return 1;
            }
            List<SkillDefinition> skills = loader.getAllSkills();
            if (skills.isEmpty()) {
                log.info("No skills installed");
                return 0;
            }
            log.info("Installed skills ({}):", skills.size());
            for (var s : skills) {
                log.info("  {} - {}", s.name(), s.description());
            }
            return 0;
        }
    }
}
