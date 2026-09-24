/**
 * DevPilot AI — Git Client Module (Phase 11)
 * Interfaces with Spring Boot Git REST API backed by JGit.
 */

import { api } from './api.js';

export const gitClient = {
  /**
   * Initializes a Git repository in the project workspace.
   */
  async initRepository(projectId) {
    return api.post(`/projects/${projectId}/git/init`, {});
  },

  /**
   * Fetches current working tree and index status.
   */
  async getStatus(projectId) {
    return api.get(`/projects/${projectId}/git/status`);
  },

  /**
   * Generates diff for working tree or staged changes.
   */
  async getDiff(projectId, options = {}) {
    const params = new URLSearchParams();
    if (options.path) params.append('path', options.path);
    if (options.staged !== undefined) params.append('staged', options.staged);
    if (options.base) params.append('base', options.base);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return api.get(`/projects/${projectId}/git/diff${qs}`);
  },

  /**
   * Stages specific file paths.
   */
  async stage(projectId, paths) {
    return api.post(`/projects/${projectId}/git/stage`, { paths });
  },

  /**
   * Unstages specific file paths.
   */
  async unstage(projectId, paths) {
    return api.post(`/projects/${projectId}/git/unstage`, { paths });
  },

  /**
   * Stages all modified and untracked changes.
   */
  async stageAll(projectId) {
    return api.post(`/projects/${projectId}/git/stage-all`, {});
  },

  /**
   * Unstages all staged changes.
   */
  async unstageAll(projectId) {
    return api.post(`/projects/${projectId}/git/unstage-all`, {});
  },

  /**
   * Commits staged changes with a commit message.
   */
  async commit(projectId, message) {
    return api.post(`/projects/${projectId}/git/commit`, { message });
  },

  /**
   * Retrieves paginated commit history.
   */
  async getCommits(projectId, options = {}) {
    const params = new URLSearchParams();
    if (options.branch) params.append('branch', options.branch);
    if (options.page !== undefined) params.append('page', options.page);
    if (options.size !== undefined) params.append('size', options.size);
    if (options.search) params.append('search', options.search);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return api.get(`/projects/${projectId}/git/commits${qs}`);
  },

  /**
   * Retrieves details for a specific commit.
   */
  async getCommit(projectId, commitHash) {
    return api.get(`/projects/${projectId}/git/commits/${commitHash}`);
  },

  /**
   * Retrieves diff files for a specific commit.
   */
  async getCommitDiff(projectId, commitHash) {
    return api.get(`/projects/${projectId}/git/commits/${commitHash}/diff`);
  },

  /**
   * Lists branches.
   */
  async getBranches(projectId) {
    return api.get(`/projects/${projectId}/git/branches`);
  },

  /**
   * Creates a new branch.
   */
  async createBranch(projectId, name) {
    return api.post(`/projects/${projectId}/git/branches`, { name });
  },

  /**
   * Switches or checkouts a branch.
   */
  async checkoutBranch(projectId, branch, createIfNotExists = false) {
    return api.post(`/projects/${projectId}/git/branches/checkout`, {
      branch,
      createIfNotExists
    });
  },

  /**
   * Deletes a branch.
   */
  async deleteBranch(projectId, branchName) {
    return api.delete(`/projects/${projectId}/git/branches/${encodeURIComponent(branchName)}`);
  },

  /**
   * Fetches refs from remote.
   */
  async fetch(projectId, remote = 'origin') {
    return api.post(`/projects/${projectId}/git/fetch?remote=${encodeURIComponent(remote)}`, {});
  },

  /**
   * Pulls changes from remote.
   */
  async pull(projectId, remote = 'origin', branch = '') {
    const b = branch ? `&branch=${encodeURIComponent(branch)}` : '';
    return api.post(`/projects/${projectId}/git/pull?remote=${encodeURIComponent(remote)}${b}`, {});
  },

  /**
   * Pushes local commits to remote.
   */
  async push(projectId, remote = 'origin', branch = '') {
    const b = branch ? `&branch=${encodeURIComponent(branch)}` : '';
    return api.post(`/projects/${projectId}/git/push?remote=${encodeURIComponent(remote)}${b}`, {});
  },

  /**
   * Lists remotes.
   */
  async getRemotes(projectId) {
    return api.get(`/projects/${projectId}/git/remotes`);
  },

  /**
   * Adds remote.
   */
  async addRemote(projectId, name, repositoryUrl) {
    return api.post(`/projects/${projectId}/git/remotes`, { name, repositoryUrl });
  },

  /**
   * Suggests conventional commit message using AI from staged/working diff.
   */
  async generateAiCommitMessage(projectId) {
    return api.post(`/projects/${projectId}/git/ai-commit-message`, {});
  }
};
