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
│   ├── google/              # Google Gemini
│   ├── deepseek/            # DeepSeek (V3/R1)
│   ├── mistral/             # Mistral AI (Large/Codestral)
│   ├── minimax/             # MiniMax (Text-01/abab)
│   └── compat-openai/       # OpenAI 兼容供应商聚合（18 家）
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
| **deepseek** | DeepSeekAgentOnboardingProvider (deepseek-chat)、DeepSeek-V3 通用对话 / DeepSeek-R1 深度推理 |
| **mistral** | MistralAgentOnboardingProvider (mistral-large-latest)、Mistral Large 通用对话 / Codestral 代码生成 |
| **minimax** | MiniMaxAgentOnboardingProvider (MiniMax-Text-01)、中文对话 / 长上下文理解 |

### OpenAI 兼容供应商模块 (compat-openai)

基于 `OpenAICompatibleProvider` 抽象基类，统一复用 `spring-ai-starter-model-openai` + 自定义 `base-url`，所有供应商共享 OpenAI 协议的参数标准化与错误处理。

| 供应商 | 默认模型 | API 端点 | 推荐场景 |
|--------|----------|----------|----------|
| **Groq** | llama-3.3-70b-versatile | api.groq.com/openai/v1 | LPU 超低延迟推理、实时对话 |
| **xAI Grok** | grok-4-latest | api.x.ai/v1 | Grok 系列模型、实时信息 |
| **OpenRouter** | anthropic/claude-sonnet-4-6 | openrouter.ai/api/v1 | 300+ 模型统一网关、跨供应商路由 |
| **Hugging Face** | meta-llama/Llama-3.3-70B-Instruct | api-inference.huggingface.co/v1 | 开源模型推理、Serverless API |
| **GitHub Copilot** | gpt-4o | models.inference.ai.azure.com | GitHub Models、GitHub PAT 认证 |
| **Qwen (通义千问)** | qwen-max-latest | dashscope.aliyuncs.com/compatible-mode/v1 | 阿里云 DashScope、中文对话 |
| **Qianfan (千帆)** | ernie-4.5-turbo-128k | qianfan.baidubce.com/v2 | 百度 ERNIE 系列、中文理解 |
| **Moonshot (Kimi)** | moonshot-v1-128k | api.moonshot.cn/v1 | 长上下文 (128K)、文档理解 |
| **StepFun (阶跃星辰)** | step-2-16k | api.stepfun.com/v1 | 多模态、Step-2 系列 |
| **Tencent Cloud (混元)** | hunyuan-turbos-latest | api.hunyuan.cloud.tencent.com/v1 | 腾讯混元、中文对话 |
| **Volcengine (火山引擎)** | doubao-1.5-pro-32k | ark.cn-beijing.volces.com/api/v3 | 字节豆包、国内合规 |
| **BytePlus** | skylark-pro-32k | ark.ap-southeast.bytepluses.com/api/v3 | 海外 ModelArk、国际合规 |
| **Z.AI (智谱 GLM)** | glm-4.5 | open.bigmodel.cn/api/paas/v4 | 智谱 GLM-4.5、代码生成 |
| **Xiaomi (MiLM)** | mimo-7b-rl | api.xiaomi.com/v1 | 小米 MiLM、轻量推理 |
| **Alibaba Model Studio** | qwen-max-latest | dashscope-intl.aliyuncs.com/compatible-mode/v1 | 阿里云百炼海外站、国际合规 |
| **SenseNova (商汤)** | SenseChat-5 | api.sensenova.cn/compatible-mode/v1 | 商汤日日新、多模态 |
| **Synthetic** | hf:meta-llama/Llama-3.3-70B-Instruct | api.synthetic.new/v1 | 开源模型托管、多模型聚合 |
| **SiliconFlow (硅基流动)** | Qwen/Qwen2.5-72B-Instruct | api.siliconflow.cn/v1 | 硅基流动 SiliconCloud、开源模型聚合 |

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

## 启动后能做什么

### 开箱即用（无需额外配置）

| 能力 | 说明 |
|------|------|
| Web 聊天界面 | http://localhost:8090/chat — 角色选择器（策划/程序员/QA 等）、消息输入框、工具列表展示 |
| 引导向导 | http://localhost:8090/onboarding — 10 步引导配置 LLM 供应商、API Key、角色绑定 |
| SSE 流式对话 | `/api/chat/sse` 端点可用，支持流式输出 |
| 角色切换 | 前端可切换 10 种角色，不同角色看到不同工具集 |
| JobRunr 后台任务 | http://localhost:8091/dashboard — 任务调度面板 |
| Prometheus 指标 | http://localhost:8090/actuator/prometheus — 7 类 AI 指标 |
| API 幻觉检测 | 引擎 API 索引已加载（Unity 269 / Unreal 151 / Godot 196），可查询引擎 API |

### 配置 LLM 后可用

完成 Onboarding（选择供应商 + 填 API Key）后，以下能力全部可用：

