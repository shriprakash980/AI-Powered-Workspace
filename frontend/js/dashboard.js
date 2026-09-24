/**
 * DevPilot AI — Dashboard Controller
 */

import { showToast, formatDate, escapeHtml } from './utils.js';
import { openModal, closeModal, setButtonLoading } from './components.js';
import { apiRequest } from './api.js';
import { storage } from './storage.js';

// Structured state placeholder (populated via API)
const state = {
  stats: {
    projects: 3,
    aiRequests: 48,
    deployments: 12,
    activeProjects: 2
  },
  projects: [
    {
      id: 'proj-1',
      name: 'devpilot-cloud-platform',
      description: 'Cloud developer workspace with Monaco editor and sandboxed runtime.',
      language: 'JavaScript',
      template: 'HTML_CSS_JS',
      status: 'ACTIVE',
      updatedAt: new Date(Date.now() - 1000 * 60 * 30).toISOString()
    },
    {
      id: 'proj-2',
      name: 'ecommerce-microservice',
      description: 'Spring Boot 3 REST API with PostgreSQL and JWT authentication.',
      language: 'Java',
      template: 'JAVA_SPRING',
      status: 'ACTIVE',
      updatedAt: new Date(Date.now() - 1000 * 60 * 60 * 4).toISOString()
    },
    {
      id: 'proj-3',
      name: 'ai-data-pipeline',
      description: 'Autonomous data extraction and embedding ingestion script.',
      language: 'Python',
      template: 'PYTHON_FLASK',
      status: 'IDLE',
      updatedAt: new Date(Date.now() - 1000 * 60 * 60 * 48).toISOString()
    }
  ],
  activities: [
    { text: 'Deployed build #104 to staging', time: '15m ago', icon: 'zap' },
    { text: 'Applied AI refactor in auth.service.js', time: '1h ago', icon: 'cpu' },
    { text: 'Created branch feature/execution-sandbox', time: '3h ago', icon: 'git-branch' },
    { text: 'Created new project ecommerce-microservice', time: '1d ago', icon: 'folder-plus' }
  ]
};

document.addEventListener('DOMContentLoaded', () => {
  // 1. Guard dashboard authentication
  if (!storage.isAuthenticated()) {
    window.location.href = 'login.html';
    return;
  }

  // 2. Load User Profile and Dashboard Elements
  loadUserProfile();
  renderStats();
  renderProjects(state.projects);
  renderActivities();
  setupEventListeners();
  loadProjectsFromApi();
});

/**
 * Fetches current authenticated user profile
 */
async function loadUserProfile() {
  const cachedUser = storage.getUser();
  if (cachedUser) {
    applyUserDataToUI(cachedUser);
  }

  try {
    const response = await apiRequest('/auth/me');
    if (response && response.data) {
      storage.setUser(response.data);
      applyUserDataToUI(response.data);
    }
  } catch (err) {
    console.warn('[DevPilot] Could not refresh profile from /auth/me:', err.message);
  }
}

function applyUserDataToUI(user) {
  const nameEl = document.getElementById('sidebar-user-name');
  const roleEl = document.getElementById('sidebar-user-role');
  const avatarEl = document.getElementById('sidebar-user-avatar');
  const welcomeTitle = document.querySelector('.welcome-title');

  if (nameEl && user.fullName) nameEl.textContent = user.fullName;
  if (roleEl && user.roles) {
    roleEl.textContent = Array.isArray(user.roles) ? user.roles.join(', ') : 'Developer';
  }
  if (avatarEl && user.fullName) {
    const initials = user.fullName.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
    avatarEl.textContent = initials || 'DEV';
  }
  if (welcomeTitle && user.fullName) {
    welcomeTitle.textContent = `Welcome back, ${escapeHtml(user.fullName)}`;
  }
}

/**
 * Loads projects from Spring Boot backend REST API
 */
async function loadProjectsFromApi() {
  try {
    const response = await apiRequest('/projects');
    if (response && response.data && Array.isArray(response.data)) {
      state.projects = response.data;
      state.stats.projects = response.data.length;
      state.stats.activeProjects = response.data.filter(p => p.status === 'ACTIVE').length;
      renderStats();
      renderProjects(state.projects);
    }
  } catch (err) {
    console.info('[DevPilot] Backend offline or loading, using default local projects:', err.message);
  }
}

/**
 * Dynamically binds statistics to DOM
 */
