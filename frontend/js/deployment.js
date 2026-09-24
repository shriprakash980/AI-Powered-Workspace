/**
 * DevPilot AI — Deployment Platform Client Module (Phase 12)
 * Interfaces with Spring Boot Deployment & Environment Variable REST APIs.
 */

import { api } from './api.js';

export const deploymentClient = {
  /**
   * Triggers a new deployment.
   */
  async createDeployment(projectId, environment = 'DEVELOPMENT', buildId = null) {
    const payload = { environment };
    if (buildId) payload.buildId = buildId;
    return api.post(`/projects/${projectId}/deployments`, payload);
  },

  /**
   * Lists deployments for a project.
   */
  async getDeployments(projectId, page = 0, size = 20) {
    return api.get(`/projects/${projectId}/deployments?page=${page}&size=${size}`);
  },

  /**
   * Gets details for a specific deployment.
   */
  async getDeploymentDetails(projectId, deploymentId) {
    return api.get(`/projects/${projectId}/deployments/${deploymentId}`);
  },

  /**
   * Gets logs for a deployment.
   */
  async getDeploymentLogs(projectId, deploymentId) {
    return api.get(`/projects/${projectId}/deployments/${deploymentId}/logs`);
  },

  /**
   * Stops a running deployment.
   */
  async stopDeployment(projectId, deploymentId) {
    return api.post(`/projects/${projectId}/deployments/${deploymentId}/stop`, {});
  },

  /**
   * Restarts a deployment.
   */
  async restartDeployment(projectId, deploymentId) {
    return api.post(`/projects/${projectId}/deployments/${deploymentId}/restart`, {});
  },

  /**
   * Rolls back to a previous successful deployment version.
   */
  async rollbackDeployment(projectId, deploymentId) {
    return api.post(`/projects/${projectId}/deployments/${deploymentId}/rollback`, {});
  },

  /**
   * Requests AI diagnosis for a deployment failure.
   */
  async diagnoseDeployment(projectId, deploymentId) {
    return api.post(`/projects/${projectId}/deployments/${deploymentId}/diagnose`, {});
  },

  /**
   * Environment Variables API
   */
  async getEnvironmentVariables(projectId, environment = 'DEVELOPMENT') {
    const envQs = environment ? `?environment=${encodeURIComponent(environment)}` : '';
    return api.get(`/projects/${projectId}/environment-variables${envQs}`);
  },

  async saveEnvironmentVariable(projectId, name, value, environment = 'DEVELOPMENT') {
    return api.post(`/projects/${projectId}/environment-variables`, { name, value, environment });
  },

  async deleteEnvironmentVariable(projectId, variableId) {
    return api.delete(`/projects/${projectId}/environment-variables/${variableId}`);
  }
};