| 能力 | 工具名 | 说明 |
|------|--------|------|
| 生成怪物配置 | `generate_monsters` | 输入描述 → 生成 JSON 配置表 → 写入沙箱 |
| 生成技能配置 | `generate_skills` | 同上 |
| 生成道具配置 | `generate_items` | 同上 |
| 生成任务配置 | `generate_quests` | 同上 |
| 生成成长曲线 | `generate_growth_curve` | 输入参数 → 生成 CSV → 写入沙箱 |
| 生成 Unity 脚本 | `generate_unity_script` | 输入需求 → 生成 C# 代码 + API 幻觉检测 → 写入沙箱 |
| 生成 Unreal 代码 | `generate_unreal_script` | 同上，C++ |
| 生成 Godot 脚本 | `generate_godot_script` | 同上，GDScript |
| 查询引擎 API | `query_engine_api` | 自然语言查 API（如"Unity 如何加载场景"→ SceneManager.LoadScene） |
| 任务创建/调度 | `TaskTool` | 创建一次性/定时/周期任务 |
| 清单管理 | `CheckListTool` | 创建结构化清单 |
| MCP 服务器注册 | `McpTool` | 运行时添加 MCP 服务器 |
| 浏览器自动化 | Playwright 工具 | 导航/点击/填表/截图（需启用 playwright 插件） |

### 配置渠道后可用

| 渠道 | 配置项 | 能力 |
|------|--------|------|
| 飞书 | `agent.channels.feishu.app-id/secret` | 飞书群聊对话、卡片消息回复、斜杠命令 |
| Telegram | `agent.channels.telegram.token` | Bot 对话、用户白名单 |
| Discord | `agent.channels.discord.token` | Bot 对话、@触发 |

## 使用前准备

### 环境清单

