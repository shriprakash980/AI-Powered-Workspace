# DevPilot AI — AI-Powered Developer Workspace

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Frontend](https://img.shields.io/badge/Frontend-Vanilla_HTML5_CSS3_JS-orange.svg)](https://developer.mozilla.org/)
[![Backend](https://img.shields.io/badge/Backend-Java_21_Spring_Boot_3-green.svg)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-PostgreSQL_16_Flyway-blue.svg)](https://www.postgresql.org/)
[![Tests](https://img.shields.io/badge/Tests-21%20Passed-brightgreen.svg)]()

**DevPilot AI** is an intelligent, cloud-native developer workspace designed to streamline code authoring, context-aware AI collaboration, isolated terminal execution, and deployment pipelines. Developed as a production-grade final-year B.Tech Computer Science and Engineering capstone project.

---

## 🌟 Core Highlights
- **Vanilla Web Frontend:** Built purely with standards-compliant HTML5, modern CSS3 (Custom Properties & Flex/Grid), and modular ES6+ JavaScript. Absolutely no frontend framework bloat (no React, Angular, Vue, Tailwind, or Bootstrap).
- **Deep Developer UX:** Dark-first aesthetic, split-pane IDE workspace, interactive virtual file tree, code editor, diff viewer, sandboxed console, and live DOM preview.
- **Enterprise Spring Boot Backend:** Java 21, Spring Boot 3.3.4, Spring Security 6 foundation, Jakarta Bean Validation, and Swagger/OpenAPI documentation.
- **Robust Database Architecture:** PostgreSQL with Flyway version-controlled migrations (`V1` to `V5`), UUID primary keys, 3NF normalization, foreign key constraints, and Hibernate `validate` mode.
- **Comprehensive Test Suite:** 21 automated integration and unit tests passing out of the box.

---

## 🚀 Repository Layout
```text
AI-Powered-Workspace/
├── frontend/             # Vanilla HTML5, CSS3, ES6+ JS
│   ├── css/              # 14 modular stylesheets (variables, reset, components, etc.)
│   ├── js/               # 13 modular ES6 controllers and services
│   └── *.html            # 8 application views (landing, auth, dashboard, workspace)
├── backend/              # Spring Boot 3 (Java 21) REST API
│   ├── src/main/java/    # Clean Controller-Service-Repository architecture
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/ # Flyway SQL migrations (V1 to V5)
│   └── src/test/         # Integration & unit test suites (H2 PostgreSQL mode)
├── database/             # Mirrored Flyway SQL migrations
├── infrastructure/       # Dockerfiles & Nginx configs
├── docs/                 # Architectural, database, API, and viva documentation
├── scripts/              # Automated verification utilities
├── docker-compose.yml    # Full-stack container orchestration
├── .env.example          # Environment variables template
└── README.md
```

---

## 🛠️ Quick Start

### 1. Frontend
Open `frontend/index.html` in any browser or launch a static HTTP server:
```bash
npx serve frontend -p 5500
# or python -m http.server 5500 --directory frontend
```
Navigate to `http://localhost:5500`.

### 2. Backend & Database
1. **Start PostgreSQL:**
   ```bash
   createdb devpilot
   ```
2. **Set Environment Variables:**
   ```bash
   cp .env.example .env
   # Update DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD if needed
   ```
3. **Run Backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Flyway will automatically execute migrations `V1` through `V5`.
4. **Access Endpoints:**
   - Health Check: `http://localhost:8080/api/v1/health`
   - Swagger Documentation: `http://localhost:8080/swagger-ui/index.html`
   - OpenAPI Specification: `http://localhost:8080/v3/api-docs`

### 3. Run Automated Tests
```bash
cd backend
mvn clean test
```
*All 21 repository, service, and controller tests run against an isolated in-memory test database.*

---

## 📅 Development Roadmap & Status
- [x] **Phase 1: Architecture & System Planning** (Clean directory structure, comprehensive docs, compose, env specs)
- [x] **Phase 2: Vanilla Frontend Foundation** (8 complete HTML pages, 14 CSS modules, 13 JS modules, 0 errors)
- [x] **Phase 3: Java Spring Boot Backend Foundation** (Java 21, Spring Boot 3, REST endpoints, DTOs, Swagger, Actuator)
- [x] **Phase 4: PostgreSQL + Flyway Implementation** (5 versioned migrations, JPA entities, repositories, 21 passing tests)
- [ ] **Phase 5: Authentication & Authorization** (Spring Security + JWT token issuance, login/register flows)
- [ ] **Phase 6: Project & Workspace APIs** (Full CRUD, project file tree synchronization)
- [ ] **Phase 7: AI Integration** (Pluggable LLM orchestrator, code generation & diff proposals)
- [ ] **Phase 8: Execution & Sandboxing** (Dockerized terminal & code execution)
- [ ] **Phase 9: Real-time Communication** (WebSockets for telemetry & terminal streaming)
- [ ] **Phase 10: Docker & Production Deployment** (Multi-stage Dockerfiles, Nginx reverse proxy)
