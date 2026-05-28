DELETE FROM tool_permissions;

INSERT INTO tool_permissions (tool, role, max_risk, requires_approval) VALUES
  ('game_design_tool',     'PLANNER',         'L3_PROJECT_WRITE', true),
  ('game_design_tool',     'PROGRAMMER',      'L1_READ',          false),
  ('game_design_tool',     'DATA_ANALYST',    'L1_READ',          false),
  ('game_design_tool',     'OPERATIONS',      'L1_READ',          false),
  ('game_design_tool',     'QA',              'L1_READ',          false),
  ('game_design_tool',     'TA',              'L1_READ',          false),
  ('game_design_tool',     'DEVOPS',          'L1_READ',          false),
  ('game_design_tool',     'PROJECT_MANAGER', 'L1_READ',          false),
  ('game_design_tool',     'ADMIN',           'L5_PRODUCTION',    false),
  ('game_design_tool',     'PLATFORM_ADMIN',  'L5_PRODUCTION',    false),

  ('game_code_tool',       'PROGRAMMER',      'L3_PROJECT_WRITE', true),
  ('game_code_tool',       'PLANNER',         'L1_READ',          false),
  ('game_code_tool',       'QA',              'L2_SANDBOX_WRITE', false),
  ('game_code_tool',       'DEVOPS',          'L2_SANDBOX_WRITE', false),
  ('game_code_tool',       'ADMIN',           'L5_PRODUCTION',    false),
  ('game_code_tool',       'PLATFORM_ADMIN',  'L5_PRODUCTION',    false),

  ('game_data_tool',       'DATA_ANALYST',    'L4_DB_WRITE',      true),
  ('game_data_tool',       'PLANNER',         'L1_READ',          false),
  ('game_data_tool',       'PROGRAMMER',      'L1_READ',          false),
  ('game_data_tool',       'OPERATIONS',      'L1_READ',          false),
  ('game_data_tool',       'PROJECT_MANAGER', 'L1_READ',          false),
  ('game_data_tool',       'QA',              'L1_READ',          false),
  ('game_data_tool',       'ADMIN',           'L5_PRODUCTION',    false),
  ('game_data_tool',       'PLATFORM_ADMIN',  'L5_PRODUCTION',    false),

  ('game_balance_tool',    'PLANNER',         'L2_SANDBOX_WRITE', false),
  ('game_balance_tool',    'DATA_ANALYST',    'L2_SANDBOX_WRITE', false),
  ('game_balance_tool',    'OPERATIONS',      'L1_READ',          false),
  ('game_balance_tool',    'ADMIN',           'L5_PRODUCTION',    false),
  ('game_balance_tool',    'PLATFORM_ADMIN',  'L5_PRODUCTION',    false),

  ('game_test_tool',       'QA',              'L3_PROJECT_WRITE', false),
  ('game_test_tool',       'PROGRAMMER',      'L2_SANDBOX_WRITE', false),
  ('game_test_tool',       'DEVOPS',          'L2_SANDBOX_WRITE', false),
  ('game_test_tool',       'ADMIN',           'L5_PRODUCTION',    false),
  ('game_test_tool',       'PLATFORM_ADMIN',  'L5_PRODUCTION',    false),

  ('game_ops_tool',        'DEVOPS',          'L5_PRODUCTION',    true),
  ('game_ops_tool',        'PROGRAMMER',      'L2_SANDBOX_WRITE', false),
  ('game_ops_tool',        'OPERATIONS',      'L1_READ',          false),
  ('game_ops_tool',        'ADMIN',           'L5_PRODUCTION',    false),
  ('game_ops_tool',        'PLATFORM_ADMIN',  'L5_PRODUCTION',    false),

  ('game_asset_tool',      'TA',              'L3_PROJECT_WRITE', true),
  ('game_asset_tool',      'PROGRAMMER',      'L1_READ',          false),
  ('game_asset_tool',      'PLANNER',         'L1_READ',          false),
  ('game_asset_tool',      'ADMIN',           'L5_PRODUCTION',    false),
  ('game_asset_tool',      'PLATFORM_ADMIN',  'L5_PRODUCTION',    false),

  ('game_project_tool',    'PROJECT_MANAGER', 'L3_PROJECT_WRITE', true),
  ('game_project_tool',    'OPERATIONS',      'L1_READ',          false),
  ('game_project_tool',    'QA',              'L1_READ',          false),
  ('game_project_tool',    'ADMIN',           'L5_PRODUCTION',    false),
  ('game_project_tool',    'PLATFORM_ADMIN',  'L5_PRODUCTION',    false),

  ('deploy_tool',          'DEVOPS',          'L5_PRODUCTION',    true),
  ('deploy_tool',          'ADMIN',           'L5_PRODUCTION',    false),
  ('deploy_tool',          'PLATFORM_ADMIN',  'L5_PRODUCTION',    false);
