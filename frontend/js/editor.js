/**
 * DevPilot AI — Monaco Code Editor Controller
 * Handles Monaco Editor initialization, models, language detection, themes, and fallback rendering.
 */

import { storage } from './storage.js';

// Supported Language Mappings
const LANGUAGE_MAP = {
  java: 'java',
  js: 'javascript',
  mjs: 'javascript',
  cjs: 'javascript',
  ts: 'typescript',
  tsx: 'typescript',
  jsx: 'javascript',
  html: 'html',
  htm: 'html',
  css: 'css',
  scss: 'css',
  less: 'css',
  json: 'json',
  xml: 'xml',
  svg: 'xml',
  sql: 'sql',
  md: 'markdown',
  markdown: 'markdown',
  py: 'python',
  c: 'c',
  h: 'c',
  cpp: 'cpp',
  hpp: 'cpp',
  cc: 'cpp',
  cxx: 'cpp',
  sh: 'shell',
  bash: 'shell',
  zsh: 'shell',
  yml: 'yaml',
  yaml: 'yaml',
  dockerfile: 'dockerfile',
  txt: 'plaintext'
};

const BINARY_EXTENSIONS = new Set([
  'png', 'jpg', 'jpeg', 'gif', 'bmp', 'ico', 'webp', 'avif',
  'pdf', 'zip', 'tar', 'gz', '7z', 'rar',
  'exe', 'dll', 'so', 'dylib', 'bin',
  'woff', 'woff2', 'ttf', 'eot',
  'mp3', 'wav', 'ogg', 'mp4', 'webm', 'mov', 'avi'
]);

export const MAX_EDITOR_FILE_SIZE_BYTES = 1_000_000; // 1 MB limit

let monacoInstance = null;
let editor = null;
const models = new Map(); // fileId -> monaco.editor.ITextModel
let fallbackTextarea = null;
let isFallbackMode = false;
let currentFileId = null;

// Event callbacks
let contentChangeCallback = null;
let cursorChangeCallback = null;
let saveShortcutCallback = null;

/**
 * Detect language string from filename
 * @param {string} fileName
 * @returns {string} Monaco language identifier
 */
export function detectLanguage(fileName) {
  if (!fileName || typeof fileName !== 'string') return 'plaintext';
  const clean = fileName.trim().toLowerCase();
  if (clean === 'dockerfile') return 'dockerfile';
  if (!clean.includes('.')) return 'plaintext';
  const ext = clean.substring(clean.lastIndexOf('.') + 1);
  return LANGUAGE_MAP[ext] || 'plaintext';
}

/**
 * Check if a filename is binary or unsupported
 * @param {string} fileName
 * @returns {boolean}
 */
export function isBinaryFile(fileName) {
  if (!fileName || typeof fileName !== 'string') return false;
  const ext = fileName.trim().toLowerCase().split('.').pop();
  return BINARY_EXTENSIONS.has(ext);
}

/**
 * Load Monaco library dynamically via AMD loader
 */
