# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

"Big Event" (大事件) — a content management platform with AI-powered article writing (撰稿人AI). Three deployable components:

| Component | Dir | Stack | Default Port |
|-----------|-----|-------|--------------|
| Backend | `src/` | Spring Boot 4.0.5, Java 21, MyBatis, MySQL | 8080 |
| Frontend | `big-event-fronted/` | Vue 3, Vite, Element Plus, Pinia | 5173 |
| AI Service | `big-event-ai/` | Python FastAPI, OpenAI SDK | 8001 |

## Build & Run

### Backend (Spring Boot)
```bash
mvn spring-boot:run            # run (uses system mvn)
mvn test                       # run all tests
mvn test -Dtest=ClassName      # run a single test class
```
Requires MySQL on port 3305 (database: `big_event`). Configuration in `src/main/resources/application.yml`.

### Frontend (Vue 3)
```bash
cd big-event-fronted
npm install        # first time
npm run dev        # dev server on :5173
npm run build      # production build
```
Vite proxies `/api` → `http://localhost:8080` (strips `/api` prefix).

### AI Service (Python)
```bash
cd big-event-ai
pip install -r requirements.txt
python main.py              # starts on :8001
```
Requires `.env` file in `big-event-ai/` with `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `UAPIPRO_API_KEY`.

## Architecture

### Platform-Based Modular Architecture

The project has been refactored to a **"Platform Module + Shared Kernel"** architecture (documented in `撰稿人AI-平台化架构分析.md`). Each target platform (Bilibili, Zhihu, Weibo, Toutiao) is an independent module implementing a common interface, while cross-cutting concerns live in a shared kernel.

```
┌─────────────────────────────────────────────────────┐
│              Orchestrator (调度层)                     │
│   orchestrator/ — ModuleRegistry, OrchestratorService │
└──────┬──────────┬──────────┬──────────┬──────────────┘
       │          │          │          │
  ┌────▼───┐ ┌───▼────┐ ┌──▼──────┐ ┌─▼──────────┐
  │Bilibili│ │ Zhihu  │ │ Weibo   │ │ Toutiao    │
  │ (完整)  │ │(占位)   │ │(占位)   │ │(占位)      │
  └────┬───┘ └───┬────┘ └──┬──────┘ └─┬──────────┘
       │         │         │          │
       └─────────┴─────────┴──────────┘
                      │
          ┌───────────▼──────────┐
          │     Shared Kernel     │
          │  AI Engine / Content  │
          │  Store / Dedup /      │
          │  Account / Monitor    │
          └───────────────────────┘
