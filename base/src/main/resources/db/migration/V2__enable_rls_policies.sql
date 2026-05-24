ALTER TABLE quests ENABLE ROW LEVEL SECURITY;
ALTER TABLE quests FORCE ROW LEVEL SECURITY;

CREATE POLICY quests_tenant_isolation ON quests
    USING (
        tenant_id = current_setting('gameclaw.tenant_id')::uuid
        AND (
            current_setting('gameclaw.project_id', true) IS NULL
            OR project_id IS NULL
            OR project_id = current_setting('gameclaw.project_id', true)::uuid
        )
    )
    WITH CHECK (
        tenant_id = current_setting('gameclaw.tenant_id')::uuid
    );

ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log FORCE ROW LEVEL SECURITY;

CREATE POLICY audit_log_tenant_isolation ON audit_log
    USING (tenant_id = current_setting('gameclaw.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('gameclaw.tenant_id')::uuid);

ALTER TABLE skills_meta ENABLE ROW LEVEL SECURITY;
ALTER TABLE skills_meta FORCE ROW LEVEL SECURITY;

CREATE POLICY skills_meta_tenant_isolation ON skills_meta
    USING (tenant_id = current_setting('gameclaw.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('gameclaw.tenant_id')::uuid);

ALTER TABLE configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE configs FORCE ROW LEVEL SECURITY;

CREATE POLICY configs_tenant_isolation ON configs
    USING (tenant_id = current_setting('gameclaw.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('gameclaw.tenant_id')::uuid);
