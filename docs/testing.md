# DevPilot AI — Testing Strategy

## 1. Quality Assurance Matrix

```text
┌────────────────────────────────────────────────────────┐
│                   TEST PYRAMID                         │
│                                                        │
│                    /   E2E   \                         │
│                   / Cypress / \                        │
│                  / Playwright  \                       │
│                 /───────────────\                      │
│                /   INTEGRATION   \                     │
│               / Testcontainers,   \                    │
│              /  MockMvc, REST Assured\                 │
│             /─────────────────────────\                │
│            /        UNIT TESTS         \               │
│           / JUnit 5, Mockito, Vanilla JS\              │
│          /───────────────────────────────\             │
└────────────────────────────────────────────────────────┘
```

## 2. Backend Testing (Java / Spring Boot)
- **Unit Testing:** JUnit 5 and Mockito verifying isolated business logic, DTO mapping, and authentication handlers.
- **Integration Testing:** `@SpringBootTest` with Testcontainers running real PostgreSQL 16 instances.
- **Security Testing:** Verification that unauthenticated requests receive `401 Unauthorized` and forbidden roles receive `403 Forbidden`.

## 3. Frontend Testing (Vanilla Web)
- Validation of form input constraints (Email regex, password complexity).
- DOM state integrity during modal opening, toast notifications, and file explorer expansion.
- Async mock API handling and error toast rendering.
