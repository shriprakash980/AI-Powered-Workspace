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
  replaceSelectedText,
  showDiff,
  hideDiff,
  toggleDiffSideBySide,
  isDiffActive
} from './editor.js';
import { initAI, updateContextBadge } from './ai.js';
import { gitClient } from './git.js';
import { gitHubClient } from './github.js';
import { buildClient } from './build.js';
import { deploymentClient } from './deployment.js';
import { cicdClient } from './cicd.js';

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
  closingTabId: null,
  gitStatus: null,
  gitStatusMap: new Map(),
  githubRemoteOwnerRepo: null,
  activeDeployment: null
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
  initSourceControl();
  initGitHubIntegration();
  initBuildAndDeploy();
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

  // Load Git & GitHub status
  loadGitStatus();
  loadGitHubStatus();
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
      fileDiv.dataset.path = node.path || node.name;
      fileDiv.setAttribute('title', node.path || node.name);

      let gitBadgeHtml = '';
      if (workspaceState.gitStatusMap && workspaceState.gitStatusMap.has(node.path)) {
        const info = workspaceState.gitStatusMap.get(node.path);
        gitBadgeHtml = `<span class="git-status-badge badge-${info.badgeClass}" title="${info.title}">${info.letter}</span>`;
      }

      const fileIcon = getFileIcon(node.name, node.fileType);
      fileDiv.innerHTML = `
        <span>${fileIcon}</span>
        <span style="flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${escapeHtml(node.name)}</span>
        ${gitBadgeHtml}
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
    loadGitStatus();

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
      if (workspaceState.projectId) loadWorkspaceData(workspaceState.projectId);
    }
  });
}

function openModal(id) {
  const el = document.getElementById(id);
  if (el) el.style.display = 'flex';
}

function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.style.display = 'none';
}

/**
 * 19. Source Control (Git) Integration
 */
function initSourceControl() {
  const actExplorer = document.getElementById('act-btn-explorer');
  const actGit = document.getElementById('act-btn-git');
  const actGitHub = document.getElementById('act-btn-github');
  const actBuildDeploy = document.getElementById('act-btn-build-deploy');
  const actCicd = document.getElementById('act-btn-cicd');

  const panelExplorer = document.getElementById('file-explorer-panel');
  const panelGit = document.getElementById('source-control-panel');
  const panelGitHub = document.getElementById('github-panel');
  const panelBuildDeploy = document.getElementById('build-deploy-panel');
  const panelCicd = document.getElementById('cicd-panel');

  function switchSidebarPanel(targetBtn, targetPanel) {
    [actExplorer, actGit, actGitHub, actBuildDeploy, actCicd].forEach(b => b?.classList.remove('active'));
    [panelExplorer, panelGit, panelGitHub, panelBuildDeploy, panelCicd].forEach(p => { if (p) p.style.display = 'none'; });

    targetBtn?.classList.add('active');
    if (targetPanel) targetPanel.style.display = 'flex';
    layout();
  }

  if (actExplorer) actExplorer.addEventListener('click', () => switchSidebarPanel(actExplorer, panelExplorer));
  if (actGit) actGit.addEventListener('click', () => { switchSidebarPanel(actGit, panelGit); loadGitStatus(); });
  if (actGitHub) actGitHub.addEventListener('click', () => { switchSidebarPanel(actGitHub, panelGitHub); loadGitHubStatus(); });
  if (actBuildDeploy) actBuildDeploy.addEventListener('click', () => { switchSidebarPanel(actBuildDeploy, panelBuildDeploy); loadBuildAndDeployStatus(); });
  if (actCicd) actCicd.addEventListener('click', () => { switchSidebarPanel(actCicd, panelCicd); loadCicdPipelines(); });

  document.getElementById('git-refresh-btn')?.addEventListener('click', () => loadGitStatus());

  const initAction = async () => {
    try {
      showToast('Initializing Git', 'Creating Git repository in workspace...', 'info');
      await gitClient.initRepository(workspaceState.projectId);
      showToast('Git Repository Initialized', 'Git is ready for commits.', 'success');
      loadGitStatus();
    } catch (err) {
      showToast('Git Init Failed', err.message || 'Failed to initialize Git', 'danger');
    }
  };
  document.getElementById('git-init-btn')?.addEventListener('click', initAction);
  document.getElementById('git-init-action-btn')?.addEventListener('click', initAction);

  document.getElementById('git-stage-all-btn')?.addEventListener('click', async () => {
    try {
      await gitClient.stageAll(workspaceState.projectId);
      loadGitStatus();
    } catch (err) {
      showToast('Stage All Failed', err.message, 'danger');
    }
  });

  document.getElementById('git-unstage-all-btn')?.addEventListener('click', async () => {
    try {
      await gitClient.unstageAll(workspaceState.projectId);
      loadGitStatus();
    } catch (err) {
      showToast('Unstage All Failed', err.message, 'danger');
    }
  });

  const commitMsgInput = document.getElementById('git-commit-msg-input');
  const commitBtn = document.getElementById('git-commit-btn');
  const handleCommit = async () => {
    const msg = commitMsgInput?.value.trim();
    if (!msg) {
      showToast('Commit Error', 'Please enter a commit message.', 'warning');
      commitMsgInput?.focus();
      return;
    }
    try {
      await gitClient.commit(workspaceState.projectId, msg);
      if (commitMsgInput) commitMsgInput.value = '';
      showToast('Committed Successfully', `Created commit: "${msg}"`, 'success');
      logOutput(`[Git Commit] ${msg}`);
      loadGitStatus();
    } catch (err) {
      showToast('Commit Failed', err.message || 'Failed to commit staged changes', 'danger');
    }
  };

  commitBtn?.addEventListener('click', handleCommit);
  commitMsgInput?.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      handleCommit();
    }
  });

  document.getElementById('git-ai-msg-btn')?.addEventListener('click', async () => {
    try {
      showToast('AI Generating', 'Analyzing staged diff to draft commit message...', 'info');
      const res = await gitClient.generateAiCommitMessage(workspaceState.projectId);
      if (res && res.data && res.data.message) {
        if (commitMsgInput) commitMsgInput.value = res.data.message;
        showToast('AI Commit Message', 'Drafted commit message from staged changes.', 'success');
      } else {
        showToast('AI Message', 'No staged changes or message generated.', 'warning');
      }
    } catch (err) {
      showToast('AI Generation Failed', err.message, 'danger');
    }
  });

  document.getElementById('git-switch-branch-btn')?.addEventListener('click', () => openSwitchBranchModal());
  document.getElementById('status-git-branch-btn')?.addEventListener('click', () => openSwitchBranchModal());

  document.getElementById('switch-branch-close-btn')?.addEventListener('click', () => closeModal('switch-branch-modal'));
  document.getElementById('switch-branch-cancel-btn')?.addEventListener('click', () => closeModal('switch-branch-modal'));
  document.getElementById('open-create-branch-btn')?.addEventListener('click', () => {
    closeModal('switch-branch-modal');
    openModal('create-branch-modal');
  });

  document.getElementById('create-branch-close-btn')?.addEventListener('click', () => closeModal('create-branch-modal'));
  document.getElementById('create-branch-cancel-btn')?.addEventListener('click', () => closeModal('create-branch-modal'));
  document.getElementById('create-branch-submit-btn')?.addEventListener('click', async () => {
    const input = document.getElementById('new-branch-name-input');
    const name = input?.value.trim();
    if (!name) {
      showToast('Branch Name Required', 'Please enter a valid branch name', 'warning');
      input?.focus();
      return;
    }
    try {
      await gitClient.createBranch(workspaceState.projectId, name);
      await gitClient.checkoutBranch(workspaceState.projectId, name);
      closeModal('create-branch-modal');
      if (input) input.value = '';
      showToast('Branch Created', `Switched to branch '${name}'`, 'success');
      loadGitStatus();
      loadWorkspaceData(workspaceState.projectId);
    } catch (err) {
      showToast('Branch Creation Failed', err.message, 'danger');
    }
  });

  document.getElementById('git-pull-btn')?.addEventListener('click', async () => {
    try {
      showToast('Pulling', 'Pulling changes from remote...', 'info');
      const res = await gitClient.pull(workspaceState.projectId);
      showToast('Pull Complete', res.data?.message || 'Pulled remote changes successfully.', 'success');
      loadGitStatus();
      loadWorkspaceData(workspaceState.projectId);
    } catch (err) {
      showToast('Pull Failed', err.message || 'Error pulling remote changes', 'danger');
    }
  });

  document.getElementById('git-push-btn')?.addEventListener('click', async () => {
    try {
      showToast('Pushing', 'Pushing commits to remote...', 'info');
      const res = await gitClient.push(workspaceState.projectId);
      showToast('Push Complete', res.data?.message || 'Pushed commits to remote.', 'success');
      loadGitStatus();
    } catch (err) {
      showToast('Push Failed', err.message || 'Error pushing commits to remote', 'danger');
    }
  });

  document.getElementById('diff-toggle-mode-btn')?.addEventListener('click', () => toggleDiffSideBySide());
  document.getElementById('diff-exit-btn')?.addEventListener('click', () => hideDiff());
}

async function loadGitStatus() {
  if (!workspaceState.projectId) return;

  try {
    const res = await gitClient.getStatus(workspaceState.projectId);
    const status = res.data;
    workspaceState.gitStatus = status;

    const uninitBox = document.getElementById('git-uninitialized-box');
    const activeUi = document.getElementById('git-active-workspace-ui');
    const initBtn = document.getElementById('git-init-btn');

    if (!status.gitInitialized) {
      if (uninitBox) uninitBox.style.display = 'block';
      if (activeUi) activeUi.style.display = 'none';
      if (initBtn) initBtn.style.display = 'block';
      document.getElementById('status-git-branch-btn')?.setAttribute('style', 'display:none;');
      return;
    }

    if (uninitBox) uninitBox.style.display = 'none';
    if (activeUi) activeUi.style.display = 'flex';
    if (initBtn) initBtn.style.display = 'none';

    const currentBranch = status.currentBranch || 'main';
    const branchLabel = document.getElementById('git-current-branch-label');
    const statusBranchName = document.getElementById('status-git-branch-name');
    const statusBranchBtn = document.getElementById('status-git-branch-btn');

    if (branchLabel) branchLabel.textContent = currentBranch;
    if (statusBranchName) statusBranchName.textContent = currentBranch;
    if (statusBranchBtn) statusBranchBtn.style.display = 'inline-flex';

    const stagedCount = status.staged ? status.staged.length : 0;
    const changesCount = (status.modified ? status.modified.length : 0) +
                         (status.untracked ? status.untracked.length : 0) +
                         (status.missing ? status.missing.length : 0);
    const totalCount = stagedCount + changesCount;

    const badgeEl = document.getElementById('git-changes-badge');
    if (badgeEl) {
      badgeEl.textContent = totalCount;
      badgeEl.style.display = totalCount > 0 ? 'inline-block' : 'none';
    }

    const stagedCountEl = document.getElementById('git-staged-count');
    const changesCountEl = document.getElementById('git-changes-count');
    if (stagedCountEl) stagedCountEl.textContent = stagedCount;
    if (changesCountEl) changesCountEl.textContent = changesCount;

    const conflictBanner = document.getElementById('git-conflict-banner');
    if (conflictBanner) conflictBanner.style.display = status.hasConflicts ? 'block' : 'none';

    const statusMap = new Map();
    if (status.staged) {
      status.staged.forEach(f => statusMap.set(f.path, { letter: f.status?.charAt(0).toUpperCase() || 'S', badgeClass: 'staged', title: `Staged (${f.status})` }));
    }
    if (status.modified) {
      status.modified.forEach(f => statusMap.set(f.path, { letter: 'M', badgeClass: 'modified', title: 'Modified' }));
    }
    if (status.untracked) {
      status.untracked.forEach(f => statusMap.set(f.path, { letter: 'U', badgeClass: 'untracked', title: 'Untracked' }));
    }
    if (status.missing) {
      status.missing.forEach(f => statusMap.set(f.path, { letter: 'D', badgeClass: 'deleted', title: 'Deleted' }));
    }
    workspaceState.gitStatusMap = statusMap;

    renderStagedList(status.staged || []);

    const changesList = [
      ...(status.modified || []).map(f => ({ ...f, changeType: 'M' })),
      ...(status.untracked || []).map(f => ({ ...f, changeType: 'U' })),
      ...(status.missing || []).map(f => ({ ...f, changeType: 'D' }))
    ];
    renderChangesList(changesList);

    updateExplorerGitBadges();

  } catch (err) {
    console.warn('[Git Status Error]', err);
  }
}

function renderStagedList(stagedFiles) {
  const container = document.getElementById('git-staged-list');
  if (!container) return;

  if (stagedFiles.length === 0) {
    container.innerHTML = '<div style="font-size:0.75rem; color:var(--color-text-muted); padding:4px 8px;">No staged changes.</div>';
    return;
  }

  container.innerHTML = stagedFiles.map(f => `
    <div class="git-file-item" data-path="${escapeHtml(f.path)}" data-staged="true">
      <span class="git-file-icon">📄</span>
      <span class="git-file-name" title="${escapeHtml(f.path)}">${escapeHtml(f.path)}</span>
      <span class="git-file-status badge-staged">${escapeHtml(f.status?.charAt(0).toUpperCase() || 'S')}</span>
      <button class="git-file-action" data-action="unstage" data-path="${escapeHtml(f.path)}" title="Unstage File">-</button>
    </div>
  `).join('');

  container.querySelectorAll('.git-file-item').forEach(item => {
    item.addEventListener('click', async (e) => {
      if (e.target.dataset.action === 'unstage') {
        e.stopPropagation();
        try {
          await gitClient.unstage(workspaceState.projectId, [item.dataset.path]);
          loadGitStatus();
        } catch (err) {
          showToast('Unstage Error', err.message, 'danger');
        }
        return;
      }
      openDiffView(item.dataset.path, true);
    });
  });
}

function renderChangesList(changesFiles) {
  const container = document.getElementById('git-changes-list');
  if (!container) return;

  if (changesFiles.length === 0) {
    container.innerHTML = '<div style="font-size:0.75rem; color:var(--color-text-muted); padding:4px 8px;">Working tree clean.</div>';
    return;
  }

  container.innerHTML = changesFiles.map(f => `
    <div class="git-file-item" data-path="${escapeHtml(f.path)}" data-staged="false">
      <span class="git-file-icon">📄</span>
      <span class="git-file-name" title="${escapeHtml(f.path)}">${escapeHtml(f.path)}</span>
      <span class="git-file-status badge-${f.changeType === 'M' ? 'modified' : (f.changeType === 'U' ? 'untracked' : 'deleted')}">${f.changeType}</span>
      <button class="git-file-action" data-action="stage" data-path="${escapeHtml(f.path)}" title="Stage File">+</button>
    </div>
  `).join('');

  container.querySelectorAll('.git-file-item').forEach(item => {
    item.addEventListener('click', async (e) => {
      if (e.target.dataset.action === 'stage') {
        e.stopPropagation();
        try {
          await gitClient.stage(workspaceState.projectId, [item.dataset.path]);
          loadGitStatus();
        } catch (err) {
          showToast('Stage Error', err.message, 'danger');
        }
        return;
      }
      openDiffView(item.dataset.path, false);
    });
  });
}

async function openDiffView(filePath, isStaged) {
  try {
    const diffRes = await gitClient.getDiff(workspaceState.projectId, { path: filePath, staged: isStaged });
    const diffData = diffRes.data;

    const originalText = diffData ? diffData.originalContent || '' : '';
    const modifiedText = diffData ? diffData.modifiedContent || '' : '';

    const bannerFilename = document.getElementById('diff-banner-filename');
    if (bannerFilename) bannerFilename.textContent = `${filePath} (${isStaged ? 'Staged' : 'Working Tree'})`;

    showDiff(originalText, modifiedText, detectLanguage(filePath), filePath);
  } catch (err) {
    showToast('Diff Error', err.message || 'Could not load diff', 'danger');
  }
}

function updateExplorerGitBadges() {
  document.querySelectorAll('#explorer-tree .tree-file').forEach(fileNode => {
    const path = fileNode.getAttribute('title') || fileNode.dataset.path;
    const existingBadge = fileNode.querySelector('.git-status-badge');
    if (existingBadge) existingBadge.remove();

    if (path && workspaceState.gitStatusMap && workspaceState.gitStatusMap.has(path)) {
      const info = workspaceState.gitStatusMap.get(path);
      const span = document.createElement('span');
      span.className = `git-status-badge badge-${info.badgeClass}`;
      span.title = info.title;
      span.textContent = info.letter;
      fileNode.appendChild(span);
    }
  });
}

async function openSwitchBranchModal() {
  const container = document.getElementById('branches-list-container');
  openModal('switch-branch-modal');

  if (container) {
    container.innerHTML = '<div style="font-size:0.8rem; color:var(--color-text-muted); text-align:center; padding:12px;">Loading branches...</div>';
  }

  try {
    const res = await gitClient.getBranches(workspaceState.projectId);
    const branches = res.data || [];

    if (!container) return;

    if (branches.length === 0) {
      container.innerHTML = '<div style="font-size:0.8rem; color:var(--color-text-muted); text-align:center;">No branches found.</div>';
      return;
    }

    container.innerHTML = branches.map(b => `
      <div class="git-branch-item ${b.current ? 'active' : ''}" data-branch="${escapeHtml(b.name)}" style="display:flex; align-items:center; justify-content:space-between; padding:8px 10px; border-radius:var(--radius-sm); border:1px solid var(--color-border); background:var(--color-bg-primary); cursor:pointer;">
        <div style="display:flex; align-items:center; gap:8px;">
          <span>${b.current ? '⭐' : '🌿'}</span>
          <strong style="font-size:0.85rem; color:${b.current ? 'var(--color-primary)' : 'var(--color-text-primary)'};">${escapeHtml(b.name)}</strong>
        </div>
        ${b.current ? '<span class="badge badge-success">CURRENT</span>' : '<span class="text-xs text-muted">Checkout &rarr;</span>'}
      </div>
    `).join('');

    container.querySelectorAll('.git-branch-item').forEach(item => {
      item.addEventListener('click', async () => {
        const targetBranch = item.dataset.branch;
        if (targetBranch === workspaceState.gitStatus?.currentBranch) {
          closeModal('switch-branch-modal');
          return;
        }
        try {
          await gitClient.checkoutBranch(workspaceState.projectId, targetBranch);
          closeModal('switch-branch-modal');
          showToast('Branch Switch', `Switched to branch '${targetBranch}'`, 'success');
          loadGitStatus();
          loadWorkspaceData(workspaceState.projectId);
        } catch (err) {
          showToast('Checkout Failed', err.message || 'Uncommitted changes conflict', 'danger');
        }
      });
    });

  } catch (err) {
    if (container) container.innerHTML = `<div style="font-size:0.8rem; color:var(--color-danger); text-align:center;">Failed to load branches: ${escapeHtml(err.message)}</div>`;
  }
}

/**
 * 20. GitHub Integration
 */
function initGitHubIntegration() {
  document.getElementById('github-refresh-btn')?.addEventListener('click', () => loadGitHubStatus());

  document.getElementById('github-connect-btn')?.addEventListener('click', async () => {
    try {
      const res = await gitHubClient.startOAuth();
      if (res && res.data && res.data.authorizationUrl) {
        window.location.href = res.data.authorizationUrl;
      } else {
        showToast('OAuth Error', 'Could not obtain GitHub authorization URL', 'danger');
      }
    } catch (err) {
      showToast('GitHub Connect Error', err.message, 'danger');
    }
  });

  document.getElementById('github-disconnect-btn')?.addEventListener('click', async () => {
    try {
      await gitHubClient.disconnect();
      showToast('Disconnected', 'GitHub account unlinked successfully.', 'info');
      loadGitHubStatus();
    } catch (err) {
      showToast('Disconnect Failed', err.message, 'danger');
    }
  });

  document.getElementById('github-add-remote-btn')?.addEventListener('click', () => openModal('add-remote-modal'));
  document.getElementById('add-remote-close-btn')?.addEventListener('click', () => closeModal('add-remote-modal'));
  document.getElementById('add-remote-cancel-btn')?.addEventListener('click', () => closeModal('add-remote-modal'));

  document.getElementById('add-remote-submit-btn')?.addEventListener('click', async () => {
    const nameInput = document.getElementById('remote-name-input');
    const urlInput = document.getElementById('remote-url-input');
    const name = nameInput?.value.trim() || 'origin';
    const url = urlInput?.value.trim();

    if (!url) {
      showToast('URL Required', 'Please enter a valid Git repository URL.', 'warning');
      urlInput?.focus();
      return;
    }

    try {
      await gitClient.addRemote(workspaceState.projectId, name, url);
      closeModal('add-remote-modal');
      showToast('Remote Configured', `Remote '${name}' set to ${url}`, 'success');
      loadGitHubStatus();
    } catch (err) {
      showToast('Add Remote Failed', err.message, 'danger');
    }
  });

  document.getElementById('github-new-pr-btn')?.addEventListener('click', () => openCreatePrModal());
  document.getElementById('create-pr-close-btn')?.addEventListener('click', () => closeModal('create-pr-modal'));
  document.getElementById('create-pr-cancel-btn')?.addEventListener('click', () => closeModal('create-pr-modal'));

  document.getElementById('create-pr-submit-btn')?.addEventListener('click', async () => {
    const titleInput = document.getElementById('pr-title-input');
    const bodyInput = document.getElementById('pr-body-input');
    const headSelect = document.getElementById('pr-head-select');
    const baseInput = document.getElementById('pr-base-input');

    const title = titleInput?.value.trim();
    const head = headSelect?.value || workspaceState.gitStatus?.currentBranch || 'main';
    const base = baseInput?.value.trim() || 'main';

    if (!title) {
      showToast('PR Title Required', 'Please enter a title for the Pull Request.', 'warning');
      titleInput?.focus();
      return;
    }

    if (!workspaceState.githubRemoteOwnerRepo) {
      showToast('No GitHub Remote', 'Configure a GitHub remote before opening pull requests.', 'warning');
      return;
    }

    const { owner, repo } = workspaceState.githubRemoteOwnerRepo;

    try {
      await gitHubClient.createPullRequest(owner, repo, { title, body: bodyInput?.value.trim(), head, base });
      closeModal('create-pr-modal');
      showToast('PR Created', `Pull request '${title}' opened on GitHub!`, 'success');
      loadPullRequests(owner, repo, 'open');
    } catch (err) {
      showToast('PR Creation Failed', err.message, 'danger');
    }
  });

  document.getElementById('pr-filter-open')?.addEventListener('click', () => {
    document.getElementById('pr-filter-open')?.classList.add('active');
    document.getElementById('pr-filter-closed')?.classList.remove('active');
    if (workspaceState.githubRemoteOwnerRepo) {
      loadPullRequests(workspaceState.githubRemoteOwnerRepo.owner, workspaceState.githubRemoteOwnerRepo.repo, 'open');
    }
  });

  document.getElementById('pr-filter-closed')?.addEventListener('click', () => {
    document.getElementById('pr-filter-closed')?.classList.add('active');
    document.getElementById('pr-filter-open')?.classList.remove('active');
    if (workspaceState.githubRemoteOwnerRepo) {
      loadPullRequests(workspaceState.githubRemoteOwnerRepo.owner, workspaceState.githubRemoteOwnerRepo.repo, 'closed');
    }
  });

  document.getElementById('pr-detail-close-btn')?.addEventListener('click', () => closeModal('pr-details-modal'));
  document.getElementById('pr-detail-ok-btn')?.addEventListener('click', () => closeModal('pr-details-modal'));
}

async function loadGitHubStatus() {
  const disconnectedBox = document.getElementById('github-disconnected-box');
  const connectedBox = document.getElementById('github-connected-box');

  try {
    const res = await gitHubClient.getStatus();
    const status = res.data;

    if (!status.connected) {
      if (disconnectedBox) disconnectedBox.style.display = 'block';
      if (connectedBox) connectedBox.style.display = 'none';
      return;
    }

    if (disconnectedBox) disconnectedBox.style.display = 'none';
    if (connectedBox) connectedBox.style.display = 'flex';

    const usernameLabel = document.getElementById('github-username-label');
    if (usernameLabel) usernameLabel.textContent = `@${status.username}`;

    if (workspaceState.projectId) {
      const remotesRes = await gitClient.getRemotes(workspaceState.projectId);
      const remotes = remotesRes.data || [];

      const remoteInfo = document.getElementById('github-remote-info');
      const origin = remotes.find(r => r.name === 'origin') || remotes[0];

      if (origin) {
        if (remoteInfo) remoteInfo.textContent = `${origin.name}: ${origin.repositoryUrl}`;

        const match = origin.repositoryUrl.match(/github\.com[:/]([^/]+)\/([^/.]+)/);
        if (match) {
          const owner = match[1];
          const repo = match[2].replace(/\.git$/, '');
          workspaceState.githubRemoteOwnerRepo = { owner, repo };
          loadPullRequests(owner, repo, 'open');
        }
      } else {
        if (remoteInfo) remoteInfo.textContent = 'No remote configured.';
        workspaceState.githubRemoteOwnerRepo = null;
      }
    }

  } catch (err) {
    console.warn('[GitHub Status Error]', err);
  }
}

async function loadPullRequests(owner, repo, state = 'open') {
  const listContainer = document.getElementById('github-pr-list');
  if (!listContainer) return;

  listContainer.innerHTML = '<div style="font-size:0.75rem; color:var(--color-text-muted); padding:6px 0;">Loading PRs...</div>';

  try {
    const res = await gitHubClient.getPullRequests(owner, repo, state);
    const prs = res.data || [];

    if (prs.length === 0) {
      listContainer.innerHTML = `<div style="font-size:0.75rem; color:var(--color-text-muted); padding:6px 0;">No ${state} pull requests.</div>`;
      return;
    }

    listContainer.innerHTML = prs.map(pr => `
      <div class="git-file-item" data-pr-number="${pr.number}" style="cursor:pointer; flex-direction:column; align-items:flex-start; gap:2px; padding:6px 8px;">
        <div class="flex items-center justify-between" style="width:100%;">
          <strong style="font-size:0.8rem; color:var(--color-text-primary);">#${pr.number} ${escapeHtml(pr.title)}</strong>
          <span class="badge ${pr.state === 'open' ? 'badge-success' : 'badge-neutral'}">${pr.state}</span>
        </div>
        <div style="font-size:0.7rem; color:var(--color-text-muted);">
          ${escapeHtml(pr.headRef)} &rarr; ${escapeHtml(pr.baseRef)} by @${escapeHtml(pr.userLogin)}
        </div>
      </div>
    `).join('');

    listContainer.querySelectorAll('.git-file-item').forEach((item, idx) => {
      item.addEventListener('click', () => {
        openPrDetailsModal(prs[idx]);
      });
    });

  } catch (err) {
    if (listContainer) listContainer.innerHTML = `<div style="font-size:0.75rem; color:var(--color-danger); padding:6px 0;">Error loading PRs: ${escapeHtml(err.message)}</div>`;
  }
}

function openCreatePrModal() {
  const headSelect = document.getElementById('pr-head-select');
  openModal('create-pr-modal');

  if (headSelect && workspaceState.gitStatus) {
    gitClient.getBranches(workspaceState.projectId).then(res => {
      const branches = res.data || [];
      headSelect.innerHTML = branches.map(b => `<option value="${escapeHtml(b.name)}" ${b.current ? 'selected' : ''}>${escapeHtml(b.name)}</option>`).join('');
    }).catch(() => {
      headSelect.innerHTML = `<option value="${escapeHtml(workspaceState.gitStatus?.currentBranch || 'main')}">${escapeHtml(workspaceState.gitStatus?.currentBranch || 'main')}</option>`;
    });
  }
}

function openPrDetailsModal(pr) {
  const titleEl = document.getElementById('pr-detail-title');
  const stateEl = document.getElementById('pr-detail-state');
  const authorEl = document.getElementById('pr-detail-author');
  const branchEl = document.getElementById('pr-detail-branches');
  const bodyEl = document.getElementById('pr-detail-body');
  const linkEl = document.getElementById('pr-detail-github-link');

  if (titleEl) titleEl.textContent = `#${pr.number} ${pr.title}`;
  if (stateEl) {
    stateEl.textContent = pr.state.toUpperCase();
    stateEl.className = `badge ${pr.state === 'open' ? 'badge-success' : 'badge-neutral'}`;
  }
  if (authorEl) authorEl.textContent = `by @${pr.userLogin}`;
  if (branchEl) branchEl.textContent = `${pr.headRef} -> ${pr.baseRef}`;
  if (bodyEl) bodyEl.textContent = pr.body || 'No description provided.';
  if (linkEl) linkEl.href = pr.htmlUrl || '#';

  openModal('pr-details-modal');
}

