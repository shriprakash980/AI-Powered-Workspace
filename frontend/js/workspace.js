/**
 * DevPilot AI — Workspace IDE Controller (Phase 7)
 * Monaco Editor, File Explorer, Tabs, Resizable Panels, Command Palette, Quick Open & Diagnostics
 */

import { apiRequest } from './api.js';
import { storage } from './storage.js';
import { showToast, escapeHtml } from './utils.js';
import {
  initializeEditor,
  loadFile,
  getContent,
  setContent,
  setLanguage,
  setTheme,
  updatePreferences,
  layout,
  focusEditor,
  formatDocument,
  goToLine,
  disposeFileModel,
  onContentChange,
  onCursorChange,
  onSaveShortcut,
  detectLanguage,
  isBinaryFile,
  MAX_EDITOR_FILE_SIZE_BYTES,
  getSelectedText,
  replaceSelectedText
} from './editor.js';
import { initAI, updateContextBadge } from './ai.js';

// Centralized Workspace State
const workspaceState = {
  projectId: null,
  project: null,
  tree: [],
  filesMap: new Map(), // fileId -> file entity
  openFiles: new Map(), // fileId -> { id, name, path, language, content, originalContent, dirty }
  activeFileId: null,
  selectedFolderId: null,
  saveStatus: 'saved', // 'saved' | 'saving' | 'unsaved' | 'error'
  sidebarWidth: 260,
  bottomPanelOpen: true,
  bottomPanelHeight: 220,
  activeBottomTab: 'panel-terminal',
  contextTargetId: null,
  contextTargetIsDir: false,
  closingTabId: null
};

document.addEventListener('DOMContentLoaded', () => {
  initWorkspace();
  initThemeObserver();
  initSplitters();
  initBottomPanel();
  initCommandPalette();
  initQuickOpen();
  initContextMenu();
  initPreferencesModal();
  initUnsavedModal();
  initAIAssistant();
  initTerminal();
  initPreview();
  initGlobalShortcuts();
});

/**
 * 1. Initialize Workspace & Monaco Editor
 */
async function initWorkspace() {
  if (!storage.isAuthenticated()) {
    window.location.href = 'login.html';
    return;
  }

  // Restore saved sidebar width
  const savedWidth = localStorage.getItem('devpilot_sidebar_width');
  if (savedWidth) {
    workspaceState.sidebarWidth = Math.max(200, Math.min(450, parseInt(savedWidth, 10)));
    document.getElementById('workspace-body')?.style.setProperty('--sidebar-width', `${workspaceState.sidebarWidth}px`);
  }

  // Extract project ID from URL query parameters
  const params = new URLSearchParams(window.location.search);
  workspaceState.projectId = params.get('project');

  try {
    if (!workspaceState.projectId) {
      const projectsRes = await apiRequest('/projects');
      const projects = projectsRes.data || [];
      if (projects.length > 0) {
        workspaceState.projectId = projects[0].id;
        const newUrl = new URL(window.location.href);
        newUrl.searchParams.set('project', workspaceState.projectId);
        window.history.replaceState({}, '', newUrl.toString());
      } else {
        showToast('No Projects Found', 'Redirecting to dashboard to create a workspace...', 'warning');
        setTimeout(() => { window.location.href = 'dashboard.html'; }, 1200);
        return;
      }
    }

    // 1. Initialize Monaco Editor in container
    const editorContainer = document.getElementById('editor-container');
    if (editorContainer) {
      await initializeEditor(editorContainer);

      onContentChange((newContent, fileId) => {
        handleEditorInput(newContent, fileId);
      });

      onCursorChange((pos) => {
        updateCursorPosition(pos.lineNumber, pos.column);
      });

      onSaveShortcut(() => {
        saveActiveFile();
      });

      logOutput('Monaco Editor initialized successfully');
    }

    // 2. Load workspace data from backend API
    await loadWorkspaceData(workspaceState.projectId);

  } catch (err) {
    console.error('[Workspace Init Error]', err);
    showToast('Failed to Load Workspace', err.message || 'Error connecting to workspace API', 'danger');
    logOutput(`[Error] Workspace init failed: ${err.message}`);
  }
}

async function loadWorkspaceData(projectId) {
  logOutput(`Loading workspace for project ${projectId}...`);
  const res = await apiRequest(`/projects/${projectId}/workspace`);
  if (!res || !res.data) {
    throw new Error('Invalid workspace payload received from server');
  }

  const workspace = res.data;
  workspaceState.project = workspace.project;
  workspaceState.tree = workspace.tree || [];

  // Update Breadcrumb & Project Name
  const nameEl = document.getElementById('workspace-project-name');
  if (nameEl && workspaceState.project) {
    nameEl.textContent = workspaceState.project.name;
    nameEl.title = workspaceState.project.description || workspaceState.project.name;
  }

  const statusEl = document.getElementById('workspace-project-status');
  if (statusEl && workspaceState.project) {
    statusEl.textContent = workspaceState.project.status || 'Active';
  }

  // Cache files
  workspaceState.filesMap.clear();
  flattenTree(workspaceState.tree);

  // Render Explorer File Tree
  renderFileTree(workspaceState.tree);

  logOutput(`Workspace loaded: ${workspace.totalFiles} files in tree`);

  // Open initial file (recent file or first in tree)
  if (workspace.recentFiles && workspace.recentFiles.length > 0) {
    openFile(workspace.recentFiles[0].id);
  } else {
    const first = findFirstFile(workspaceState.tree);
    if (first) openFile(first.id);
  }
}

