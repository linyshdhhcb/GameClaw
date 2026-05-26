package ai.gameclaw.cost;

public class QuotaExhaustedException extends RuntimeException {

    private final QuotaType quotaType;

    public QuotaExhaustedException(QuotaType quotaType) {
        super("Quota exhausted: " + quotaType);
        this.quotaType = quotaType;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }
}
