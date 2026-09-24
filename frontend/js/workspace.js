/**
 * DevPilot AI — Workspace IDE Controller
 */

import { showToast, escapeHtml } from './utils.js';

// Project Mock Virtual File Tree
const projectFiles = {
  'index.html': {
    name: 'index.html',
    path: '/index.html',
    lang: 'html',
    content: `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DevPilot Web Project</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>
  <div class="app-container">
    <header>
      <h1>DevPilot Autonomous Environment</h1>
      <p>Real-time client sandbox</p>
    </header>
    <main id="content">
      <button id="action-btn" class="btn">Click Me</button>
      <div id="output-box"></div>
    </main>
  </div>
  <script src="js/app.js"></script>
</body>
</html>`
  },
  'style.css': {
    name: 'style.css',
    path: '/css/style.css',
    lang: 'css',
    content: `:root {
  --bg-primary: #0b0f14;
  --text-main: #f8fafc;
  --accent: #6366f1;
}

body {
  margin: 0;
  padding: 2rem;
  background-color: var(--bg-primary);
  color: var(--text-main);
  font-family: -apple-system, sans-serif;
}

.btn {
  background: var(--accent);
  color: #fff;
  border: none;
  padding: 0.6rem 1.2rem;
  border-radius: 6px;
  cursor: pointer;
}`
  },
  'app.js': {
    name: 'app.js',
    path: '/js/app.js',
    lang: 'javascript',
    content: `// Client application entrypoint
document.addEventListener('DOMContentLoaded', () => {
  const btn = document.getElementById('action-btn');
  const box = document.getElementById('output-box');

  btn.addEventListener('click', () => {
    box.innerHTML = '<p>Event triggered at ' + new Date().toLocaleTimeString() + '</p>';
  });
});`
  },
  'README.md': {
    name: 'README.md',
    path: '/README.md',
    lang: 'markdown',
    content: `# DevPilot Sample Project

This is a demonstration project running in the DevPilot AI IDE.

## Features
- Modular web architecture
- Real-time syntax rendering
- Contextual AI Assistant`
  }
};

let activeFileName = 'index.html';
const openTabs = new Set(['index.html', 'style.css', 'app.js']);

document.addEventListener('DOMContentLoaded', () => {
  initFileTree();
  initEditor();
  initAIAssistant();
  initTerminal();
  initPreview();
  initPanelToggles();
});

/**
 * 1. File Tree Interactions
 */
function initFileTree() {
  const fileNodes = document.querySelectorAll('.tree-file');
  fileNodes.forEach(node => {
    node.addEventListener('click', () => {
      const fileName = node.dataset.fileName;
      if (fileName && projectFiles[fileName]) {
        switchToFile(fileName);
      }
    });
  });

  const folderNodes = document.querySelectorAll('.tree-folder');
  folderNodes.forEach(node => {
    node.addEventListener('click', (e) => {
      e.stopPropagation();
      const chevron = node.querySelector('.tree-chevron');
      const children = node.nextElementSibling;
      if (children && children.classList.contains('tree-children')) {
        children.classList.toggle('collapsed');
        node.classList.toggle('collapsed');
      }
    });
  });

  // Action buttons (New File, New Folder)
  const newFileBtn = document.getElementById('new-file-btn');
  const newFolderBtn = document.getElementById('new-folder-btn');

  if (newFileBtn) {
    newFileBtn.addEventListener('click', () => {
      const name = prompt('Enter new file name:');
      if (name) {
        projectFiles[name] = {
          name,
          path: `/${name}`,
          lang: 'plaintext',
          content: `// New file: ${name}\n`
        };
        openTabs.add(name);
        renderTabs();
        switchToFile(name);
        showToast('File Created', `Created file ${name}`, 'success');
      }
    });
  }

  if (newFolderBtn) {
    newFolderBtn.addEventListener('click', () => {
      const folderName = prompt('Enter new folder name:');
      if (folderName) {
        showToast('Folder Created', `Created folder ${folderName}`, 'info');
      }
    });
  }
}

/**
 * 2. Editor & Tabs Management
 */
function initEditor() {
  renderTabs();
  renderActiveFileContent();
}

function renderTabs() {
  const tabsContainer = document.getElementById('editor-tabs-list');
  if (!tabsContainer) return;

  tabsContainer.innerHTML = Array.from(openTabs).map(fileName => {
    const isActive = fileName === activeFileName;
    return `
      <div class="editor-tab ${isActive ? 'active' : ''}" data-file="${fileName}">
        <span class="file-name">${escapeHtml(fileName)}</span>
        <span class="editor-tab-close" data-close-file="${fileName}">&times;</span>
      </div>
    `;
  }).join('');

  // Tab click listeners
  tabsContainer.querySelectorAll('.editor-tab').forEach(tab => {
    tab.addEventListener('click', (e) => {
      if (e.target.dataset.closeFile) {
        const toClose = e.target.dataset.closeFile;
        closeTab(toClose);
        return;
      }
      switchToFile(tab.dataset.file);
    });
  });
}