/**
 * 21. Build System & Deployment Platform Integration (Phase 12)
 */
function initBuildAndDeploy() {
  document.getElementById('build-refresh-btn')?.addEventListener('click', () => loadBuildAndDeployStatus());

  document.getElementById('trigger-build-btn')?.addEventListener('click', () => triggerBuildRun('BUILD'));
  document.getElementById('trigger-test-btn')?.addEventListener('click', () => triggerBuildRun('TEST'));
  document.getElementById('trigger-build-test-btn')?.addEventListener('click', () => triggerBuildRun('BUILD_AND_TEST'));

  document.getElementById('trigger-deploy-btn')?.addEventListener('click', () => triggerDeploymentRun());
  document.getElementById('trigger-stop-deploy-btn')?.addEventListener('click', () => stopDeploymentRun());
  document.getElementById('trigger-restart-deploy-btn')?.addEventListener('click', () => restartDeploymentRun());
  document.getElementById('trigger-rollback-deploy-btn')?.addEventListener('click', () => rollbackDeploymentRun());

  document.getElementById('open-env-vars-btn')?.addEventListener('click', () => {
    openModal('env-vars-modal');
    loadEnvironmentVariables();
  });
  document.getElementById('env-vars-close-btn')?.addEventListener('click', () => closeModal('env-vars-modal'));
  document.getElementById('env-vars-ok-btn')?.addEventListener('click', () => closeModal('env-vars-modal'));

  document.getElementById('save-env-var-btn')?.addEventListener('click', async () => {
    const nameInput = document.getElementById('new-env-var-name');
    const valInput = document.getElementById('new-env-var-val');
    const envSelect = document.getElementById('new-env-var-env');

    const name = nameInput?.value.trim();
    const value = valInput?.value.trim();
    const environment = envSelect?.value || 'DEVELOPMENT';

    if (!name || !value) {
      showToast('Validation Error', 'Key Name and Secret Value are required.', 'warning');
      return;
    }

    try {
      await deploymentClient.saveEnvironmentVariable(workspaceState.projectId, name, value, environment);
      if (nameInput) nameInput.value = '';
      if (valInput) valInput.value = '';
      showToast('Variable Saved', `Saved environment variable '${name}'`, 'success');
      loadEnvironmentVariables();
    } catch (err) {
      showToast('Save Error', err.message || 'Failed to save environment variable', 'danger');
    }
  });
}

