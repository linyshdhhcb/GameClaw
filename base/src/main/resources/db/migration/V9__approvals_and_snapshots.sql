CREATE TABLE pending_approvals (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       uuid NOT NULL,
    requester_id    uuid NOT NULL,
    resource        text NOT NULL,
    action          text NOT NULL,
    risk_level      text NOT NULL,
    impact_summary  jsonb,
    params          jsonb,
    quorum          int  NOT NULL DEFAULT 1,
    approvals       jsonb NOT NULL DEFAULT '[]',
    state           text NOT NULL DEFAULT 'PENDING',
    expires_at      timestamptz NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_pa_state ON pending_approvals(tenant_id, state, expires_at);

ALTER TABLE pending_approvals ENABLE ROW LEVEL SECURITY;
ALTER TABLE pending_approvals FORCE ROW LEVEL SECURITY;

CREATE POLICY pa_isolation ON pending_approvals
    USING (tenant_id = current_setting('gameclaw.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('gameclaw.tenant_id')::uuid);

CREATE TABLE rollback_snapshots (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       uuid NOT NULL,
    approval_id     uuid REFERENCES pending_approvals(id),
    resource        text NOT NULL,
    snapshot        jsonb NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE rollback_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE rollback_snapshots FORCE ROW LEVEL SECURITY;

CREATE POLICY rs_isolation ON rollback_snapshots
    USING (tenant_id = current_setting('gameclaw.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('gameclaw.tenant_id')::uuid);