```

#### Platform Module Interface (`modules/core/PlatformModule.java`)
Each platform module implements: `crawlHotTopics()` → `scoreTopics()` → `getContentStrategy()` → `publish()`, plus `checkAccount()` and `capabilities()`.

#### Module Status

| Module | Package | Status | Publisher |
|--------|---------|--------|-----------|
| Bilibili | `modules.bilibili` | Complete: crawl→score→generate→publish loop | Placeholder (awaiting API/OpenClaw) |
| Zhihu | `modules.zhihu` | Placeholder (`isEnabled=false`) | OpenClaw (planned) |
| Weibo | `modules.weibo` | Placeholder (`isEnabled=false`) | OpenClaw (planned) |
| Toutiao | `modules.toutiao` | Placeholder (`isEnabled=false`) | Toutiao API (planned) |

### Backend Layered Architecture (Legacy + New)
```
controller/  →  service/  →  mapper (interface)  →  mapper/*.xml (SQL)
     ↓              ↓
  DTO/VO          Entity
```
Standard response wrapper: `Result<T>` with fields `code` (0=ok, 1=error), `message`, `data`. Paginated queries return `Result<PageResult>` where `PageResult` has `total` and `records`.

### Authentication — Dual JWT System
The app runs **both** Spring Security OAuth2 Resource Server (for Bearer JWT on all routes) AND custom interceptors (for legacy user/admin path-based filtering):

- **User JWT** (`JwtTokenUserInterceptor`): intercepts `/user/**`, expects token in `authentication` header or `Authorization: Bearer <token>`. Secret key: `bubbles.jwt.user-secret-key`.
- **Admin JWT** (`JwtTokenAdminInterceptor`): intercepts `/admin/**`, expects token in `token` header. Secret key: `bubbles.jwt.admin-secret-key`.

Public endpoints (permitAll): `/user/user/login`, `/user/user/register`, `/oauth2/**`, Swagger UI paths.

JWT keys are configured in `application.yml` as hex-encoded bytes under `bubbles.jwt.*`.

### Package Map

#### New Module Packages (Platform Architecture)
| Package | Purpose |
|---------|---------|
| `com.bubbles.modules.core` | PlatformModule interface, Capability enum, RawSignal/ScoredTopic/PublishResult/AccountStatus DTOs |
| `com.bubbles.modules.bilibili` | B站 module: crawler (UApiPro), scorer (5D), strategy (column format), publisher (placeholder) |
| `com.bubbles.modules.bilibili.crawler` | BilibiliCrawler — UApiPro SDK hotlist fetching |
| `com.bubbles.modules.bilibili.scorer` | BilibiliScorer — 5-dimension scoring (hot/sustain/depth/diversity/audience) |
| `com.bubbles.modules.bilibili.strategy` | BilibiliContentStrategy — B站 column prompt/length/style/format |
| `com.bubbles.modules.bilibili.publisher` | BilibiliPublisher — placeholder for B站 column publishing |
| `com.bubbles.modules.zhihu` | Zhihu placeholder (`isEnabled=false`, documents future OpenClaw integration) |
| `com.bubbles.modules.weibo` | Weibo placeholder (`isEnabled=false`) |
| `com.bubbles.modules.toutiao` | Toutiao placeholder (`isEnabled=false`) |
| `com.bubbles.shared.ai` | AIWriterEngine interface, WriteResult DTO, PromptTemplateManager interface |
| `com.bubbles.shared.ai.impl` | DefaultAIWriterEngine — adapts existing WriterAIService to modular interface |
| `com.bubbles.shared.content` | ContentStore interface for unified article storage |
| `com.bubbles.shared.content.impl` | DefaultContentStore — wraps ArticleService + mappers |
| `com.bubbles.shared.account` | AccountManager interface |
| `com.bubbles.shared.account.impl` | SimpleAccountManager — in-memory account status |
| `com.bubbles.shared.dedup` | TopicDedupService interface (cross-platform dedup) |
| `com.bubbles.shared.dedup.impl` | SimpleTopicDedupService — Jaccard keyword similarity |
| `com.bubbles.shared.monitor` | MonitorService interface |
| `com.bubbles.shared.monitor.impl` | SimpleMonitorService — in-memory publish event tracking |
| `com.bubbles.orchestrator` | ModuleRegistry (auto-discovers PlatformModule beans), OrchestratorService (scheduled pipeline), PipelineResult |

#### Legacy Packages (Still Active)
| Package | Purpose |
|---------|---------|
| `com.bubbles.server.controller.user` | User-facing API (articles, categories, user profile, upload, hot topics, modules) |
| `com.bubbles.server.controller.admin` | Admin API |
| `com.bubbles.server.controller.writterAI` | AI writer proxy endpoints (calls Python AI service via WriterAIService) |
| `com.bubbles.server.service` | Service interfaces (WriterAIService, ArticleService, etc.) |
| `com.bubbles.server.service.impl` | Service implementations (WriterAIServiceImpl, HotScoreCalculator, AutoPublishService) |
| `com.bubbles.server.mapper` | MyBatis mapper interfaces (SQL in `resources/mapper/*.xml`) |
| `com.bubbles.server.config` | Spring Security, JWT, WebMVC, OSS config |
| `com.bubbles.server.interceptor` | JWT interceptors |
| `com.bubbles.common` | Shared: `Result`/`PageResult`, enums, constants, Jackson JSON config, JWT/AliOss/AiWriter properties |
| `com.bubbles.pojo.dto` | Request DTOs |
| `com.bubbles.pojo.entity` | DB entities |
| `com.bubbles.pojo.vo` | Response VOs |

### Key API Endpoints

#### Legacy AI Writer (backward-compatible)
- `POST /user/writer/write` — AI article generation
- `POST /user/writer/write-from-hot` — Hot-topic-based article generation + auto-save
- `POST /user/writer/auto-publish` — Manual trigger: score → filter → generate → save
- `GET /user/writer/status` — AI service health check
- `GET /user/hot/topics` — Get top-N scored hot topics (uses legacy HotScoreCalculator)

#### New Modular API (`/user/modules/*`)
- `GET /user/modules` — List all registered platform modules
- `GET /user/modules/status` — Module registration status + capabilities + account health
- `GET /user/modules/accounts` — All platform account statuses
- `POST /user/modules/{platform}/pipeline` — Trigger full pipeline for a platform
- `POST /user/modules/{platform}/crawl` — Crawl only, no follow-up
- `POST /user/modules/{platform}/score` — Score a signal list (debug endpoint)

### Bilibili Scoring Model (5-Dimensional)

The B站 scorer uses a 5-dimension model instead of the legacy single hot-score:

| Dimension | Weight | What it measures |
|-----------|--------|-----------------|
| Hot (热度) | 0.30 | Log-normalized weighted interaction metrics (view/like/coin/fav/share) |
| Sustain (持续性) | 0.20 | Trend analysis from historical snapshots: rising/stable/falling |
| Depth (深度潜力) | 0.25 | Rule-based: title informativeness + category depth potential |
| Diversity (差异性) | 0.15 | Duplicate check: already processed = 0, new = 100 |
| Audience (受众匹配) | 0.10 | Category-to-target-audience affinity map (tech=95, dance=25, etc.) |

### Frontend Structure
- `src/api/` — Axios API modules (one per domain: `article.js`, `user.js`, `writer.js`, `bilibili.js`, etc.)
- `src/views/` — Page components, grouped by feature (`article/`, `user/`, `writer/`, `bilibili/`)
- `src/stores/` — Pinia stores (`token.js`, `user.js`), persisted to localStorage via `pinia-persistedstate-plugin`
- `src/router/index.js` — All routes lazy-loaded, Layout.vue wraps authenticated pages
- `src/utils/request.js` — Axios instance with request interceptor (attaches Bearer JWT) and response interceptor (401→redirect login, status-based error messages)

### AI Service Endpoints (Python, port 8001)
- `POST /api/writer/write` — Generate article
- `POST /api/writer/write/stream` — Stream article generation (SSE)
- `POST /api/writer/write-from-hot` — Generate article from Bilibili hot item context
- `POST /api/hot-topic/analyze` — Analyze trending topics
- `GET /api/bilibili/hot` — Fetch Bilibili hot trending videos (via UApiPro)
- `POST /api/info/collect` — Collect/search info on a topic
- `GET /health` — Health check

### Python AI Service Structure (Modular)
```
big-event-ai/
├── main.py                  # FastAPI entry point (backward-compatible endpoints)
├── config.py                # Env-based config (OPENAI_API_KEY, UAPIPRO_API_KEY, etc.)
├── schemas/
│   ├── request.py           # Pydantic request models
│   └── response.py          # Pydantic response models (BiliHotItem, BiliHotResponse, etc.)
├── services/                # Legacy service layer (still active, unchanged)
│   ├── writer_service.py
│   ├── hot_topic_service.py
│   └── info_collect_service.py
├── modules/                 # New: platform modules
│   ├── bilibili/
│   │   ├── crawler.py       # BilibiliCrawler — UApiPro SDK hotlist (extracted from hot_topic_service)
│   │   ├── scorer.py        # BilibiliScorer — 5-dimension scoring
│   │   └── strategy.py      # BilibiliContentStrategy — prompt/format templates
│   ├── zhihu/__init__.py    # Placeholder (future: HTTP crawler + OpenClaw)
│   ├── weibo/__init__.py    # Placeholder (future: 3rd-party API + OpenClaw)
│   └── toutiao/__init__.py  # Placeholder (future: Toutiao API)
└── shared/                  # New: shared kernel
    ├── ai/prompt_templates.py     # PromptTemplateManager — cross-platform prompt registry
    └── dedup/topic_dedup.py       # TopicDedupService — Jaccard keyword similarity dedup
```

The Spring Boot backend proxies AI requests through `WriterController` → `WriterAIService` (using Spring WebClient) to the Python service.

## Key Dependencies
- **MyBatis** (not MyBatis-Plus) with XML mappers at `classpath:mapper/*.xml`
- **PageHelper** (`com.github.pagehelper`) for pagination — call `PageHelper.startPage()` before a mapper query
- **Spring Security OAuth2 Authorization Server + Resource Server** — manages OAuth2 flows and JWT decoding
- **jjwt** (io.jsonwebtoken) 0.12.5 for custom JWT creation
- **Aliyun OSS** for file/cover image uploads
- **springdoc-openapi** 3.0.2 for OpenAPI/Swagger docs (accessible at `/doc.html`)

## Related Documents
- `撰稿人AI-平台化架构分析.md` — Platform architecture analysis (why modular, what to share/isolate)
- `撰稿人AI-问题分析.md` — Core problems deep-dive (signal normalization, 5D scoring, feedback loop)
- `OAuth2认证系统文档.md` — OAuth2 authentication system documentation
- `src/大事件接口文档.md` — API interface documentation V1.0