async function loadBuildAndDeployStatus() {
  if (!workspaceState.projectId) return;

  // 1. Detect project type
  try {
    const detectRes = await buildClient.detectProject(workspaceState.projectId);
    const detection = detectRes.data;
    const typeLabel = document.getElementById('build-detected-type-label');
    const cmdLabel = document.getElementById('build-suggested-cmd-label');

    if (detection) {
      if (typeLabel) typeLabel.textContent = `${detection.detectedType || 'UNKNOWN'} (${detection.confidenceScore || 0}% match)`;
      if (cmdLabel) cmdLabel.textContent = `Build: ${detection.suggestedBuildCommand || '--'} | Test: ${detection.suggestedTestCommand || '--'}`;
    }
  } catch (err) {
    console.warn('[Build Detection Error]', err);
  }

  // 2. Fetch recent builds history
  try {
    const buildsRes = await buildClient.getBuilds(workspaceState.projectId);
    const builds = buildsRes.data || [];
    renderBuildHistory(builds);
  } catch (err) {
    console.warn('[Build History Error]', err);
  }

  // 3. Fetch deployment status
  try {
    const deployRes = await deploymentClient.getDeployments(workspaceState.projectId);
    const deployments = deployRes.data || [];
    renderDeploymentStatus(deployments);
  } catch (err) {
    console.warn('[Deployment Status Error]', err);
  }
}

