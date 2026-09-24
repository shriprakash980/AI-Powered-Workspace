# DevPilot AI — Frontend Client Application

## 1. Frontend Overview
The **DevPilot AI** frontend is a high-performance, responsive developer workspace application built strictly with **standards-compliant Vanilla Web Technologies**:
- **HTML5:** Semantic layouts, accessible ARIA attributes, clean structural hierarchies.
- **CSS3:** Native CSS Custom Properties (CSS variables), modern Flexbox and Grid layouts, fluid animations, and mobile-friendly responsive design.
- **Vanilla JavaScript (ES6+ Modules):** Native ES6 modules (`import`/`export`), `async`/`await`, `fetch` API, DOM manipulation, custom event dispatching, and zero framework bloat (no React, Angular, Vue, Tailwind, or Bootstrap).

---

## 2. Directory Structure

```text
frontend/
├── index.html             # Product landing page with live interactive preview
├── login.html             # Secure user authentication with JWT storage
├── register.html          # New user registration & validation
├── dashboard.html         # Cloud projects repository & workspace manager
├── workspace.html         # Full-featured Monaco Code Editor & IDE workspace
├── settings.html          # Profile settings, preferences & API tokens
├── profile.html           # Developer profile & activity log history
├── 404.html               # Custom 404 Not Found error view
├── css/                   # 14 Modular stylesheets
│   ├── variables.css      # Design tokens (colors, fonts, radii, spacing, z-indices)
│   ├── reset.css          # CSS modern reset & baseline box-sizing
│   ├── animations.css     # Micro-interactions, keyframe animations, spinners
│   ├── global.css         # Typography, body layout, scrollbars
│   ├── components.css     # Buttons, badges, forms, cards, toasts, modals
│   ├── navbar.css         # Top fixed shell navigation
│   ├── sidebar.css        # Dashboard side navigation drawer
│   ├── landing.css        # Landing hero, feature grid, interactive preview
│   ├── auth.css           # Auth split card & form validation styling
│   ├── dashboard.css      # Project cards, metrics, filters, template grid
│   ├── workspace.css      # IDE workspace layout, Monaco, tabs, resizers, palette
│   ├── settings.css       # Preference tabs & security forms
│   ├── profile.css        # Avatar & user stats
│   └── responsive.css     # Media queries for tablet and mobile devices
└── js/                    # 14 Modular ES6 JavaScript controllers & services
    ├── config.js          # App configuration, base URL, template catalog
    ├── storage.js         # Safe session & preference storage wrapper
    ├── utils.js           # Toast notifications, HTML escaping, date formatting
    ├── api.js             # Fetch API client with Bearer auth & automatic 401 refresh
    ├── components.js      # Modal managers, tabs, accordions, theme toggle
    ├── navigation.js      # Route guards, active links, theme observer
    ├── landing.js         # Landing page interactivity & mock compilation
    ├── login.js           # Login form submission & error handling
    ├── register.js        # Registration form with password strength checks
    ├── dashboard.js       # Project listing, search, creation modal, template cards
    ├── editor.js          # Monaco Editor controller, models, languages, themes
    ├── workspace.js       # IDE layout, file explorer, tabs, command palette, shortcuts
    ├── settings.js        # Editor preferences & profile update handlers
    └── profile.js         # Activity log visualization
```

---

## 3. Monaco Editor Integration (`js/editor.js`)

- **Dynamic Loading:** Loads Monaco Editor dynamically via AMD loader (`vs/loader.min.js`) with an automatic CDN fallback and `<textarea>` fallback.
- **Model Isolation:** Uses explicit Monaco URI models (`devpilot://project/{id}/file/{fileId}`) to isolate buffers and prevent naming collisions.
- **Language Detection:** Detects and applies syntax highlighting for 14+ languages (Java, JavaScript, TypeScript, HTML, CSS, JSON, SQL, Python, C, C++, Shell, Markdown, etc.).
- **Theme Coupling:** Synchronizes editor theme (`vs-dark` / `vs`) directly with the application's global dark/light theme switch.
- **Preferences:** Configurable font sizes (12-20px), tab sizes (2, 4, 8 spaces), word wrap, and minimap settings saved in `localStorage`.

---

## 4. Quality & Verification Suite

A built-in Node.js verification script audits HTML integrity, duplicate element IDs, missing stylesheet/script references, and module exports:

```bash
node scripts/verify-frontend.js
```

All checks pass with **0 errors**.
