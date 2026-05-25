CREATE TABLE users (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid NOT NULL,
    external_id text NOT NULL,
    display_name text,
    email       text,
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, external_id)
);
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
CREATE POLICY users_tenant_isolation ON users
    USING (tenant_id = current_setting('gameclaw.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('gameclaw.tenant_id')::uuid);

CREATE TABLE roles (
    code        text PRIMARY KEY,
    description text NOT NULL
);

CREATE TABLE user_roles (
    user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        text NOT NULL REFERENCES roles(code),
    project_id  uuid,
    granted_at  timestamptz NOT NULL DEFAULT now(),
    granted_by  uuid,
    PRIMARY KEY (user_id, role, project_id)
);
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles FORCE ROW LEVEL SECURITY;
CREATE POLICY user_roles_tenant_isolation ON user_roles
    USING (EXISTS (SELECT 1 FROM users u WHERE u.id = user_id
                     AND u.tenant_id = current_setting('gameclaw.tenant_id')::uuid));

CREATE TABLE tool_permissions (
    tool              text NOT NULL,
    role              text NOT NULL REFERENCES roles(code),
    max_risk          text NOT NULL,
    requires_approval boolean NOT NULL DEFAULT false,
    PRIMARY KEY (tool, role)
);
