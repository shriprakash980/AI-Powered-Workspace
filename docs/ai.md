# DevPilot AI — Phase 8: Multi-Provider AI Architecture & Coding Assistant

DevPilot AI features an enterprise-grade, multi-provider AI coding assistant layer tightly integrated with the Monaco Editor IDE workspace.

---

## 1. High-Level Architecture

The AI layer is structured into modular tiers adhering to clean separation of concerns and dependency inversion:

```
[Monaco Editor / Web Client]
           │
           │  REST / Server-Sent Events (SSE)
           ▼
[AIController / ConversationController]
           │
           │  AuthenticationPrincipal (Ownership Security)
           ▼
[AIService / ConversationService]
    │                 │
    │ Context         │ Factory Dispatch
    ▼                 ▼
[AIContextBuilder]  [AIProviderFactory]
                      ├── OpenAIProvider (gpt-4o-mini, gpt-4o)
                      ├── GeminiProvider (gemini-1.5-flash, gemini-1.5-pro)
                      ├── AnthropicProvider (claude-3-5-sonnet, claude-3-haiku)
                      └── SimulatedAIProvider (Local Demo & Offline Fallback)
```

---

## 2. Multi-Provider Abstraction

The `AIProvider` contract abstracts model differences across major AI services:

```java
public interface AIProvider {
    AIProviderType getProviderType();
    String getDefaultModel();
    List<String> getAvailableModels();
    boolean isConfigured();
    AIResponse generate(AIRequest request);
    void generateStream(AIRequest request, AIStreamConsumer consumer);
}
```

### Supported Providers:
1. **OpenAI**: Connects to `https://api.openai.com/v1/chat/completions` using Bearer tokens. Supports synchronous JSON responses and real-time SSE token streaming.
2. **Google Gemini**: Connects to `https://generativelanguage.googleapis.com/v1beta/` using Google API keys. Converts conversation history to Gemini `contents` parts and supports `streamGenerateContent`.
3. **Anthropic Claude**: Connects to `https://api.anthropic.com/v1/messages` using `x-api-key`. Supports system prompts, message roles, and `content_block_delta` SSE streaming.
4. **Simulated AI Engine**: Built-in intelligent simulator enabling offline demonstrations, grading, and local presentations without requiring external API keys.

---

## 3. Database Schema (Flyway V6, V7, V8)

### `ai_conversations` (V6)
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`
- `project_id UUID REFERENCES projects(id) ON DELETE CASCADE`
- `title VARCHAR(255) NOT NULL`
- `provider VARCHAR(50) NOT NULL`
- `model VARCHAR(100) NOT NULL`
- `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`

### `ai_messages` (V7)
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE`
- `role VARCHAR(20) NOT NULL` (`user`, `assistant`, `system`)
- `content TEXT NOT NULL`
- `file_id UUID REFERENCES project_files(id) ON DELETE SET NULL`
- `created_at TIMESTAMPTZ`

### `ai_requests` (V8)
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`
- `project_id UUID REFERENCES projects(id) ON DELETE SET NULL`
- `conversation_id UUID REFERENCES ai_conversations(id) ON DELETE SET NULL`
- `provider VARCHAR(50) NOT NULL`
- `model VARCHAR(100) NOT NULL`
- `request_type VARCHAR(50) NOT NULL`
- `prompt_tokens INT`, `completion_tokens INT`, `total_tokens INT`
- `latency_ms BIGINT`
- `status VARCHAR(50) NOT NULL`
- `error_message TEXT`
- `created_at TIMESTAMPTZ`

---

## 4. REST & Streaming API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/ai/providers` | Lists supported AI engines and configurations |
| `POST` | `/api/v1/ai/chat` | Synchronous AI chat with workspace context |
| `POST` | `/api/v1/ai/chat/stream` | Real-time token-by-token SSE streaming |
| `POST` | `/api/v1/ai/code/explain` | Explains highlighted code snippet |
| `POST` | `/api/v1/ai/code/fix` | Diagnoses bug and generates diff fix |
| `POST` | `/api/v1/ai/code/generate` | Generates code based on instruction |
| `POST` | `/api/v1/ai/code/refactor` | Refactors code for performance & readability |
| `POST` | `/api/v1/ai/code/tests` | Generates automated unit test suites |
| `POST` | `/api/v1/ai/code/debug` | Diagnoses stack traces & error messages |
| `POST` | `/api/v1/ai/code/document` | Generates docstrings and Javadoc |
| `GET` | `/api/v1/ai/conversations` | Lists user's persistent chat threads |
| `GET` | `/api/v1/ai/conversations/{id}` | Retrieves conversation message history |
| `POST` | `/api/v1/ai/conversations` | Creates a new chat thread |
| `DELETE`| `/api/v1/ai/conversations/{id}` | Deletes a conversation thread |

---

## 5. Monaco Editor & Human-in-the-Loop Safety

1. **Context Awareness**: Whenever code is selected in Monaco Editor, DevPilot AI detects the highlighted range and displays an active selection badge (`✂️ Selection (N lines)`).
2. **Interactive Diff Review**: Code action responses generate a unified diff preview card showing exact line additions (`+`, green) and deletions (`-`, red).
3. **Explicit User Save Enforced**:
   - Clicking **Apply Change** replaces the selected range directly in the Monaco editor buffer.
   - It marks the active tab as dirty (`●`) and save state as `unsaved`.
   - The application **never** auto-commits AI changes to PostgreSQL without developer confirmation. The user must review and press `Ctrl+S` to persist.
   - Clicking **Reject** dismisses the proposed changes without altering the buffer.
