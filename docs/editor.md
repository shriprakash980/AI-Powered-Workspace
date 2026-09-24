# DevPilot AI — Monaco Code Editor & Professional IDE Workspace (Phase 7)

## 1. Overview & Architecture

Phase 7 transforms the **DevPilot AI** developer workspace into a full-featured, browser-based integrated development environment (IDE). It integrates Microsoft's **Monaco Editor** (the core engine behind Visual Studio Code) into the vanilla web frontend and pairs it with dynamic tab buffering, resizable panels, status bar tracking, a command palette, quick open file searching, and context menus.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Top Navigation Bar                              │
│   [Breadcrumb / Project]   [Quick Open] [Commands] [Save] [Run]        │
├───────────────────┬────────────────────────────────────────────────────┤
│                   │ Editor Tabs (● unsaved indicator, × close)         │
│                   ├────────────────────────────────────────────────────┤
│   File Explorer   │                                                    │
│   (Folders, Files,│              Monaco Code Editor                    │
│   Context Menu,   │  (Syntax Highlighting, Line Gutter, Folding,       │
│   Inline Actions) │   Minimap, Model Management, Bracket Matching)     │
│                   │                                                    │
├─[Sidebar Resizer]─┼────────────────────────────────────────────────────┤
│                   │ [Bottom Panel Resizer]                             │
│                   ├────────────────────────────────────────────────────┤
│                   │ Bottom Panel: Terminal | Problems | Output | Live  │
├───────────────────┴────────────────────────────────────────────────────┤
│ Status Bar: Java | Ln 12, Col 4 | Spaces: 4 | UTF-8 | LF | Saved       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Monaco Editor Integration (`frontend/js/editor.js`)

- **Dynamic Loading via AMD Loader**: Monaco is loaded asynchronously using `vs/loader.min.js`. A secondary fallback CDN is configured in case of CDN unavailability, and a local `<textarea>` fallback is implemented so that the developer is never blocked offline.
- **Model Isolation & URIs**:
  Each opened file receives its own `monaco.editor.ITextModel` with a custom URI:
  ```text
  devpilot://project/{projectId}/file/{fileId}
  ```
  This guarantees that files with identical names across different directories never collide. When a tab is closed, `model.dispose()` is invoked to prevent memory leaks.
- **Dynamic Layout Reflow**:
  The editor dynamically reflows using `editor.layout()` on browser window resize, sidebar width drag, bottom panel toggle, and mobile drawer transitions.

---

## 3. Supported Languages & Syntax Highlighting

| Extension | Language Identifier | Highlighting & Features |
|---|---|---|
| `.java` | `java` | Keywords, classes, annotations, types, braces |
| `.js`, `.mjs`, `.cjs`, `.jsx` | `javascript` | ES6+, async/await, DOM APIs, JSX |
| `.ts`, `.tsx` | `typescript` | Types, interfaces, generics, decorators |
| `.html`, `.htm` | `html` | Tag matching, attributes, inline CSS/JS |
| `.css`, `.scss`, `.less` | `css` | Rulesets, pseudo-selectors, color previews |
| `.json` | `json` | Keys, values, arrays, syntax validation |
| `.xml`, `.svg` | `xml` | Tag hierarchies, attributes |
| `.sql` | `sql` | DDL, DML, queries, joins, keywords |
| `.md`, `.markdown` | `markdown` | Headers, lists, links, inline code |
| `.py` | `python` | Indentation, decorators, keywords |
| `.c`, `.h` | `c` | Directives, pointers, types |
| `.cpp`, `.hpp`, `.cc` | `cpp` | Classes, templates, namespaces |
| `.sh`, `.bash`, `.zsh` | `shell` | Environment variables, scripts |
| `.yml`, `.yaml` | `yaml` | Key-value pairs, nested sequences |
| `Dockerfile` | `dockerfile` | Container build steps |
| *other* | `plaintext` | Plain text rendering |

