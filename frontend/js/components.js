/**
 * DevPilot AI — Reusable UI Components Engine
 * Handles modals, tabs, dropdowns, and button state management.
 */

/**
 * Opens a modal dialog with accessibility focus management
 * @param {string} modalId - Element ID of the modal backdrop
 */
export function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;

  modal.classList.add('active');
  modal.setAttribute('aria-hidden', 'false');
  document.body.style.overflow = 'hidden';

  // Focus first input if available
  const firstInput = modal.querySelector('input, textarea, select, button.btn-primary');
  if (firstInput) {
    setTimeout(() => firstInput.focus(), 50);
  }
}

/**
 * Closes a modal dialog
 * @param {string} modalId - Element ID of the modal backdrop
 */
export function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;

  modal.classList.remove('active');
  modal.setAttribute('aria-hidden', 'true');
  document.body.style.overflow = '';
}

/**
 * Initializes global event listeners for all modals
 */
export function initModalSystem() {
  // Backdrop click and close button listeners
  document.addEventListener('click', (e) => {
    // Check if clicked close button
    const closeBtn = e.target.closest('[data-close-modal]');
    if (closeBtn) {
      const modal = closeBtn.closest('.modal-backdrop');
      if (modal) closeModal(modal.id);
      return;
    }

    // Check if clicked outside modal-dialog on the backdrop directly
    if (e.target.classList.contains('modal-backdrop')) {
      closeModal(e.target.id);
    }
  });

  // ESC key dismisses active modal
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      const activeModal = document.querySelector('.modal-backdrop.active');
      if (activeModal) {
        closeModal(activeModal.id);
      }
    }
  });
}

/**
 * Toggles a button's loading state with a spinner
 * @param {HTMLButtonElement} button 
 * @param {boolean} isLoading 
 * @param {string} loadingText 
 */
export function setButtonLoading(button, isLoading, loadingText = 'Processing...') {
  if (!button) return;

  if (isLoading) {
    button.dataset.originalHtml = button.innerHTML;
    button.disabled = true;
    button.innerHTML = `<span class="spinner"></span> <span>${loadingText}</span>`;
  } else {
    button.disabled = false;
    if (button.dataset.originalHtml) {
      button.innerHTML = button.dataset.originalHtml;
    }
  }
}

/**
 * Initializes tab containers marked with data-tabs
 */
export function initTabs(containerSelector = '.tabs-container') {
  const containers = document.querySelectorAll(containerSelector);
  containers.forEach(container => {
    const tabButtons = container.querySelectorAll('.tab-btn, .bottom-tab-btn');
    const panes = container.querySelectorAll('.tab-pane, .panel-pane');

    tabButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const targetId = btn.dataset.tabTarget;
        if (!targetId) return;

        // Toggle active states on buttons
        tabButtons.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        // Toggle active states on panes
        panes.forEach(pane => {
          if (pane.id === targetId) {
            pane.classList.add('active');
          } else {
            pane.classList.remove('active');
          }
        });
      });
    });
  });
}

// Auto-initialize standard component listeners on load
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    initModalSystem();
    initTabs();
  });
}
