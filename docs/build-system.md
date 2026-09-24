# DevPilot AI — Build System Architecture

## Overview

The DevPilot AI Build System provides automated project type detection, sandboxed build execution, test execution, log streaming, and AI-assisted build failure diagnosis.

---

## 1. Project Type Detectors

The build engine automatically inspects the Virtual File System (VFS) to determine project framework and configuration:

| Project Type | Trigger Files / Markers | Default Build Command | Default Test Command |
| :--- | :--- | :--- | :--- |
| `JavaSpring` | `pom.xml` containing `spring-boot-starter` | `mvn clean package -DskipTests` | `mvn test` |
| `JavaMaven` | `pom.xml` | `mvn clean package` | `mvn test` |
| `JavaGradle` | `build.gradle` or `build.gradle.kts` | `./gradlew build` | `./gradlew test` |
| `NodeJS` | `package.json` | `npm run build` | `npm test` |
| `Python` | `requirements.txt` / `pyproject.toml` | `python -m pip install -r requirements.txt` | `pytest` |
| `StaticWeb` | `index.html` | `-- (Static)` | `--` |
| `Docker` | `Dockerfile` | `docker build -t app .` | `--` |

---

## 2. Build Modes & Sandboxing

Builds run inside isolated temporary worktrees with strict resource bounds:

- **Build Modes**: `BUILD`, `TEST`, `BUILD_AND_TEST`, `PACKAGE`.
- **Timeout**: Enforces a maximum execution time limit of 600 seconds (10 minutes).
- **Concurrency Guard**: Limits running builds to a maximum of 5 active concurrent tasks per user.
- **Log Streaming**: Structured capturing of `STDOUT` and `STDERR` into the `build_logs` database table.

---

## 3. AI Build Failure Diagnosis

When a build status transitions to `FAILED`, the `BuildAiService`:
1. Extracts the last 100 lines of build failure logs and stack traces.
2. Formulates a targeted prompt for the configured AI provider (`OpenAI`, `Gemini`, or `Claude`).
3. Synthesizes a structured `BuildDiagnosisResponse` containing:
   - Root cause summary
   - Specific source file & line references
   - Recommended resolution steps

---

## 4. REST API Endpoints

- `POST /api/v1/projects/{projectId}/builds/detect`: Auto-detects project type and suggested build/test commands.
- `POST /api/v1/projects/{projectId}/builds`: Triggers a new build run.
- `GET /api/v1/projects/{projectId}/builds`: Lists paginated build history.
- `GET /api/v1/projects/{projectId}/builds/{buildId}`: Fetches details for a specific build run.
- `GET /api/v1/projects/{projectId}/builds/{buildId}/logs`: Retrieves full execution logs for a build.
- `POST /api/v1/projects/{projectId}/builds/{buildId}/cancel`: Cancels an in-progress build execution.
- `POST /api/v1/projects/{projectId}/builds/{buildId}/diagnose`: Requests AI diagnosis for a build failure.
