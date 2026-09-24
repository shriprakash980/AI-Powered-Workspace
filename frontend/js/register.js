/**
 * DevPilot AI — Registration Page Controller
 */

import { showToast } from './utils.js';
import { setButtonLoading } from './components.js';
import { apiRequest } from './api.js';

document.addEventListener('DOMContentLoaded', () => {
  const registerForm = document.getElementById('register-form');
  const nameInput = document.getElementById('full-name');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const confirmPasswordInput = document.getElementById('confirm-password');
  const termsCheckbox = document.getElementById('terms');
  const submitBtn = document.getElementById('register-submit-btn');
  const strengthMeter = document.getElementById('strength-meter');
  const strengthText = document.getElementById('strength-text');

  // 1. Password Strength Evaluation
  if (passwordInput && strengthMeter && strengthText) {
    passwordInput.addEventListener('input', () => {
      const val = passwordInput.value;
      strengthMeter.className = 'strength-meter';

      if (!val) {
        strengthText.textContent = 'Enter password';
        return;
      }

      let score = 0;
      if (val.length >= 8) score++;
      if (/[A-Z]/.test(val) && /[a-z]/.test(val)) score++;
      if (/[0-9]/.test(val) || /[^A-Za-z0-9]/.test(val)) score++;

      if (score === 1) {
        strengthMeter.classList.add('strength-weak');
        strengthText.textContent = 'Weak (add numbers & symbols)';
      } else if (score === 2) {
        strengthMeter.classList.add('strength-medium');
        strengthText.textContent = 'Medium (good password)';
      } else if (score >= 3) {
        strengthMeter.classList.add('strength-strong');
        strengthText.textContent = 'Strong (secure)';
      }
    });
  }

  // 2. Form Submission & Comprehensive Validation
  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      let isValid = true;
      const fullName = nameInput.value.trim();
      const email = emailInput.value.trim();
      const password = passwordInput.value;
      const confirmPassword = confirmPasswordInput.value;

      // Validate Name
      const nameError = document.getElementById('name-error');
      if (!fullName) {
        setFieldError(nameInput, nameError, 'Full name is required');
        isValid = false;
      } else {
        clearFieldError(nameInput, nameError);
      }

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
      } else if (password.length < 8) {
        setFieldError(passwordInput, passwordError, 'Password must be at least 8 characters');
        isValid = false;
      } else {
        clearFieldError(passwordInput, passwordError);
      }

      // Validate Confirm Password
      const confirmError = document.getElementById('confirm-password-error');
      if (!confirmPassword) {
        setFieldError(confirmPasswordInput, confirmError, 'Please confirm your password');
        isValid = false;
      } else if (password !== confirmPassword) {
        setFieldError(confirmPasswordInput, confirmError, 'Passwords do not match');
        isValid = false;
      } else {
        clearFieldError(confirmPasswordInput, confirmError);
      }

      // Validate Terms Checkbox
      const termsError = document.getElementById('terms-error');
      if (!termsCheckbox.checked) {
        termsError.textContent = 'You must accept the terms of service';
        termsError.classList.add('visible');
        isValid = false;
      } else {
        termsError.textContent = '';
        termsError.classList.remove('visible');
      }

      if (!isValid) return;

      setButtonLoading(submitBtn, true, 'Creating Account in Database...');

      try {
        const response = await apiRequest('/auth/register', {
          method: 'POST',
          body: JSON.stringify({ fullName, email, password })
        });

        if (response && response.success) {
          showToast('Account Created!', 'Registration successful. Redirecting to sign in...', 'success');
          setTimeout(() => {
            window.location.href = 'login.html';
          }, 800);
        } else {
          throw new Error('Registration failed. Please try again.');
        }
      } catch (err) {
        setButtonLoading(submitBtn, false);
        const errMsg = err.message || 'An error occurred during registration.';
        showToast('Registration Error', errMsg, 'error');
        if (errMsg.toLowerCase().includes('email')) {
          setFieldError(emailInput, emailError, errMsg);
        }
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