function renderStats() {
  const statElements = {
    totalProjects: document.getElementById('stat-total-projects'),
    aiRequests: document.getElementById('stat-ai-requests'),
    deployments: document.getElementById('stat-deployments'),
    activeProjects: document.getElementById('stat-active-projects')
  };

  if (statElements.totalProjects) statElements.totalProjects.textContent = state.stats.projects;
  if (statElements.aiRequests) statElements.aiRequests.textContent = state.stats.aiRequests;
  if (statElements.deployments) statElements.deployments.textContent = state.stats.deployments;
  if (statElements.activeProjects) statElements.activeProjects.textContent = state.stats.activeProjects;
}

/**
 * Renders the projects grid or empty state
 */
function renderProjects(projectsList) {
  const container = document.getElementById('projects-grid');
  if (!container) return;

  if (projectsList.length === 0) {
    container.innerHTML = `
      <div class="empty-state" style="grid-column: 1 / -1;">
        <svg class="empty-state-icon" viewBox="0 0 24 24"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
        <h3 class="empty-state-title">No projects found</h3>
        <p class="empty-state-desc">You haven't created any developer projects yet or your search query yielded no results.</p>
        <button class="btn btn-primary" id="empty-create-btn">Create Your First Project</button>
      </div>
    `;
    const emptyBtn = document.getElementById('empty-create-btn');
    if (emptyBtn) emptyBtn.addEventListener('click', () => openModal('create-project-modal'));
    return;
  }

  container.innerHTML = projectsList.map(project => {
    const langBadgeClass = project.language === 'Java' ? 'badge-warning' : (project.language === 'Python' ? 'badge-primary' : 'badge-success');
    return `
      <div class="project-card" data-project-id="${project.id}">
        <div>
          <div class="project-card-header">
            <div class="project-brand">
              <div class="project-lang-icon">${escapeHtml((project.language || 'JS').slice(0, 2).toUpperCase())}</div>
              <h3 class="project-name">${escapeHtml(project.name)}</h3>
            </div>
            <span class="badge ${langBadgeClass}">${escapeHtml(project.language || 'JavaScript')}</span>
          </div>
          <p class="project-desc">${escapeHtml(project.description || 'No description provided.')}</p>
        </div>
        <div class="project-footer">
          <span>Updated ${formatDate(project.updatedAt)}</span>
          <a href="workspace.html?project=${project.id}" class="btn btn-sm btn-outline">Open IDE &rarr;</a>
        </div>
      </div>
    `;
  }).join('');
}

/**
 * Renders recent activity feed
 */
function renderActivities() {
  const list = document.getElementById('activity-list');
  if (!list) return;

  list.innerHTML = state.activities.map(act => `
    <div class="activity-item">
      <div class="activity-icon-badge">
        <svg class="icon icon-sm" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
      </div>
      <div>
        <div class="activity-text">${escapeHtml(act.text)}</div>
        <div class="activity-time">${escapeHtml(act.time)}</div>
      </div>
    </div>
  `).join('');
}

/**
 * UI Event Listeners
 */
