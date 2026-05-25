# GameClaw

**Game Enterprise R&D AI Agent Control Plane**

面向游戏企业研发场景的 AI Agent 控制平面，基于 Spring Boot 4 + Java 25 构建。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 25 | 虚拟线程 GA / ScopedValue GA / StructuredTaskScope |
| Spring Boot | 4.0.6 | 虚拟线程自动启用 |
| Spring AI | 2.0.0-M6 | LLM 抽象层 + MCP Client |
| Spring Modulith | 2.0.6 | 模块边界强制 |
| PostgreSQL | 16 | Row-Level Security 多租户隔离 |
| Resilience4j | 2.4.0 | Bulkhead / RateLimiter / CircuitBreaker |
| JobRunr | 8.6.0 | 后台任务调度 |
| Flyway | — | 数据库迁移 |
| Testcontainers | 1.21.4 | 集成测试 |

## 项目结构

```
GameClaw/
├── base/                    # 核心模块（18 个子包）
├── providers/               # LLM 供应商实现
│   ├── anthropic/           # Anthropic Claude
│   ├── openai/              # OpenAI GPT
│   ├── ollama/              # Ollama 本地模型
│   └── google/              # Google Gemini
├── plugins/                 # 渠道/工具插件
│   ├── feishu/              # 飞书 Bot
│   ├── telegram/            # Telegram Bot
│   ├── discord/             # Discord Bot
│   ├── brave/               # Brave Web Search
│   └── playwright/          # Playwright 浏览器自动化
├── mcp-servers/java/        # MCP Server 模块
├── app/                     # Spring Boot 启动模块
└── deploy/docker/           # Docker 部署 (PG16)
```

## 功能模块

### 核心模块 (base)

| 模块 | 功能点 |
|------|--------|
| **agent** | Agent 接口 (`respondTo`/`prompt`)、DefaultAgent 实现、LlmClient 统一抽象 (call/stream/embed)、Spring AI / LangChain4j 双适配、对话记忆 (FileSystem + JDBC) |
| **channels** | Channel 接口 + ChannelRegistry 注册中心、Conversation/Message 实体、ConversationService 会话管理、JdbcChatMemoryRepository 桥接 Spring AI |
| **configuration** | ConfigurationManager YAML 读写、DualFormatConfigurationManager 双格式同步 (YAML + JSON)、ConfigurationChangedEvent 变更事件 |
| **concurrency** | Scopes.race()/all() 封装 StructuredTaskScope、ConcurrencyBanner 启动横幅、PinningWatcher JFR 监控 |
| **cost** | QuotaManager 配额管理接口 (check/consume/remaining) |
| **files** | YamlParser 轻量 frontmatter 解析器、YamlDocument 文档模型 |
| **governance** | ValidationGate 闸门接口、ValidationGate1Schema (Jackson + Bean Validation)、ValidatedLlmOutput 多闸门链式验证 + 自动重试、GovernancePolicy 治理策略 |
| **mcp** | McpConnectionsProperties 连接配置、McpHeaderCustomizer 请求头注入 |
| **observability** | AiMetrics Micrometer 指标集 (7 类)、AiMetricsAspect AOP 自动采集、AuditLogger 审计日志、PinningWatcher 虚拟线程 Pinning 监控 |
| **onboarding** | OnboardingProvider 引导步骤接口、AgentOnboardingProvider 供应商引导接口、AgentOnboardingProviders 注册中心 |
| **persistence** | TenantAwareDataSourceConfig 多租户数据源、TenantSettingsAspect RLS 会话变量注入、TenantAwareRepository 租户感知仓储 |
| **project** | Project 实体、ProjectManager 项目管理接口 |
| **providers** | AgentProvider 供应商聚合器、getDefaultChatModel 默认模型获取 |
| **security** | TenantContext + TenantContextHolder (ScopedValue)、RBAC 5 级风险 × 10 种角色、@RequireRole/@RequireRiskLevel 注解 + AOP、DefaultRbacService (DB + Caffeine 双缓存 + fallback 矩阵)、PromptSanitizer 7 种注入检测、OutboundUrlFilter 出站白名单、PiiMasking PII 脱敏、SingleTenantFallback 单租户回退 |
| **skills** | GameClawSkillParser SKILL.md 解析、GameClawSkillsLoader 四级优先级加载 (classpath → ~/.openclaw → ~/.gameclaw → workspace)、Caffeine LRU 缓存 |
| **tasks** | Task/RecurringTask 实体、TaskManager (JobRunr 调度)、TaskHandler Agent 执行、FileSystemTaskRepository YAML 文件存储 |
| **tools** | TaskTool 任务工具、CheckListTool 清单工具、McpTool MCP 服务器管理、AgentEnvironment 运行环境信息、Lucene 动态工具发现 |
| **tools/game** | GameDesignTool 策划配置生成 (怪物/技能/道具/任务/成长曲线)、GameCodeTool 代码生成 (Unity/Unreal/Godot)、ApiHallucinationDetector API 幻觉检测 + 引擎 API 查询、Engine 枚举 (UNITY/UNREAL/GODOT) |
| **tools/sandbox** | SandboxWriter 租户隔离沙箱写入 (防 path traversal)、workspace/output/ 输出隔离 |
| **compat** | ConfigPathMapper OpenClaw → GameClaw 配置键映射 |

