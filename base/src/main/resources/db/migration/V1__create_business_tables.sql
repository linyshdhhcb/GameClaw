CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE quests (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid NOT NULL,
    project_id  uuid,
    title       text NOT NULL,
    body        jsonb,
    status      text NOT NULL DEFAULT 'open',
    created_by  uuid NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_quests_tenant_project ON quests(tenant_id, project_id);

CREATE TABLE audit_log (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid NOT NULL,
    actor_id    uuid NOT NULL,
    action      text NOT NULL,
    resource    text NOT NULL,
    resource_id uuid,
    detail      jsonb,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_tenant ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);

CREATE TABLE skills_meta (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid NOT NULL,
    name        text NOT NULL,
    description text,
    instructions text,
    enabled     boolean NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_skills_meta_tenant ON skills_meta(tenant_id);
CREATE UNIQUE INDEX idx_skills_meta_tenant_name ON skills_meta(tenant_id, name);

CREATE TABLE configs (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid NOT NULL,
    config_key  text NOT NULL,
    config_value jsonb NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_configs_tenant ON configs(tenant_id);
CREATE UNIQUE INDEX idx_configs_tenant_key ON configs(tenant_id, config_key);
