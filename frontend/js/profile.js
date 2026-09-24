/**
 * DevPilot AI — Profile Page Controller
 */

import { showToast } from './utils.js';
import { setButtonLoading } from './components.js';

document.addEventListener('DOMContentLoaded', () => {
  const profileForm = document.getElementById('profile-form');
  const passwordForm = document.getElementById('change-password-form');

  if (profileForm) {
    profileForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const saveBtn = profileForm.querySelector('button[type="submit"]');
      setButtonLoading(saveBtn, true, 'Updating Profile...');

      setTimeout(() => {
        setButtonLoading(saveBtn, false);
        showToast('Profile Updated', 'Your profile details have been saved.', 'success');
      }, 700);
    });
  }

  if (passwordForm) {
    passwordForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const updateBtn = passwordForm.querySelector('button[type="submit"]');
      setButtonLoading(updateBtn, true, 'Changing Password...');

      setTimeout(() => {
        setButtonLoading(updateBtn, false);
        passwordForm.reset();
        showToast('Password Changed', 'Security credentials updated.', 'success');
      }, 800);
    });
  }
});
