/**
 * DevPilot AI — Navigation & Shell Controller
 */

import { storage } from './storage.js';

export function initNavigation() {
  // 1. Enforce Protected Route Guard
  checkAuthGuard();

  // 2. Initialize Theme from Storage
  const currentTheme = storage.getTheme();
  document.documentElement.setAttribute('data-theme', currentTheme);

  // 3. Setup Theme Toggle Triggers
  const themeToggles = document.querySelectorAll('[data-theme-toggle]');
  themeToggles.forEach(toggle => {
    toggle.addEventListener('click', () => {
      const active = document.documentElement.getAttribute('data-theme') || 'dark';
      const next = active === 'dark' ? 'light' : 'dark';
      storage.setTheme(next);
      updateThemeIcons(next);
    });
  });
  updateThemeIcons(currentTheme);

  // 4. Mobile Navbar Menu Toggle
  const mobileToggle = document.querySelector('.mobile-nav-toggle');
  const mobileMenu = document.querySelector('.mobile-nav-menu');
  if (mobileToggle && mobileMenu) {
    mobileToggle.addEventListener('click', () => {
      mobileMenu.classList.toggle('active');
    });
  }

  // 5. Dashboard / Shell Sidebar Toggle
  const sidebar = document.querySelector('.sidebar');
  const sidebarToggles = document.querySelectorAll('[data-sidebar-toggle]');
  if (sidebar && sidebarToggles.length > 0) {
    sidebarToggles.forEach(btn => {
      btn.addEventListener('click', () => {
        sidebar.classList.toggle('open');
      });
    });
  }

  // 6. Highlight Active Route
  highlightActiveLinks();

  // 7. Wire global logout triggers if present
  const logoutButtons = document.querySelectorAll('[data-action="logout"], #sidebar-logout-btn');
  logoutButtons.forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      storage.clearAuth();
      window.location.href = 'login.html';
    });
  });
}

function checkAuthGuard() {
  const currentPath = window.location.pathname.split('/').pop() || '';
  const protectedPages = ['dashboard.html', 'workspace.html', 'settings.html', 'profile.html'];

  if (protectedPages.includes(currentPath) && !storage.isAuthenticated()) {
    console.info('[DevPilot] Unauthenticated access to protected page. Redirecting to login.html');
    window.location.href = 'login.html';
  }
}

function updateThemeIcons(theme) {
  const icons = document.querySelectorAll('[data-theme-icon]');
  icons.forEach(icon => {
    if (theme === 'light') {
      icon.innerHTML = `<svg class="icon icon-sm" viewBox="0 0 24 24"><path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/></svg>`;
    } else {
      icon.innerHTML = `<svg class="icon icon-sm" viewBox="0 0 24 24"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>`;
    }
  });
}

function highlightActiveLinks() {
  const currentPath = window.location.pathname.split('/').pop() || 'index.html';
  const navLinks = document.querySelectorAll('.nav-link, .sidebar-link');
  navLinks.forEach(link => {
    const href = link.getAttribute('href');
    if (href && (href === currentPath || href.endsWith(currentPath))) {
      link.classList.add('active');
    }
  });
}

if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', initNavigation);
}