async function triggerBuildRun(mode) {
  if (!workspaceState.projectId) return;
  try {
    showToast('Build Triggered', `Executing project ${mode}...`, 'info');
    logOutput(`[Build] Triggering ${mode} task...`);
    const res = await buildClient.triggerBuild(workspaceState.projectId, mode);
    showToast('Build Completed', `Build #${res.data?.id || ''} finished with status: ${res.data?.status || 'UNKNOWN'}`, res.data?.status === 'SUCCESS' ? 'success' : 'danger');
    logOutput(`[Build Status] Build #${res.data?.id} -> ${res.data?.status}`);
    loadBuildAndDeployStatus();
  } catch (err) {
    showToast('Build Error', err.message || 'Failed to trigger build', 'danger');
    logOutput(`[Build Error] ${err.message}`);
  }
}

async function triggerDeploymentRun() {
  if (!workspaceState.projectId) return;
  try {
    showToast('Deploying Application', 'Building container and starting local Docker deployment...', 'info');
    logOutput('[Deployment] Starting deployment process...');
    const res = await deploymentClient.createDeployment(workspaceState.projectId, 'DEVELOPMENT');
    const dep = res.data;
    if (dep && dep.status === 'RUNNING') {
      showToast('Deployment Live', `App running at ${dep.deploymentUrl}`, 'success');
      logOutput(`[Deployment Live] URL: ${dep.deploymentUrl}`);
    } else {
      showToast('Deployment Status', `Deployment ended with status: ${dep?.status || 'UNKNOWN'}`, dep?.status === 'STOPPED' ? 'warning' : 'danger');
    }
    loadBuildAndDeployStatus();
  } catch (err) {
    showToast('Deployment Failed', err.message || 'Error deploying project', 'danger');
    logOutput(`[Deployment Error] ${err.message}`);
  }
}

