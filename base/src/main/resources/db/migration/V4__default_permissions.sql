INSERT INTO roles (code, description) VALUES
  ('PLANNER', '游戏策划'),
  ('PROGRAMMER', '游戏程序员'),
  ('DATA_ANALYST', '数据分析师'),
  ('OPERATIONS', '游戏运营'),
  ('QA', 'QA/测试'),
  ('TA', '技术美术'),
  ('DEVOPS', 'DevOps/运维'),
  ('PROJECT_MANAGER', '项目经理'),
  ('ADMIN', '管理员'),
  ('PLATFORM_ADMIN', '平台管理员');

INSERT INTO tool_permissions (tool, role, max_risk, requires_approval) VALUES
  ('game_design_tool',  'PLANNER',         'L3_PROJECT_WRITE', true),
  ('game_design_tool',  'PROGRAMMER',      'L1_READ', false),
  ('game_design_tool',  'ADMIN',           'L5_PRODUCTION', false),
  ('game_code_tool',    'PROGRAMMER',      'L3_PROJECT_WRITE', true),
  ('game_code_tool',    'PLANNER',         'L1_READ', false),
  ('game_code_tool',    'ADMIN',           'L5_PRODUCTION', false),
  ('game_data_tool',    'DATA_ANALYST',    'L1_READ', false),
  ('game_data_tool',    'OPERATIONS',      'L1_READ', false),
  ('game_data_tool',    'ADMIN',           'L5_PRODUCTION', false),
  ('deploy_tool',       'DEVOPS',          'L5_PRODUCTION', true),
  ('deploy_tool',       'ADMIN',           'L5_PRODUCTION', false),
  ('test_tool',         'QA',              'L3_PROJECT_WRITE', false),
  ('test_tool',         'PROGRAMMER',      'L2_PROJECT_READ', false),
  ('test_tool',         'ADMIN',           'L5_PRODUCTION', false),
  ('asset_tool',        'TA',              'L3_PROJECT_WRITE', true),
  ('asset_tool',        'ADMIN',           'L5_PRODUCTION', false);
