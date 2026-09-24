# DevPilot AI — AI-Powered Developer Workspace

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Frontend](https://img.shields.io/badge/Frontend-Vanilla_HTML5_CSS3_JS-orange.svg)](https://developer.mozilla.org/)
[![Backend](https://img.shields.io/badge/Backend-Java_21_Spring_Boot_3-green.svg)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-PostgreSQL_16_Flyway-blue.svg)](https://www.postgresql.org/)
[![Security](https://img.shields.io/badge/Security-Spring_Security_6_JWT-red.svg)](https://jwt.io/)
[![Tests](https://img.shields.io/badge/Tests-67%20Passed-brightgreen.svg)]()

**DevPilot AI** is an intelligent, cloud-native developer workspace designed to streamline code authoring, context-aware AI collaboration, isolated terminal execution, and deployment pipelines. Developed as a production-grade final-year B.Tech Computer Science and Engineering capstone project.

---

## 🌟 Core Highlights
- **Vanilla Web Frontend:** Built purely with standards-compliant HTML5, modern CSS3 (Custom Properties & Flex/Grid), and modular ES6+ JavaScript. Absolutely no frontend framework bloat (no React, Angular, Vue, Tailwind, or Bootstrap).
- **Monaco Code Editor & IDE:** Full-fledged Monaco Editor integration, multi-model buffer, syntax highlighting for 14+ languages, dirty change indicators (`●`), `Ctrl+S` instant save, Command Palette (`Ctrl+Shift+P`), Quick Open (`Ctrl+P`), custom context menus, and resizable layout splitters.
- **Virtual Filesystem Backend:** PostgreSQL-backed hierarchical files and folders with path traversal security, cycle-prevention on folder moves, cascading path updates, and 1MB size limit.
- **Enterprise Spring Boot Backend:** Java 21, Spring Boot 3.3.4, Spring Security 6 with stateless JWT authentication, and Swagger/OpenAPI documentation.
- **Robust Database Architecture:** PostgreSQL with Flyway version-controlled migrations (`V1` to `V5`), UUID primary keys, 3NF normalization, foreign key constraints, and Hibernate `validate` mode.
- **Production-Style Security & Auth:** BCrypt password hashing, short-lived JWT access tokens, SHA-256 hashed refresh token rotation, project ownership isolation, and role-based access control (`ROLE_USER`, `ROLE_ADMIN`).
- **Comprehensive Test Suite:** 67 automated integration, unit, and end-to-end security lifecycle tests passing out of the box.

---

## 🚀 Repository Layout
```text
AI-Powered-Workspace/
├── frontend/             # Vanilla HTML5, CSS3, ES6+ JS
│   ├── css/              # 14 modular stylesheets (variables, reset, components, etc.)
│   ├── js/               # 14 modular ES6 controllers and services (editor.js, workspace.js, etc.)
│   └── *.html            # 8 application views (landing, auth, dashboard, workspace)
├── backend/              # Spring Boot 3 (Java 21) REST API
│   ├── src/main/java/    # Clean Controller-Service-Repository architecture
│   │   ├── config/       # Swagger/OpenAPI with BearerAuth, CORS
│   │   ├── controller/   # REST Controllers (Auth, Projects, Files, Workspace, Health)
│   │   ├── dto/          # Data Transfer Objects with Bean Validation
│   │   ├── entity/       # JPA Entities (User, Role, Project, ProjectFile, RefreshToken, ActivityLog)
│   │   ├── repository/   # Spring Data JPA Repositories
│   │   ├── security/     # JwtService, JwtFilter, UserPrincipal, UserDetailsService
│   │   └── service/      # Business Services (Auth, RefreshToken, Project, ProjectFile, Workspace)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/ # Flyway SQL migrations (V1 to V5)
│   └── src/test/         # 67 Automated unit & end-to-end integration tests
├── database/             # Mirrored Flyway SQL migrations
├── infrastructure/       # Dockerfiles, Nginx configs, and CI/CD templates
├── docs/                 # Architectural, database, security, workspace, editor, and viva documentation
│   ├── editor.md         # Monaco Editor, tabs, shortcuts, and IDE architecture
│   └── workspace.md      # Virtual filesystem & workspace APIs
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
   # Update DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET if needed
   ```
3. **Run Backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Flyway will automatically execute migrations `V1` through `V5`.
4. **Access Endpoints:**
   - Health Check: `http://localhost:8080/api/v1/health`
   - Swagger Documentation: `http://localhost:8080/swagger-ui/index.html` (Supports Bearer JWT)
   - OpenAPI Specification: `http://localhost:8080/v3/api-docs`

### 3. Run Automated Tests
```bash
cd backend
mvn clean test
```
*All 48 repository, service, controller, and end-to-end security lifecycle tests execute against an isolated in-memory test database.*

---

## 📅 Development Roadmap & Status
- [x] **Phase 1: Architecture & System Planning** (Clean directory structure, comprehensive docs, compose, env specs)
- [x] **Phase 2: Vanilla Frontend Foundation** (8 complete HTML pages, 14 CSS modules, 13 JS modules, 0 errors)
- [x] **Phase 3: Java Spring Boot Backend Foundation** (Java 21, Spring Boot 3, REST endpoints, DTOs, Swagger, Actuator)
- [x] **Phase 4: PostgreSQL + Flyway Implementation** (5 versioned migrations, JPA entities, repositories, 21 passing tests)
- [x] **Phase 5: Authentication & Authorization** (Spring Security + JWT, refresh token rotation, project ownership, 48 passing tests)
- [ ] **Phase 6: Project & Workspace Backend** (Folder/file CRUD, directory trees, Monaco editor bridge)
- [ ] **Phase 7: AI Integration** (Pluggable LLM orchestrator, code generation & diff proposals)
- [ ] **Phase 8: Execution & Sandboxing** (Dockerized terminal & code execution)
- [ ] **Phase 9: Real-time Communication** (WebSockets for telemetry & terminal streaming)
- [ ] **Phase 10: Docker & Production Deployment** (Multi-stage Dockerfiles, Nginx reverse proxy)