function flattenTree(nodes) {
  if (!nodes || !Array.isArray(nodes)) return;
  for (const node of nodes) {
    workspaceState.filesMap.set(node.id, node);
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
 * 2. File Explorer Tree Rendering
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
    <span>📁 ${escapeHtml(workspaceState.project ? workspaceState.project.name.toUpperCase() : 'PROJECT')}</span>
  `;

  rootFolderNode.addEventListener('click', (e) => {
    e.stopPropagation();
    workspaceState.selectedFolderId = null; // select root
    document.querySelectorAll('.tree-node').forEach(n => n.classList.remove('selected-parent'));
    rootFolderNode.classList.add('selected-parent');
    const childrenContainer = rootFolderNode.nextElementSibling;
    if (childrenContainer) {
      childrenContainer.classList.toggle('collapsed');
      rootFolderNode.classList.toggle('collapsed');
    }
  });

  // Right-click root context menu
  rootFolderNode.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    openContextMenu(e.clientX, e.clientY, null, true);
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

  wireExplorerHeaderButtons();
}

function renderNodes(nodes, container, depth = 1) {
  for (const node of nodes) {
    const isDir = Boolean(node.isDirectory || node.directory);

    if (isDir) {
      const folderDiv = document.createElement('div');
      folderDiv.className = `tree-node tree-folder ${depth === 1 ? 'tree-node-nested' : 'tree-node-deep'}`;
      folderDiv.dataset.folderId = node.id;
      folderDiv.innerHTML = `
        <svg class="icon icon-sm tree-chevron" viewBox="0 0 24 24"><polyline points="6 9 12 15 18 9"/></svg>
        <span style="flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">📁 ${escapeHtml(node.name)}</span>
      `;

      const childContainer = document.createElement('div');
      childContainer.className = 'tree-children';

      folderDiv.addEventListener('click', (e) => {
        e.stopPropagation();
        workspaceState.selectedFolderId = node.id;
        document.querySelectorAll('.tree-node').forEach(n => n.classList.remove('selected-parent'));
        folderDiv.classList.add('selected-parent');
        childContainer.classList.toggle('collapsed');
        folderDiv.classList.toggle('collapsed');
      });

      folderDiv.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        e.stopPropagation();
        openContextMenu(e.clientX, e.clientY, node.id, true);
      });

      container.appendChild(folderDiv);
      container.appendChild(childContainer);

      if (node.children && node.children.length > 0) {
        renderNodes(node.children, childContainer, depth + 1);
      }
    } else {
      const fileDiv = document.createElement('div');
      fileDiv.className = `tree-node tree-file ${depth === 1 ? 'tree-node-nested' : 'tree-node-deep'} ${node.id === workspaceState.activeFileId ? 'active' : ''}`;
      fileDiv.dataset.fileId = node.id;
      fileDiv.dataset.fileName = node.name;

      const fileIcon = getFileIcon(node.name, node.fileType);
      fileDiv.innerHTML = `
        <span>${fileIcon}</span>
        <span style="flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${escapeHtml(node.name)}</span>
      `;

      fileDiv.addEventListener('click', () => {
        openFile(node.id);
      });

      fileDiv.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        e.stopPropagation();
        openContextMenu(e.clientX, e.clientY, node.id, false);
      });

      container.appendChild(fileDiv);
    }
  }
}

function getFileIcon(name, fileType) {
  if (name.endsWith('.html') || fileType === 'html') return '📄';
  if (name.endsWith('.css') || fileType === 'css') return '🎨';
  if (name.endsWith('.js') || name.endsWith('.jsx') || name.endsWith('.ts') || name.endsWith('.tsx') || fileType === 'javascript') return '⚡';
  if (name.endsWith('.md') || fileType === 'markdown') return '📘';
  if (name.endsWith('.json') || fileType === 'json') return '⚙️';
  if (name.endsWith('.java') || fileType === 'java') return '☕';
  if (name.endsWith('.py') || fileType === 'python') return '🐍';
  if (name.endsWith('.c') || name.endsWith('.cpp')) return '🔷';
  if (name.endsWith('.sql') || fileType === 'sql') return '🗄️';
  if (isBinaryFile(name)) return '📦';
  return '📝';
}

function wireExplorerHeaderButtons() {
  const newFileBtn = document.getElementById('new-file-btn');
  const newFolderBtn = document.getElementById('new-folder-btn');
  const refreshBtn = document.getElementById('refresh-tree-btn');

  if (newFileBtn) {
    newFileBtn.onclick = () => triggerCreateFile(workspaceState.selectedFolderId);
  }

  if (newFolderBtn) {
    newFolderBtn.onclick = () => triggerCreateFolder(workspaceState.selectedFolderId);
  }

  if (refreshBtn) {
    refreshBtn.onclick = async () => {
      await loadWorkspaceData(workspaceState.projectId);
      showToast('Files Refreshed', 'Virtual tree updated from database', 'info');
    };
  }
}

/**
 * 3. File Opening & Tab Management
 */
export async function openFile(fileId) {
  try {
    let file = workspaceState.filesMap.get(fileId);

    // If file content not cached, fetch via API
    if (!file || file.content === undefined || file.content === null) {
      const res = await apiRequest(`/projects/${workspaceState.projectId}/files/${fileId}`);
      file = res.data;
      workspaceState.filesMap.set(fileId, file);
    }

    // Check for binary files
    if (isBinaryFile(file.name)) {
      displayBinaryView(file);
      return;
    }

    hideBinaryView();

    // Check size limit warning
    const size = file.size || (file.content ? file.content.length : 0);
    if (size > MAX_EDITOR_FILE_SIZE_BYTES) {
      const proceed = confirm(`File "${file.name}" is large (${(size / 1024).toFixed(1)} KB). Loading it into the editor may affect performance. Continue?`);
      if (!proceed) return;
    }

    // Save in-progress changes of currently active file tab before switching
    if (workspaceState.activeFileId && workspaceState.openFiles.has(workspaceState.activeFileId)) {
      const currentTab = workspaceState.openFiles.get(workspaceState.activeFileId);
      currentTab.content = getContent();
    }

    workspaceState.activeFileId = fileId;

    // Add to open tabs if not present
    if (!workspaceState.openFiles.has(fileId)) {
      workspaceState.openFiles.set(fileId, {
        id: file.id,
        name: file.name,
        path: file.path,
        language: detectLanguage(file.name),
        content: file.content != null ? file.content : '',
        originalContent: file.content != null ? file.content : '',
        dirty: false
      });
    }

    // Load file into Monaco
    loadFile(file);

    // Update active class in explorer tree
    document.querySelectorAll('.tree-file').forEach(node => {
      node.classList.toggle('active', node.dataset.fileId === fileId);
    });

    renderTabs();
    updateStatusBar(file);
    logOutput(`Opened file: ${file.name}`);

  } catch (err) {
    console.error('[Open File Error]', err);
    showToast('Failed to Open File', err.message || 'Error loading file content', 'danger');
  }
}

function displayBinaryView(file) {
  const binaryView = document.getElementById('binary-file-view');
  const nameEl = document.getElementById('binary-file-name');
  const metaEl = document.getElementById('binary-file-meta');
  if (binaryView) {
    binaryView.style.display = 'flex';
    if (nameEl) nameEl.textContent = file.name;
    if (metaEl) metaEl.textContent = `Type: ${file.fileType || 'binary'} | Path: ${file.path}`;
  }
}

function hideBinaryView() {
  const binaryView = document.getElementById('binary-file-view');
  if (binaryView) binaryView.style.display = 'none';
}

function renderTabs() {
  const tabsContainer = document.getElementById('editor-tabs-list');
  if (!tabsContainer) return;

  tabsContainer.innerHTML = Array.from(workspaceState.openFiles.values()).map(tab => {
    const isActive = tab.id === workspaceState.activeFileId;
    const icon = getFileIcon(tab.name, tab.language);
    return `
      <div class="editor-tab ${isActive ? 'active' : ''}" data-file-id="${tab.id}" title="${escapeHtml(tab.path)}">
        <span>${icon}</span>
        <span class="file-name">${escapeHtml(tab.name)}</span>
        <span class="dirty-indicator" style="display: ${tab.dirty ? 'inline-block' : 'none'};"></span>
        <span class="editor-tab-close" data-close-file-id="${tab.id}" title="Close tab">&times;</span>
      </div>
    `;
  }).join('');

  tabsContainer.querySelectorAll('.editor-tab').forEach(tabEl => {
    tabEl.addEventListener('click', (e) => {
      const closeId = e.target.dataset.closeFileId;
      if (closeId) {
        e.stopPropagation();
        handleCloseTabRequest(closeId);
        return;
      }
      openFile(tabEl.dataset.fileId);
    });
  });
}

function handleCloseTabRequest(fileId) {
  const tab = workspaceState.openFiles.get(fileId);
  if (!tab) return;

  if (tab.dirty) {
    // Show unsaved confirmation dialog
    workspaceState.closingTabId = fileId;
    const modal = document.getElementById('unsaved-confirm-modal');
    const nameEl = document.getElementById('unsaved-file-name');
    if (nameEl) nameEl.textContent = tab.name;
    if (modal) modal.style.display = 'flex';
  } else {
    closeTab(fileId);
  }
}

function closeTab(fileId) {
  workspaceState.openFiles.delete(fileId);
  disposeFileModel(fileId);

  if (workspaceState.activeFileId === fileId) {
    const remaining = Array.from(workspaceState.openFiles.keys());
    if (remaining.length > 0) {
      openFile(remaining[remaining.length - 1]);
    } else {
      workspaceState.activeFileId = null;
      setContent('');
      const statusFile = document.getElementById('status-current-file');
      if (statusFile) statusFile.textContent = 'No file open';
      updateSaveStatus('saved');
    }
  }
  renderTabs();
}

/**
 * 4. Content Changes & Dirty State
 */
function handleEditorInput(newContent, fileId) {
  const targetId = fileId || workspaceState.activeFileId;
  if (!targetId || !workspaceState.openFiles.has(targetId)) return;

  const tab = workspaceState.openFiles.get(targetId);
  tab.content = newContent;

  const isNowDirty = tab.content !== tab.originalContent;
  if (tab.dirty !== isNowDirty) {
    tab.dirty = isNowDirty;
    renderTabs();
    updateSaveStatus(isNowDirty ? 'unsaved' : 'saved');
  }
}

function updateSaveStatus(status) {
  workspaceState.saveStatus = status;
  const statusEl = document.getElementById('status-save-state');
  const dotEl = document.getElementById('status-dirty-dot');

  if (!statusEl) return;

  if (status === 'saving') {
    statusEl.textContent = 'Saving...';
    if (dotEl) dotEl.style.display = 'none';
  } else if (status === 'unsaved') {
    statusEl.textContent = 'Unsaved changes';
    if (dotEl) dotEl.style.display = 'inline-block';
  } else if (status === 'error') {
    statusEl.textContent = 'Save failed';
    if (dotEl) dotEl.style.display = 'inline-block';
  } else {
    statusEl.textContent = 'Saved';
    if (dotEl) dotEl.style.display = 'none';
  }
}

function updateStatusBar(file) {
  const pathEl = document.getElementById('status-current-file');
  const langEl = document.getElementById('status-current-lang');
  const tabSizeEl = document.getElementById('status-tab-size');

  if (pathEl && file) {
    pathEl.textContent = file.path.startsWith('/') ? file.path : `/${file.path}`;
  }
  if (langEl && file) {
    const lang = detectLanguage(file.name);
    langEl.textContent = lang.toUpperCase();
  }
  if (tabSizeEl) {
    const prefs = storage.getEditorPreferences();
    tabSizeEl.textContent = `Spaces: ${prefs.tabSize || 4}`;
  }
}

function updateCursorPosition(line, col) {
  const posEl = document.getElementById('status-cursor-pos');
  if (posEl) {
    posEl.textContent = `Ln ${line}, Col ${col}`;
  }
}

/**
 * 5. Save File to Backend API
 */
export async function saveActiveFile() {
  const fileId = workspaceState.activeFileId;
  if (!fileId || !workspaceState.openFiles.has(fileId)) return;

  const tab = workspaceState.openFiles.get(fileId);
  const content = getContent();

  updateSaveStatus('saving');

  try {
    const res = await apiRequest(`/projects/${workspaceState.projectId}/files/${fileId}`, {
      method: 'PUT',
      body: JSON.stringify({ content })
    });

    tab.originalContent = content;
    tab.content = content;
    tab.dirty = false;

    // Update cache in filesMap
    const cached = workspaceState.filesMap.get(fileId);
    if (cached) {
      cached.content = content;
      workspaceState.filesMap.set(fileId, cached);
    }

    renderTabs();
    updateSaveStatus('saved');
    showToast('Saved', `Saved ${tab.name} successfully`, 'success');
    logOutput(`[Saved] ${tab.path} at ${new Date().toLocaleTimeString()}`);

  } catch (err) {
    console.error('[Save File Error]', err);
    updateSaveStatus('error');
    showToast('Save Failed', err.message || 'Error saving file to server', 'danger');
    logOutput(`[Error] Failed to save ${tab.name}: ${err.message}`);
  }
}

/**
 * 6. File & Folder CRUD Operations
 */
async function triggerCreateFile(parentId = null) {
  const parentName = parentId && workspaceState.filesMap.has(parentId) ? workspaceState.filesMap.get(parentId).name : 'root';
  const name = prompt(`Create new file under [${parentName}]:\n(e.g. index.html, style.css, Main.java)`);
  if (!name || !name.trim()) return;

  try {
    const res = await apiRequest(`/projects/${workspaceState.projectId}/files`, {
      method: 'POST',
      body: JSON.stringify({
        name: name.trim(),
        parentId: parentId,
        content: `// ${name.trim()}\n`
      })
    });

    showToast('File Created', `Created file ${res.data.name}`, 'success');
    logOutput(`Created file: ${res.data.path}`);
    await loadWorkspaceData(workspaceState.projectId);
    if (res.data && res.data.id) {
      openFile(res.data.id);
    }
  } catch (err) {
    showToast('Failed to Create File', err.message || 'Error creating file', 'danger');
  }
}