async function stopDeploymentRun() {
  if (!workspaceState.projectId || !workspaceState.activeDeployment) {
    showToast('No Active Deployment', 'No running deployment to stop.', 'warning');
    return;
  }
  try {
    showToast('Stopping Deployment', 'Terminating container execution...', 'info');
    await deploymentClient.stopDeployment(workspaceState.projectId, workspaceState.activeDeployment.id);
    showToast('Deployment Stopped', 'Application container stopped safely.', 'success');
    logOutput('[Deployment] Container stopped.');
    loadBuildAndDeployStatus();
  } catch (err) {
    showToast('Stop Failed', err.message, 'danger');
  }
}

async function restartDeploymentRun() {
  if (!workspaceState.projectId || !workspaceState.activeDeployment) {
    showToast('No Active Deployment', 'No deployment found to restart.', 'warning');
    return;
  }
  try {
    showToast('Restarting Deployment', 'Re-building and restarting container...', 'info');
    const res = await deploymentClient.restartDeployment(workspaceState.projectId, workspaceState.activeDeployment.id);
    showToast('Deployment Restarted', `App running at ${res.data?.deploymentUrl || ''}`, 'success');
    logOutput(`[Deployment Restarted] URL: ${res.data?.deploymentUrl}`);
    loadBuildAndDeployStatus();
  } catch (err) {
    showToast('Restart Failed', err.message, 'danger');
  }
}

