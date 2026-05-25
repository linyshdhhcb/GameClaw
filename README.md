# GameClaw

**Game Enterprise R&D AI Agent Control Plane**

GameClaw 是一个面向游戏企业研发场景的 AI Agent 控制平面，基于 Spring Boot 4 + Java 25 构建，提供多租户隔离、虚拟线程并发、LLM 多模型适配、MCP 工具集成等企业级能力。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 25 | 虚拟线程 GA / ScopedValue GA / StructuredTaskScope (preview) |
| Spring Boot | 4.0.6 | 虚拟线程自动启用 |
| Spring AI | 2.0.0-M6 | LLM 抽象层 + MCP Client |
| Spring Modulith | 2.0.6 | 模块边界强制 |
| PostgreSQL | 16 | Row-Level Security 多租户隔离 |
| Resilience4j | 2.4.0 | Bulkhead / RateLimiter / CircuitBreaker |
| ArchUnit | 1.4.2 | 架构规则静态检查 |
| JobRunr | 8.6.0 | 后台任务调度 |
| Caffeine | — | RBAC + Skills 缓存 |
| Flyway | — | 数据库迁移 |
| Testcontainers | 1.21.4 | 集成测试 |

## 项目结构

```
GameClaw/
├── pom.xml                          # 根 POM（BOM 管理）
├── base/                            # 核心模块
│   └── src/main/java/ai/gameclaw/
│       ├── agent/                   # Agent 核心 + LLM 抽象
│       ├── channels/                # 通道注册
│       ├── compat/                  # OpenClaw 兼容层
│       ├── concurrency/             # StructuredTaskScope + 启动横幅
│       ├── configuration/           # 双格式配置 (YAML + JSON)
│       ├── cost/                    # 配额管理
│       ├── files/                   # YAML 解析
│       ├── governance/              # 治理策略 + 校验闸门
│       ├── mcp/                     # MCP 连接配置
│       ├── observability/           # 指标 + 审计日志 + PinningWatcher
│       ├── onboarding/              # 引导流程
│       ├── persistence/             # RLS 切面 + 数据源
│       ├── project/                 # 项目管理
│       ├── providers/               # Provider 抽象
│       ├── security/                # 多租户 + RBAC + 安全防护
│       ├── skills/                  # SKILL.md 解析 + 三级加载
│       └── tasks/                   # 任务系统
├── providers/                       # LLM Provider 实现
│   ├── anthropic/                   # Anthropic Claude
│   ├── openai/                      # OpenAI GPT
│   ├── ollama/                      # Ollama 本地模型
│   └── google/                      # Google Gemini
├── plugins/                         # 通道/工具插件
│   ├── brave/                       # Brave Web Search
│   ├── discord/                     # Discord Bot
│   ├── playwright/                  # Playwright 浏览器
│   └── telegram/                    # Telegram Bot
├── mcp-servers/java/                # MCP Server 模块
├── app/                             # Spring Boot 启动模块
│   └── src/main/resources/
│       ├── application.yaml         # 主配置
│       ├── logback-spring.xml       # 日志配置 (JSON/PLAIN)
│       └── templates/               # Pebble 模板
├── deploy/docker/                   # Docker 部署
│   └── docker-compose.yml           # PG16 容器
├── .github/workflows/ci.yml         # GitHub Actions CI
├── scripts/rls-lint.sh              # RLS 静态检查
├── checkstyle.xml                   # Checkstyle 规则
├── spotbugs-exclude.xml             # SpotBugs 排除规则
└── docs/playbooks/                  # 开发 Playbook 文档
```

## 快速开始

### 环境要求

- JDK 25+
- Maven 3.9+
- PostgreSQL 16（多租户模式）

### 启动（H2 单租户模式）

```bash
# 设置 JAVA_HOME
export JAVA_HOME=/path/to/jdk-25

# 编译
mvn compile

# 运行
mvn spring-boot:run -pl app
```

应用启动后访问：
- Web 界面：http://localhost:8090
- 引导向导：http://localhost:8090/onboarding
- Prometheus 指标：http://localhost:8090/actuator/prometheus
- JobRunr Dashboard：http://localhost:8091/dashboard

### 启动（PG16 多租户模式）

```bash
# 1. 启动 PostgreSQL
cd deploy/docker
docker-compose up -d

# 2. 修改 application.yaml
# spring.datasource.url=jdbc:postgresql://localhost:5432/gameclaw
# spring.datasource.username=gameclaw_app
# spring.datasource.password=gameclaw_app_pwd
# spring.flyway.enabled=true
# gameclaw.multi-tenancy.enabled=true

# 3. 运行
mvn spring-boot:run -pl app
```

## 已实现功能

### Phase 0 — 基础架构

#### ✅ Playbook 01：仓库改造与模块结构

- Gradle → Maven 多模块迁移（12 个子模块）
- 包名 `ai.javaclaw` → `ai.gameclaw` 全量重命名
- Spring Modulith 模块边界强制 + `allowedDependencies` 白名单
- ArchUnit 架构规则：禁止裸 `Thread.startVirtualThread`、模块依赖校验

#### ✅ Playbook 02：多租户与 PG16 RLS 隔离

- PostgreSQL 16 Row-Level Security 强制数据隔离
- `TenantContextHolder` 基于 Java 25 `ScopedValue` (JEP 506 GA) 传播租户上下文
- `TenantSettingsAspect` 切面自动注入 `SET LOCAL gameclaw.tenant_id`
- HikariCP `connectionInitSql=RESET ALL` 纵深防御
- `SingleTenantFallback` 单租户回退模式
- Flyway 迁移：V0（bootstrap roles）+ V1（business tables）+ V2（RLS policies）

#### ✅ Playbook 03：原生并发与虚拟线程