async function triggerCreateFolder(parentId = null) {
  const parentName = parentId && workspaceState.filesMap.has(parentId) ? workspaceState.filesMap.get(parentId).name : 'root';
  const name = prompt(`Create new folder under [${parentName}]:\n(e.g. components, utils, assets)`);
  if (!name || !name.trim()) return;

  try {
    const res = await apiRequest(`/projects/${workspaceState.projectId}/files/folders`, {
      method: 'POST',
      body: JSON.stringify({
        name: name.trim(),
        parentId: parentId
      })
    });

    showToast('Folder Created', `Created folder ${res.data.name}`, 'success');
    logOutput(`Created folder: ${res.data.path}`);
    await loadWorkspaceData(workspaceState.projectId);
  } catch (err) {
    showToast('Failed to Create Folder', err.message || 'Error creating folder', 'danger');
  }
}

async function triggerRenameItem(fileId) {
  const file = workspaceState.filesMap.get(fileId);
  if (!file) return;

  const newName = prompt(`Rename "${file.name}" to:`, file.name);
  if (!newName || !newName.trim() || newName.trim() === file.name) return;

  try {
    const res = await apiRequest(`/projects/${workspaceState.projectId}/files/${fileId}`, {
      method: 'PATCH',
      body: JSON.stringify({ name: newName.trim() })
    });

    showToast('Renamed', `Renamed to ${res.data.name}`, 'success');
    logOutput(`Renamed ${file.name} -> ${res.data.name}`);

    // Update tab if open
    if (workspaceState.openFiles.has(fileId)) {
      const tab = workspaceState.openFiles.get(fileId);
      tab.name = res.data.name;
      tab.path = res.data.path;
      tab.language = detectLanguage(res.data.name);
      setLanguage(tab.language);
      renderTabs();
      updateStatusBar(res.data);
    }

    await loadWorkspaceData(workspaceState.projectId);

  } catch (err) {
    showToast('Rename Failed', err.message || 'Error renaming item', 'danger');
  }
}

