package ai.gameclaw.cost;

public interface QuotaManager {

    boolean checkQuota(String tenantId, String resource);

    void consumeQuota(String tenantId, String resource, double amount);

    double getRemainingQuota(String tenantId, String resource);
}
