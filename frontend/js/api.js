/**
 * DevPilot AI — Reusable API Service Wrapper
 * Structured for seamless connection to Spring Boot backend in Phase 3/4.
 */

import { CONFIG } from './config.js';

export const API_BASE_URL = CONFIG.API_BASE_URL;

/**
 * Reusable HTTP Request Wrapper with JSON handling, Authorization injection, and error formatting
 * @param {string} endpoint - API path (e.g., '/projects' or '/auth/login')
 * @param {RequestInit} options - Fetch options (method, headers, body, etc.)
 * @returns {Promise<any>}
 */
export async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  // Default headers
  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  };

  // Prepare authorization token if present in session memory
  const token = sessionStorage.getItem('devpilot_access_token');
  if (token) {
    defaultHeaders['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...(options.headers || {})
    }
  };

  try {
    const response = await fetch(url, config);

    // Parse JSON safely
    let data;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      data = await response.text();
    }

    if (!response.ok) {
      const errorMessage = (typeof data === 'object' && data.message) ? data.message : `HTTP error: ${response.status}`;
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  } catch (error) {
    // Structured error forwarding
    console.error(`[DevPilot API Error] ${options.method || 'GET'} ${url}:`, error.message);
    throw error;
  }
}