async function triggerDeleteItem(fileId, isDir) {
  const file = workspaceState.filesMap.get(fileId);
  const name = file ? file.name : 'item';
  const confirmed = confirm(`Are you sure you want to delete ${isDir ? 'folder' : 'file'} "${name}"?${isDir ? ' All files inside will also be deleted.' : ''}`);
  if (!confirmed) return;

  try {
    await apiRequest(`/projects/${workspaceState.projectId}/files/${fileId}`, {
      method: 'DELETE'
    });

    showToast('Deleted', `Deleted ${name}`, 'info');
    logOutput(`Deleted: ${name}`);

    if (workspaceState.openFiles.has(fileId)) {
      closeTab(fileId);
    }

    await loadWorkspaceData(workspaceState.projectId);

  } catch (err) {
    showToast('Delete Failed', err.message || 'Error deleting item', 'danger');
  }
}

/**
 * 7. Resizable Splitters
 */
function initSplitters() {
  const sidebarResizer = document.getElementById('sidebar-resizer');
  const bottomResizer = document.getElementById('bottom-panel-resizer');
  const workspaceBody = document.getElementById('workspace-body');
  const bottomPanel = document.getElementById('bottom-panel');

  if (sidebarResizer && workspaceBody) {
    let isDragging = false;

    sidebarResizer.addEventListener('mousedown', (e) => {
      isDragging = true;
      sidebarResizer.classList.add('resizing');
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    });

    window.addEventListener('mousemove', (e) => {
      if (!isDragging) return;
      const newWidth = Math.max(200, Math.min(450, e.clientX));
      workspaceState.sidebarWidth = newWidth;
      workspaceBody.style.setProperty('--sidebar-width', `${newWidth}px`);
      layout();
    });

    window.addEventListener('mouseup', () => {
      if (isDragging) {
        isDragging = false;
        sidebarResizer.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        localStorage.setItem('devpilot_sidebar_width', String(workspaceState.sidebarWidth));
        layout();
      }
    });
  }

  if (bottomResizer && bottomPanel) {
    let isDragging = false;

    bottomResizer.addEventListener('mousedown', () => {
      isDragging = true;
      bottomResizer.classList.add('resizing');
      document.body.style.cursor = 'row-resize';
      document.body.style.userSelect = 'none';
    });

    window.addEventListener('mousemove', (e) => {
      if (!isDragging) return;
      const windowHeight = window.innerHeight;
      const newHeight = Math.max(80, Math.min(500, windowHeight - e.clientY - 24)); // 24 is statusbar
      workspaceState.bottomPanelHeight = newHeight;
      bottomPanel.style.height = `${newHeight}px`;
      layout();
    });

    window.addEventListener('mouseup', () => {
      if (isDragging) {
        isDragging = false;
        bottomResizer.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        layout();
      }
    });
  }

  // Window resize handler
  window.addEventListener('resize', () => {
    layout();
  });
}

