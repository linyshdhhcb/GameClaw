package ai.gameclaw.cost;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gameclaw.quota")
public record QuotaProperties(
        Double userDailyLimit,
        Double projectMonthlyLimit,
        Double globalDailyLimit,
        Boolean enabled
) {

    public QuotaProperties {
        if (userDailyLimit == null) userDailyLimit = 1.0;
        if (projectMonthlyLimit == null) projectMonthlyLimit = 1000.0;
        if (globalDailyLimit == null) globalDailyLimit = 10000.0;
        if (enabled == null) enabled = true;
    }
}