async function rollbackDeploymentRun() {
  if (!workspaceState.projectId || !workspaceState.activeDeployment) {
    showToast('No Active Deployment', 'No deployment found to rollback.', 'warning');
    return;
  }
  try {
    showToast('Rolling Back', 'Restoring previous stable deployment artifact...', 'info');
    const res = await deploymentClient.rollbackDeployment(workspaceState.projectId, workspaceState.activeDeployment.id);
    showToast('Rollback Complete', `Rolled back to deployment #${res.data?.id || ''}`, 'success');
    logOutput(`[Deployment Rollback] Rolled back to #${res.data?.id}`);
    loadBuildAndDeployStatus();
  } catch (err) {
    showToast('Rollback Failed', err.message, 'danger');
  }
}

async function loadEnvironmentVariables() {
  const container = document.getElementById('env-vars-list-container');
  if (!container || !workspaceState.projectId) return;

  container.innerHTML = '<div style="font-size:0.75rem; color:var(--color-text-muted); padding:6px 0;">Loading variables...</div>';

  try {
    const res = await deploymentClient.getEnvironmentVariables(workspaceState.projectId);
    const vars = res.data || [];

    if (vars.length === 0) {
      container.innerHTML = '<div style="font-size:0.75rem; color:var(--color-text-muted); padding:6px 0;">No environment variables configured.</div>';
      return;
    }

    container.innerHTML = vars.map(v => `
      <div style="display:flex; align-items:center; justify-content:space-between; padding:6px 8px; background:var(--color-bg-primary); border-radius:var(--radius-sm); border:1px solid var(--color-border); font-size:0.8rem;">
        <div style="display:flex; flex-direction:column; gap:2px;">
          <div style="display:flex; align-items:center; gap:6px;">
            <strong style="color:var(--color-primary);">${escapeHtml(v.name)}</strong>
            <span class="badge badge-neutral" style="font-size:0.65rem;">${escapeHtml(v.environment)}</span>
          </div>
          <span style="font-size:0.72rem; color:var(--color-text-muted); font-family:monospace;">${escapeHtml(v.maskedValue || '••••••••')}</span>
        </div>
        <button class="btn btn-xs btn-ghost text-danger" data-env-id="${v.id}" title="Delete Variable">&times;</button>
      </div>
    `).join('');

    container.querySelectorAll('button[data-env-id]').forEach(btn => {
      btn.addEventListener('click', async () => {
        const envId = btn.dataset.envId;
        try {
          await deploymentClient.deleteEnvironmentVariable(workspaceState.projectId, envId);
          showToast('Variable Deleted', 'Environment variable removed.', 'success');
          loadEnvironmentVariables();
        } catch (err) {
          showToast('Delete Failed', err.message, 'danger');
        }
      });
    });
  } catch (err) {
    if (container) container.innerHTML = `<div style="font-size:0.75rem; color:var(--color-danger); padding:6px 0;">Error loading variables: ${escapeHtml(err.message)}</div>`;
  }
}