---

## 4. Multi-File Buffer & Dirty State Lifecycle

The frontend maintains an in-memory buffer of open files in `workspaceState.openFiles`:

```javascript
openFiles = Map<fileId, {
  id: string,
  name: string,
  path: string,
  language: string,
  content: string,          // Current in-memory content
  originalContent: string,  // Last saved content from database
  dirty: boolean            // Flag indicating unsaved modifications
}>
```

### Save & Tab Switching Rules:
1. **Switching Tabs**: When switching from one tab to another, the editor state is preserved in memory. **No database write occurs merely because the user switches tabs**.
2. **Dirty Indicator**: When `content !== originalContent`, `dirty` becomes `true`, displaying a yellow dot (`●`) on the active tab and updating the status bar to `Unsaved changes`.
3. **Saving (`Ctrl+S` or Save Button)**:
   - Fetches current editor text via `getContent()`.
   - Sends `PUT /api/v1/projects/{projectId}/files/{fileId}` with the content payload.
   - On success, updates `originalContent`, clears `dirty` flag, displays `"Saved just now"`, and records an entry in the Output log panel.
4. **Before-Unload & Close Protection**:
   - Closing a dirty tab displays a confirmation modal with options: **Save and Close**, **Discard**, or **Cancel**.
   - Attempting to refresh or close the browser tab triggers a native `beforeunload` warning if any file is dirty.

---

## 5. Keyboard Shortcuts Reference

| Shortcut | Action | Description |
|---|---|---|
| `Ctrl+S` / `Cmd+S` | **Save File** | Persists active file content to PostgreSQL database |
| `Ctrl+P` / `Cmd+P` | **Quick Open** | Search and jump to any project file by name or path |
| `Ctrl+Shift+P` | **Command Palette** | Access common IDE actions, themes, settings, and toggles |
| `Ctrl+F` / `Cmd+F` | **Find** | Monaco built-in find widget with regex and match count |
| `Ctrl+H` / `Cmd+H` | **Replace** | Monaco built-in find and replace tool |
| `Ctrl+G` | **Go to Line** | Jump directly to a line number in the active file |
| `Ctrl+B` | **Toggle Explorer** | Show or collapse the file explorer sidebar |
| `Ctrl+\`` | **Toggle Bottom Panel**| Expand or collapse the bottom panel drawer |
| `Tab` | **Indent** | Inserts configurable spaces (2, 4, or 8) |
| `Escape` | **Dismiss** | Closes Command Palette, Quick Open, and open modals |

---

## 6. Configurable Editor Preferences

Stored locally in `localStorage` under `devpilot_editor_prefs`:

- **Font Size**: Selectable between 12px, 13px, 14px (default), 15px, 16px, 18px, and 20px.
- **Tab Size**: 2 spaces, 4 spaces (default), or 8 spaces.
- **Word Wrap**: `on` (wraps long lines) or `off` (horizontal scrolling).
- **Minimap**: `enabled` (default on desktop) or `disabled`.

---

## 7. Large & Binary File Safeguards

- **Binary Detection**: Files with binary extensions (`.png`, `.jpg`, `.pdf`, `.zip`, `.exe`, etc.) are intercepted. A specialized card is displayed showing filename, type, and size instead of feeding raw binary bytes into Monaco.
- **File Size Ceiling**: Files exceeding `MAX_EDITOR_FILE_SIZE_BYTES = 1_000_000` (1 MB) trigger a user confirmation prompt before loading to protect browser memory.

---

## 8. Theme Synchronization

Monaco's theme is dynamically coupled to DevPilot AI's global theme system:
- **Dark Theme** (`data-theme="dark"`): Maps to Monaco `vs-dark`.
- **Light Theme** (`data-theme="light"`): Maps to Monaco `vs`.
- Theme changes trigger a `MutationObserver` on `document.documentElement` to switch Monaco instantly without reloading the editor.