- `spring.threads.virtual.enabled=true` 全局启用虚拟线程
- `Scopes.race()` / `Scopes.all()` 封装 JDK 25 `StructuredTaskScope` 接口 API
- `PinningWatcher` JFR 监控虚拟线程 Pinning 事件
- `ConcurrencyBanner` 启动横幅展示并发配置
- Resilience4j Bulkhead 背压隔离（4 个实例）

#### ✅ Playbook 04：五层安全骨架

| 层级 | 组件 | 实现类 |
|------|------|--------|
| L1 网络 | TLS + 出站白名单 | `OutboundUrlFilter`, `OutboundGuardConfig` |
| L2 访问 | Spring Security 双模式 | `SecurityConfig`（dev/sso） |
| L3 应用 | RBAC + Prompt 净化 | `RbacService`, `RbacAspect`, `PromptSanitizer` |
| L4 数据 | RLS + PII 脱敏 | `PiiMasking`, `TenantSettingsAspect` |
| L5 审计 | Tool 调用审计 | `AuditLogger` |

- `@RequireRole` / `@RequireRiskLevel` 注解 + AOP 切面
- `PromptSanitizer` 7 种注入模式检测 + 系统角色注入防护
- `PiiMasking` 手机号/邮箱/身份证静态脱敏

#### ✅ Playbook 05：可观测性与 CI/CD 流水线

**指标（Micrometer + Prometheus）：**
- `llm_request_total` — LLM 请求计数（按 model/role 标签）
- `llm_tokens_in_total` / `llm_tokens_out_total` — Token 消耗
- `llm_cost_cny_total` — LLM 调用成本（按 model/project 标签）
- `llm_latency_seconds` — LLM 延迟直方图
- `tool_call_total` — 工具调用计数（按 tool/result 标签）
- `validation_gate_failure_total` — 校验闸门失败计数
- `quota_exhausted_total` — 配额耗尽计数

**日志（Logback + MDC）：**
- `logback-spring.xml`：prod 环境 JSON 输出（logstash-logback-encoder），dev 环境 PLAIN 输出
- MDC 自动注入 `traceId` / `spanId` / `tenantId`

**CI/CD（GitHub Actions）：**
- `ci.yml`：4 个 Job（build+test / spotbugs+checkstyle / rls-lint / gitleaks）
- `rls-lint.sh`：扫描 Flyway 迁移中 CREATE TABLE 缺少 ENABLE ROW LEVEL SECURITY 的表
- `checkstyle.xml`：宽松规则集（避免星号导入/冗余导入/布尔简化/equals-hashCode）
- `spotbugs-exclude.xml`：排除测试代码和常见误报

**校验闸门：**
- `ValidationGate` 接口 + `ValidationResult` record
- `ValidationGate1Schema`：Jackson 反序列化 + JSR-380 Bean Validation
- `ValidatedLlmOutput`：LLM 输出校验 + 自动重试（默认最多 2 次）

#### ✅ Playbook 06：Skills 兼容层与 RBAC 基础

**Skills 兼容层：**
- `GameClawSkillParser`：解析 OpenClaw `SKILL.md`（YAML frontmatter + Markdown body）
- 不支持字段优雅降级（`metadata.gate` / `user-invocable` / `command-dispatch` 解析 + INFO 日志）
- `{baseDir}` 占位符替换 + path traversal 防护（`resolveResource` 校验路径在 baseDir 子树内）
- `GameClawSkillsLoader`：三级优先级加载（classpath → `~/.openclaw/skills` → `~/.gameclaw/skills` → workspace/skills），高优先级覆盖低优先级
- Caffeine LRU 缓存（200 条 / 30 分钟过期）

**RBAC DB 持久化：**
- Flyway 迁移：V3（users/roles/user_roles/tool_permissions 表 + RLS）+ V4（10 角色 + 16 条权限）
- `DefaultRbacService`：JdbcTemplate DB 查询 + Caffeine 双缓存（角色 30s / 权限 30s）+ fallback 矩阵
- 10 个默认角色：PLANNER / PROGRAMMER / DATA_ANALYST / OPERATIONS / QA / TA / DEVOPS / PROJECT_MANAGER / ADMIN / PLATFORM_ADMIN
- `RbacAspect`：拒绝时写入 `audit_log`（role_check_failed / risk_level_denied）
- Onboarding S10 步骤：用户选择角色并绑定

## 测试

```bash
# 全模块测试
mvn test

# 单模块测试
mvn test -pl base

# 跳过测试打包
mvn package -DskipTests
```

## 配置参考

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8090 | HTTP 端口 |
| `gameclaw.llm.adapter` | spring-ai | LLM 适配器 (spring-ai / langchain4j) |
| `gameclaw.workspace` | file:./workspace/ | 工作空间路径 |
| `gameclaw.multi-tenancy.enabled` | false | 多租户开关 |
| `gameclaw.multi-tenancy.default-tenant-id` | 00000000-...-000000000001 | 默认租户 ID |
| `gameclaw.security.mode` | dev | 安全模式 (dev / sso) |
| `gameclaw.security.rbac.enabled` | true | RBAC 开关 |
| `gameclaw.security.outbound.enabled` | true | 出站白名单开关 |
| `gameclaw.audit.enabled` | true | 审计日志开关 |
| `spring.threads.virtual.enabled` | true | 虚拟线程开关 |
| `spring.flyway.enabled` | false | Flyway 迁移开关 |
| `management.endpoints.web.exposure.include` | prometheus,health,info,metrics | Actuator 端点暴露 |
| `jobrunr.dashboard.port` | 8091 | JobRunr Dashboard 端口 |

## License

This project is licensed under the **GNU Lesser General Public License v3.0** — see [LICENSE](LICENSE) for details.
