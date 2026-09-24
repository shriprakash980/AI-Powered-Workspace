/**
 * DevPilot AI — Dashboard Controller
 */

import { showToast, formatDate, escapeHtml } from './utils.js';
import { openModal, closeModal, setButtonLoading } from './components.js';
import { apiRequest } from './api.js';

// Structured state placeholder (Ready to be populated via API in Phase 3/4)
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
  renderStats();
  renderProjects(state.projects);
  renderActivities();
  setupEventListeners();
  loadProjectsFromApi();
});

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
    // Graceful fallback to default seed/demo projects when backend is starting or offline
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
              <div class="project-lang-icon">${escapeHtml(project.language.slice(0, 2).toUpperCase())}</div>
              <h3 class="project-name">${escapeHtml(project.name)}</h3>
            </div>
            <span class="badge ${langBadgeClass}">${escapeHtml(project.language)}</span>
          </div>
          <p class="project-desc">${escapeHtml(project.description)}</p>
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
  // Modal Triggers
  const openModalBtns = document.querySelectorAll('[data-open-create-modal]');
  openModalBtns.forEach(btn => {
    btn.addEventListener('click', () => openModal('create-project-modal'));
  });

  // Search Filter
  const searchInput = document.getElementById('project-search-input');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      const query = e.target.value.toLowerCase().trim();
      const filtered = state.projects.filter(p =>
        p.name.toLowerCase().includes(query) ||
        p.description.toLowerCase().includes(query) ||
        p.language.toLowerCase().includes(query)
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
        // Fallback for offline mode or network error
        console.warn('[DevPilot] API request failed, saving locally:', err.message);

        const fallbackProject = {
          id: `proj-${Date.now()}`,
          name: name.toLowerCase().replace(/\s+/g, '-'),
          description: descInput.value.trim() || 'No description provided.',
          language: langSelect.value,
          template: templateSelect.value,
          status: 'ACTIVE',
          updatedAt: new Date().toISOString()
        };

        state.projects.unshift(fallbackProject);
        state.stats.projects = state.projects.length;
        state.stats.activeProjects++;

        renderStats();
        renderProjects(state.projects);
        setButtonLoading(submitBtn, false);
        closeModal('create-project-modal');
        createForm.reset();

        showToast('Project Created', `Project '${fallbackProject.name}' scaffolded (offline mode).`, 'info');
      }
    });
  }
}
