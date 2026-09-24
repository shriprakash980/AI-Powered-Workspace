/**
 * DevPilot AI — Client Configuration Module
 */

export const CONFIG = {
  APP_NAME: 'DevPilot AI',
  APP_VERSION: '1.0.0-rc',
  API_BASE_URL: 'http://localhost:8080/api/v1',
  DEFAULT_THEME: 'dark',
  WS_URL: 'ws://localhost:8080/ws',
  
  // Storage Keys (Only non-sensitive user preferences)
  STORAGE_KEYS: {
    THEME: 'devpilot_theme',
    SIDEBAR_COLLAPSED: 'devpilot_sidebar_collapsed',
    EDITOR_SETTINGS: 'devpilot_editor_prefs',
    ACTIVE_PROJECT_ID: 'devpilot_active_project_id',
    RECENT_PROJECTS: 'devpilot_recent_projects'
  },

  // Starter Templates Catalog
  TEMPLATES: [
    { id: 'BLANK', name: 'Blank Project', lang: 'Text', icon: 'file' },
    { id: 'HTML_CSS_JS', name: 'HTML / CSS / JavaScript', lang: 'JavaScript', icon: 'globe' },
    { id: 'JAVA_SPRING', name: 'Java Spring Boot', lang: 'Java', icon: 'code' },
    { id: 'FULLSTACK_JAVA', name: 'Full Stack Java', lang: 'Java', icon: 'layers' },
    { id: 'NODE_EXPRESS', name: 'Node.js Express', lang: 'JavaScript', icon: 'server' },
    { id: 'PYTHON_FLASK', name: 'Python Flask', lang: 'Python', icon: 'terminal' }
  ],

  // Supported Programming Languages
  LANGUAGES: ['JavaScript', 'Java', 'Python', 'HTML', 'CSS', 'JSON', 'Markdown', 'SQL']
};