function switchToFile(fileName) {
  if (!projectFiles[fileName]) return;
  activeFileName = fileName;
  openTabs.add(fileName);

  // Update Tree active class
  document.querySelectorAll('.tree-file').forEach(node => {
    if (node.dataset.fileName === fileName) {
      node.classList.add('active');
    } else {
      node.classList.remove('active');
    }
  });

  renderTabs();
  renderActiveFileContent();
}

function closeTab(fileName) {
  openTabs.delete(fileName);
  if (activeFileName === fileName) {
    const remaining = Array.from(openTabs);
    if (remaining.length > 0) {
      switchToFile(remaining[0]);
    } else {
      activeFileName = '';
      const canvas = document.getElementById('editor-canvas');
      if (canvas) canvas.textContent = '// No file opened';
    }
  }
  renderTabs();
}

function renderActiveFileContent() {
  const canvas = document.getElementById('editor-canvas');
  const gutter = document.getElementById('editor-gutter');
  const statusFile = document.getElementById('status-current-file');
  const statusLang = document.getElementById('status-current-lang');

  if (!canvas || !activeFileName || !projectFiles[activeFileName]) return;

  const file = projectFiles[activeFileName];
  canvas.textContent = file.content;

  // Generate Line Numbers
  const lineCount = file.content.split('\n').length;
  if (gutter) {
    gutter.innerHTML = Array.from({ length: lineCount }, (_, i) => `<div>${i + 1}</div>`).join('');
  }

  if (statusFile) statusFile.textContent = file.path;
  if (statusLang) statusLang.textContent = file.lang.toUpperCase();
}

/**
 * 3. AI Assistant Panel
 */
function initAIAssistant() {
  const sendBtn = document.getElementById('ai-send-btn');
  const input = document.getElementById('ai-input-box');
  const messagesBox = document.getElementById('ai-messages-container');

  if (sendBtn && input) {
    const sendMessage = () => {
      const text = input.value.trim();
      if (!text) return;

      // Append User Message
      appendChatMessage('user', text);
      input.value = '';

      // Simulate AI Assistant response
      setTimeout(() => {
        handleAIMockResponse(text);
      }, 700);
    };

    sendBtn.addEventListener('click', sendMessage);
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
  }

  // Quick action pills
  const actionChips = document.querySelectorAll('.ai-action-chip');
  actionChips.forEach(chip => {
    chip.addEventListener('click', () => {
      const action = chip.dataset.action;
      const promptMap = {
        generate: 'Generate a clean modal component in HTML/CSS/JS',
        explain: `Explain the current code in ${activeFileName}`,
        debug: `Check for potential memory leaks or syntax errors in ${activeFileName}`,
        refactor: 'Refactor this logic to follow clean ES6+ coding standards',
        optimize: 'Optimize the DOM rendering performance',
        tests: 'Generate comprehensive unit test cases'
      };

      if (promptMap[action]) {
        appendChatMessage('user', promptMap[action]);
        setTimeout(() => handleAIMockResponse(promptMap[action]), 700);
      }
    });
  });
}

function appendChatMessage(role, text) {
  const box = document.getElementById('ai-messages-container');
  if (!box) return;

  const msgDiv = document.createElement('div');
  msgDiv.className = `ai-chat-bubble ${role}`;
  msgDiv.innerHTML = `<strong>${role === 'user' ? 'Developer' : 'DevPilot AI'}</strong><div>${escapeHtml(text)}</div>`;
  box.appendChild(msgDiv);
  box.scrollTop = box.scrollHeight;
}