function renderBuildHistory(builds) {
  const container = document.getElementById('build-history-list');
  if (!container) return;

  if (!builds || builds.length === 0) {
    container.innerHTML = '<div style="font-size:0.75rem; color:var(--color-text-muted); padding:6px 0;">No build history available.</div>';
    return;
  }

  container.innerHTML = builds.map(b => {
    const badgeClass = b.status === 'SUCCESS' ? 'badge-success' : (b.status === 'FAILED' ? 'badge-danger' : 'badge-neutral');
    const duration = b.durationSeconds ? `${b.durationSeconds}s` : '--';
    return `
      <div class="git-file-item" style="flex-direction:column; align-items:flex-start; gap:4px; padding:6px 8px;">
        <div class="flex items-center justify-between" style="width:100%;">
          <div style="display:flex; align-items:center; gap:6px;">
            <strong style="font-size:0.8rem;">Build #${b.id}</strong>
            <span class="badge ${badgeClass}" style="font-size:0.65rem;">${b.status}</span>
          </div>
          <span style="font-size:0.7rem; color:var(--color-text-muted);">${b.mode} (${duration})</span>
        </div>
        ${b.status === 'FAILED' ? `
          <button class="btn btn-xs btn-outline text-warning" data-build-diagnose="${b.id}" style="font-size:0.68rem; padding:1px 6px;">✨ AI Diagnose</button>
        ` : ''}
      </div>
    `;
  }).join('');

  container.querySelectorAll('button[data-build-diagnose]').forEach(btn => {
    btn.addEventListener('click', async () => {
      const buildId = btn.dataset.buildDiagnose;
      try {
        showToast('AI Diagnosing', 'Analyzing build logs and failure stack trace...', 'info');
        const diagRes = await buildClient.diagnoseBuild(workspaceState.projectId, buildId);
        const diag = diagRes.data;
        if (diag) {
          alert(`✨ AI BUILD DIAGNOSIS\n\nRoot Cause:\n${diag.rootCause}\n\nSuggested Fix:\n${diag.suggestedFix}`);
        }
      } catch (err) {
        showToast('Diagnosis Error', err.message, 'danger');
      }
    });
  });
}

function renderDeploymentStatus(deployments) {
  const badgeEl = document.getElementById('deploy-status-badge');
  const urlBox = document.getElementById('deploy-url-box');
  const urlLink = document.getElementById('deploy-url-link');

  const active = deployments.find(d => d.status === 'RUNNING') || deployments[0];
  workspaceState.activeDeployment = active;

  if (!active) {
    if (badgeEl) { badgeEl.textContent = 'NOT DEPLOYED'; badgeEl.className = 'badge badge-neutral'; }
    if (urlBox) urlBox.style.display = 'none';
    return;
  }

  if (active.status === 'RUNNING') {
    if (badgeEl) { badgeEl.textContent = 'RUNNING'; badgeEl.className = 'badge badge-success'; }
    if (urlBox) urlBox.style.display = 'block';
    if (urlLink) {
      urlLink.href = active.deploymentUrl || '#';
      urlLink.textContent = active.deploymentUrl || 'http://localhost:...';
    }
  } else if (active.status === 'STOPPED') {
    if (badgeEl) { badgeEl.textContent = 'STOPPED'; badgeEl.className = 'badge badge-neutral'; }
    if (urlBox) urlBox.style.display = 'none';
  } else if (active.status === 'FAILED') {
    if (badgeEl) { badgeEl.textContent = 'FAILED'; badgeEl.className = 'badge badge-danger'; }
    if (urlBox) urlBox.style.display = 'none';
  } else {
    if (badgeEl) { badgeEl.textContent = active.status; badgeEl.className = 'badge badge-neutral'; }
    if (urlBox) urlBox.style.display = 'none';
  }
}

/**
 * 22. CI/CD Pipeline IDE Integration (Phase 13)
 */
async function loadCicdPipelines() {
  if (!workspaceState.projectId) return;
  try {
    const res = await cicdClient.getPipelines(workspaceState.projectId);
    const pipelines = res.data || [];
    if (pipelines.length === 0) return;

    const pipeline = pipelines[0];
    workspaceState.activePipeline = pipeline;

    const titleEl = document.getElementById('cicd-pipeline-title');
    const badgeEl = document.getElementById('cicd-pipeline-status-badge');
    const descEl = document.getElementById('cicd-pipeline-desc');

    if (titleEl) titleEl.textContent = pipeline.name || 'Active Pipeline';
    if (badgeEl) {
      badgeEl.textContent = pipeline.enabled ? 'ENABLED' : 'DISABLED';
      badgeEl.className = pipeline.enabled ? 'badge badge-success' : 'badge badge-neutral';
    }
    if (descEl) descEl.textContent = pipeline.description || 'Automated build & test pipeline';

    const runsRes = await cicdClient.getPipelineRuns(workspaceState.projectId, pipeline.id, 0, 10);
    const runs = runsRes.data ? (runsRes.data.content || runsRes.data) : [];
    if (runs && runs.length > 0) {
      const latestRun = runs[0];
      workspaceState.activePipelineRun = latestRun;
      await renderActivePipelineRun(pipeline.id, latestRun);
    }
  } catch (err) {
    console.error('[CI/CD Pipelines Load Error]', err);
  }
}

