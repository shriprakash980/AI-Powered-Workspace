/**
 * DevPilot AI — CI/CD Pipeline Client Module (Phase 13)
 * Handles API integration for automated pipelines, webhooks, runs, steps, and live logs.
 */

import { api } from './api.js';

export const cicdClient = {
  /**
   * Fetches all pipelines for a project.
   */
  async getPipelines(projectId) {
    return api.get(`/projects/${projectId}/pipelines`);
  },

  /**
   * Creates a new pipeline for a project.
   */
  async createPipeline(projectId, data) {
    return api.post(`/projects/${projectId}/pipelines`, data);
  },

  /**
   * Gets details for a specific pipeline.
   */
  async getPipelineDetails(projectId, pipelineId) {
    return api.get(`/projects/${projectId}/pipelines/${pipelineId}`);
  },

  /**
   * Updates pipeline settings.
   */
  async updatePipeline(projectId, pipelineId, data) {
    return api.put(`/projects/${projectId}/pipelines/${pipelineId}`, data);
  },

  /**
   * Deletes a pipeline.
   */
  async deletePipeline(projectId, pipelineId) {
    return api.delete(`/projects/${projectId}/pipelines/${pipelineId}`);
  },

  /**
   * Lists runs for a pipeline.
   */
  async getPipelineRuns(projectId, pipelineId, page = 0, size = 20) {
    return api.get(`/projects/${projectId}/pipelines/${pipelineId}/runs?page=${page}&size=${size}`);
  },

  /**
   * Gets details for a specific pipeline run.
   */
  async getRunDetails(projectId, pipelineId, runId) {
    return api.get(`/projects/${projectId}/pipelines/${pipelineId}/runs/${runId}`);
  },

  /**
   * Gets steps for a pipeline run.
   */
  async getRunSteps(projectId, pipelineId, runId) {
    return api.get(`/projects/${projectId}/pipelines/${pipelineId}/runs/${runId}/steps`);
  },

  /**
   * Gets logs for a pipeline run.
   */
  async getRunLogs(projectId, pipelineId, runId, stepName = '') {
    const query = stepName ? `?stepName=${encodeURIComponent(stepName)}` : '';
    return api.get(`/projects/${projectId}/pipelines/${pipelineId}/runs/${runId}/logs${query}`);
  },

  /**
   * Manually triggers a pipeline run.
   */
  async triggerPipelineRun(projectId, pipelineId, branch = 'main', commitSha = 'HEAD') {
    return api.post(`/projects/${projectId}/pipelines/${pipelineId}/run`, { branch, commitSha });
  },

  /**
   * Cancels an active pipeline run.
   */
  async cancelPipelineRun(projectId, pipelineId, runId) {
    return api.post(`/projects/${projectId}/pipelines/${pipelineId}/runs/${runId}/cancel`, {});
  },

  /**
   * Reruns a completed or failed pipeline run.
   */
  async rerunPipelineRun(projectId, pipelineId, runId) {
    return api.post(`/projects/${projectId}/pipelines/${pipelineId}/runs/${runId}/rerun`, {});
  },

  /**
   * Reads raw devpilot-ci.yml configuration.
   */
  async getPipelineConfig(projectId, pipelineId) {
    return api.get(`/projects/${projectId}/pipelines/${pipelineId}/config`);
  },

  /**
   * Requests AI diagnosis for a failed pipeline run.
   */
  async diagnosePipelineRun(projectId, pipelineId, runId) {
    return api.post(`/projects/${projectId}/pipelines/${pipelineId}/runs/${runId}/diagnose`, {});
  }
};
