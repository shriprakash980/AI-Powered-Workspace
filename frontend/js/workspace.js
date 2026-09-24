/**
 * DevPilot AI — Workspace IDE Controller
 * Fully integrated with Spring Boot REST APIs for Project, Virtual Filesystem, and Workspace Tree
 */

import { apiRequest } from './api.js';
import { storage } from './storage.js';
import { showToast, escapeHtml } from './utils.js';

let currentProjectId = null;
let currentProject = null;
let activeFileId = null;
const filesMap = new Map(); // fileId -> file entity / node
const openTabs = new Map(); // fileId -> { id, name, path, fileType, isDirty }
let selectedFolderId = null; // currently selected folder for new files/folders

document.addEventListener('DOMContentLoaded', () => {
  initWorkspace();
  initEditorEvents();
  initAIAssistant();
  initTerminal();
  initPreview();
  initPanelToggles();
});

/**
 * 1. Initialize Workspace & Load Project Tree from Backend
 */
async function initWorkspace() {
  // Enforce authentication
  if (!storage.isAuthenticated()) {
    window.location.href = 'login.html';
    return;
  }

  // Extract project ID from URL query parameters
  const params = new URLSearchParams(window.location.search);
  currentProjectId = params.get('project');

  try {
    // If no project ID is provided in URL, retrieve the user's active projects
    if (!currentProjectId) {
      const projectsRes = await apiRequest('/projects');
      const projects = projectsRes.data || [];
      if (projects.length > 0) {
        currentProjectId = projects[0].id;
        const newUrl = new URL(window.location.href);
        newUrl.searchParams.set('project', currentProjectId);
        window.history.replaceState({}, '', newUrl.toString());
      } else {
        showToast('No Project Found', 'Redirecting to dashboard to create a workspace...', 'warning');
        setTimeout(() => { window.location.href = 'dashboard.html'; }, 1200);
        return;
      }
    }

    // Load workspace data from backend
    await loadWorkspaceData(currentProjectId);

  } catch (err) {
    console.error('[Workspace Init Error]', err);
    showToast('Failed to Load Workspace', err.message || 'Error connecting to workspace API', 'danger');
  }
}

async function loadWorkspaceData(projectId) {
  const res = await apiRequest(`/projects/${projectId}/workspace`);
  if (!res || !res.data) {
    throw new Error('Invalid workspace payload received from server');
  }

  const workspace = res.data;
  currentProject = workspace.project;

  // Update Breadcrumb & Project Name
  const nameEl = document.getElementById('workspace-project-name');
  if (nameEl && currentProject) {
    nameEl.textContent = currentProject.name;
    nameEl.title = currentProject.description || currentProject.name;
  }

  const statusEl = document.getElementById('workspace-project-status');
  if (statusEl && currentProject) {
    statusEl.textContent = currentProject.status || 'Active';
  }

  // Cache all files and folders
  filesMap.clear();
  flattenTree(workspace.tree);

  // Render Explorer File Tree
  renderFileTree(workspace.tree);

  // Open starter file
  if (workspace.recentFiles && workspace.recentFiles.length > 0) {
    openFile(workspace.recentFiles[0].id);
  } else {
    // Pick the first file found in tree
    const firstFile = findFirstFile(workspace.tree);
    if (firstFile) {
      openFile(firstFile.id);
    }
  }
}

function flattenTree(nodes) {
  if (!nodes || !Array.isArray(nodes)) return;
  for (const node of nodes) {
    filesMap.set(node.id, node);
    if (node.children && node.children.length > 0) {
      flattenTree(node.children);
    }
  }
}

function findFirstFile(nodes) {
  if (!nodes) return null;
  for (const node of nodes) {
    if (!node.isDirectory && !node.directory) return node;
    if (node.children && node.children.length > 0) {
      const found = findFirstFile(node.children);
      if (found) return found;
    }
  }
  return null;
}

/**
 * 2. Render Explorer File Tree
 */
