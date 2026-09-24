// DevPilot AI — Frontend Quality Verification Suite (Node.js)

const fs = require('fs');
const path = require('path');

const frontendDir = path.join(__dirname, '..', 'frontend');
const pages = [
  'index.html',
  'login.html',
  'register.html',
  'dashboard.html',
  'workspace.html',
  'settings.html',
  'profile.html',
  '404.html'
];

const cssFiles = [
  'variables.css',
  'reset.css',
  'animations.css',
  'global.css',
  'components.css',
  'navbar.css',
  'sidebar.css',
  'landing.css',
  'auth.css',
  'dashboard.css',
  'workspace.css',
  'settings.css',
  'profile.css',
  'responsive.css'
];

const jsFiles = [
  'config.js',
  'storage.js',
  'utils.js',
  'api.js',
  'components.js',
  'navigation.js',
  'landing.js',
  'login.js',
  'register.js',
  'dashboard.js',
  'editor.js',
  'workspace.js',
  'settings.js',
  'profile.js'
];

let allPassed = true;

console.log('\n==========================================');
console.log(' DevPilot AI — Frontend Verification Suite');
console.log('==========================================\n');

// 1. Check HTML Pages
console.log('[1] Verifying HTML Pages:');
for (const page of pages) {
  const p = path.join(frontendDir, page);
  if (fs.existsSync(p)) {
    console.log(`  ✓ ${page} exists (${fs.statSync(p).size} bytes)`);
  } else {
    console.error(`  ✗ MISSING: ${page}`);
    allPassed = false;
  }
}

// 2. Check CSS Files
console.log('\n[2] Verifying CSS Modules:');
for (const css of cssFiles) {
  const p = path.join(frontendDir, 'css', css);
  if (fs.existsSync(p)) {
    console.log(`  ✓ css/${css} exists (${fs.statSync(p).size} bytes)`);
  } else {
    console.error(`  ✗ MISSING: css/${css}`);
    allPassed = false;
  }
}

// 3. Check JS Files
console.log('\n[3] Verifying JavaScript Modules:');
for (const js of jsFiles) {
  const p = path.join(frontendDir, 'js', js);
  if (fs.existsSync(p)) {
    console.log(`  ✓ js/${js} exists (${fs.statSync(p).size} bytes)`);
  } else {
    console.error(`  ✗ MISSING: js/${js}`);
    allPassed = false;
  }
}

// 4. Duplicate ID Check across HTML pages
console.log('\n[4] Scanning for duplicate HTML element IDs:');
const idRegex = /id=["']([^"']+)["']/g;

for (const page of pages) {
  const p = path.join(frontendDir, page);
  if (!fs.existsSync(p)) continue;

  const content = fs.readFileSync(p, 'utf-8');
  const ids = [];
  let match;
  while ((match = idRegex.exec(content)) !== null) {
    ids.push(match[1]);
  }

  const counts = {};
  const duplicates = [];
  for (const id of ids) {
    counts[id] = (counts[id] || 0) + 1;
    if (counts[id] === 2) {
      duplicates.push(id);
    }
  }

  if (duplicates.length > 0) {
    console.error(`  ✗ ${page} has duplicate IDs: ${duplicates.join(', ')}`);
    allPassed = false;
  } else {
    console.log(`  ✓ ${page} has unique IDs (${ids.length} IDs checked)`);
  }
}

// 5. Check CSS & JS references in HTML pages
console.log('\n[5] Checking Asset & Script links in HTML pages:');
const linkRegex = /<link[^>]+href=["']([^"']+)["']/g;
const scriptRegex = /<script[^>]+src=["']([^"']+)["']/g;

for (const page of pages) {
  const p = path.join(frontendDir, page);
  if (!fs.existsSync(p)) continue;
  const content = fs.readFileSync(p, 'utf-8');

  let match;
  while ((match = linkRegex.exec(content)) !== null) {
    const href = match[1];
    if (!href.startsWith('http') && !href.startsWith('//') && !href.startsWith('data:')) {
      const target = path.join(frontendDir, href);
      if (!fs.existsSync(target)) {
        console.error(`  ✗ ${page}: broken stylesheet link -> ${href}`);
        allPassed = false;
      }
    }
  }

  while ((match = scriptRegex.exec(content)) !== null) {
    const src = match[1];
    if (!src.startsWith('http') && !src.startsWith('//') && !src.startsWith('data:')) {
      const target = path.join(frontendDir, src);
      if (!fs.existsSync(target)) {
        console.error(`  ✗ ${page}: broken script link -> ${src}`);
        allPassed = false;
      }
    }
  }
}
console.log('  ✓ All local CSS and JS references verified');

console.log('\n------------------------------------------');
if (allPassed) {
  console.log('✓ ALL FRONTEND QUALITY CHECKS PASSED (0 ERRORS)');
  process.exit(0);
} else {
  console.error('✗ SOME CHECKS FAILED');
  process.exit(1);
}