/**
 * 8. Bottom Panel Tabs
 */
function initBottomPanel() {
  const tabBtns = document.querySelectorAll('.bottom-tab-btn');
  const panes = document.querySelectorAll('.bottom-panel-content .panel-pane');
  const toggleBtn = document.getElementById('toggle-bottom-panel-btn');
  const bottomPanel = document.getElementById('bottom-panel');
  const clearBtn = document.getElementById('bottom-panel-clear-btn');

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.dataset.tabTarget;
      tabBtns.forEach(b => b.classList.remove('active'));
      panes.forEach(p => p.classList.remove('active'));

      btn.classList.add('active');
      const targetPane = document.getElementById(targetId);
      if (targetPane) targetPane.classList.add('active');

      workspaceState.activeBottomTab = targetId;
      layout();
    });
  });

  if (toggleBtn && bottomPanel) {
    toggleBtn.addEventListener('click', () => {
      toggleBottomPanel();
    });
  }

  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      if (workspaceState.activeBottomTab === 'panel-terminal') {
        const termScreen = document.getElementById('terminal-screen');
        if (termScreen) termScreen.innerHTML = '<div class="term-line term-output">Terminal cleared.</div>';
      } else if (workspaceState.activeBottomTab === 'panel-output') {
        const outScreen = document.getElementById('output-screen');
        if (outScreen) outScreen.innerHTML = '';
      }
    });
  }
}

function toggleBottomPanel() {
  const bottomPanel = document.getElementById('bottom-panel');
  if (!bottomPanel) return;

  workspaceState.bottomPanelOpen = !workspaceState.bottomPanelOpen;
  if (workspaceState.bottomPanelOpen) {
    bottomPanel.style.display = 'flex';
  } else {
    bottomPanel.style.display = 'none';
  }
  layout();
}

function logOutput(message) {
  const outScreen = document.getElementById('output-screen');
  if (!outScreen) return;
  const time = new Date().toLocaleTimeString();
  const line = document.createElement('div');
  line.className = 'term-line';
  line.innerHTML = `<span style="color:var(--color-text-muted);">[${time}]</span> ${escapeHtml(message)}`;
  outScreen.appendChild(line);
  outScreen.scrollTop = outScreen.scrollHeight;
}

/**
 * 9. Command Palette (Ctrl+Shift+P)
 */