| 依赖 | 最低版本 | 必需 | 说明 |
|------|----------|------|------|
| JDK | 25 | ✅ | 下载 [jdk-25](https://jdk.java.net/25/)，设置 `JAVA_HOME` |
| Maven | 3.9 | ✅ | 构建 + 启动项目 |
| LLM API Key | — | ✅ | 至少准备一个：Anthropic / OpenAI / Google Gemini / DeepSeek / Mistral / MiniMax / Groq / xAI / OpenRouter / Qwen / Moonshot / Z.AI / SiliconFlow / Ollama 本地 等 25 家供应商 |
| PostgreSQL | 16 | ❌ | 仅多租户模式需要，单租户用内嵌 H2 |
| Docker | — | ❌ | 仅 PG16 多租户模式需要 |

### LLM 供应商选择

| 供应商 | 需要 API Key | 配置项 | 推荐场景 |
|--------|-------------|--------|----------|
| Ollama | ❌ 无需 | 安装 [Ollama](https://ollama.ai) → `ollama pull qwen3.5:27b` | 本地开发、零成本体验 |
| Anthropic | ✅ | `spring.ai.anthropic.api-key` | 生产环境、Claude 系列模型 |
| OpenAI | ✅ | `spring.ai.openai.api-key` | 生产环境、GPT 系列模型 |
| Google | ✅ | `spring.ai.gemini.api-key` | 生产环境、Gemini 系列模型 |
| DeepSeek | ✅ | `spring.ai.deepseek.api-key` | 深度推理 (R1)、高性价比对话 (V3)、游戏策划辅助 |
| Mistral AI | ✅ | `spring.ai.mistral-ai.api-key` | 代码生成 (Codestral)、多语言对话、欧洲合规 |
| MiniMax | ✅ | `spring.ai.minimax.api-key` | 中文对话、长上下文理解、国内合规 |
| Groq | ✅ | `spring.ai.openai.api-key` + `base-url: api.groq.com/openai/v1` | LPU 超低延迟推理、实时对话 |
| xAI Grok | ✅ | `spring.ai.openai.api-key` + `base-url: api.x.ai/v1` | Grok 系列模型、实时信息 |
| OpenRouter | ✅ | `spring.ai.openai.api-key` + `base-url: openrouter.ai/api/v1` | 300+ 模型统一网关、跨供应商路由 |
| Hugging Face | ✅ | `spring.ai.openai.api-key` + `base-url: api-inference.huggingface.co/v1` | 开源模型推理、Serverless API |
| GitHub Copilot | ✅ | `spring.ai.openai.api-key` + `base-url: models.inference.ai.azure.com` | GitHub Models、GitHub PAT 认证 |
| Qwen (通义千问) | ✅ | `spring.ai.openai.api-key` + `base-url: dashscope.aliyuncs.com/compatible-mode/v1` | 阿里云 DashScope、中文对话 |
| Qianfan (千帆) | ✅ | `spring.ai.openai.api-key` + `base-url: qianfan.baidubce.com/v2` | 百度 ERNIE 系列、中文理解 |
| Moonshot (Kimi) | ✅ | `spring.ai.openai.api-key` + `base-url: api.moonshot.cn/v1` | 长上下文 (128K)、文档理解 |
| StepFun (阶跃星辰) | ✅ | `spring.ai.openai.api-key` + `base-url: api.stepfun.com/v1` | 多模态、Step-2 系列 |
| Tencent Cloud (混元) | ✅ | `spring.ai.openai.api-key` + `base-url: api.hunyuan.cloud.tencent.com/v1` | 腾讯混元、中文对话 |
| Volcengine (火山引擎) | ✅ | `spring.ai.openai.api-key` + `base-url: ark.cn-beijing.volces.com/api/v3` | 字节豆包、国内合规 |
| BytePlus | ✅ | `spring.ai.openai.api-key` + `base-url: ark.ap-southeast.bytepluses.com/api/v3` | 海外 ModelArk、国际合规 |
| Z.AI (智谱 GLM) | ✅ | `spring.ai.openai.api-key` + `base-url: open.bigmodel.cn/api/paas/v4` | 智谱 GLM-4.5、代码生成 |
| Xiaomi (MiLM) | ✅ | `spring.ai.openai.api-key` + `base-url: api.xiaomi.com/v1` | 小米 MiLM、轻量推理 |
| Alibaba Model Studio | ✅ | `spring.ai.openai.api-key` + `base-url: dashscope-intl.aliyuncs.com/compatible-mode/v1` | 阿里云百炼海外站、国际合规 |
| SenseNova (商汤) | ✅ | `spring.ai.openai.api-key` + `base-url: api.sensenova.cn/compatible-mode/v1` | 商汤日日新、多模态 |
| Synthetic | ✅ | `spring.ai.openai.api-key` + `base-url: api.synthetic.new/v1` | 开源模型托管、多模型聚合 |
| SiliconFlow (硅基流动) | ✅ | `spring.ai.openai.api-key` + `base-url: api.siliconflow.cn/v1` | 硅基流动 SiliconCloud、开源模型聚合 |

## 快速开始

### 1. 克隆 & 编译

```bash
git clone https://github.com/your-org/claw.git
cd claw/GameClaw

# Windows
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"

# macOS / Linux
export JAVA_HOME=/path/to/jdk-25

mvn compile
```

### 2. 配置 LLM（二选一）

**方式 A：Ollama 本地模型（零成本）**

```bash
# 安装 Ollama 并拉取模型
ollama pull qwen3.5:27b
```

启动后在 Onboarding 向导中选择 Ollama 即可，无需填写 API Key。

**方式 B：云端 API Key**

创建 `app/src/main/resources/application.private.yaml`（已被 .gitignore 忽略）：

```yaml
# Anthropic
spring.ai.anthropic.api-key: sk-ant-xxx

# 或 OpenAI
spring.ai.openai.api-key: sk-xxx

# 或 Google Gemini
spring.ai.gemini.api-key: AIzaxxx

# 或 DeepSeek
spring.ai.deepseek.api-key: sk-xxx

# 或 Mistral AI
spring.ai.mistral-ai.api-key: xxx

# 或 MiniMax
spring.ai.minimax.api-key: xxx

# 或 OpenAI 兼容供应商（以 Groq 为例，Onboarding 向导会自动设置 base-url）
# spring.ai.openai.api-key: gsk_xxx
# spring.ai.openai.base-url: https://api.groq.com/openai/v1
```

> `application.yaml` 中已配置 `spring.config.import: optional:classpath:application.private.yaml`，私有配置会自动加载。

### 3. 启动（H2 单租户模式）

```bash
mvn spring-boot:run -pl app
```

启动成功后访问：
- Web 界面：http://localhost:8090
- 引导向导：http://localhost:8090/onboarding（首次启动必须完成引导）
- Prometheus：http://localhost:8090/actuator/prometheus
- JobRunr：http://localhost:8091/dashboard

### 4. 完成 Onboarding 引导

首次启动后访问 http://localhost:8090/onboarding，按步骤完成：

1. 选择 LLM 供应商
2. 填写 API Key（Ollama 跳过）
3. 选择默认模型
4. 绑定用户角色（策划/程序员/QA 等）
5. 完成后即可开始对话

### 5. 启动（PG16 多租户模式，可选）

```bash
# 启动 PostgreSQL 容器
cd deploy/docker
docker-compose up -d

# 修改 app/src/main/resources/application.yaml
# spring.datasource.url=jdbc:postgresql://localhost:5432/gameclaw
# spring.datasource.username=linyi
# spring.datasource.password=your_password
# spring.flyway.enabled=true
# gameclaw.multi-tenancy.enabled=true

mvn spring-boot:run -pl app
```

### 6. 配置渠道（可选）

在 `application.private.yaml` 中添加：

```yaml
# 飞书
agent.channels.feishu.app-id: cli_xxx
agent.channels.feishu.app-secret: xxx
agent.channels.feishu.verification-token: xxx

# Telegram
agent.channels.telegram.token: 123456:ABC-xxx
agent.channels.telegram.username: your_bot

# Discord
agent.channels.discord.token: your-discord-bot-token
agent.channels.discord.allowed-user: your-user-id
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
