CREATE TABLE conversations (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      uuid NOT NULL,
    user_id        uuid NOT NULL,
    project_id     uuid,
    channel        text NOT NULL,
    title          text,
    started_at     timestamptz NOT NULL DEFAULT now(),
    last_active_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_conv_user_active ON conversations(tenant_id, user_id, last_active_at DESC);
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations FORCE ROW LEVEL SECURITY;
CREATE POLICY conv_isolation ON conversations
    USING (tenant_id = current_setting('gameclaw.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('gameclaw.tenant_id')::uuid);

CREATE TABLE conversation_messages (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id uuid NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    tenant_id       uuid NOT NULL,
    role            text NOT NULL,
    content         jsonb NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE conversation_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversation_messages FORCE ROW LEVEL SECURITY;
CREATE POLICY conv_msg_isolation ON conversation_messages
    USING (tenant_id = current_setting('gameclaw.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('gameclaw.tenant_id')::uuid);

CREATE TABLE feishu_tenants (
    tenant_key  text PRIMARY KEY,
    tenant_id   uuid NOT NULL
);
ALTER TABLE feishu_tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE feishu_tenants FORCE ROW LEVEL SECURITY;
CREATE POLICY feishu_tenants_isolation ON feishu_tenants
    USING (tenant_id = current_setting('gameclaw.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('gameclaw.tenant_id')::uuid);