function initCommandPalette() {
  const modal = document.getElementById('command-palette-modal');
  const input = document.getElementById('command-palette-input');
  const list = document.getElementById('command-palette-list');
  const triggerBtn = document.getElementById('workspace-command-palette-btn');

  const commands = [
    { title: 'File: Save Active File', shortcut: 'Ctrl+S', action: () => saveActiveFile() },
    { title: 'File: Close Active Tab', shortcut: 'Ctrl+W', action: () => { if (workspaceState.activeFileId) handleCloseTabRequest(workspaceState.activeFileId); } },
    { title: 'File: Close All Tabs', shortcut: '', action: () => closeAllTabs() },
    { title: 'View: Toggle File Explorer', shortcut: 'Ctrl+B', action: () => toggleExplorer() },
    { title: 'View: Toggle Bottom Panel', shortcut: 'Ctrl+`', action: () => toggleBottomPanel() },
    { title: 'View: Quick Open File', shortcut: 'Ctrl+P', action: () => openQuickOpen() },
    { title: 'Editor: Format Document', shortcut: 'Shift+Alt+F', action: () => formatDocument() },
    { title: 'Editor: Preferences', shortcut: '', action: () => openPreferencesModal() },
    { title: 'Theme: Toggle Dark / Light', shortcut: '', action: () => toggleGlobalTheme() }
  ];

  function openCommandPalette() {
    if (!modal || !input) return;
    modal.style.display = 'flex';
    input.value = '';
    renderCommandList(commands);
    setTimeout(() => input.focus(), 50);
  }

  function renderCommandList(items) {
    if (!list) return;
    if (items.length === 0) {
      list.innerHTML = '<div class="palette-empty">No matching commands found.</div>';
      return;
    }

    list.innerHTML = items.map((cmd, i) => `
      <div class="palette-item ${i === 0 ? 'selected' : ''}" data-cmd-index="${i}">
        <span class="palette-item-main">${escapeHtml(cmd.title)}</span>
        ${cmd.shortcut ? `<span class="palette-item-shortcut">${cmd.shortcut}</span>` : ''}
      </div>
    `).join('');

    list.querySelectorAll('.palette-item').forEach((itemEl, idx) => {
      itemEl.addEventListener('click', () => {
        modal.style.display = 'none';
        items[idx].action();
      });
    });
  }

  if (input) {
    input.addEventListener('input', () => {
      const q = input.value.trim().toLowerCase();
      const filtered = commands.filter(c => c.title.toLowerCase().includes(q));
      renderCommandList(filtered);
    });

    input.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        modal.style.display = 'none';
        focusEditor();
      } else if (e.key === 'Enter') {
        const first = list?.querySelector('.palette-item');
        if (first) first.click();
      }
    });
  }

  if (modal) {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        modal.style.display = 'none';
        focusEditor();
      }
    });
  }

  if (triggerBtn) {
    triggerBtn.addEventListener('click', openCommandPalette);
  }

  window.openCommandPalette = openCommandPalette;
}

function closeAllTabs() {
  const ids = Array.from(workspaceState.openFiles.keys());
  for (const id of ids) {
    closeTab(id);
  }
}

function toggleExplorer() {
  const panel = document.getElementById('file-explorer-panel');
  if (panel) {
    panel.classList.toggle('open');
    layout();
  }
}

/**
 * 10. Quick Open (Ctrl+P)
 */
function initQuickOpen() {
  const modal = document.getElementById('quick-open-modal');
  const input = document.getElementById('quick-open-input');
  const list = document.getElementById('quick-open-list');
  const triggerBtn = document.getElementById('workspace-quick-open-btn');

  function openQuickOpen() {
    if (!modal || !input) return;
    modal.style.display = 'flex';
    input.value = '';
    renderQuickOpenList(Array.from(workspaceState.filesMap.values()).filter(f => !f.isDirectory && !f.directory));
    setTimeout(() => input.focus(), 50);
  }

  function renderQuickOpenList(files) {
    if (!list) return;
    if (files.length === 0) {
      list.innerHTML = '<div class="palette-empty">No matching files found in project.</div>';
      return;
    }

    list.innerHTML = files.slice(0, 20).map((file, i) => `
      <div class="palette-item ${i === 0 ? 'selected' : ''}" data-file-id="${file.id}">
        <span class="palette-item-main">
          <span>${getFileIcon(file.name, file.fileType)}</span>
          <strong>${escapeHtml(file.name)}</strong>
          <span class="palette-item-desc">${escapeHtml(file.path || file.name)}</span>
        </span>
      </div>
    `).join('');

    list.querySelectorAll('.palette-item').forEach(itemEl => {
      itemEl.addEventListener('click', () => {
        modal.style.display = 'none';
        openFile(itemEl.dataset.fileId);
      });
    });
  }

  if (input) {
    input.addEventListener('input', () => {
      const q = input.value.trim().toLowerCase();
      const allFiles = Array.from(workspaceState.filesMap.values()).filter(f => !f.isDirectory && !f.directory);
      const filtered = allFiles.filter(f => f.name.toLowerCase().includes(q) || (f.path && f.path.toLowerCase().includes(q)));
      renderQuickOpenList(filtered);
    });

    input.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        modal.style.display = 'none';
        focusEditor();
      } else if (e.key === 'Enter') {
        const first = list?.querySelector('.palette-item');
        if (first) first.click();
      }
    });
  }

  if (modal) {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        modal.style.display = 'none';
        focusEditor();
      }
    });
  }

  if (triggerBtn) {
    triggerBtn.addEventListener('click', openQuickOpen);
  }

  window.openQuickOpen = openQuickOpen;
}

/**
 * 11. Custom File Explorer Context Menu
 */
