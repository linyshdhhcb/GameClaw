package ai.gameclaw.cli;

import ai.gameclaw.cost.JdbcQuotaManager;
import ai.gameclaw.cost.QuotaManager;
import ai.gameclaw.skills.ClawHubClient;
import ai.gameclaw.skills.ClawHubSearchResult;
import ai.gameclaw.skills.GameClawSkillsLoader;
import ai.gameclaw.skills.SkillDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "gameclaw", mixinStandardHelpOptions = true,
        description = "GameClaw AI Agent Control Plane CLI",
        subcommands = {SkillCommand.class, QuotaCommand.class})
public class GameClawCli implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }
}
