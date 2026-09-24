/**
 * DevPilot AI — GitHub Client Module (Phase 11)
 * Interfaces with Spring Boot GitHub REST API.
 */

import { api } from './api.js';

export const gitHubClient = {
  /**
   * Retrieves GitHub connection status for current user.
   */
  async getStatus() {
    return api.get('/github/status');
  },

  /**
   * Starts OAuth flow and gets authorization URL.
   */
  async startOAuth() {
    return api.get('/github/oauth/start');
  },

  /**
   * Completes OAuth callback with code and state.
   */
  async handleCallback(code, state) {
    const s = state ? `&state=${encodeURIComponent(state)}` : '';
    return api.get(`/github/oauth/callback?code=${encodeURIComponent(code)}${s}`);
  },

  /**
   * Disconnects GitHub account.
   */
  async disconnect() {
    return api.post('/github/disconnect', {});
  },

  /**
   * Lists repositories for connected user.
   */
  async getRepositories(page = 1, perPage = 30, search = '') {
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('perPage', perPage);
    if (search) params.append('search', search);
    return api.get(`/github/repositories?${params.toString()}`);
  },

  /**
   * Creates a new GitHub repository.
   */
  async createRepository(data) {
    return api.post('/github/repositories', data);
  },

  /**
   * Clones and imports a repository into a new DevPilot project.
   */
  async importRepository(data) {
    return api.post('/github/repositories/import', data);
  },

  /**
   * Lists pull requests for a repository.
   */
  async getPullRequests(owner, repo, state = 'open') {
    return api.get(`/github/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/pulls?state=${encodeURIComponent(state)}`);
  },

  /**
   * Creates a new pull request.
   */
  async createPullRequest(owner, repo, data) {
    return api.post(`/github/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/pulls`, data);
  }
};