### 供应商模块 (providers)

| 模块 | 功能点 |
|------|--------|
| **anthropic** | AnthropicAgentOnboardingProvider (claude-sonnet-4-6)、Claude Code OAuth Token 自动发现 (macOS Keychain / Linux credentials)、自定义 Backend (Bearer Token + anthropic-beta) |
| **openai** | OpenAIAgentOnboardingProvider (gpt-5.4) |
| **ollama** | OllamaAgentOnboardingProvider (qwen3.5:27b, 无需 API Key) |
| **google** | GoogleGenAIAgentOnboardingProvider (gemini-3-flash-preview) |

### 插件模块 (plugins)

| 模块 | 功能点 |
|------|--------|
| **feishu** | FeishuChannel 渠道实现、FeishuEventController 事件回调 (HMAC-SHA256 签名验证 + Nonce 防重放)、FeishuApiClient (tenant_access_token 自动管理)、FeishuCardBuilder 卡片消息 (markdown/代码块/表格/按钮)、SlashCommandRouter 斜杠命令 (/design /query /review)、FeishuTenantRegistry 租户映射 |
| **telegram** | TelegramChannel (SpringLongPollingBot)、Markdown→HTML 转换、用户白名单、线程对话 |
| **discord** | DiscordChannel (JDA ListenerAdapter)、私聊/@触发、用户白名单 |
| **brave** | Brave Web Search 自动配置 (ConditionalOnProperty) |
| **playwright** | PlaywrightBrowserTool 浏览器自动化 (导航/点击/填表/提取/截图/JS执行) |

### 引擎 API 索引 (workspace/game-skills)

| 引擎 | API 数量 | 说明 |
|------|----------|------|
| Unity | 269 | UnityEngine / UnityEditor 核心 API |
| Unreal | 151 | U/A/F 前缀 + GEngine/GetWorld |
| Godot | 196 | GDScript 核心 + Server API |

## 快速开始

### 环境要求

- JDK 25+
- Maven 3.9+
- PostgreSQL 16（多租户模式）

### 启动（H2 单租户模式）

```bash
export JAVA_HOME=/path/to/jdk-25
mvn compile
mvn spring-boot:run -pl app
```

启动后访问：
- Web 界面：http://localhost:8090
- 引导向导：http://localhost:8090/onboarding
- Prometheus：http://localhost:8090/actuator/prometheus
- JobRunr：http://localhost:8091/dashboard

### 启动（PG16 多租户模式）

```bash
cd deploy/docker && docker-compose up -d
# 修改 application.yaml: datasource.url / flyway.enabled=true / multi-tenancy.enabled=true
mvn spring-boot:run -pl app
```

## 配置参考

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8090 | HTTP 端口 |
| `gameclaw.llm.adapter` | spring-ai | LLM 适配器 (spring-ai / langchain4j) |
| `gameclaw.workspace` | file:./workspace/ | 工作空间路径 |
| `gameclaw.multi-tenancy.enabled` | false | 多租户开关 |
| `gameclaw.security.mode` | dev | 安全模式 (dev / sso) |
| `gameclaw.security.rbac.enabled` | true | RBAC 开关 |
| `gameclaw.security.outbound.enabled` | true | 出站白名单开关 |
| `gameclaw.audit.enabled` | true | 审计日志开关 |
| `spring.threads.virtual.enabled` | true | 虚拟线程开关 |
| `spring.flyway.enabled` | false | Flyway 迁移开关 |

## License

GNU Lesser General Public License v3.0 — see [LICENSE](LICENSE)