function initContextMenu() {
  const menu = document.getElementById('file-context-menu');
  const openItem = document.getElementById('ctx-open-file');
  const newFileItem = document.getElementById('ctx-new-file');
  const newFolderItem = document.getElementById('ctx-new-folder');
  const renameItem = document.getElementById('ctx-rename-item');
  const deleteItem = document.getElementById('ctx-delete-item');

  function openContextMenu(x, y, targetId, isDir) {
    workspaceState.contextTargetId = targetId;
    workspaceState.contextTargetIsDir = isDir;

    if (openItem) openItem.style.display = isDir ? 'none' : 'block';
    if (newFileItem) newFileItem.style.display = isDir ? 'block' : 'none';
    if (newFolderItem) newFolderItem.style.display = isDir ? 'block' : 'none';

    // Position menu safely inside screen
    const menuWidth = 180;
    const menuHeight = 160;
    const posX = Math.min(x, window.innerWidth - menuWidth - 10);
    const posY = Math.min(y, window.innerHeight - menuHeight - 10);

    menu.style.left = `${posX}px`;
    menu.style.top = `${posY}px`;
    menu.style.display = 'block';
  }

  // Close context menu on any outside click
  window.addEventListener('click', () => {
    if (menu) menu.style.display = 'none';
  });

  if (openItem) {
    openItem.addEventListener('click', () => {
      if (workspaceState.contextTargetId) {
        openFile(workspaceState.contextTargetId);
      }
    });
  }

  if (newFileItem) {
    newFileItem.addEventListener('click', () => {
      triggerCreateFile(workspaceState.contextTargetId);
    });
  }

  if (newFolderItem) {
    newFolderItem.addEventListener('click', () => {
      triggerCreateFolder(workspaceState.contextTargetId);
    });
  }

  if (renameItem) {
    renameItem.addEventListener('click', () => {
      if (workspaceState.contextTargetId) {
        triggerRenameItem(workspaceState.contextTargetId);
      }
    });
  }

  if (deleteItem) {
    deleteItem.addEventListener('click', () => {
      if (workspaceState.contextTargetId) {
        triggerDeleteItem(workspaceState.contextTargetId, workspaceState.contextTargetIsDir);
      }
    });
  }

  window.openContextMenu = openContextMenu;
}

/**
 * 12. Editor Preferences Modal
 */
function initPreferencesModal() {
  const modal = document.getElementById('editor-settings-modal');
  const triggerBtn = document.getElementById('workspace-editor-settings-btn');
  const closeBtn = document.getElementById('editor-settings-close-btn');
  const cancelBtn = document.getElementById('editor-settings-cancel-btn');
  const saveBtn = document.getElementById('editor-settings-save-btn');

  const fontSelect = document.getElementById('pref-font-size');
  const tabSelect = document.getElementById('pref-tab-size');
  const wrapSelect = document.getElementById('pref-word-wrap');
  const minimapSelect = document.getElementById('pref-minimap');

  function openPreferencesModal() {
    if (!modal) return;
    const prefs = storage.getEditorPreferences();
    if (fontSelect) fontSelect.value = String(prefs.fontSize || 14);
    if (tabSelect) tabSelect.value = String(prefs.tabSize || 4);
    if (wrapSelect) wrapSelect.value = prefs.wordWrap || 'on';
    if (minimapSelect) minimapSelect.value = String(prefs.minimap !== false);
    modal.style.display = 'flex';
  }

  function closePreferencesModal() {
    if (modal) modal.style.display = 'none';
  }

  if (triggerBtn) triggerBtn.addEventListener('click', openPreferencesModal);
  if (closeBtn) closeBtn.addEventListener('click', closePreferencesModal);
  if (cancelBtn) cancelBtn.addEventListener('click', closePreferencesModal);

  if (saveBtn) {
    saveBtn.addEventListener('click', () => {
      const prefs = {
        fontSize: parseInt(fontSelect?.value || '14', 10),
        tabSize: parseInt(tabSelect?.value || '4', 10),
        wordWrap: wrapSelect?.value || 'on',
        minimap: minimapSelect?.value === 'true'
      };

      storage.setEditorPreferences(prefs);
      updatePreferences(prefs);
      closePreferencesModal();
      showToast('Preferences Saved', 'Editor preferences updated', 'success');

      const tabSizeEl = document.getElementById('status-tab-size');
      if (tabSizeEl) tabSizeEl.textContent = `Spaces: ${prefs.tabSize}`;
    });
  }

  window.openPreferencesModal = openPreferencesModal;
}

/**
 * 13. Unsaved Changes Modal
 */
function initUnsavedModal() {
  const modal = document.getElementById('unsaved-confirm-modal');
  const cancelBtn = document.getElementById('unsaved-cancel-btn');
  const discardBtn = document.getElementById('unsaved-discard-btn');
  const saveBtn = document.getElementById('unsaved-save-btn');

  function closeModal() {
    if (modal) modal.style.display = 'none';
    workspaceState.closingTabId = null;
  }

  if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

  if (discardBtn) {
    discardBtn.addEventListener('click', () => {
      const targetId = workspaceState.closingTabId;
      closeModal();
      if (targetId) closeTab(targetId);
    });
  }

  if (saveBtn) {
    saveBtn.addEventListener('click', async () => {
      const targetId = workspaceState.closingTabId;
      closeModal();
      if (targetId) {
        await saveActiveFile();
        closeTab(targetId);
      }
    });
  }

  // Before unload warning if any file is dirty
  window.addEventListener('beforeunload', (e) => {
    const hasDirty = Array.from(workspaceState.openFiles.values()).some(f => f.dirty);
    if (hasDirty) {
      e.preventDefault();
      e.returnValue = 'You have unsaved changes.';
    }
  });
}

