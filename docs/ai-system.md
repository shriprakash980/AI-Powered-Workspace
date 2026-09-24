# DevPilot AI — AI Architecture & Context Protocol

## 1. Provider-Agnostic Interface
DevPilot AI employs a strategy pattern to decouple application logic from specific LLM vendors:

```java
public interface AIProvider {
    String getProviderName();
    AIChatResponse sendPrompt(AIRequestContext context);
    AICodeDiffResponse generateDiff(AICodeRequest request);
}
```

Implementations:
- `OpenAIProvider` (GPT-4o / GPT-4o-mini)
- `GeminiProvider` (Gemini 1.5 Pro / Flash)
- `AnthropicProvider` (Claude 3.5 Sonnet)

All credentials are strictly read from backend environment variables:
- `OPENAI_API_KEY`
- `GEMINI_API_KEY`
- `ANTHROPIC_API_KEY`
- Default active provider is configured via `AI_DEFAULT_PROVIDER`.

---

## 2. Dynamic Context Assembly (`AIContextService`)

When a developer prompts DevPilot AI, context is aggregated into a structured payload:

```json
{
  "project": {
    "name": "ecommerce-api",
    "language": "Java",
    "framework": "Spring Boot",
    "structure": [
      "src/main/java/com/example/Application.java",
      "src/main/java/com/example/controller/UserController.java",
      "pom.xml"
    ]
  },
  "currentFile": {
    "path": "src/main/java/com/example/controller/UserController.java",
    "totalLines": 84,
    "content": "..."
  },
  "selection": {
    "startLine": 24,
    "endLine": 35,
    "selectedCode": "@GetMapping(\"/{id}\")\npublic ResponseEntity<User> get(@PathVariable Long id) { ... }"
  },
  "recentTerminalError": "org.springframework.dao.EmptyResultDataAccessException: No class found",
  "userPrompt": "Fix the null check in this endpoint"
}
```

---

## 3. Human-in-the-Loop Code Modification Protocol

Under no circumstance does the AI directly mutate files without explicit human verification.
- The AI outputs a standardized Unified Diff (`diff -u`) format or structured insertion patch.
- The workspace UI renders a side-by-side or inline Diff Viewer highlighting additions in green (`+`) and deletions in red (`-`).
- The developer retains three atomic actions:
  - **Apply:** Replaces the target code in Monaco Editor and marks file as dirty for auto-save.
  - **Reject:** Dismisses the suggestion card.
  - **Copy:** Copies the snippet to the clipboard for manual pasting.
