/**
 * DevPilot AI — Build System Client Module (Phase 12)
 * Interfaces with Spring Boot Build REST API.
 */

import { api } from './api.js';

export const buildClient = {
  /**
   * Detects project type and suggested build/test settings.
   */
  async detectProject(projectId) {
    return api.post(`/projects/${projectId}/builds/detect`, {});
  },

  /**
   * Triggers a build/test run.
   */
  async triggerBuild(projectId, mode = 'BUILD', customCommand = '') {
    return api.post(`/projects/${projectId}/builds`, { mode, customCommand });
  },

  /**
   * Lists builds for a project.
   */
  async getBuilds(projectId, page = 0, size = 20) {
    return api.get(`/projects/${projectId}/builds?page=${page}&size=${size}`);
  },

  /**
   * Gets details for a specific build.
   */
  async getBuildDetails(projectId, buildId) {
    return api.get(`/projects/${projectId}/builds/${buildId}`);
  },

  /**
   * Gets logs for a build.
   */
  async getBuildLogs(projectId, buildId) {
    return api.get(`/projects/${projectId}/builds/${buildId}/logs`);
  },

  /**
   * Cancels an in-progress build.
   */
  async cancelBuild(projectId, buildId) {
    return api.post(`/projects/${projectId}/builds/${buildId}/cancel`, {});
  },

  /**
   * Requests AI diagnosis for a build failure.
   */
  async diagnoseBuild(projectId, buildId) {
    return api.post(`/projects/${projectId}/builds/${buildId}/diagnose`, {});
  }
};