function setupEventListeners() {
  // Logout Trigger
  const logoutBtn = document.getElementById('sidebar-logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', async (e) => {
      e.preventDefault();
      try {
        const refreshToken = storage.getRefreshToken();
        await apiRequest('/auth/logout', {
          method: 'POST',
          body: JSON.stringify({ refreshToken })
        });
      } catch (err) {
        console.warn('[DevPilot] Logout API notice:', err.message);
      } finally {
        storage.clearAuth();
        showToast('Signed Out', 'You have been logged out safely.', 'info');
        setTimeout(() => {
          window.location.href = 'login.html';
        }, 500);
      }
    });
  }

  // Modal Triggers
  const openModalBtns = document.querySelectorAll('[data-open-create-modal]');
  openModalBtns.forEach(btn => {
    btn.addEventListener('click', () => openModal('create-project-modal'));
  });

  const openImportBtns = document.querySelectorAll('[data-open-import-modal]');
  openImportBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      openModal('import-github-modal');
      checkGitHubImportAuthStatus();
    });
  });

  // Auto-extract project name from repository URL
  const importUrlInput = document.getElementById('import-repo-url');
  const importNameInput = document.getElementById('import-project-name');
  if (importUrlInput && importNameInput) {
    importUrlInput.addEventListener('input', (e) => {
      const url = e.target.value.trim();
      const match = url.match(/\/([^/]+?)(\.git)?$/);
      if (match && match[1] && !importNameInput.value) {
        importNameInput.value = match[1].toLowerCase().replace(/[^a-z0-9-_]/g, '-');
      }
    });
  }

  // Import GitHub Repo Form Handler
  const importForm = document.getElementById('import-github-form');
  if (importForm) {
    importForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const urlInput = document.getElementById('import-repo-url');
      const nameInput = document.getElementById('import-project-name');
      const descInput = document.getElementById('import-project-desc');
      const submitBtn = document.getElementById('import-submit-btn');

      const cloneUrl = urlInput?.value.trim();
      const name = nameInput?.value.trim();
      if (!cloneUrl || !name) {
        showToast('Validation Error', 'Repository URL and project name are required.', 'error');
        return;
      }

      setButtonLoading(submitBtn, true, 'Cloning & Importing Repository...');

      try {
        const { gitHubClient } = await import('./github.js');
        const response = await gitHubClient.importRepository({
          cloneUrl: cloneUrl,
          name: name,
          description: descInput?.value.trim() || 'Imported from GitHub repository.'
        });

        const newProject = response.data;
        if (newProject) {
          state.projects.unshift(newProject);
          state.stats.projects = state.projects.length;
          state.stats.activeProjects = state.projects.filter(p => p.status === 'ACTIVE').length;
          renderStats();
          renderProjects(state.projects);
        }

        setButtonLoading(submitBtn, false);
        closeModal('import-github-modal');
        importForm.reset();

        showToast('Repository Imported', `Project '${name}' imported successfully! Opening workspace...`, 'success');
        if (newProject && newProject.id) {
          setTimeout(() => {
            window.location.href = `workspace.html?project=${newProject.id}`;
          }, 800);
        }
      } catch (err) {
        setButtonLoading(submitBtn, false);
        showToast('Import Failed', err.message || 'Error cloning repository from GitHub.', 'error');
      }
    });
  }

  // Search Filter
  const searchInput = document.getElementById('project-search-input');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      const query = e.target.value.toLowerCase().trim();
      const filtered = state.projects.filter(p =>
        p.name.toLowerCase().includes(query) ||
        (p.description && p.description.toLowerCase().includes(query)) ||
        (p.language && p.language.toLowerCase().includes(query))
      );
      renderProjects(filtered);
    });
  }

  // Create Project Form Handler
  const createForm = document.getElementById('create-project-form');
  if (createForm) {
    createForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const nameInput = document.getElementById('new-project-name');
      const descInput = document.getElementById('new-project-desc');
      const templateSelect = document.getElementById('new-project-template');
      const langSelect = document.getElementById('new-project-lang');
      const submitBtn = document.getElementById('create-submit-btn');

      const name = nameInput.value.trim();
      if (!name) {
        showToast('Validation Error', 'Project name is required', 'error');
        nameInput.focus();
        return;
      }

      setButtonLoading(submitBtn, true, 'Creating Project in Database...');

      const payload = {
        name: name,
        description: descInput.value.trim() || 'No description provided.',
        template: templateSelect.value || 'HTML_CSS_JS',
        language: langSelect.value || 'JavaScript',
        framework: ''
      };

      try {
        const response = await apiRequest('/projects', {
          method: 'POST',
          body: JSON.stringify(payload)
        });

        const newProject = (response && response.data) ? response.data : {
          id: `proj-${Date.now()}`,
          ...payload,
          status: 'ACTIVE',
          updatedAt: new Date().toISOString()
        };

        state.projects.unshift(newProject);
        state.stats.projects = state.projects.length;
        state.stats.activeProjects = state.projects.filter(p => p.status === 'ACTIVE').length;

        renderStats();
        renderProjects(state.projects);
        setButtonLoading(submitBtn, false);
        closeModal('create-project-modal');
        createForm.reset();

        showToast('Project Created', `Project '${newProject.name}' saved to PostgreSQL database.`, 'success');
      } catch (err) {
        setButtonLoading(submitBtn, false);
        const errMsg = err.message || 'Failed to create project.';
        showToast('Error', errMsg, 'error');
      }
    });
  }
}

async function checkGitHubImportAuthStatus() {
  const banner = document.getElementById('github-import-auth-banner');
  if (!banner) return;

  try {
    const { gitHubClient } = await import('./github.js');
    const res = await gitHubClient.getStatus();
    if (!res.data || !res.data.connected) {
      banner.style.display = 'flex';
      const connectBtn = document.getElementById('import-connect-github-btn');
      if (connectBtn) {
        connectBtn.onclick = async () => {
          const authRes = await gitHubClient.startOAuth();
          if (authRes && authRes.data && authRes.data.authorizationUrl) {
            window.location.href = authRes.data.authorizationUrl;
          }
        };
      }
    } else {
      banner.style.display = 'none';
    }
  } catch (err) {
    banner.style.display = 'none';
  }
}
