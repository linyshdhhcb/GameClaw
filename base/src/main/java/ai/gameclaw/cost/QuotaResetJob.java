package ai.gameclaw.cost;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class QuotaResetJob {

    private static final Logger LOGGER = new JobRunrDashboardLogger(LoggerFactory.getLogger(QuotaResetJob.class));

    private final JdbcTemplate jdbc;

    public QuotaResetJob(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Recurring(id = "reset-user-daily-quotas", cron = "0 0 0 * * *")
    @Job(name = "Reset user daily quotas")
    public void resetUserDailyQuotas() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        int updated = jdbc.update(
                "UPDATE quotas SET used_amount = 0, period_start = ?, period_end = ?, updated_at = NOW() " +
                        "WHERE quota_type = ? AND period_end <= NOW()",
                today, tomorrow, QuotaType.USER_DAILY.name());
        LOGGER.info("Reset {} user daily quota rows", updated);
    }

    @Recurring(id = "reset-project-monthly-quotas", cron = "0 0 0 1 * *")
    @Job(name = "Reset project monthly quotas")
    public void resetProjectMonthlyQuotas() {
        LocalDateTime monthStart = LocalDateTime.now().toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        int updated = jdbc.update(
                "UPDATE quotas SET used_amount = 0, period_start = ?, period_end = ?, updated_at = NOW() " +
                        "WHERE quota_type = ? AND period_end <= NOW()",
                monthStart, monthEnd, QuotaType.PROJECT_MONTHLY.name());
        LOGGER.info("Reset {} project monthly quota rows", updated);
    }

    @Recurring(id = "reset-global-daily-quotas", cron = "0 0 0 * * *")
    @Job(name = "Reset global daily quotas")
    public void resetGlobalDailyQuotas() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        int updated = jdbc.update(
                "UPDATE quotas SET used_amount = 0, period_start = ?, period_end = ?, updated_at = NOW() " +
                        "WHERE quota_type = ? AND period_end <= NOW()",
                today, tomorrow, QuotaType.GLOBAL_DAILY.name());
        LOGGER.info("Reset {} global daily quota rows", updated);
    }
}
