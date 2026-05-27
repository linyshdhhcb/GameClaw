<div align="center">

# GameClaw

**Game Enterprise R&D AI Agent Control Plane**

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://jdk.java.net/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M6-6DB33F?logo=spring)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/License-LGPL%20v3-blue)](LICENSE)

**English** | [简体中文](docs/readme/README.zh-CN.md)

[Features](#features) · [Quick Start](#quick-start) · [Architecture](#architecture) · [Configuration](#configuration) · [Contributing](#contributing)

</div>

---

## What is GameClaw

GameClaw is an AI Agent control plane designed for game development teams. It enables designers, programmers, QA, and ops to collaborate with AI through natural language, handling daily tasks such as game config generation, code writing, data queries, and test automation.

**Core Values**:

- **Game-Specific** — Built-in Unity / Unreal / Godot API index + hallucination detection; AI won't fabricate non-existent APIs
- **Five-Layer Security** — Network TLS → Access OAuth2 → Application RBAC → Data RLS → Audit logging, enterprise-grade security out of the box
- **Multi-Tenant Isolation** — PostgreSQL 16 Row-Level Security, strict data isolation across projects and teams
- **25+ LLM Providers** — Anthropic / OpenAI / DeepSeek / Ollama / Groq / Qwen / Kimi, switch with a single config
- **Omnichannel Access** — Web / Feishu / Telegram / Discord, the same Agent across all platforms
- **Plugin Ecosystem** — OpenClaw L3 compatibility + Skills hot-reload + MCP protocol, extensible by third-party developers

---

## Features

### Game Design Tools

| Tool | Description |
|------|-------------|
| `generate_monsters` | Natural language description → monster JSON config table |
| `generate_skills` | Generate skill configs |
| `generate_items` | Generate item configs |
| `generate_quests` | Generate quest configs |
| `generate_growth_curve` | Generate growth curve CSV |
| `query_engine_api` | Query engine APIs ("How to load a scene in Unity" → SceneManager.LoadScene) |

### Programmer Tools

| Tool | Description |
|------|-------------|
| `generate_unity_script` | Requirement → C# code + API hallucination check → write to sandbox |
| `generate_unreal_script` | Same as above, C++ |
| `generate_godot_script` | Same as above, GDScript |

### Data Analysis Tools

| Tool | Description |
|------|-------------|
| `query_data` | Natural language → SQL → dual-layer validation → execute → PII-masked response |

### Governance & Security

| Layer | Capability |
|-------|------------|
| Gate 1 | Schema validation (Jackson + Bean Validation) |
| Gate 2 | Rule engine (10 default rules + YAML custom rules) |
| RBAC | 5 risk levels x 10 roles, `@RequireRole` / `@RequireRiskLevel` annotations |
| Quota | Three-tier quotas (user daily / project monthly / global daily budget) |
| PII | Auto-masking + role-based decryption |

### Plugins & Extensions

| Capability | Description |
|------------|-------------|
| OpenClaw L3 Compatible | `@OpenClawPlugin` + `PluginClassLoader` isolation + resource sandbox |
| Skills Hot-Reload | Auto-reload within 250ms after SKILL.md file changes |
| MCP Protocol | Spring AI MCP Client + built-in MCP Server |
| ClawHub Skill Market | install / search / update CLI commands |

---

## Architecture

![GameClaw Architecture](../img/architecture.svg)


### Project Structure

```
GameClaw/
├── base/                    # Core module (20 sub-packages)
│   ├── agent/               # Agent core + LLM abstraction
│   ├── channels/            # Channel registration + session management
│   ├── compat/              # OpenClaw compatibility layer + L3 plugin system
│   ├── concurrency/         # StructuredTaskScope wrapper
│   ├── configuration/       # YAML/JSON dual-format config
│   ├── cost/                # Three-tier quota management
│   ├── files/               # YAML frontmatter parsing
│   ├── governance/          # Four-layer gates + rule engine
│   ├── mcp/                 # MCP connection config
│   ├── observability/       # Micrometer metrics + audit
│   ├── persistence/         # Multi-tenant datasource + RLS
│   ├── project/             # Project management
│   ├── security/            # RBAC + PII + Prompt injection detection
│   ├── skills/              # Skills parsing + hot-reload + ClawHub
│   ├── tasks/               # JobRunr task scheduling
│   └── tools/               # Game tools + Data tools + Sandbox
├── providers/               # LLM providers (7 native + 18 OpenAI-compatible)
├── plugins/                 # Channel plugins (Feishu/Telegram/Discord/Brave/Playwright)
├── mcp-servers/java/        # MCP Server (ClickHouse)
├── app/                     # Spring Boot entry module + CLI
└── deploy/docker/           # Docker deployment (PG16)
```

### Tech Stack

| Component | Version | Description |
|-----------|---------|-------------|
| Java | 25 | Virtual Threads GA / ScopedValue GA / StructuredTaskScope |
| Spring Boot | 4.0.6 | Virtual threads auto-enabled |
| Spring AI | 2.0.0-M6 | LLM abstraction layer + MCP Client |
| Spring Modulith | 2.0.6 | Module boundary enforcement |
| PostgreSQL | 16 | Row-Level Security multi-tenant isolation |
| Resilience4j | 2.4.0 | Bulkhead / RateLimiter / CircuitBreaker |
| JobRunr | 8.6.0 | Background task scheduling |
| Flyway | — | Database migration |
| Testcontainers | 1.21.4 | Integration testing |

---

## Quick Start

### Prerequisites

| Dependency | Min Version | Required | Description |
|------------|-------------|----------|-------------|
| JDK | 25 | Yes | [Download jdk-25](https://jdk.java.net/25/) |
| Maven | 3.9 | Yes | Build + Run |
| LLM API Key | — | Yes | 25+ providers available (Ollama for zero-cost local) |
| PostgreSQL | 16 | No | Multi-tenant mode only; embedded H2 for single-tenant |
| Docker | — | No | PG16 multi-tenant mode only |

### 1. Clone & Build

```bash
git clone https://github.com/linyshdhhcb/GameClaw.git
cd GameClaw

# Windows
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"

# macOS / Linux
export JAVA_HOME=/path/to/jdk-25

mvn compile
```

### 2. Configure LLM

**Option A: Ollama Local Model (Zero Cost)**

```bash
ollama pull qwen3.5:27b
```

Select Ollama in the Onboarding wizard after startup. No API Key required.

**Option B: Cloud API Key**

Create `app/src/main/resources/application.private.yaml` (already .gitignored):

```yaml
spring.ai.model.chat: deepseek
spring.ai.deepseek.api-key: sk-xxx
```

> `spring.ai.model.chat` must be explicitly set; the default value `unknown` does not activate any model.

### 3. Run

```bash
mvn spring-boot:run -pl app
```

After successful startup, visit:
- Web UI: http://localhost:8090
- Onboarding wizard: http://localhost:8090/onboarding
- Prometheus: http://localhost:8090/actuator/prometheus
- JobRunr: http://localhost:8091/dashboard

### 4. Complete Onboarding

On first launch, visit http://localhost:8090/onboarding → Select provider → Enter API Key → Choose model → Assign role → Start chatting

### 5. Multi-Tenant Mode (Optional)

```bash
cd deploy/docker && docker-compose up -d
# Update application.yaml: spring.datasource.url / flyway.enabled / multi-tenancy.enabled
mvn spring-boot:run -pl app
```

### 6. Configure Channels (Optional)

```yaml
# Feishu
agent.channels.feishu.app-id: cli_xxx
agent.channels.feishu.app-secret: xxx
agent.channels.feishu.verification-token: xxx

# Telegram
agent.channels.telegram.token: 123456:ABC-xxx

# Discord
agent.channels.discord.token: your-discord-bot-token
```

---

## LLM Providers

### Native Providers

| Provider | Default Model | Recommended Use Case |
|----------|---------------|---------------------|
| Anthropic | claude-sonnet-4-6 | Production, Claude series |
| OpenAI | gpt-5.4 | Production, GPT series |
| Ollama | qwen3.5:27b | Local development, zero-cost experience |
| Google Gemini | gemini-3-flash-preview | Production, Gemini series |
| DeepSeek | deepseek-chat | Deep reasoning (R1), game design assistance |
| Mistral AI | mistral-large-latest | Code generation (Codestral), EU compliance |
| MiniMax | MiniMax-Text-01 | Chinese dialogue, long context |

### OpenAI-Compatible Providers (18)

Built on `OpenAICompatibleProvider` abstract base class, reusing `spring-ai-starter-model-openai` + custom `base-url`:

| Provider | Recommended Use Case |
|----------|---------------------|
| Groq | LPU ultra-low-latency inference |
| xAI Grok | Grok series models |
| OpenRouter | 300+ model unified gateway |
| Hugging Face | Open-source model Serverless API |
| GitHub Copilot | GitHub Models |
| Qwen | Alibaba Cloud DashScope |
| Qianfan | Baidu ERNIE series |
| Moonshot (Kimi) | Long context 128K |
| StepFun | Multi-modal |
| Tencent Cloud (Hunyuan) | Tencent Hunyuan |
| Volcengine | ByteDance Doubao |
| BytePlus | Overseas ModelArk |
| Z.AI (Zhipu GLM) | GLM-4.5 code generation |
| Xiaomi (MiLM) | Lightweight inference |
| Alibaba Model Studio | Bailian overseas |
| SenseNova | Multi-modal |
| Synthetic | Open-source model hosting |
| SiliconFlow | Open-source model aggregation |

---

## Plugins & Extensions

### Channel Plugins

| Plugin | Description |
|--------|-------------|
| **feishu** | Feishu Bot (HMAC signature + card messages + slash commands) |
| **telegram** | Telegram Bot (Markdown→HTML + whitelist) |
| **discord** | Discord Bot (@trigger + whitelist) |
| **brave** | Brave Web Search |
| **playwright** | Browser automation (navigate/click/screenshot/JS execution) |

### MCP Server

| Server | Description |
|--------|-------------|
| **clickhouse-mcp-server** | ClickHouse data query (SELECT-only + Bearer Token auth) |

### CLI Commands

```bash
gameclaw skill install <name>     # Install skill package
gameclaw skill search <query>     # Search skill market
gameclaw skill update --all       # Update all skills
gameclaw skill list --installed   # List installed skills
gameclaw quota check <tenantId>   # Check quota
gameclaw quota remaining <tenantId> # View remaining quota
```

### Engine API Index

| Engine | API Count |
|--------|-----------|
| Unity | 269 |
| Unreal | 151 |
| Godot | 196 |

---

## Configuration

| Config Key | Default | Description |
|------------|---------|-------------|
| `server.port` | 8090 | HTTP port |
| `spring.ai.model.chat` | unknown | Active LLM provider (must be explicitly set) |
| `gameclaw.llm.adapter` | spring-ai | LLM adapter (spring-ai / langchain4j) |
| `gameclaw.workspace` | file:./workspace/ | Workspace path |
| `gameclaw.multi-tenancy.enabled` | false | Multi-tenant switch |
| `gameclaw.security.mode` | dev | Security mode (dev / sso) |
| `gameclaw.security.rbac.enabled` | true | RBAC switch |
| `gameclaw.security.outbound.enabled` | true | Outbound whitelist switch |
| `gameclaw.audit.enabled` | true | Audit logging switch |
| `spring.threads.virtual.enabled` | true | Virtual threads switch |
| `spring.flyway.enabled` | false | Flyway migration switch |
| `gameclaw.quota.enabled` | true | Quota management switch |
| `gameclaw.quota.user-daily-limit` | 1.0 | User daily budget (CNY) |
| `gameclaw.quota.project-monthly-limit` | 1000.0 | Project monthly budget (CNY) |
| `gameclaw.quota.global-daily-limit` | 10000.0 | Global daily budget (CNY) |
| `gameclaw.llm.model-map.*` | haiku/sonnet/opus | Complexity → model mapping |
| `gameclaw.llm.fallback-map.*` | sonnet/haiku/sonnet | Complexity → fallback model mapping |
| `gameclaw.skills.polling-interval` | 0 | Skills polling interval (ms, 0=disabled) |
| `gameclaw.clawhub.enabled` | false | ClawHub skill market switch |
| `gameclaw.clawhub.registry-url` | https://registry.clawhub.io | ClawHub registry |
| `gameclaw.plugins.enabled` | false | OpenClaw L3 plugin system switch |
| `spring.messages.basename` | i18n/messages | i18n message resource path |

---

## Security Model

GameClaw employs a five-layer defense-in-depth architecture:

| Layer | Component | Description |
|-------|-----------|-------------|
| L1 Network | TLS 1.3 + OutboundUrlFilter | Transport encryption + outbound whitelist |
| L2 Access | Spring Security + OAuth2 | Authentication & authorization |
| L3 Application | RBAC + PromptSanitizer | 5 risk levels x 10 roles + 7 injection detection patterns |
| L4 Data | RLS + PiiMasking | PostgreSQL row-level security + PII masking |
| L5 Audit | AuditLogger | Full-chain audit logging |

---

## Contributing

Contributions are welcome! Please follow this workflow:

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push the branch (`git push origin feature/amazing-feature`)
5. Create a Pull Request

### Development Guidelines

- JDK 25 + Maven
- Follow Spring Modulith module boundaries
- Unit tests with AssertJ + JUnit 5
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/)

---

## License

GNU Lesser General Public License v3.0 — see [LICENSE](../../LICENSE)

---

## Author

**linyi**

Contact: jingshuihuayue@qq.com

## References

GameClaw's design and implementation were inspired by the following projects:

| Project | Description |
|---------|-------------|
| [OpenClaw](https://github.com/openclaw/openclaw) | AI Agent open-source specification and compatibility interface definitions |
| [GoClaw](https://github.com/nextlevelbuilder/goclaw) | Go implementation of AI Agent control plane |
| [JavaClaw](https://github.com/jobrunr/JavaClaw) | Java implementation of AI Agent framework, GameClaw's migration predecessor |
