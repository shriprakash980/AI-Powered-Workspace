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
});
