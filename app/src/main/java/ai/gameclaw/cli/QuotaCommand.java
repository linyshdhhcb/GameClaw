package ai.gameclaw.cli;

import ai.gameclaw.cost.QuotaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "quota", description = "Check and manage AI usage quotas")
@Component
public class QuotaCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(QuotaCommand.class);

    private final ObjectProvider<QuotaManager> quotaManagerProvider;

    public QuotaCommand(ObjectProvider<QuotaManager> quotaManagerProvider) {
        this.quotaManagerProvider = quotaManagerProvider;
    }

    @Override
    public Integer call() {
        log.info("Usage: gameclaw quota [check|remaining]");
        return 0;
    }

    @Command(name = "check", description = "Check if quota is available")
    static class CheckCommand implements Callable<Integer> {

        private final ObjectProvider<QuotaManager> quotaManagerProvider;

        @Parameters(index = "0", description = "Tenant ID")
        String tenantId;

        @Parameters(index = "1", defaultValue = "llm_cost_cny", description = "Resource name")
        String resource;

        CheckCommand(ObjectProvider<QuotaManager> quotaManagerProvider) {
            this.quotaManagerProvider = quotaManagerProvider;
        }

        @Override
        public Integer call() {
            var manager = quotaManagerProvider.getIfAvailable();
            if (manager == null) {
                log.error("Quota manager not available");
                return 1;
            }
            boolean hasQuota = manager.checkQuota(tenantId, resource);
            if (hasQuota) {
                double remaining = manager.getRemainingQuota(tenantId, resource);
                log.info("Quota available for tenant {}: {} remaining for {}", tenantId, remaining, resource);
                return 0;
            } else {
                log.warn("Quota exhausted for tenant {} on resource {}", tenantId, resource);
                return 2;
            }
        }
    }

    @Command(name = "remaining", description = "Show remaining quota")
    static class RemainingCommand implements Callable<Integer> {

        private final ObjectProvider<QuotaManager> quotaManagerProvider;

        @Parameters(index = "0", description = "Tenant ID")
        String tenantId;

        @Parameters(index = "1", defaultValue = "llm_cost_cny", description = "Resource name")
        String resource;

        RemainingCommand(ObjectProvider<QuotaManager> quotaManagerProvider) {
            this.quotaManagerProvider = quotaManagerProvider;
        }

        @Override
        public Integer call() {
            var manager = quotaManagerProvider.getIfAvailable();
            if (manager == null) {
                log.error("Quota manager not available");
                return 1;
            }
            double remaining = manager.getRemainingQuota(tenantId, resource);
            log.info("Remaining quota for tenant {} on {}: {}", tenantId, resource, remaining);
            return 0;
        }
    }
}
