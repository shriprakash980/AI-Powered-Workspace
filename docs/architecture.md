# DevPilot AI — System Architecture Document

## 1. System Overview
**DevPilot AI** is an enterprise-grade, AI-powered cloud developer workspace engineered as a full-stack final-year B.Tech CSE capstone project. The platform integrates a modern web-based code editor, real-time file tree explorer, an autonomous AI coding assistant with contextual repository awareness, controlled sandboxed command execution, and deployment pipeline management.

---

## 2. High-Level Architectural Model

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER (BROWSER)                         │
│                                                                        │
│   HTML5 + CSS3 + Vanilla ES6+ Modules                                 │
│   ├── Monaco Code Editor                                               │
│   ├── Reactive File Explorer & Project Tree                           │
│   ├── Streaming AI Assistant & Unified Diff Inspector                 │
│   ├── Interactive Terminal & Sandboxed Execution Console               │
│   └── Live DOM / Web Sandbox Preview                                   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTPS / REST API / WSS
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY / REVERSE PROXY                     │
│                        (Nginx / Spring Cloud Gateway)                  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 APPLICATION BACKEND (JAVA 21 / SPRING BOOT 3)          │
│                                                                        │
│   ├── Security & Auth: Spring Security 6, JWT, BCrypt, Refresh Token   │
│   ├── Controller Layer: REST Controllers, Bean Validation             │
│   ├── Service Layer:                                                   │
│   │   ├── Project & File Management Service                            │
│   │   ├── AI Orchestrator & Context Builder (OpenAI/Gemini/Anthropic)  │
│   │   ├── Sandboxed Execution Engine (Docker Socket client)           │
│   │   ├── Git Integration Service (JGit / GitHub REST API)             │
│   │   └── Deployment Automation Worker                                 │
│   └── Persistence Layer: Spring Data JPA + Hibernate 6                 │
└──────────────────┬─────────────────────────────┬───────────────────────┘
                   │                             │
                   ▼                             ▼
┌──────────────────────────────────┐ ┌──────────────────────────────────┐
│        DATA PERSISTENCE          │ │        ISOLATED SANDBOXES        │
│                                  │ │                                  │
│  PostgreSQL 16 Engine            │ │  Docker Engine Containers        │
│  ├── Flyway Database Migrations │ │  ├── Node.js / Python / Java     │
│  ├── Normalized Schema           │ │  ├── Resource Bounds (CPU, RAM)  │
│  └── Indexed Foreign Relations   │ │  └── Ephemeral Working Dirs      │
└──────────────────────────────────┘ └──────────────────────────────────┘
```

---

## 3. Component Breakdown

### 3.1 Frontend Subsystem (Vanilla Web Core)
* **Design Philosophy:** Standard-compliant, dependency-free vanilla web technologies (`HTML5`, `CSS3`, `Vanilla JavaScript ES6+`).
* **State & Configuration:** Modular configuration in `config.js`, secure token handling, non-sensitive preferences in `localStorage`.
* **Editor Integration:** Browser-compatible Monaco Editor instance supporting multi-language syntax highlighting, line decorations, code diffs, and AST lint hints.
* **Component Modularity:** Reusable CSS design tokens, customizable theme variables (Dark-first with Light toggle), responsive CSS grid/flexbox layouts.

### 3.2 Backend Subsystem (Spring Boot 3 Core)
* **Runtime:** Java 21 LTS with virtual threads enabled for high-throughput non-blocking I/O.
* **Authentication Pipeline:** Stateless JWT validation filter preceding standard Spring Security authorization filters. Role-Based Access Control (`ROLE_USER`, `ROLE_ADMIN`).
* **Data Transfer Objects (DTOs):** Strict separation between Hibernate entity models and API request/response contracts to eliminate over-fetching and injection risks.
* **Global Exception Interception:** Controller advice translating runtime exceptions into standardized RFC-7807 compliant error payloads.

### 3.3 AI Context & Orchestration Subsystem
* **Provider Agnostic Core (`AIProvider` interface):** Pluggable backends supporting OpenAI (GPT-4o), Google Gemini (Gemini 1.5 Pro/Flash), and Anthropic Claude.
* **Context Assembly Pipeline (`AIContextService`):**
  1. Active file contents + cursor/selection range.
  2. Project structure metadata (directory tree, dependency files `package.json`, `pom.xml`).
  3. Recent terminal errors / execution logs.
  4. User conversation history stored in PostgreSQL.
* **Code Modification Protocol:** AI generates unified diff representations or structured JSON edits, strictly requiring user confirmation (`Apply`, `Reject`, `Copy`) prior to disk writes.

### 3.4 Execution & Isolation Subsystem
* **Host Protection:** No direct shell commands execute on host operating system.
* **Container Sandboxing:** Commands are routed to pre-configured Docker runner containers with:
  - 512MB RAM ceiling
  - 1.0 CPU quota
  - 30-second execution timeout
  - Disabled network ingress/egress for untrusted scripts
  - Ephemeral volume lifecycle

---

## 4. Quality & Operational Attributes
* **Portability:** Containerized end-to-end via multi-stage Dockerfiles and `docker-compose.yml`.
* **Maintainability:** Separation of concerns across MVC layers, clean database migration scripts, comprehensive automated tests.
* **Scalability:** Stateless API design allowing horizontal backend replicas behind Nginx load balancers.
