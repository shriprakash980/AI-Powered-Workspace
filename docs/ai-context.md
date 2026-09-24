# DevPilot AI — Context Engine Architecture (Phase 9)

## 1. Overview
The DevPilot AI Context Engine intelligently extracts, scores, ranks, and chunks repository context to construct highly relevant, token-budgeted prompts for LLMs (OpenAI, Anthropic Claude, Google Gemini).

Rather than dumping naive file dumps into the LLM, DevPilot AI computes deterministic relevance metrics based on AST/lexical imports, component naming affinities, active cursor focus, and query keywords.

---

## 2. Context Ranking Heuristic (Deterministic Scoring)

Every file in the project workspace is evaluated by `ContextRanker`:

| Criteria | Score Points | Rationale |
| :--- | :--- | :--- |
| **Active Open File** | `+100` | The developer is actively inspecting or modifying this file. |
| **Active Code Selection** | `+100` | The developer has explicitly highlighted lines of interest. |
| **Direct Dependency Match** | `+80` | Referenced via Java import, ES module import, CommonJS `require()`, or HTML `src`/`href`. |
| **Component Naming Pattern** | `+50` | Sibling files sharing naming prefixes (e.g. `UserService.js` and `UserController.js`). |
| **Query Keyword Match** | `+40` | File path or name contains non-stopword tokens from the user's prompt or error log. |
| **Same Directory Proximity**| `+30` | Sibling files sharing directory path with active file. |
| **Same File Extension** | `+10` | Sibling files sharing language ecosystem. |

Files with a computed score $> 0$ are ranked in descending order.

---

## 3. Dependency Analysis Engine

`DependencyAnalyzer` detects cross-file imports across multi-language projects:
- **Java**: Regex detection of `import [static] com.domain.package.Class;`
- **JavaScript / TypeScript**: Regex detection of `import ... from './path'` and `require('./path')`
- **HTML**: Regex detection of `<script src="...">` and `<link href="...">`
- **CSS**: Regex detection of `@import url(...)`

Extracted dependencies match both relative paths (`src/services/UserService.js`) and simplified module identifiers (`UserService`).

---

## 4. Code Chunking & Token Budgeting

Large codebases easily exceed LLM token limits and introduce needle-in-a-haystack distractions. `ContextChunker` segments files into chunks:
- **`SELECTED`**: Verbatim code selection preserved at top priority (`fileScore + 50`).
- **`IMPORT_HEADER`**: First 25 lines containing imports, package statements, or module headers.
- **`BODY_SEGMENT`**: Cleanly split line segments bounded by `maxChunkChars` (default: 12,000 chars).

`ContextBudget` monitors character consumption and enforces limits:
- `maxFiles`: 20 files max
- `maxTotalChars`: 50,000 characters (~12,500 tokens)
- `maxChunkChars`: 12,000 characters

---

## 5. REST API: Context Preview

Developers can inspect what context will be provided to the AI before issuing requests.

### Endpoint
`POST /api/v1/ai/context/preview`

### Request Payload
```json
{
  "projectId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "fileId": "8b087093-68d2-43f1-a1b7-b08bc8d91c5e",
  "selectedCode": "function calculateTax(amount) { ... }",
  "query": "Fix null pointer in checkout flow"
}
```

### Response Payload
```json
{
  "success": true,
  "data": {
    "projectId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "totalProjectFiles": 24,
    "selectedFiles": [
      {
        "fileId": "8b087093-68d2-43f1-a1b7-b08bc8d91c5e",
        "path": "src/services/CheckoutService.js",
        "score": 200,
        "reason": "Current file, Selected code"
      },
      {
        "fileId": "f7a34612-4211-4eb2-a39c-19d29f8f2371",
        "path": "src/models/Order.js",
        "score": 80,
        "reason": "Direct dependency"
      }
    ],
    "totalCharactersUsed": 4820,
    "estimatedTokens": 1205
  },
  "message": "Context assembled successfully"
}
```
