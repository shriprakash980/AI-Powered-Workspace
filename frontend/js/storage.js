/**
 * DevPilot AI — Local Storage Wrapper
 * Strictly handles non-sensitive user preferences.
 */

import { CONFIG } from './config.js';

export const storage = {
  getTheme() {
    return localStorage.getItem(CONFIG.STORAGE_KEYS.THEME) || CONFIG.DEFAULT_THEME;
  },

  setTheme(theme) {
    if (theme === 'light' || theme === 'dark') {
      localStorage.setItem(CONFIG.STORAGE_KEYS.THEME, theme);
      document.documentElement.setAttribute('data-theme', theme);
    }
  },

  getSidebarCollapsed() {
    return localStorage.getItem(CONFIG.STORAGE_KEYS.SIDEBAR_COLLAPSED) === 'true';
  },

  setSidebarCollapsed(isCollapsed) {
    localStorage.setItem(CONFIG.STORAGE_KEYS.SIDEBAR_COLLAPSED, String(isCollapsed));
  },

  getEditorPreferences() {
    try {
      const data = localStorage.getItem(CONFIG.STORAGE_KEYS.EDITOR_SETTINGS);
      return data ? JSON.parse(data) : { fontSize: 14, tabSize: 2, minimap: true, wordWrap: 'on' };
    } catch {
      return { fontSize: 14, tabSize: 2, minimap: true, wordWrap: 'on' };
    }
  },

  setEditorPreferences(prefs) {
    localStorage.setItem(CONFIG.STORAGE_KEYS.EDITOR_SETTINGS, JSON.stringify(prefs));
  },

  getActiveProjectId() {
    return localStorage.getItem(CONFIG.STORAGE_KEYS.ACTIVE_PROJECT_ID);
  },

  setActiveProjectId(id) {
    if (id) {
      localStorage.setItem(CONFIG.STORAGE_KEYS.ACTIVE_PROJECT_ID, id);
    } else {
      localStorage.removeItem(CONFIG.STORAGE_KEYS.ACTIVE_PROJECT_ID);
    }
  }
};
