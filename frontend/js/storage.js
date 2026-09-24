/**
 * DevPilot AI — Storage Wrapper
 * Handles user preferences in localStorage and session auth tokens in sessionStorage.
 */

import { CONFIG } from './config.js';

const AUTH_KEYS = {
  ACCESS_TOKEN: 'devpilot_access_token',
  REFRESH_TOKEN: 'devpilot_refresh_token',
  USER_PROFILE: 'devpilot_user_profile'
};

export const storage = {
  // Theme & Appearance
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
  },

  // Authentication & Session Management (SessionStorage scoped to active browser tab)
  getAccessToken() {
    return sessionStorage.getItem(AUTH_KEYS.ACCESS_TOKEN);
  },

  setAccessToken(token) {
    if (token) {
      sessionStorage.setItem(AUTH_KEYS.ACCESS_TOKEN, token);
    } else {
      sessionStorage.removeItem(AUTH_KEYS.ACCESS_TOKEN);
    }
  },

  getRefreshToken() {
    return sessionStorage.getItem(AUTH_KEYS.REFRESH_TOKEN);
  },

  setRefreshToken(token) {
    if (token) {
      sessionStorage.setItem(AUTH_KEYS.REFRESH_TOKEN, token);
    } else {
      sessionStorage.removeItem(AUTH_KEYS.REFRESH_TOKEN);
    }
  },

  getUser() {
    try {
      const data = sessionStorage.getItem(AUTH_KEYS.USER_PROFILE);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  setUser(user) {
    if (user) {
      sessionStorage.setItem(AUTH_KEYS.USER_PROFILE, JSON.stringify(user));
    } else {
      sessionStorage.removeItem(AUTH_KEYS.USER_PROFILE);
    }
  },

  setAuth(authData) {
    if (!authData) return;
    if (authData.accessToken) {
      this.setAccessToken(authData.accessToken);
    }
    if (authData.refreshToken) {
      this.setRefreshToken(authData.refreshToken);
    }
    if (authData.user) {
      this.setUser(authData.user);
    }
  },

  clearAuth() {
    sessionStorage.removeItem(AUTH_KEYS.ACCESS_TOKEN);
    sessionStorage.removeItem(AUTH_KEYS.REFRESH_TOKEN);
    sessionStorage.removeItem(AUTH_KEYS.USER_PROFILE);
  },

  isAuthenticated() {
    return Boolean(this.getAccessToken());
  },

  getUserRoles() {
    const user = this.getUser();
    return (user && Array.isArray(user.roles)) ? user.roles : [];
  },

  hasRole(role) {
    const roles = this.getUserRoles();
    return roles.includes(role) || roles.includes(`ROLE_${role}`);
  }
};