function renderFileTree(treeNodes) {
  const treeContainer = document.getElementById('explorer-tree');
  if (!treeContainer) return;

  treeContainer.innerHTML = '';

  // Root Project Folder Node
  const rootFolderNode = document.createElement('div');
  rootFolderNode.className = 'tree-node tree-folder';
  rootFolderNode.innerHTML = `
    <svg class="icon icon-sm tree-chevron" viewBox="0 0 24 24"><polyline points="6 9 12 15 18 9"/></svg>
    <span>📁 ${escapeHtml(currentProject ? currentProject.name.toUpperCase() : 'PROJECT')}</span>
  `;
  rootFolderNode.addEventListener('click', (e) => {
    e.stopPropagation();
    selectedFolderId = null; // select root
    document.querySelectorAll('.tree-node').forEach(n => n.classList.remove('selected-parent'));
    rootFolderNode.classList.add('selected-parent');
    const childrenContainer = rootFolderNode.nextElementSibling;
    if (childrenContainer) {
      childrenContainer.classList.toggle('collapsed');
      rootFolderNode.classList.toggle('collapsed');
    }
  });

  const rootChildren = document.createElement('div');
  rootChildren.className = 'tree-children';

  if (!treeNodes || treeNodes.length === 0) {
    rootChildren.innerHTML = '<div style="padding: 8px 16px; color: var(--color-text-muted); font-size: 0.72rem;">No files created yet.</div>';
  } else {
    renderNodes(treeNodes, rootChildren, 1);
  }

  treeContainer.appendChild(rootFolderNode);
  treeContainer.appendChild(rootChildren);

  // Wire up New File & New Folder action buttons
  wireExplorerActionButtons();
}

function renderNodes(nodes, container, depth = 1) {
  for (const node of nodes) {
    const isDir = Boolean(node.isDirectory || node.directory);

    if (isDir) {
      // Folder Item
      const folderDiv = document.createElement('div');
      folderDiv.className = `tree-node tree-folder ${depth === 1 ? 'tree-node-nested' : 'tree-node-deep'}`;
      folderDiv.dataset.folderId = node.id;
      folderDiv.innerHTML = `
        <svg class="icon icon-sm tree-chevron" viewBox="0 0 24 24"><polyline points="6 9 12 15 18 9"/></svg>
        <span style="flex:1;">📁 ${escapeHtml(node.name)}</span>
        <button class="btn btn-icon btn-ghost btn-xs tree-action-del" title="Delete folder" data-del-id="${node.id}" data-is-dir="true">&times;</button>
      `;

      const childContainer = document.createElement('div');
      childContainer.className = 'tree-children';

      folderDiv.addEventListener('click', (e) => {
        if (e.target.dataset.delId) return;
        e.stopPropagation();
        selectedFolderId = node.id;
        document.querySelectorAll('.tree-node').forEach(n => n.classList.remove('selected-parent'));
        folderDiv.classList.add('selected-parent');
        childContainer.classList.toggle('collapsed');
        folderDiv.classList.toggle('collapsed');
      });

      container.appendChild(folderDiv);
      container.appendChild(childContainer);

      if (node.children && node.children.length > 0) {
        renderNodes(node.children, childContainer, depth + 1);
      }
    } else {
      // File Item
      const fileDiv = document.createElement('div');
      fileDiv.className = `tree-node tree-file ${depth === 1 ? 'tree-node-nested' : 'tree-node-deep'} ${node.id === activeFileId ? 'active' : ''}`;
      fileDiv.dataset.fileId = node.id;
      fileDiv.dataset.fileName = node.name;

      const fileIcon = getFileIcon(node.name, node.fileType);
      fileDiv.innerHTML = `
        <span>${fileIcon}</span>
        <span style="flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${escapeHtml(node.name)}</span>
        <button class="btn btn-icon btn-ghost btn-xs tree-action-del" title="Delete file" data-del-id="${node.id}" data-is-dir="false">&times;</button>
      `;

      fileDiv.addEventListener('click', (e) => {
        if (e.target.dataset.delId) return;
        openFile(node.id);
      });

      container.appendChild(fileDiv);
    }
  }

  // Bind delete buttons in tree
  container.querySelectorAll('.tree-action-del').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const id = btn.dataset.delId;
      const isDir = btn.dataset.isDir === 'true';
      deleteItem(id, isDir);
    });
  });
}