function handleAIMockResponse(prompt) {
  const box = document.getElementById('ai-messages-container');
  if (!box) return;

  const aiDiv = document.createElement('div');
  aiDiv.className = 'ai-chat-bubble assistant';
  aiDiv.innerHTML = `
    <strong>DevPilot AI</strong>
    <p>I reviewed <code>${escapeHtml(activeFileName)}</code>. Here is a suggested enhancement:</p>
    <div class="ai-diff-card">
      <div class="diff-header">
        <span>Suggested Changes (${escapeHtml(activeFileName)})</span>
        <span>+4 -1</span>
      </div>
      <div class="diff-content">
        <div class="diff-line-remove">-  const btn = document.getElementById('action-btn');</div>
        <div class="diff-line-add">+  const btn = document.querySelector('#action-btn');</div>
        <div class="diff-line-add">+  if (!btn) return; // Defensive guard</div>
      </div>
      <div class="diff-actions">
        <button class="btn btn-sm btn-primary" id="diff-apply-btn">Apply Diff</button>
        <button class="btn btn-sm btn-secondary" id="diff-reject-btn">Reject</button>
      </div>
    </div>
  `;
  box.appendChild(aiDiv);
  box.scrollTop = box.scrollHeight;

  const applyBtn = aiDiv.querySelector('#diff-apply-btn');
  const rejectBtn = aiDiv.querySelector('#diff-reject-btn');

  if (applyBtn) {
    applyBtn.addEventListener('click', () => {
      showToast('Diff Applied', 'Modified lines updated in active editor view', 'success');
      applyBtn.disabled = true;
      applyBtn.textContent = 'Applied';
    });
  }

  if (rejectBtn) {
    rejectBtn.addEventListener('click', () => {
      aiDiv.querySelector('.ai-diff-card').remove();
      showToast('Diff Rejected', 'Suggestion dismissed', 'info');
    });
  }
}

/**
 * 4. Terminal Simulation
 */
function initTerminal() {
  const terminalScreen = document.getElementById('terminal-screen');
  const terminalInput = document.getElementById('terminal-command-input');
  const clearBtn = document.getElementById('terminal-clear-btn');
  const stopBtn = document.getElementById('terminal-stop-btn');

  if (terminalInput && terminalScreen) {
    terminalInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const cmd = terminalInput.value.trim();
        if (!cmd) return;

        appendTerminalLine(`$ ${escapeHtml(cmd)}`, 'term-cmd');
        terminalInput.value = '';

        if (cmd === 'clear') {
          terminalScreen.innerHTML = '';
          return;
        }

        if (cmd === 'npm run dev' || cmd === 'run') {
          appendTerminalLine('[DevPilot Container #402] Starting local preview server...', 'term-output');
          setTimeout(() => {
            appendTerminalLine('[Server] Dev server running at http://localhost:3000', 'term-success');
          }, 500);
        } else if (cmd === 'ls') {
          appendTerminalLine('index.html  css/  js/  README.md', 'term-output');
        } else {
          appendTerminalLine(`Command not found in container sandbox: ${escapeHtml(cmd)}`, 'term-error');
        }
      }
    });
  }

  if (clearBtn && terminalScreen) {
    clearBtn.addEventListener('click', () => {
      terminalScreen.innerHTML = '<div class="term-line term-output">Terminal cleared.</div>';
    });
  }

  if (stopBtn) {
    stopBtn.addEventListener('click', () => {
      appendTerminalLine('Process execution terminated by user.', 'term-error');
      showToast('Execution Stopped', 'Container process killed gracefully', 'warning');
    });
  }
}

function appendTerminalLine(text, className = 'term-output') {
  const terminalScreen = document.getElementById('terminal-screen');
  if (!terminalScreen) return;

  const div = document.createElement('div');
  div.className = `term-line ${className}`;
  div.innerHTML = text;
  terminalScreen.appendChild(div);
  terminalScreen.scrollTop = terminalScreen.scrollHeight;
}

/**
 * 5. Live Preview
 */
function initPreview() {
  const refreshBtn = document.getElementById('preview-refresh-btn');
  const previewFrame = document.getElementById('preview-iframe');

  if (refreshBtn && previewFrame) {
    refreshBtn.addEventListener('click', () => {
      showToast('Preview Refreshed', 'Reloading virtual web container...', 'info');
      // Update iframe content with index.html content
      const html = projectFiles['index.html'] ? projectFiles['index.html'].content : '<h1>Preview</h1>';
      previewFrame.srcdoc = html;
    });
  }
}

/**
 * 6. Responsive Panel Toggles
 */
function initPanelToggles() {
  const toggleExplorerBtn = document.getElementById('toggle-explorer-btn');
  const toggleAiBtn = document.getElementById('toggle-ai-btn');
  const explorerPanel = document.getElementById('file-explorer-panel');
  const aiPanel = document.getElementById('ai-sidebar-panel');

  if (toggleExplorerBtn && explorerPanel) {
    toggleExplorerBtn.addEventListener('click', () => {
      explorerPanel.classList.toggle('open');
    });
  }

  if (toggleAiBtn && aiPanel) {
    toggleAiBtn.addEventListener('click', () => {
      aiPanel.classList.toggle('open');
    });
  }
}
