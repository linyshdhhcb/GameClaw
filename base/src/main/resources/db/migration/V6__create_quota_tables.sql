CREATE TABLE IF NOT EXISTS quotas (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    project_id      UUID,
    user_id         UUID,
    quota_type      VARCHAR(20) NOT NULL,
    resource        VARCHAR(50) NOT NULL DEFAULT 'llm_cost_cny',
    limit_amount    DOUBLE PRECISION NOT NULL DEFAULT 0,
    used_amount     DOUBLE PRECISION NOT NULL DEFAULT 0,
    period_start    TIMESTAMP NOT NULL,
    period_end      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_quota UNIQUE (tenant_id, project_id, user_id, quota_type, resource)
);

CREATE INDEX idx_quotas_tenant ON quotas(tenant_id);
CREATE INDEX idx_quotas_period ON quotas(period_end);

ALTER TABLE quotas ENABLE ROW LEVEL SECURITY;