async function renderActivePipelineRun(pipelineId, run) {
  const card = document.getElementById('cicd-active-run-card');
  const badgeEl = document.getElementById('cicd-run-status-badge');
  const metaEl = document.getElementById('cicd-run-meta');
  const cancelBtn = document.getElementById('cancel-pipeline-run-btn');

  if (card) card.style.display = 'block';
  if (badgeEl) {
    badgeEl.textContent = run.status;
    badgeEl.className = `badge cicd-badge-${run.status}`;
  }
  if (metaEl) {
    metaEl.textContent = `Branch: ${run.branch || 'main'} • Commit: ${(run.commitSha || 'HEAD').substring(0, 7)}`;
  }
  if (cancelBtn) {
    cancelBtn.style.display = (run.status === 'RUNNING' || run.status === 'QUEUED') ? 'inline-block' : 'none';
  }

  try {
    const stepsRes = await cicdClient.getRunSteps(workspaceState.projectId, pipelineId, run.id);
    const steps = stepsRes.data || [];
    renderPipelineSteps(steps);
  } catch (err) {
    console.error('[CI/CD Steps Error]', err);
  }

  try {
    const logsRes = await cicdClient.getRunLogs(workspaceState.projectId, pipelineId, run.id);
    const logs = logsRes.data ? (logsRes.data.logs || []) : [];
    renderPipelineLogs(logs);
  } catch (err) {
    console.error('[CI/CD Logs Error]', err);
  }
}

function renderPipelineSteps(steps) {
  const container = document.getElementById('cicd-steps-list');
  if (!container) return;

  if (!steps || steps.length === 0) {
    container.innerHTML = '<div style="font-size:0.75rem; color:var(--color-text-muted); padding:4px 0;">No steps executed yet.</div>';
    return;
  }

  container.innerHTML = steps.map(s => {
    const statusIcon = s.status === 'SUCCESS' ? '✓' : (s.status === 'FAILED' ? '✕' : (s.status === 'RUNNING' ? '⏳' : '•'));
    const badgeClass = `cicd-badge-${s.status}`;
    const duration = s.durationMs ? `${(s.durationMs / 1000).toFixed(1)}s` : '';
    return `
      <div class="step-item">
        <span class="step-item-name">${statusIcon} ${escapeHtml(s.stepName)}</span>
        <div style="display:flex; align-items:center; gap:6px;">
          <span style="font-size:0.7rem; color:var(--color-text-muted);">${duration}</span>
          <span class="badge ${badgeClass}" style="font-size:0.65rem;">${s.status}</span>
        </div>
      </div>
    `;
  }).join('');
}

function renderPipelineLogs(logs) {
  const container = document.getElementById('cicd-terminal-output');
  if (!container) return;

  if (!logs || logs.length === 0) {
    container.innerHTML = '<div class="cicd-terminal-line cicd-log-stdout">DevPilot CI/CD terminal ready. Waiting for log output...</div>';
    return;
  }

  container.innerHTML = logs.map(l => {
    const streamClass = l.stream === 'stderr' ? 'cicd-log-stderr' : 'cicd-log-stdout';
    const ts = l.timestamp ? new Date(l.timestamp).toLocaleTimeString() : '';
    return `
      <div class="cicd-terminal-line ${streamClass}">
        <span class="cicd-log-ts">[${ts}]</span>
        <span>[${escapeHtml(l.stepName)}]</span>
        <span>${escapeHtml(l.message)}</span>
      </div>
    `;
  }).join('');

  container.scrollTop = container.scrollHeight;
}

// Event listeners for CI/CD panel buttons
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('cicd-refresh-btn')?.addEventListener('click', () => loadCicdPipelines());

  document.getElementById('trigger-pipeline-run-btn')?.addEventListener('click', async () => {
    if (!workspaceState.activePipeline) return;
    try {
      showToast('Pipeline Triggered', 'Queued pipeline execution run...', 'info');
      const res = await cicdClient.triggerPipelineRun(workspaceState.projectId, workspaceState.activePipeline.id, 'main', 'HEAD');
      loadCicdPipelines();
    } catch (err) {
      showToast('Trigger Failed', err.message, 'danger');
    }
  });

  document.getElementById('cancel-pipeline-run-btn')?.addEventListener('click', async () => {
    if (!workspaceState.activePipeline || !workspaceState.activePipelineRun) return;
    try {
      await cicdClient.cancelPipelineRun(workspaceState.projectId, workspaceState.activePipeline.id, workspaceState.activePipelineRun.id);
      showToast('Pipeline Cancelled', 'Pipeline run execution cancelled.', 'warning');
      loadCicdPipelines();
    } catch (err) {
      showToast('Cancel Failed', err.message, 'danger');
    }
  });

  document.getElementById('rerun-pipeline-run-btn')?.addEventListener('click', async () => {
    if (!workspaceState.activePipeline || !workspaceState.activePipelineRun) return;
    try {
      await cicdClient.rerunPipelineRun(workspaceState.projectId, workspaceState.activePipeline.id, workspaceState.activePipelineRun.id);
      showToast('Pipeline Rerun', 'Queued new rerun execution.', 'info');
      loadCicdPipelines();
    } catch (err) {
      showToast('Rerun Failed', err.message, 'danger');
    }
  });

  document.getElementById('diagnose-pipeline-run-btn')?.addEventListener('click', async () => {
    if (!workspaceState.activePipeline || !workspaceState.activePipelineRun) return;
    try {
      showToast('AI Diagnosing', 'Analyzing pipeline failure step and stderr logs...', 'info');
      const res = await cicdClient.diagnosePipelineRun(workspaceState.projectId, workspaceState.activePipeline.id, workspaceState.activePipelineRun.id);
      const diag = res.data;
      if (diag) {
        alert(`✨ AI PIPELINE DIAGNOSIS\n\nFailed Step: ${diag.failedStep}\nSummary: ${diag.summary}\n\nLikely Cause:\n${diag.likelyCause}\n\nSuggested Fix:\n${diag.suggestedFix}`);
      }
    } catch (err) {
      showToast('Diagnosis Error', err.message, 'danger');
    }
  });

  document.getElementById('clear-cicd-logs-btn')?.addEventListener('click', () => {
    const container = document.getElementById('cicd-terminal-output');
    if (container) container.innerHTML = '<div class="cicd-terminal-line cicd-log-stdout">Terminal cleared.</div>';
  });
});


