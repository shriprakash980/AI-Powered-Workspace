/**
 * DevPilot AI — Login Page Controller
 */

import { showToast } from './utils.js';
import { setButtonLoading } from './components.js';
import { apiRequest } from './api.js';
import { storage } from './storage.js';

document.addEventListener('DOMContentLoaded', () => {
  // If user is already authenticated, redirect to dashboard
  if (storage.isAuthenticated()) {
    window.location.href = 'dashboard.html';
    return;
  }

  const loginForm = document.getElementById('login-form');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const togglePasswordBtn = document.getElementById('toggle-password-btn');
  const submitBtn = document.getElementById('login-submit-btn');
  const githubBtn = document.getElementById('github-login-btn');

  // 1. Password Visibility Toggle
  if (togglePasswordBtn && passwordInput) {
    togglePasswordBtn.addEventListener('click', () => {
      const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
      passwordInput.setAttribute('type', type);
      togglePasswordBtn.innerHTML = type === 'password'
        ? `<svg class="icon icon-sm" viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`
        : `<svg class="icon icon-sm" viewBox="0 0 24 24"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`;
    });
  }

  // 2. GitHub Login (UI Only for Phase 2/5)
  if (githubBtn) {
    githubBtn.addEventListener('click', () => {
      showToast('GitHub OAuth', 'GitHub OAuth authentication is scheduled for Phase 12.', 'info');
    });
  }

  // 3. Form Validation and Submission
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      let isValid = true;
      const email = emailInput.value.trim();
      const password = passwordInput.value;

      // Validate Email
      const emailError = document.getElementById('email-error');
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!email) {
        setFieldError(emailInput, emailError, 'Email address is required');
        isValid = false;
      } else if (!emailRegex.test(email)) {
        setFieldError(emailInput, emailError, 'Please enter a valid email address');
        isValid = false;
      } else {
        clearFieldError(emailInput, emailError);
      }

      // Validate Password
      const passwordError = document.getElementById('password-error');
      if (!password) {
        setFieldError(passwordInput, passwordError, 'Password is required');
        isValid = false;
      } else if (password.length < 6) {
        setFieldError(passwordInput, passwordError, 'Password must be at least 6 characters');
        isValid = false;
      } else {
        clearFieldError(passwordInput, passwordError);
      }

      if (!isValid) return;

      setButtonLoading(submitBtn, true, 'Verifying Credentials...');

      try {
        const response = await apiRequest('/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password })
        });

        if (response && response.data) {
          storage.setAuth(response.data);
          showToast('Welcome Back!', 'Authentication successful. Redirecting...', 'success');
          setTimeout(() => {
            window.location.href = 'dashboard.html';
          }, 600);
        } else {
          throw new Error('Invalid response structure from authentication server');
        }
      } catch (err) {
        setButtonLoading(submitBtn, false);
        const errMsg = err.message || 'Invalid email or password. Please try again.';
        showToast('Authentication Failed', errMsg, 'error');
        setFieldError(passwordInput, passwordError, errMsg);
      }
    });
  }

  function setFieldError(input, errorElement, message) {
    input.classList.add('is-invalid');
    if (errorElement) {
      errorElement.textContent = message;
      errorElement.classList.add('visible');
    }
  }

  function clearFieldError(input, errorElement) {
    input.classList.remove('is-invalid');
    if (errorElement) {
      errorElement.textContent = '';
      errorElement.classList.remove('visible');
    }
  }
});
