/**
 * DevPilot AI — Landing Page Scripts
 */

import { showToast } from './utils.js';

document.addEventListener('DOMContentLoaded', () => {
  // 1. Hero IDE Mockup Tab Switching
  const mockTabs = document.querySelectorAll('.ide-mockup .ide-tab');
  const codeContent = document.getElementById('mock-code-content');

  const mockFiles = {
    'index.html': `&lt;!DOCTYPE html&gt;
&lt;<span class="syn-tag">html</span> lang="en"&gt;
&lt;<span class="syn-tag">head</span>&gt;
  &lt;<span class="syn-tag">meta</span> charset="UTF-8"&gt;
  &lt;<span class="syn-tag">title</span>&gt;DevPilot Application&lt;/<span class="syn-tag">title</span>&gt;
  &lt;<span class="syn-tag">link</span> rel="stylesheet" href="style.css"&gt;
&lt;/<span class="syn-tag">head</span>&gt;
&lt;<span class="syn-tag">body</span>&gt;
  &lt;<span class="syn-tag">div</span> id="app"&gt;&lt;/<span class="syn-tag">div</span>&gt;
  &lt;<span class="syn-tag">script</span> src="app.js"&gt;&lt;/<span class="syn-tag">script</span>&gt;
&lt;/<span class="syn-tag">body</span>&gt;
&lt;/<span class="syn-tag">html</span>&gt;`,

    'style.css': `<span class="syn-keyword">:root</span> {
  <span class="syn-tag">--primary</span>: <span class="syn-string">#6366f1</span>;
  <span class="syn-tag">--bg-dark</span>: <span class="syn-string">#080b10</span>;
}

<span class="syn-tag">body</span> {
  <span class="syn-keyword">margin</span>: 0;
  <span class="syn-keyword">background-color</span>: <span class="syn-func">var</span>(--bg-dark);
  <span class="syn-keyword">font-family</span>: <span class="syn-string">sans-serif</span>;
}`,

    'app.js': `<span class="syn-comment">// DevPilot Autonomous Agent Engine</span>
<span class="syn-keyword">import</span> { Workspace } <span class="syn-keyword">from</span> <span class="syn-string">'./core.js'</span>;

<span class="syn-keyword">async function</span> <span class="syn-func">bootstrapProject</span>() {
  <span class="syn-keyword">const</span> env = <span class="syn-keyword">await</span> Workspace.<span class="syn-func">initialize</span>({
    aiProvider: <span class="syn-string">'Gemini-1.5-Pro'</span>,
    sandboxed: <span class="syn-keyword">true</span>
  });
  console.<span class="syn-func">log</span>(<span class="syn-string">'Workspace active on container #320'</span>);
}
<span class="syn-func">bootstrapProject</span>();`
  };

  mockTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      mockTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const fileName = tab.dataset.file;
      if (fileName && mockFiles[fileName] && codeContent) {
        codeContent.innerHTML = mockFiles[fileName];
      }
    });
  });

  // 2. Demo AI Pill Action on Hero
  const demoAiBtn = document.getElementById('demo-ai-action');
  if (demoAiBtn) {
    demoAiBtn.addEventListener('click', () => {
      showToast('AI Refactor Generated', 'Suggested 12 line performance optimization for app.js', 'success');
    });
  }
});
