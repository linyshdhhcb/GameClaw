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
│       ├── governance/              # 治理策略
│       ├── mcp/                     # MCP 连接配置
│       ├── observability/           # PinningWatcher + 指标
│       ├── onboarding/              # 引导流程
│       ├── persistence/             # RLS 切面 + 数据源
│       ├── project/                 # 项目管理
│       ├── providers/               # Provider 抽象
│       ├── security/                # TenantContext + ScopedValue
│       ├── skills/                  # 技能注册
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
│       └── templates/               # Pebble 模板
├── deploy/docker/                   # Docker 部署
│   └── docker-compose.yml           # PG16 容器
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

## 核心特性

### 多租户隔离 (PG16 RLS)

- PostgreSQL 16 Row-Level Security 强制数据隔离
- `TenantContextHolder` 基于 Java 25 `ScopedValue` 传播租户上下文
- `TenantSettingsAspect` 切面自动注入 `SET LOCAL gameclaw.tenant_id`
- HikariCP `connectionInitSql=RESET ALL` 纵深防御
- `SingleTenantFallback` 单租户回退模式

### 虚拟线程 + 结构化并发

- `spring.threads.virtual.enabled=true` 全局启用
- `Scopes.race()` / `Scopes.all()` 封装 `StructuredTaskScope`
- `PinningWatcher` 监控虚拟线程 Pinning 事件
- ArchUnit 禁止裸 `Thread.startVirtualThread`
- Resilience4j Bulkhead 背压隔离（4 个实例）

### LLM 多模型适配

- `LlmClient` 接口抽象
- `SpringAiLlmClient` / `LangChain4jLlmClient` 双实现
- `gameclaw.llm.adapter` 配置切换

### 双格式配置

- `application.yaml` + `openclaw.json` 双格式支持
- `ConfigPathMapper` OpenClaw → GameClaw 键映射
- `DualFormatConfigurationManager` 统一管理

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
| `spring.threads.virtual.enabled` | true | 虚拟线程开关 |
| `spring.flyway.enabled` | false | Flyway 迁移开关 |
| `jobrunr.dashboard.port` | 8091 | JobRunr Dashboard 端口 |

## License

This project is licensed under the **GNU Lesser General Public License v3.0** — see [LICENSE](LICENSE) for details.
