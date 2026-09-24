# DevPilot AI — Backend Service

## 1. Backend Overview
The **DevPilot AI Backend** is an enterprise-grade RESTful API built on **Java 21 LTS** and **Spring Boot 3+**, serving as the core orchestrator for the DevPilot AI cloud developer workspace. It handles project scaffolding, virtual filesystem hierarchies, AI prompt contextualization, sandboxed Docker executions, and deployment tracking with real PostgreSQL persistence and Flyway migrations.

---

## 2. Technology Stack
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.3.4
- **Security:** Spring Security 6 (Stateless architecture, CORS configured)
- **Validation:** Jakarta Bean Validation (Hibernate Validator)
- **Persistence:** Spring Data JPA + Hibernate 6 (`ddl-auto: validate`)
- **Database Engine:** PostgreSQL 16
- **Database Migrations:** Flyway 10 (`V1__create_users_and_roles.sql` to `V5__create_activity_logs.sql`)
- **Connection Pool:** HikariCP
- **Telemetry:** Spring Boot Actuator
- **API Documentation:** Springdoc OpenAPI 2.6.0 (Swagger UI)
- **Testing:** JUnit 5, Mockito, MockMvc, H2 (test profile)

---

## 3. Package Structure
```text
com.devpilot.ai/
├── DevPilotApplication.java       # Application entry point
├── config/                        # OpenAPI, CORS, and web configurations
├── controller/                    # REST API Controllers (HealthController, ProjectController)
├── dto/                           # Data Transfer Objects (ApiResponse, ErrorResponse, ProjectDTOs)
├── entity/                        # JPA Entities (User, Role, Project, ProjectFile, RefreshToken, ActivityLog)
│   └── enums/                     # Domain enums (ProjectStatus, ProjectTemplate, FileType, UserStatus)
├── exception/                     # Global exception advice and custom domain exceptions
├── repository/                    # Spring Data JPA Repositories (UserRepository, ProjectRepository, etc.)
├── security/                      # Spring Security filter chain and entry points
├── service/                       # Business logic services (ProjectService)
└── util/                          # Internal utilities
```

---

## 4. Environment Variables
Sensitive parameters are externalized through environment variables:
| Variable | Default Value | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP listening port |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |
| `FRONTEND_URL` | `http://localhost:5500` | Allowed CORS frontend origin |
| `JWT_SECRET` | *(256-bit string)* | Cryptographic HMAC secret key |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/devpilot` | PostgreSQL connection string |
| `DATABASE_USERNAME` | `devpilot` | Database user |
| `DATABASE_PASSWORD` | `devpilot_secret` | Database password |

---

## 5. How to Run

### Requirements
- **JDK 21** or later (`java -version`)
- **Apache Maven 3.9+** (`mvn -version`)
- **PostgreSQL 15+** running locally (or Docker container)

### Build and Run Tests
```bash
mvn clean test
```
*Note: Automated tests execute using the isolated `test` profile with in-memory PostgreSQL compatibility mode. 21/21 tests pass out of the box.*

### Start Local Development Server
```bash
mvn spring-boot:run
```
Once started, the backend is reachable at:
- **Base URL:** `http://localhost:8080`
- **Health Check:** `http://localhost:8080/api/v1/health`
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI v3 JSON:** `http://localhost:8080/v3/api-docs`

---

## 6. Flyway Migrations (Phase 4)
Migrations are located in `src/main/resources/db/migration/`:
- `V1__create_users_and_roles.sql` — Users and RBAC roles
- `V2__create_projects.sql` — Developer workspace projects
- `V3__create_project_files.sql` — Virtual filesystem hierarchy
- `V4__create_refresh_tokens.sql` — Refresh token store
- `V5__create_activity_logs.sql` — Audit and telemetry logs

---

## 7. API Reference
- `GET  /api/v1/health` — Verifies backend service status.
- `GET  /api/v1/projects` — Lists all active developer workspaces.
- `GET  /api/v1/projects/{id}` — Fetches workspace details by UUID.
- `POST /api/v1/projects` — Scaffolds a new project with template validation.
- `PUT  /api/v1/projects/{id}` — Updates existing workspace metadata.
- `DELETE /api/v1/projects/{id}` — Soft-deletes a project workspace.
