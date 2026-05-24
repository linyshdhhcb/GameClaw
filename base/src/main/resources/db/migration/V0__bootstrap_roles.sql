CREATE ROLE gameclaw_app LOGIN PASSWORD 'gameclaw_app_pwd';
CREATE ROLE gameclaw_readonly LOGIN PASSWORD 'gameclaw_readonly_pwd';
CREATE ROLE gameclaw_migration LOGIN PASSWORD 'gameclaw_migration_pwd' BYPASSRLS;
CREATE ROLE gameclaw_admin LOGIN PASSWORD 'gameclaw_admin_pwd' BYPASSRLS SUPERUSER;
CREATE ROLE mcp_data_warehouse LOGIN PASSWORD 'mcp_data_warehouse_pwd';

GRANT CONNECT ON DATABASE gameclaw TO gameclaw_app;
GRANT USAGE ON SCHEMA public TO gameclaw_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO gameclaw_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO gameclaw_app;

GRANT CONNECT ON DATABASE gameclaw TO gameclaw_readonly;
GRANT USAGE ON SCHEMA public TO gameclaw_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO gameclaw_readonly;

GRANT CONNECT ON DATABASE gameclaw TO gameclaw_migration;
GRANT USAGE ON SCHEMA public TO gameclaw_migration;
GRANT ALL ON SCHEMA public TO gameclaw_migration;
GRANT ALL ON ALL TABLES IN SCHEMA public TO gameclaw_migration;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO gameclaw_migration;

GRANT CONNECT ON DATABASE gameclaw TO mcp_data_warehouse;
GRANT USAGE ON SCHEMA public TO mcp_data_warehouse;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO mcp_data_warehouse;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO gameclaw_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO gameclaw_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO mcp_data_warehouse;
