<div align="center">

# GameClaw

**Game Enterprise R&D AI Agent Control Plane**

面向游戏企业研发场景的 AI Agent 控制平面

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://jdk.java.net/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M6-6DB33F?logo=spring)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/License-LGPL%20v3-blue)](LICENSE)

[功能特性](#功能特性) · [快速开始](#快速开始) · [架构](#架构) · [配置](#配置参考) · [贡献](#贡献)

</div>

---

## GameClaw 是什么

GameClaw 是一个专为游戏研发团队设计的 AI Agent 控制平面。它让策划、程序员、QA、运维等角色通过自然语言与 AI 协作，完成游戏配置生成、代码编写、数据查询、测试自动化等日常工作。

**核心价值**：

- **游戏专属** — 内置 Unity / Unreal / Godot 三引擎 API 索引 + 幻觉检测，AI 不会编造不存在的 API
- **五层安全** — 网络 TLS → 接入 OAuth2 → 应用 RBAC → 数据 RLS → 审计日志，企业级安全开箱即用
- **多租户隔离** — PostgreSQL 16 Row-Level Security，不同项目/团队数据严格隔离
- **25+ LLM 供应商** — Anthropic / OpenAI / DeepSeek / Ollama / Groq / 通义千问 / Kimi 等，一键切换
- **全渠道接入** — Web / 飞书 / Telegram / Discord，同一个 Agent 多端触达
- **插件生态** — OpenClaw L3 兼容 + Skills 热重载 + MCP 协议，第三方开发者可扩展

---

## 功能特性

### 策划工具

| 工具 | 说明 |
|------|------|
| `generate_monsters` | 自然语言描述 → 生成怪物 JSON 配置表 |
| `generate_skills` | 生成技能配置 |
| `generate_items` | 生成道具配置 |
| `generate_quests` | 生成任务配置 |
| `generate_growth_curve` | 生成成长曲线 CSV |
| `query_engine_api` | 查询引擎 API（"Unity 如何加载场景"→ SceneManager.LoadScene） |

### 程序员工具

| 工具 | 说明 |
|------|------|
| `generate_unity_script` | 需求 → C# 代码 + API 幻觉检测 → 写入沙箱 |
| `generate_unreal_script` | 同上，C++ |
| `generate_godot_script` | 同上，GDScript |

### 数据分析工具

| 工具 | 说明 |
|------|------|
| `query_data` | 自然语言 → SQL → 双层校验 → 执行 → PII 脱敏返回 |

### 治理与安全

| 层级 | 能力 |
|------|------|
| 闸门 1 | Schema 校验（Jackson + Bean Validation） |
| 闸门 2 | 规则引擎（10 条默认规则 + YAML 自定义） |
| RBAC | 5 级风险 × 10 种角色，`@RequireRole` / `@RequireRiskLevel` 注解 |
| 配额 | 三级配额（用户日预算 / 项目月预算 / 全局日预算） |
| PII | 自动脱敏 + 角色化解密 |

### 插件与扩展

| 能力 | 说明 |
|------|------|
| OpenClaw L3 兼容 | `@OpenClawPlugin` + `PluginClassLoader` 隔离 + 资源沙箱 |
| Skills 热重载 | 修改 SKILL.md 文件 250ms 内自动重载 |
| MCP 协议 | Spring AI MCP Client + 自建 MCP Server |
| ClawHub 技能市场 | install / search / update CLI 命令 |

---

## 架构

```
┌─────────────────────────────────────────────────────────┐
│                    Channels (渠道层)                       │
│   Web Chat │ Feishu │ Telegram │ Discord │ Slack │ WeCom │
├─────────────────────────────────────────────────────────┤
│                    Agent (Agent 核心)                      │
│  LlmClient │ ModelRouter │ FallbackChain │ ChatMemory    │
├─────────────────────────────────────────────────────────┤
│                    Tools (工具层)                          │
│  GameDesign │ GameCode │ GameData │ Sandbox │ MCP │ CLI  │
├─────────────────────────────────────────────────────────┤
│                 Governance (治理层)                        │
│  Gate1 Schema │ Gate2 Rules │ Gate3 Impact │ Gate4 Human │
├─────────────────────────────────────────────────────────┤
│                 Infrastructure (基础设施)                  │
│  Security(RBAC/RLS/PII) │ Observability │ Quota │ Skills │
└─────────────────────────────────────────────────────────┘
```

### 项目结构

```
GameClaw/
├── base/                    # 核心模块（20 个子包）
│   ├── agent/               # Agent 核心 + LLM 抽象
│   ├── channels/            # 渠道注册 + 会话管理
│   ├── compat/              # OpenClaw 兼容层 + L3 插件系统
│   ├── concurrency/         # StructuredTaskScope 封装
│   ├── configuration/       # YAML/JSON 双格式配置
│   ├── cost/                # 三级配额管理
│   ├── files/               # YAML frontmatter 解析
│   ├── governance/          # 四层闸门 + 规则引擎
│   ├── mcp/                 # MCP 连接配置
│   ├── observability/       # Micrometer 指标 + 审计
│   ├── persistence/         # 多租户数据源 + RLS
│   ├── project/             # 项目管理
│   ├── security/            # RBAC + PII + Prompt 注入检测
│   ├── skills/              # Skills 解析 + 热重载 + ClawHub
│   ├── tasks/               # JobRunr 任务调度
│   └── tools/               # 游戏工具 + 数据工具 + 沙箱
├── providers/               # LLM 供应商（7 家原生 + 18 家 OpenAI 兼容）
├── plugins/                 # 渠道插件（飞书/Telegram/Discord/Brave/Playwright）
├── mcp-servers/java/        # MCP Server（ClickHouse）
├── app/                     # Spring Boot 启动模块 + CLI
└── deploy/docker/           # Docker 部署 (PG16)
```

### 技术栈

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

---

## 快速开始

### 环境要求

| 依赖 | 最低版本 | 必需 | 说明 |
|------|----------|------|------|
| JDK | 25 | Yes | [下载 jdk-25](https://jdk.java.net/25/) |
| Maven | 3.9 | Yes | 构建 + 启动 |
| LLM API Key | — | Yes | 25+ 供应商可选（Ollama 可零成本本地运行） |
| PostgreSQL | 16 | No | 仅多租户模式，单租户用内嵌 H2 |
| Docker | — | No | 仅 PG16 多租户模式 |

### 1. 克隆 & 编译

```bash
git clone https://github.com/linyshdhhcb/GameClaw.git
cd GameClaw

# Windows
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"

# macOS / Linux
export JAVA_HOME=/path/to/jdk-25

mvn compile
```

### 2. 配置 LLM

**方式 A：Ollama 本地模型（零成本）**

```bash
ollama pull qwen3.5:27b
```

启动后在 Onboarding 向导中选择 Ollama，无需 API Key。

**方式 B：云端 API Key**

创建 `app/src/main/resources/application.private.yaml`（已 .gitignore）：

```yaml
spring.ai.model.chat: deepseek
spring.ai.deepseek.api-key: sk-xxx
```

> `spring.ai.model.chat` 必须显式指定，默认值 `unknown` 不激活任何模型。

### 3. 启动

```bash
mvn spring-boot:run -pl app
```

启动成功后访问：
- Web 界面：http://localhost:8090
- 引导向导：http://localhost:8090/onboarding
- Prometheus：http://localhost:8090/actuator/prometheus
- JobRunr：http://localhost:8091/dashboard

### 4. 完成 Onboarding

首次启动访问 http://localhost:8090/onboarding → 选择供应商 → 填 API Key → 选模型 → 绑定角色 → 开始对话

### 5. 多租户模式（可选）

```bash
cd deploy/docker && docker-compose up -d
# 修改 application.yaml: spring.datasource.url / flyway.enabled / multi-tenancy.enabled
mvn spring-boot:run -pl app
```

### 6. 配置渠道（可选）

```yaml
# 飞书
agent.channels.feishu.app-id: cli_xxx
agent.channels.feishu.app-secret: xxx
agent.channels.feishu.verification-token: xxx

# Telegram
agent.channels.telegram.token: 123456:ABC-xxx

# Discord
agent.channels.discord.token: your-discord-bot-token
```

---

## LLM 供应商

### 原生供应商

| 供应商 | 默认模型 | 推荐场景 |
|--------|----------|----------|
| Anthropic | claude-sonnet-4-6 | 生产环境、Claude 系列 |
| OpenAI | gpt-5.4 | 生产环境、GPT 系列 |
| Ollama | qwen3.5:27b | 本地开发、零成本体验 |
| Google Gemini | gemini-3-flash-preview | 生产环境、Gemini 系列 |
| DeepSeek | deepseek-chat | 深度推理 (R1)、游戏策划辅助 |
| Mistral AI | mistral-large-latest | 代码生成 (Codestral)、欧洲合规 |
| MiniMax | MiniMax-Text-01 | 中文对话、长上下文 |

### OpenAI 兼容供应商（18 家）

基于 `OpenAICompatibleProvider` 抽象基类，统一复用 `spring-ai-starter-model-openai` + 自定义 `base-url`：

| 供应商 | 推荐场景 |
|--------|----------|
| Groq | LPU 超低延迟推理 |
| xAI Grok | Grok 系列模型 |
| OpenRouter | 300+ 模型统一网关 |
| Hugging Face | 开源模型 Serverless API |
| GitHub Copilot | GitHub Models |
| Qwen (通义千问) | 阿里云 DashScope |
| Qianfan (千帆) | 百度 ERNIE 系列 |
| Moonshot (Kimi) | 长上下文 128K |
| StepFun (阶跃星辰) | 多模态 |
| Tencent Cloud (混元) | 腾讯混元 |
| Volcengine (火山引擎) | 字节豆包 |
| BytePlus | 海外 ModelArk |
| Z.AI (智谱 GLM) | GLM-4.5 代码生成 |
| Xiaomi (MiLM) | 轻量推理 |
| Alibaba Model Studio | 百炼海外站 |
| SenseNova (商汤) | 多模态 |
| Synthetic | 开源模型托管 |
| SiliconFlow (硅基流动) | 开源模型聚合 |

---

## 插件与扩展

### 渠道插件

| 插件 | 说明 |
|------|------|
| **feishu** | 飞书 Bot（HMAC 签名 + 卡片消息 + 斜杠命令） |
| **telegram** | Telegram Bot（Markdown→HTML + 白名单） |
| **discord** | Discord Bot（@触发 + 白名单） |
| **brave** | Brave Web Search |
| **playwright** | 浏览器自动化（导航/点击/截图/JS执行） |

### MCP Server

| Server | 说明 |
|--------|------|
| **clickhouse-mcp-server** | ClickHouse 数据查询（SELECT-only + Bearer Token 鉴权） |

### CLI 命令

```bash
gameclaw skill install <name>     # 安装技能包
gameclaw skill search <query>     # 搜索技能市场
gameclaw skill update --all       # 更新所有技能
gameclaw skill list --installed   # 列出已安装技能
gameclaw quota check <tenantId>   # 检查配额
gameclaw quota remaining <tenantId> # 查看剩余配额
```

### 引擎 API 索引

| 引擎 | API 数量 |
|------|----------|
| Unity | 269 |
| Unreal | 151 |
| Godot | 196 |

---

## 配置参考

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8090 | HTTP 端口 |
| `spring.ai.model.chat` | unknown | 激活的 LLM 供应商（必须显式指定） |
| `gameclaw.llm.adapter` | spring-ai | LLM 适配器 (spring-ai / langchain4j) |
| `gameclaw.workspace` | file:./workspace/ | 工作空间路径 |
| `gameclaw.multi-tenancy.enabled` | false | 多租户开关 |
| `gameclaw.security.mode` | dev | 安全模式 (dev / sso) |
| `gameclaw.security.rbac.enabled` | true | RBAC 开关 |
| `gameclaw.security.outbound.enabled` | true | 出站白名单开关 |
| `gameclaw.audit.enabled` | true | 审计日志开关 |
| `spring.threads.virtual.enabled` | true | 虚拟线程开关 |
| `spring.flyway.enabled` | false | Flyway 迁移开关 |
| `gameclaw.quota.enabled` | true | 配额管理开关 |
| `gameclaw.quota.user-daily-limit` | 1.0 | 用户日预算 (CNY) |
| `gameclaw.quota.project-monthly-limit` | 1000.0 | 项目月预算 (CNY) |
| `gameclaw.quota.global-daily-limit` | 10000.0 | 全局日预算 (CNY) |
| `gameclaw.llm.model-map.*` | haiku/sonnet/opus | 复杂度→模型映射 |
| `gameclaw.llm.fallback-map.*` | sonnet/haiku/sonnet | 复杂度→降级模型映射 |
| `gameclaw.skills.polling-interval` | 0 | Skills 轮询间隔 (ms, 0=禁用) |
| `gameclaw.clawhub.enabled` | false | ClawHub 技能市场开关 |
| `gameclaw.clawhub.registry-url` | https://registry.clawhub.io | ClawHub 注册中心 |
| `gameclaw.plugins.enabled` | false | OpenClaw L3 插件系统开关 |
| `spring.messages.basename` | i18n/messages | i18n 消息资源路径 |

---

## 安全模型

GameClaw 采用五层纵深防御架构：

| 层级 | 组件 | 说明 |
|------|------|------|
| L1 网络 | TLS 1.3 + OutboundUrlFilter | 传输加密 + 出站白名单 |
| L2 接入 | Spring Security + OAuth2 | 认证鉴权 |
| L3 应用 | RBAC + PromptSanitizer | 5级风险×10角色 + 7种注入检测 |
| L4 数据 | RLS + PiiMasking | PostgreSQL 行级安全 + PII 脱敏 |
| L5 审计 | AuditLogger | 全链路审计日志 |

---

## 贡献

欢迎贡献！请遵循以下流程：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交改动 (`git commit -m 'feat: add amazing feature'`)
4. 推送分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 开发规范

- JDK 25 + Maven
- 遵循 Spring Modulith 模块边界
- 单元测试使用 AssertJ + JUnit 5
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)

---

## License

GNU Lesser General Public License v3.0 — see [LICENSE](LICENSE)

---

## 作者

**linyi**

联系：jingshuihuayue@qq.com

## 项目参考

GameClaw 的设计与实现参考了以下优秀项目：

| 项目 | 说明 |
|------|------|
| [OpenClaw](https://github.com/openclaw/openclaw) | AI Agent 开源规范与兼容接口定义 |
| [GoClaw](https://github.com/nextlevelbuilder/goclaw) | Go 语言实现的 AI Agent 控制平面 |
| [JavaClaw](https://github.com/jobrunr/JavaClaw) | Java 语言实现的 AI Agent 框架，GameClaw 的迁移前身 |