/**
 * 14. Theme Synchronization
 */
function initThemeObserver() {
  // Listen to documentElement data-theme attribute modifications
  const observer = new MutationObserver(() => {
    const active = document.documentElement.getAttribute('data-theme') || 'dark';
    setTheme(active === 'light' ? 'light' : 'dark');
  });

  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
}

function toggleGlobalTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'dark';
  const next = current === 'dark' ? 'light' : 'dark';
  storage.setTheme(next);
}

/**
 * 15. Global Keyboard Shortcuts
 */
function initGlobalShortcuts() {
  window.addEventListener('keydown', (e) => {
    // Ctrl+S / Cmd+S: Save Active File
    if ((e.ctrlKey || e.metaKey) && e.key === 's' && !e.shiftKey) {
      e.preventDefault();
      saveActiveFile();
    }
    // Ctrl+P / Cmd+P: Quick Open
    else if ((e.ctrlKey || e.metaKey) && e.key === 'p' && !e.shiftKey) {
      e.preventDefault();
      window.openQuickOpen();
    }
    // Ctrl+Shift+P / Cmd+Shift+P: Command Palette
    else if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'P' || e.key === 'p')) {
      e.preventDefault();
      window.openCommandPalette();
    }
    // Ctrl+B: Toggle Sidebar
    else if ((e.ctrlKey || e.metaKey) && (e.key === 'b' || e.key === 'B')) {
      e.preventDefault();
      toggleExplorer();
    }
    // Ctrl+`: Toggle Bottom Panel
    else if ((e.ctrlKey || e.metaKey) && e.key === '`') {
      e.preventDefault();
      toggleBottomPanel();
    }
    // Escape: Close open modals
    else if (e.key === 'Escape') {
      document.getElementById('quick-open-modal')?.setAttribute('style', 'display: none;');
      document.getElementById('command-palette-modal')?.setAttribute('style', 'display: none;');
      document.getElementById('editor-settings-modal')?.setAttribute('style', 'display: none;');
      document.getElementById('unsaved-confirm-modal')?.setAttribute('style', 'display: none;');
      document.getElementById('file-context-menu')?.setAttribute('style', 'display: none;');
    }
  });

  const saveBtn = document.getElementById('workspace-save-btn');
  if (saveBtn) {
    saveBtn.addEventListener('click', () => saveActiveFile());
  }

  const toggleExplorerBtn = document.getElementById('toggle-explorer-btn');
  if (toggleExplorerBtn) {
    toggleExplorerBtn.addEventListener('click', toggleExplorer);
  }
}

/**
 * 16. Terminal Simulation Placeholder
 */
function initTerminal() {
  const terminalInput = document.getElementById('terminal-command-input');
  const terminalScreen = document.getElementById('terminal-screen');

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

        if (cmd === 'help') {
          appendTerminalLine('DevPilot Sandbox Console v1.0.0 — Available safe commands: help, clear, status', 'term-output');
        } else if (cmd === 'status') {
          appendTerminalLine('[Sandbox Status] Memory: 142MB / 512MB | Network: Sandbox isolated', 'term-success');
        } else {
          appendTerminalLine(`[Note] Terminal execution will be available in a future phase. Command recorded: ${escapeHtml(cmd)}`, 'term-warning');
        }
      }
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
 * 17. Live Preview
 */
function initPreview() {
  const refreshBtn = document.getElementById('preview-refresh-btn');
  const previewFrame = document.getElementById('preview-iframe');

  if (refreshBtn && previewFrame) {
    refreshBtn.addEventListener('click', () => {
      showToast('Preview Refreshed', 'Reloading virtual sandbox preview...', 'info');
      let indexHtml = '';
      for (const file of workspaceState.filesMap.values()) {
        if (file.name === 'index.html' && file.content) {
          indexHtml = file.content;
          break;
        }
      }
      previewFrame.srcdoc = indexHtml || '<h2 style="font-family:sans-serif; text-align:center; padding-top:2rem; color:#94a3b8;">DevPilot Live Sandbox Preview Ready</h2>';
    });
  }
}

/**
 * 18. AI Assistant Drawer & Integration
 */
function initAIAssistant() {
  const toggleAiBtn = document.getElementById('toggle-ai-btn');
  const aiPanel = document.getElementById('ai-sidebar-panel');

  if (toggleAiBtn && aiPanel) {
    toggleAiBtn.addEventListener('click', () => {
      aiPanel.classList.toggle('open');
      layout();
    });
  }

  // Connect AI module to Monaco workspace bridge
  initAI({
    getActiveFile: () => {
      if (!workspaceState.activeFileId) return null;
      return workspaceState.openFiles.get(workspaceState.activeFileId) || null;
    },
    getSelectedCode: () => {
      return getSelectedText();
    },
    replaceSelection: (replacement) => {
      return replaceSelectedText(replacement);
    },
    setDirty: (dirty) => {
      if (workspaceState.activeFileId) {
        const fileTab = workspaceState.openFiles.get(workspaceState.activeFileId);
        if (fileTab) {
          fileTab.dirty = dirty;
          renderTabs();
          updateSaveState(dirty ? 'unsaved' : 'saved');
        }
      }
    },
    getProjectId: () => {
      return workspaceState.projectId;
    },
    openFile: (fileId) => {
      openFile(fileId);
    },
    refreshExplorer: () => {
      loadTree();
    }
  });
}

