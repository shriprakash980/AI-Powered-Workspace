# DevPilot AI — Backend Service

## 1. Backend Overview
The **DevPilot AI Backend** is an enterprise-grade RESTful API built on **Java 21 LTS** and **Spring Boot 3+**, serving as the core orchestrator for the DevPilot AI cloud developer workspace. It handles user authentication (JWT + Refresh Tokens), role-based authorization (USER / ADMIN), project ownership isolation, virtual filesystem hierarchies, and deployment tracking with PostgreSQL persistence and Flyway migrations.

---

## 2. Technology Stack
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.3.4
- **Security:** Spring Security 6 (Stateless architecture, JWT authentication filter, CORS configured)
- **Authentication & JWT:** JJWT 0.12.6 (HMAC-SHA256, access token expiration, SHA-256 hashed refresh token rotation)
- **Password Hashing:** BCryptPasswordEncoder (strength factor 12)
- **Validation:** Jakarta Bean Validation (Hibernate Validator)
- **Persistence:** Spring Data JPA + Hibernate 6 (`ddl-auto: validate`)
- **Database Engine:** PostgreSQL 16
- **Database Migrations:** Flyway 10 (`V1__create_users_and_roles.sql` to `V5__create_activity_logs.sql`)
- **Connection Pool:** HikariCP
- **Telemetry:** Spring Boot Actuator
- **API Documentation:** Springdoc OpenAPI 2.6.0 (Swagger UI with Bearer JWT support)
- **Testing:** JUnit 5, Mockito, MockMvc, H2 (test profile) — 48 automated tests passing

---

## 3. Package Structure
```text
com.devpilot.ai/
├── DevPilotApplication.java       # Application entry point
├── config/                        # OpenAPI (BearerAuth), CORS, and Web MVC configurations
├── controller/                    # REST API Controllers (AuthController, AdminTestController, ProjectController, HealthController)
├── dto/                           # Data Transfer Objects (ApiResponse, ErrorResponse, UserDTOs, ProjectDTOs)
├── entity/                        # JPA Entities (User, Role, Project, ProjectFile, RefreshToken, ActivityLog)
│   └── enums/                     # Domain enums (ProjectStatus, ProjectTemplate, FileType, UserStatus)
├── exception/                     # Global exception advice and custom domain exceptions (UnauthorizedException, ForbiddenException, etc.)
├── repository/                    # Spring Data JPA Repositories (UserRepository, RefreshTokenRepository, ProjectRepository, etc.)
├── security/                      # Spring Security (SecurityConfig, JwtService, JwtAuthenticationFilter, UserPrincipal, CustomUserDetailsService)
├── service/                       # Business logic services (AuthService, RefreshTokenService, ProjectService)
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
| `JWT_ACCESS_TOKEN_EXPIRATION` | `900000` | Access token lifespan (15 minutes in ms) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | `604800000` | Refresh token lifespan (7 days in ms) |
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
*Note: Automated tests execute using the isolated `test` profile with in-memory PostgreSQL compatibility mode. 48/48 tests pass out of the box.*

### Start Local Development Server
```bash
mvn spring-boot:run
```
Once started, the backend is reachable at:
- **Base URL:** `http://localhost:8080`
- **Health Check:** `http://localhost:8080/api/v1/health`
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html` (Supports Bearer token authorization)
- **OpenAPI v3 JSON:** `http://localhost:8080/v3/api-docs`

---

## 6. Authentication & Security Endpoints (Phase 5)
- `POST /api/v1/auth/register` — Register new user account.
- `POST /api/v1/auth/login` — Login and receive JWT access token + refresh token.
- `POST /api/v1/auth/refresh` — Rotate refresh token and issue new access token.
- `POST /api/v1/auth/logout` — Revoke active refresh token.
- `GET  /api/v1/auth/me` — Get current user profile (requires Bearer token).
- `GET  /api/v1/admin/test` — Admin-only verification (requires `ROLE_ADMIN`).
- `GET  /api/v1/projects` — List user's own projects (ownership enforced).
