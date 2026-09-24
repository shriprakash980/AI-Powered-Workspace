/**
 * DevPilot AI — Reusable API Service Wrapper
 * Handles HTTP requests, JWT Bearer token injection, automatic 401 token refresh, and logout redirection.
 */

import { CONFIG } from './config.js';
import { storage } from './storage.js';

export const API_BASE_URL = CONFIG.API_BASE_URL;

let isRefreshing = false;
let refreshSubscribers = [];

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb);
}

function onRefreshed(newAccessToken) {
  refreshSubscribers.forEach(cb => cb(newAccessToken));
  refreshSubscribers = [];
}

/**
 * Reusable HTTP Request Wrapper with automatic 401 refresh rotation
 * @param {string} endpoint - API path (e.g. '/projects' or '/auth/login')
 * @param {RequestInit} options - Fetch options
 * @param {boolean} isRetry - Internal flag to prevent infinite refresh loops
 * @returns {Promise<any>}
 */
export async function apiRequest(endpoint, options = {}, isRetry = false) {
  const url = `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  };

  const accessToken = storage.getAccessToken();
  if (accessToken) {
    defaultHeaders['Authorization'] = `Bearer ${accessToken}`;
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

    // If 401 Unauthorized and not already retrying, attempt token refresh once
    if (response.status === 401 && !isRetry && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
      const refreshToken = storage.getRefreshToken();
      if (refreshToken) {
        if (!isRefreshing) {
          isRefreshing = true;
          try {
            const refreshRes = await fetch(`${API_BASE_URL}/auth/refresh`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
              body: JSON.stringify({ refreshToken })
            });

            if (refreshRes.ok) {
              const refreshData = await refreshRes.json();
              if (refreshData && refreshData.data && refreshData.data.accessToken) {
                storage.setAuth(refreshData.data);
                isRefreshing = false;
                onRefreshed(refreshData.data.accessToken);

                // Retry original request with new token
                return apiRequest(endpoint, options, true);
              }
            }
          } catch (refreshErr) {
            console.warn('[DevPilot] Refresh token error:', refreshErr.message);
          }

          // If refresh failed, clear session and redirect to login
          isRefreshing = false;
          refreshSubscribers = [];
          storage.clearAuth();
          handleAuthFailureRedirect();
          throw new Error('Session expired. Please log in again.');
        } else {
          // If already refreshing, wait for new token and retry
          return new Promise((resolve, reject) => {
            subscribeTokenRefresh((newAccessToken) => {
              options.headers = {
                ...(options.headers || {}),
                'Authorization': `Bearer ${newAccessToken}`
              };
              apiRequest(endpoint, options, true).then(resolve).catch(reject);
            });
          });
        }
      } else {
        // No refresh token available
        storage.clearAuth();
        handleAuthFailureRedirect();
      }
    }

    let data;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      data = await response.text();
    }

    if (!response.ok) {
      let errorMessage = `HTTP error: ${response.status}`;
      if (typeof data === 'object' && data !== null) {
        if (data.message) {
          errorMessage = data.message;
        } else if (Array.isArray(data.errors) && data.errors.length > 0) {
          errorMessage = data.errors[0].message || data.errors[0];
        }
      }
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  } catch (error) {
    console.error(`[DevPilot API Error] ${options.method || 'GET'} ${url}:`, error.message);
    throw error;
  }
}

function handleAuthFailureRedirect() {
  const currentPath = window.location.pathname.split('/').pop() || '';
  const publicPages = ['index.html', 'login.html', 'register.html', '404.html', ''];
  if (!publicPages.includes(currentPath)) {
    window.location.href = 'login.html';
  }
}