function getFileIcon(name, fileType) {
  if (name.endsWith('.html') || fileType === 'html') return '📄';
  if (name.endsWith('.css') || fileType === 'css') return '🎨';
  if (name.endsWith('.js') || name.endsWith('.jsx') || name.endsWith('.ts') || name.endsWith('.tsx') || fileType === 'javascript') return '⚡';
  if (name.endsWith('.md') || fileType === 'markdown') return '📘';
  if (name.endsWith('.json') || fileType === 'json') return '⚙️';
  if (name.endsWith('.java') || fileType === 'java') return '☕';
  if (name.endsWith('.py') || fileType === 'python') return '🐍';
  return '📝';
}

function wireExplorerActionButtons() {
  const newFileBtn = document.getElementById('new-file-btn');
  const newFolderBtn = document.getElementById('new-folder-btn');

  if (newFileBtn) {
    newFileBtn.onclick = async () => {
      const parentName = selectedFolderId && filesMap.has(selectedFolderId) ? filesMap.get(selectedFolderId).name : 'root';
      const name = prompt(`Create new file under [${parentName}]:\n(e.g. index.html, style.css, Main.java)`);
      if (name && name.trim()) {
        try {
          const res = await apiRequest(`/projects/${currentProjectId}/files`, {
            method: 'POST',
            body: JSON.stringify({
              name: name.trim(),
              parentId: selectedFolderId,
              content: `// ${name.trim()}\n`
            })
          });
          showToast('File Created', `Created file ${res.data.name}`, 'success');
          await loadWorkspaceData(currentProjectId);
          if (res.data && res.data.id) {
            openFile(res.data.id);
          }
        } catch (err) {
          showToast('Failed to Create File', err.message || 'Error creating file', 'danger');
        }
      }
    };
  }

  if (newFolderBtn) {
    newFolderBtn.onclick = async () => {
      const parentName = selectedFolderId && filesMap.has(selectedFolderId) ? filesMap.get(selectedFolderId).name : 'root';
      const name = prompt(`Create new folder under [${parentName}]:\n(e.g. components, utils, styles)`);
      if (name && name.trim()) {
        try {
          const res = await apiRequest(`/projects/${currentProjectId}/files/folders`, {
            method: 'POST',
            body: JSON.stringify({
              name: name.trim(),
              parentId: selectedFolderId
            })
          });
          showToast('Folder Created', `Created folder ${res.data.name}`, 'success');
          await loadWorkspaceData(currentProjectId);
        } catch (err) {
          showToast('Failed to Create Folder', err.message || 'Error creating folder', 'danger');
        }
      }
    };
  }
}

/**
 * 3. Open, Read, and Display File in Editor
 */
async function openFile(fileId) {
  try {
    let file = filesMap.get(fileId);

    // If file content is not loaded in memory, fetch it via API
    if (!file || file.content === undefined || file.content === null) {
      const res = await apiRequest(`/projects/${currentProjectId}/files/${fileId}`);
      file = res.data;
      filesMap.set(fileId, file);
    }

    activeFileId = fileId;

    // Add to open tabs if not present
    if (!openTabs.has(fileId)) {
      openTabs.set(fileId, {
        id: file.id,
        name: file.name,
        path: file.path,
        fileType: file.fileType || 'plaintext',
        isDirty: false
      });
    }

    // Update active class in explorer file tree
    document.querySelectorAll('.tree-file').forEach(node => {
      if (node.dataset.fileId === fileId) {
        node.classList.add('active');
      } else {
        node.classList.remove('active');
      }
    });

    renderTabs();
    renderEditorContent(file);

  } catch (err) {
    console.error('[Open File Error]', err);
    showToast('Failed to Open File', err.message || 'Could not load file content', 'danger');
  }
}