async function loadMonaco() {
  if (window.monaco) {
    monacoInstance = window.monaco;
    return monacoInstance;
  }

  return new Promise((resolve, reject) => {
    // Check if AMD loader already present
    if (window.require && typeof window.require.config === 'function') {
      window.require(['vs/editor/editor.main'], () => {
        monacoInstance = window.monaco;
        resolve(monacoInstance);
      });
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.45.0/min/vs/loader.min.js';
    script.onload = () => {
      window.require.config({
        paths: { vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.45.0/min/vs' }
      });
      window.require(['vs/editor/editor.main'], () => {
        monacoInstance = window.monaco;
        resolve(monacoInstance);
      });
    };

    script.onerror = () => {
      // Fallback CDN if primary fails
      const fallbackScript = document.createElement('script');
      fallbackScript.src = 'https://cdn.jsdelivr.net/npm/monaco-editor@0.45.0/min/vs/loader.js';
      fallbackScript.onload = () => {
        window.require.config({
          paths: { vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.45.0/min/vs' }
        });
        window.require(['vs/editor/editor.main'], () => {
          monacoInstance = window.monaco;
          resolve(monacoInstance);
        });
      };
      fallbackScript.onerror = () => {
        reject(new Error('Monaco CDN unreachable; switching to fallback text editor.'));
      };
      document.head.appendChild(fallbackScript);
    };

    document.head.appendChild(script);
  });
}

/**
 * Initialize Monaco Editor inside the specified container
 * @param {HTMLElement} containerEl
 * @param {object} options
 */
export async function initializeEditor(containerEl, options = {}) {
  const prefs = storage.getEditorPreferences();
  const theme = storage.getTheme() === 'light' ? 'vs' : 'vs-dark';

  try {
    const monaco = await loadMonaco();

    // Clean container
    containerEl.innerHTML = '';

    editor = monaco.editor.create(containerEl, {
      value: options.initialValue || '',
      language: options.initialLanguage || 'plaintext',
      theme: theme,
      fontSize: prefs.fontSize || 14,
      tabSize: prefs.tabSize || 4,
      insertSpaces: true,
      wordWrap: prefs.wordWrap || 'on',
      minimap: { enabled: prefs.minimap !== false },
      lineNumbers: 'on',
      lineNumbersMinChars: 3,
      folding: true,
      bracketPairColorization: { enabled: true },
      autoClosingBrackets: 'always',
      autoClosingQuotes: 'always',
      renderWhitespace: 'selection',
      smoothScrolling: true,
      cursorBlinking: 'smooth',
      scrollBeyondLastLine: false,
      automaticLayout: true,
      fontFamily: "'JetBrains Mono', 'Fira Code', Consolas, monospace",
      overviewRulerBorder: false,
      renderLineHighlight: 'all',
      fixedOverflowWidgets: true
    });

    isFallbackMode = false;

    // Track content edits
    editor.onDidChangeModelContent((e) => {
      if (contentChangeCallback) {
        contentChangeCallback(getContent(), currentFileId, e);
      }
    });

    // Track cursor changes
    editor.onDidChangeCursorPosition((e) => {
      if (cursorChangeCallback) {
        cursorChangeCallback({
          lineNumber: e.position.lineNumber,
          column: e.position.column
        });
      }
    });

    // Register Save Command (Ctrl+S / Cmd+S)
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => {
      if (saveShortcutCallback) {
        saveShortcutCallback();
      }
    });

    return editor;

  } catch (err) {
    console.warn('[Monaco Init Warning] Falling back to textarea editor:', err.message);
    initFallbackEditor(containerEl, options);
    return null;
  }
}

function initFallbackEditor(containerEl, options) {
  containerEl.innerHTML = '';
  isFallbackMode = true;

  fallbackTextarea = document.createElement('textarea');
  fallbackTextarea.className = 'editor-canvas fallback-editor-textarea';
  fallbackTextarea.spellcheck = false;
  fallbackTextarea.placeholder = '// Code editor loaded in fallback mode';
  fallbackTextarea.value = options.initialValue || '';

  fallbackTextarea.addEventListener('input', () => {
    if (contentChangeCallback) {
      contentChangeCallback(fallbackTextarea.value, currentFileId);
    }
  });

  fallbackTextarea.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault();
      if (saveShortcutCallback) saveShortcutCallback();
    } else if (e.key === 'Tab') {
      e.preventDefault();
      const start = fallbackTextarea.selectionStart;
      const end = fallbackTextarea.selectionEnd;
      fallbackTextarea.value = fallbackTextarea.value.substring(0, start) + '    ' + fallbackTextarea.value.substring(end);
      fallbackTextarea.selectionStart = fallbackTextarea.selectionEnd = start + 4;
      fallbackTextarea.dispatchEvent(new Event('input'));
    }
  });

  containerEl.appendChild(fallbackTextarea);
}

/**
 * Load a file into the editor, creating or reusing a Monaco model
 * @param {object} file - { id, projectId, name, path, content, fileType }
 */
export function loadFile(file) {
  if (!file) return;
  currentFileId = file.id;

  const content = file.content != null ? file.content : '';
  const lang = detectLanguage(file.name);

  if (!isFallbackMode && editor && monacoInstance) {
    let model = models.get(file.id);

    if (!model || model.isDisposed()) {
      const uriStr = `devpilot://project/${file.projectId || 'default'}/file/${file.id}`;
      const uri = monacoInstance.Uri.parse(uriStr);

      // Check if existing model by URI exists in Monaco
      const existing = monacoInstance.editor.getModel(uri);
      if (existing) {
        model = existing;
        model.setValue(content);
        monacoInstance.editor.setModelLanguage(model, lang);
      } else {
        model = monacoInstance.editor.createModel(content, lang, uri);
      }
      models.set(file.id, model);
    }

    editor.setModel(model);
    editor.focus();
  } else if (fallbackTextarea) {
    fallbackTextarea.value = content;
    fallbackTextarea.focus();
  }
}

/**
 * Get current editor text content
 */
export function getContent() {
  if (!isFallbackMode && editor) {
    return editor.getValue();
  }
  return fallbackTextarea ? fallbackTextarea.value : '';
}

/**
 * Set content directly for active editor
 */
