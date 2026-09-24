/**
 * DevPilot AI — Settings Page Controller
 */

import { storage } from './storage.js';
import { showToast } from './utils.js';

document.addEventListener('DOMContentLoaded', () => {
  // Theme Selection Cards
  const themeCards = document.querySelectorAll('.theme-card');
  const activeTheme = storage.getTheme();

  themeCards.forEach(card => {
    if (card.dataset.theme === activeTheme) {
      card.classList.add('active');
    }

    card.addEventListener('click', () => {
      themeCards.forEach(c => c.classList.remove('active'));
      card.classList.add('active');
      const selected = card.dataset.theme;
      storage.setTheme(selected);
      showToast('Theme Updated', `Switched to ${selected} theme.`, 'info');
    });
  });

  // Editor Settings Form
  const editorForm = document.getElementById('editor-settings-form');
  const prefs = storage.getEditorPreferences();

  const fontSizeInput = document.getElementById('pref-font-size');
  const tabSizeInput = document.getElementById('pref-tab-size');
  const minimapInput = document.getElementById('pref-minimap');

  if (fontSizeInput) fontSizeInput.value = prefs.fontSize || 14;
  if (tabSizeInput) tabSizeInput.value = prefs.tabSize || 2;
  if (minimapInput) minimapInput.checked = prefs.minimap !== false;

  if (editorForm) {
    editorForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const updatedPrefs = {
        fontSize: parseInt(fontSizeInput.value, 10),
        tabSize: parseInt(tabSizeInput.value, 10),
        minimap: minimapInput.checked
      };
      storage.setEditorPreferences(updatedPrefs);
      showToast('Preferences Saved', 'Editor settings updated successfully.', 'success');
    });
  }

  // Fetch and display active AI Providers & GitHub status
  loadAIProviders();
  loadGitHubStatus();
});

async function loadAIProviders() {
  const container = document.getElementById('ai-providers-status-list');
  if (!container) return;

  try {
    const { apiRequest } = await import('./api.js');
    const res = await apiRequest('/ai/providers');
    const data = res.data;

    if (!data || !data.providers) return;

    let html = '';
    data.providers.forEach(p => {
      const isConfigured = p.configured;
      const statusBadge = isConfigured
        ? '<span class="badge badge-success">API Key Active</span>'
        : '<span class="badge badge-warning">Simulated Fallback</span>';

      html += `
        <div class="settings-row">
          <div class="settings-row-info">
            <span class="settings-row-label">${p.name}</span>
            <span class="settings-row-desc">Default Model: <code>${p.defaultModel}</code> | Available: ${p.availableModels.join(', ')}</span>
          </div>
          ${statusBadge}
        </div>
      `;
    });

    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = `
      <div class="settings-row">
        <div class="settings-row-info">
          <span class="settings-row-label text-muted">Offline / Unauthenticated</span>
          <span class="settings-row-desc">Log in to view live AI provider availability.</span>
        </div>
        <span class="badge badge-neutral">Offline</span>
      </div>
    `;
  }
}

async function loadGitHubStatus() {
  const labelEl = document.getElementById('github-account-status-label');
  const descEl = document.getElementById('github-account-status-desc');
  const actionBox = document.getElementById('github-account-action-box');

  if (!labelEl || !actionBox) return;

  try {
    const { gitHubClient } = await import('./github.js');
    const res = await gitHubClient.getStatus();
    const status = res.data;

    if (status.connected) {
      if (labelEl) labelEl.textContent = `Connected as @${status.username}`;
      if (descEl) descEl.textContent = `GitHub account linked. Access token encrypted with AES-256-GCM.`;
      actionBox.innerHTML = `
        <div class="flex items-center gap-2">
          <span class="badge badge-success">Connected</span>
          <button class="btn btn-sm btn-ghost text-danger" id="settings-github-disconnect-btn">Disconnect</button>
        </div>
      `;

      document.getElementById('settings-github-disconnect-btn')?.addEventListener('click', async () => {
        try {
          await gitHubClient.disconnect();
          showToast('Disconnected', 'GitHub account unlinked successfully.', 'info');
          loadGitHubStatus();
        } catch (err) {
          showToast('Disconnect Failed', err.message, 'danger');
        }
      });
    } else {
      if (labelEl) labelEl.textContent = 'Not Connected';
      if (descEl) descEl.textContent = 'Link your GitHub account to enable repository clone, push, and pull request features.';
      actionBox.innerHTML = `
        <button class="btn btn-sm btn-primary" id="settings-github-connect-btn">Connect GitHub Account</button>
      `;

      document.getElementById('settings-github-connect-btn')?.addEventListener('click', async () => {
        try {
          const oauthRes = await gitHubClient.startOAuth();
          if (oauthRes && oauthRes.data && oauthRes.data.authorizationUrl) {
            window.location.href = oauthRes.data.authorizationUrl;
          }
        } catch (err) {
          showToast('OAuth Error', err.message, 'danger');
        }
      });
    }
  } catch (err) {
    if (labelEl) labelEl.textContent = 'GitHub Connection Status Unavailable';
    if (descEl) descEl.textContent = 'Sign in to manage GitHub OAuth configurations.';
    if (actionBox) actionBox.innerHTML = '<span class="badge badge-neutral">Offline</span>';
  }
}