function renderTabs() {
  const tabsContainer = document.getElementById('editor-tabs-list');
  if (!tabsContainer) return;

  tabsContainer.innerHTML = Array.from(openTabs.values()).map(tab => {
    const isActive = tab.id === activeFileId;
    return `
      <div class="editor-tab ${isActive ? 'active' : ''}" data-file-id="${tab.id}">
        <span class="file-name">${escapeHtml(tab.name)}</span>
        ${tab.isDirty ? '<span class="dirty-indicator" style="display:inline-block; margin-left:4px;"></span>' : ''}
        <span class="editor-tab-close" data-close-file-id="${tab.id}">&times;</span>
      </div>
    `;
  }).join('');

  tabsContainer.querySelectorAll('.editor-tab').forEach(tabEl => {
    tabEl.addEventListener('click', (e) => {
      const closeId = e.target.dataset.closeFileId;
      if (closeId) {
        e.stopPropagation();
        closeTab(closeId);
        return;
      }
      openFile(tabEl.dataset.fileId);
    });
  });
}

function closeTab(fileId) {
  openTabs.delete(fileId);
  if (activeFileId === fileId) {
    const remaining = Array.from(openTabs.keys());
    if (remaining.length > 0) {
      openFile(remaining[remaining.length - 1]);
    } else {
      activeFileId = null;
      const canvas = document.getElementById('editor-canvas');
      if (canvas) canvas.value = '';
      const gutter = document.getElementById('editor-gutter');
      if (gutter) gutter.innerHTML = '';
      const statusFile = document.getElementById('status-current-file');
      if (statusFile) statusFile.textContent = 'No file open';
    }
  }
  renderTabs();
}

function renderEditorContent(file) {
  const canvas = document.getElementById('editor-canvas');
  const gutter = document.getElementById('editor-gutter');
  const statusFile = document.getElementById('status-current-file');
  const statusLang = document.getElementById('status-current-lang');

  if (!canvas || !file) return;

  const content = file.content != null ? file.content : '';
  canvas.value = content;

  // Generate line numbers
  updateLineNumbers(content);

  if (statusFile) statusFile.textContent = file.path ? `/${file.path}` : `/${file.name}`;
  if (statusLang) statusLang.textContent = (file.fileType || 'plaintext').toUpperCase();
}

function updateLineNumbers(content) {
  const gutter = document.getElementById('editor-gutter');
  if (!gutter) return;
  const lineCount = (content.split('\n') || []).length || 1;
  gutter.innerHTML = Array.from({ length: lineCount }, (_, i) => `<div>${i + 1}</div>`).join('');
}

/**
 * 4. Save File Content to Backend API
 */
async function saveActiveFile() {
  if (!activeFileId || !currentProjectId) return;

  const canvas = document.getElementById('editor-canvas');
  if (!canvas) return;

  const content = canvas.value;
  const file = filesMap.get(activeFileId);

  try {
    const res = await apiRequest(`/projects/${currentProjectId}/files/${activeFileId}`, {
      method: 'PUT',
      body: JSON.stringify({ content })
    });

    if (file) {
      file.content = content;
      filesMap.set(activeFileId, file);
    }

    if (openTabs.has(activeFileId)) {
      openTabs.get(activeFileId).isDirty = false;
      renderTabs();
    }

    showToast('File Saved', `Saved ${file ? file.name : 'file'} successfully`, 'success');

  } catch (err) {
    console.error('[Save File Error]', err);
    showToast('Save Failed', err.message || 'Error saving file to server', 'danger');
  }
}

async function deleteItem(itemId, isDir) {
  const target = filesMap.get(itemId);
  const name = target ? target.name : 'item';
  const confirmed = confirm(`Are you sure you want to delete ${isDir ? 'folder' : 'file'} "${name}"?${isDir ? ' All files inside it will also be deleted.' : ''}`);
  if (!confirmed) return;

  try {
    await apiRequest(`/projects/${currentProjectId}/files/${itemId}`, {
      method: 'DELETE'
    });

    showToast('Deleted', `Deleted ${name}`, 'info');

    if (openTabs.has(itemId)) {
      closeTab(itemId);
    }

    await loadWorkspaceData(currentProjectId);

  } catch (err) {
    console.error('[Delete Item Error]', err);
    showToast('Delete Failed', err.message || 'Error deleting item', 'danger');
  }
}