export function setContent(content) {
  if (!isFallbackMode && editor) {
    editor.setValue(content != null ? content : '');
  } else if (fallbackTextarea) {
    fallbackTextarea.value = content != null ? content : '';
  }
}

/**
 * Set language for the current model
 */
export function setLanguage(language) {
  if (!isFallbackMode && editor && monacoInstance) {
    const model = editor.getModel();
    if (model) {
      monacoInstance.editor.setModelLanguage(model, language);
    }
  }
}

/**
 * Set editor theme
 * @param {'dark'|'light'|'hc'} themeName
 */
export function setTheme(themeName) {
  if (!isFallbackMode && monacoInstance) {
    let monacoTheme = 'vs-dark';
    if (themeName === 'light') monacoTheme = 'vs';
    else if (themeName === 'hc' || themeName === 'high-contrast') monacoTheme = 'hc-black';
    monacoInstance.editor.setTheme(monacoTheme);
  }
}

/**
 * Update editor preferences (font size, tab size, minimap, word wrap)
 */
export function updatePreferences(prefs) {
  if (!prefs) return;
  if (!isFallbackMode && editor) {
    editor.updateOptions({
      fontSize: prefs.fontSize || 14,
      tabSize: prefs.tabSize || 4,
      wordWrap: prefs.wordWrap || 'on',
      minimap: { enabled: prefs.minimap !== false }
    });
  }
}

/**
 * Trigger dynamic layout update
 */
export function layout() {
  if (!isFallbackMode && editor) {
    editor.layout();
  }
}

/**
 * Focus the editor
 */
export function focusEditor() {
  if (!isFallbackMode && editor) {
    editor.focus();
  } else if (fallbackTextarea) {
    fallbackTextarea.focus();
  }
}

/**
 * Format the document using Monaco's formatter
 */
export function formatDocument() {
  if (!isFallbackMode && editor) {
    const action = editor.getAction('editor.action.formatDocument');
    if (action) action.run();
  }
}

/**
 * Navigate to a specific line
 */
export function goToLine(lineNumber) {
  if (!isFallbackMode && editor) {
    editor.revealLineInCenter(lineNumber);
    editor.setPosition({ lineNumber, column: 1 });
    editor.focus();
  }
}

/**
 * Dispose a specific file model when closing a tab
 */
export function disposeFileModel(fileId) {
  const model = models.get(fileId);
  if (model && !model.isDisposed()) {
    model.dispose();
  }
  models.delete(fileId);
}

/**
 * Dispose the entire Monaco editor
 */
export function disposeEditor() {
  for (const model of models.values()) {
    if (model && !model.isDisposed()) {
      model.dispose();
    }
  }
  models.clear();

  if (editor) {
    editor.dispose();
    editor = null;
  }
}

/**
 * Event Listeners Registration
 */
export function onContentChange(cb) {
  contentChangeCallback = cb;
}

export function onCursorChange(cb) {
  cursorChangeCallback = cb;
}

export function onSaveShortcut(cb) {
  saveShortcutCallback = cb;
}

export function getEditor() {
  return editor;
}

/**
 * Get selected code text from Monaco or fallback editor
 */
export function getSelectedText() {
  if (!isFallbackMode && editor) {
    const selection = editor.getSelection();
    if (selection && !selection.isEmpty()) {
      return editor.getModel()?.getValueInRange(selection) || '';
    }
    return '';
  } else if (fallbackTextarea) {
    const start = fallbackTextarea.selectionStart;
    const end = fallbackTextarea.selectionEnd;
    if (start !== end) {
      return fallbackTextarea.value.substring(start, end);
    }
  }
  return '';
}

/**
 * Replace selected text (or full content if nothing selected) in Monaco editor
 */
export function replaceSelectedText(replacement) {
  if (replacement == null) return false;
  if (!isFallbackMode && editor) {
    const selection = editor.getSelection();
    if (selection && !selection.isEmpty()) {
      editor.executeEdits('devpilot-ai', [
        { range: selection, text: replacement, forceMoveMarkers: true }
      ]);
      editor.focus();
      return true;
    } else {
      setContent(replacement);
      return true;
    }
  } else if (fallbackTextarea) {
    const start = fallbackTextarea.selectionStart;
    const end = fallbackTextarea.selectionEnd;
    if (start !== end) {
      fallbackTextarea.value = fallbackTextarea.value.substring(0, start) + replacement + fallbackTextarea.value.substring(end);
      fallbackTextarea.dispatchEvent(new Event('input'));
      return true;
    } else {
      fallbackTextarea.value = replacement;
      fallbackTextarea.dispatchEvent(new Event('input'));
      return true;
    }
  }
  return false;
}