/**
 * 5. Editor Event Listeners (Typing, Tab indent, Save Shortcut)
 */
function initEditorEvents() {
  const canvas = document.getElementById('editor-canvas');
  const saveBtn = document.getElementById('workspace-save-btn');

  if (saveBtn) {
    saveBtn.addEventListener('click', () => {
      saveActiveFile();
    });
  }

  if (canvas) {
    // Handle input to mark dirty & update line numbers
    canvas.addEventListener('input', () => {
      if (activeFileId && openTabs.has(activeFileId)) {
        openTabs.get(activeFileId).isDirty = true;
        renderTabs();
      }
      updateLineNumbers(canvas.value);
    });

    // Keyboard shortcuts (Ctrl+S / Cmd+S, Tab support)
    canvas.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        saveActiveFile();
      } else if (e.key === 'Tab') {
        e.preventDefault();
        const start = canvas.selectionStart;
        const end = canvas.selectionEnd;
        canvas.value = canvas.value.substring(0, start) + '  ' + canvas.value.substring(end);
        canvas.selectionStart = canvas.selectionEnd = start + 2;
        canvas.dispatchEvent(new Event('input'));
      }
    });
  }

  // Global Ctrl+S shortcut
  window.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault();
      saveActiveFile();
    }
  });
}

/**
 * 6. AI Assistant Panel
 */
function initAIAssistant() {
  const sendBtn = document.getElementById('ai-send-btn');
  const input = document.getElementById('ai-input-box');
  const messagesBox = document.getElementById('ai-messages-container');

  if (sendBtn && input) {
    const sendMessage = () => {
      const text = input.value.trim();
      if (!text) return;

      appendChatMessage('user', text);
      input.value = '';

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
      const activeFile = filesMap.get(activeFileId);
      const fileName = activeFile ? activeFile.name : 'active file';
      const promptMap = {
        generate: 'Generate a clean component in HTML/CSS/JS',
        explain: `Explain the current code in ${fileName}`,
        debug: `Check for potential memory leaks or syntax errors in ${fileName}`,
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

  const activeFile = filesMap.get(activeFileId);
  const fileName = activeFile ? activeFile.name : 'active file';

  const aiDiv = document.createElement('div');
  aiDiv.className = 'ai-chat-bubble assistant';
  aiDiv.innerHTML = `
    <strong>DevPilot AI</strong>
    <p>I reviewed <code>${escapeHtml(fileName)}</code>. Here is a suggested enhancement:</p>
    <div class="ai-diff-card">
      <div class="diff-header">
        <span>Suggested Changes (${escapeHtml(fileName)})</span>
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
 * 7. Terminal Simulation
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
          const names = Array.from(filesMap.values()).map(f => f.name).join('  ');
          appendTerminalLine(names || 'No files found', 'term-output');
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
 * 8. Live Preview
 */
function initPreview() {
  const refreshBtn = document.getElementById('preview-refresh-btn');
  const previewFrame = document.getElementById('preview-iframe');

  if (refreshBtn && previewFrame) {
    refreshBtn.addEventListener('click', () => {
      showToast('Preview Refreshed', 'Reloading virtual web container...', 'info');
      // Look for index.html in filesMap
      let indexHtml = '';
      for (const file of filesMap.values()) {
        if (file.name === 'index.html' && file.content) {
          indexHtml = file.content;
          break;
        }
      }
      previewFrame.srcdoc = indexHtml || '<h2 style="font-family:sans-serif; text-align:center; padding-top:2rem;">DevPilot Live Sandbox Preview Ready</h2>';
    });
  }
}

/**
 * 9. Responsive Panel Toggles
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
