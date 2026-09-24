/**
 * DevPilot AI — AI Coding Assistant & Multi-Provider Integration (Phase 8)
 * Handles OpenAI, Google Gemini, Anthropic Claude, SSE streaming, Code Actions & Diff Reviews
 */

import { apiRequest } from './api.js';
import { storage } from './storage.js';
import { showToast, escapeHtml } from './utils.js';
import { showDiff, hideDiff, toggleDiffSideBySide } from './editor.js';

let providersData = null;
let activeConversationId = null;
let isGenerating = false;
let currentAbortController = null;
let editorBridge = null;

/**
 * Initialize AI Assistant Module
 * @param {object} bridge - { getActiveFile, getSelectedCode, replaceSelection, setDirty, getProjectId }
 */
export async function initAI(bridge) {
  editorBridge = bridge;

  bindElements();
  initPhase9Features();
  await loadProviders();
  await loadConversations();
  updateContextBadge();

  // Periodically synchronize selection status if editor changes
  setInterval(updateContextBadge, 1000);
}

/**
 * Bind DOM Event Handlers
 */
function bindElements() {
  const sendBtn = document.getElementById('ai-send-btn');
  const stopBtn = document.getElementById('ai-stop-btn');
  const input = document.getElementById('ai-input-box');
  const providerSelect = document.getElementById('ai-provider-select');
  const modelSelect = document.getElementById('ai-model-select');
  const convSelect = document.getElementById('ai-conversation-select');
  const newChatBtn = document.getElementById('ai-new-chat-btn');
  const deleteChatBtn = document.getElementById('ai-delete-chat-btn');

  if (sendBtn && input) {
    const handleSend = () => {
      const text = input.value.trim();
      if (!text || isGenerating) return;
      input.value = '';
      sendChatMessage(text);
    };

    sendBtn.addEventListener('click', handleSend);
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
      }
    });
  }

  if (stopBtn) {
    stopBtn.addEventListener('click', () => {
      if (currentAbortController) {
        currentAbortController.abort();
        currentAbortController = null;
      }
      setGenerating(false);
      showToast('AI Generation Stopped', 'Streaming was halted by user.', 'info');
    });
  }

  if (providerSelect) {
    providerSelect.addEventListener('change', () => {
      updateModelDropdown();
    });
  }

  if (convSelect) {
    convSelect.addEventListener('change', (e) => {
      const id = e.target.value;
      if (id) {
        switchConversation(id);
      } else {
        startNewConversation();
      }
    });
  }

  if (newChatBtn) {
    newChatBtn.addEventListener('click', () => {
      startNewConversation();
    });
  }

  if (deleteChatBtn) {
    deleteChatBtn.addEventListener('click', () => {
      deleteCurrentConversation();
    });
  }

  // Quick Action Chips (Explain, Fix, Refactor, Tests, Debug, Document, Generate)
  const actionChips = document.querySelectorAll('.ai-action-chip');
  actionChips.forEach((chip) => {
    chip.addEventListener('click', () => {
      const action = chip.dataset.action;
      if (action) {
        handleQuickAction(action);
      }
    });
  });
}

/**
 * Load Available Providers and Populate Dropdowns
 */
async function loadProviders() {
  try {
    const res = await apiRequest('/ai/providers');
    providersData = res.data;

    const providerSelect = document.getElementById('ai-provider-select');
    if (!providerSelect || !providersData) return;

    providerSelect.innerHTML = '';
    providersData.providers.forEach((prov) => {
      const opt = document.createElement('option');
      opt.value = prov.id;
      opt.textContent = `${prov.name} ${prov.configured ? '✓' : '(Simulated)'}`;
      providerSelect.appendChild(opt);
    });

    const defaultProvider = providersData.defaultProvider || 'OPENAI';
    if ([...providerSelect.options].some((o) => o.value === defaultProvider)) {
      providerSelect.value = defaultProvider;
    }

    updateModelDropdown();
  } catch (err) {
    console.warn('Failed to load AI providers:', err);
  }
}

/**
 * Update Model Dropdown Based on Selected Provider
 */
function updateModelDropdown() {
  const providerSelect = document.getElementById('ai-provider-select');
  const modelSelect = document.getElementById('ai-model-select');
  if (!providerSelect || !modelSelect || !providersData) return;

  const currentProv = providerSelect.value;
  const provObj = providersData.providers.find((p) => p.id === currentProv);

  modelSelect.innerHTML = '';
  if (provObj && provObj.availableModels) {
    provObj.availableModels.forEach((m) => {
      const opt = document.createElement('option');
      opt.value = m;
      opt.textContent = m;
      if (m === provObj.defaultModel) opt.selected = true;
      modelSelect.appendChild(opt);
    });
  }
}

/**
 * Load User Conversations for Active Project
 */
async function loadConversations() {
  const convSelect = document.getElementById('ai-conversation-select');
  if (!convSelect) return;

  const projectId = editorBridge?.getProjectId();
  const url = projectId ? `/ai/conversations?projectId=${projectId}` : '/ai/conversations';

  try {
    const res = await apiRequest(url);
    const conversations = res.data || [];

    convSelect.innerHTML = '<option value="">-- New Thread --</option>';
    conversations.forEach((c) => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = `${c.title} (${c.provider})`;
      convSelect.appendChild(opt);
    });

    if (activeConversationId) {
      convSelect.value = activeConversationId;
    }
  } catch (err) {
    console.warn('Could not load conversations:', err);
  }
}

/**
 * Switch Active Conversation and Render Thread
 */
async function switchConversation(conversationId) {
  activeConversationId = conversationId;
  const box = document.getElementById('ai-messages-container');
  if (!box) return;

  try {
    const res = await apiRequest(`/ai/conversations/${conversationId}`);
    const data = res.data;

    box.innerHTML = '';
    if (data.messages && data.messages.length > 0) {
      data.messages.forEach((msg) => {
        appendChatMessage(msg.role, msg.content);
      });
    } else {
      appendChatMessage('assistant', `Resumed thread: "${data.title}". How can I help you proceed?`);
    }
  } catch (err) {
    showToast('Failed to Load Chat', err.message, 'error');
  }
}

/**
 * Start New Empty Conversation
 */
function startNewConversation() {
  activeConversationId = null;
  const convSelect = document.getElementById('ai-conversation-select');
  if (convSelect) convSelect.value = '';

  const box = document.getElementById('ai-messages-container');
  if (box) {
    box.innerHTML = `
      <div class="ai-chat-bubble assistant">
        <strong>DevPilot AI</strong>
        <p>New conversation started. Select code in the editor to run actions or ask questions directly.</p>
      </div>
    `;
  }
  showToast('New Chat Session', 'Ready for new instructions.', 'info');
}

/**
 * Delete Active Conversation
 */
async function deleteCurrentConversation() {
  if (!activeConversationId) {
    showToast('No Active Thread', 'You are already in an unsaved new chat.', 'info');
    return;
  }

  if (!confirm('Are you sure you want to delete this chat thread?')) {
    return;
  }

  try {
    await apiRequest(`/ai/conversations/${activeConversationId}`, { method: 'DELETE' });
    showToast('Thread Deleted', 'Conversation has been removed.', 'success');
    activeConversationId = null;
    await loadConversations();
    startNewConversation();
  } catch (err) {
    showToast('Delete Failed', err.message, 'error');
  }
}

/**
 * Update Context Bar (Active File & Selection)
 */
export function updateContextBadge() {
  const fileBadge = document.getElementById('ai-active-file-badge');
  const selBadge = document.getElementById('ai-selection-badge');
  if (!editorBridge) return;

  const activeFile = editorBridge.getActiveFile ? editorBridge.getActiveFile() : null;
  if (fileBadge) {
    fileBadge.textContent = activeFile ? `📄 ${activeFile.name}` : '📄 No file open';
    fileBadge.title = activeFile ? activeFile.path : '';
  }

  const selectedCode = editorBridge.getSelectedCode ? editorBridge.getSelectedCode() : '';
  if (selBadge) {
    if (selectedCode && selectedCode.trim().length > 0) {
      const lines = selectedCode.split('\n').length;
      selBadge.style.display = 'inline-flex';
      selBadge.textContent = `✂️ Selection (${lines} lines)`;
      selBadge.title = `${selectedCode.length} characters selected`;
    } else {
      selBadge.style.display = 'none';
    }
  }
}

/**
 * Send Chat Message with Streaming Support
 */
async function sendChatMessage(promptText) {
  const provider = document.getElementById('ai-provider-select')?.value || 'OPENAI';
  const model = document.getElementById('ai-model-select')?.value || 'gpt-4o-mini';
  const activeFile = editorBridge?.getActiveFile ? editorBridge.getActiveFile() : null;
  const selectedCode = editorBridge?.getSelectedCode ? editorBridge.getSelectedCode() : '';
  const projectId = editorBridge?.getProjectId ? editorBridge.getProjectId() : null;

  appendChatMessage('user', promptText);

  const assistantBubble = appendChatMessage('assistant', '');
  const contentHolder = assistantBubble.querySelector('.ai-bubble-body');
  const cursor = document.createElement('span');
  cursor.className = 'ai-typing-cursor';
  contentHolder.appendChild(cursor);

  setGenerating(true);
  currentAbortController = new AbortController();

  let accumulatedText = '';

  const payload = {
    conversationId: activeConversationId || null,
    projectId: projectId || null,
    fileId: activeFile ? activeFile.id : null,
    message: promptText,
    provider: provider,
    model: model,
    selectedCode: selectedCode || null,
    language: activeFile ? activeFile.language : 'plaintext'
  };

  try {
    const token = storage.getAccessToken();
    const response = await fetch('/api/v1/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify(payload),
      signal: currentAbortController.signal
    });

    if (!response.ok) {
      // Stream endpoint failed or unauthorized, fallback to synchronous endpoint
      throw new Error(`Stream HTTP ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop(); // keep remainder

      for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed.startsWith('data:')) {
          const rawData = trimmed.substring(5).trim();
          if (rawData === '[DONE]') break;

          try {
            const parsed = JSON.parse(rawData);
            if (parsed.token) {
              accumulatedText += parsed.token;
              contentHolder.innerHTML = formatMarkdown(accumulatedText);
              contentHolder.appendChild(cursor);
              scrollToBottom();
            } else if (parsed.type === 'meta' && parsed.conversationId) {
              activeConversationId = parsed.conversationId;
            } else if (parsed.type === 'done') {
              if (parsed.conversationId) activeConversationId = parsed.conversationId;
            } else if (parsed.error) {
              throw new Error(parsed.error);
            }
          } catch (pe) {
            // Unparsed chunk ignored
          }
        }
      }
    }

    // Finished streaming cleanly
    if (cursor.parentNode) cursor.remove();
    contentHolder.innerHTML = formatMarkdown(accumulatedText);
    setupCodeBlockCopyButtons(contentHolder);
    await loadConversations();

  } catch (err) {
    if (err.name === 'AbortError') {
      if (cursor.parentNode) cursor.remove();
      return;
    }

    console.warn('Streaming error, falling back to sync endpoint:', err.message);
    try {
      const fallbackRes = await apiRequest('/ai/chat', {
        method: 'POST',
        body: payload
      });

      if (cursor.parentNode) cursor.remove();
      const content = fallbackRes.data.content;
      activeConversationId = fallbackRes.data.conversationId;
      contentHolder.innerHTML = formatMarkdown(content);
      setupCodeBlockCopyButtons(contentHolder);
      await loadConversations();
    } catch (fallbackErr) {
      if (cursor.parentNode) cursor.remove();
      contentHolder.innerHTML = `<span class="text-danger">⚠️ Error: ${escapeHtml(fallbackErr.message)}</span>`;
    }
  } finally {
    setGenerating(false);
    currentAbortController = null;
    scrollToBottom();
  }
}

/**
 * Handle Code Action Chips (Explain, Fix, Refactor, Tests, Debug, Document, Generate)
 */
async function handleQuickAction(action) {
  const selectedCode = editorBridge?.getSelectedCode ? editorBridge.getSelectedCode() : '';
  const activeFile = editorBridge?.getActiveFile ? editorBridge.getActiveFile() : null;

  if (action !== 'generate' && (!selectedCode || !selectedCode.trim())) {
    showToast('Selection Required', `Please highlight code in the editor to use the "${action.toUpperCase()}" action.`, 'warning');
    return;
  }

  const promptPreview = action === 'generate'
    ? 'Generate code for active file'
    : `${action.toUpperCase()}: ${selectedCode.trim().split('\n')[0].substring(0, 45)}...`;

  appendChatMessage('user', `AI Action: ${action.toUpperCase()}`);

  const assistantBubble = appendChatMessage('assistant', '');
  const contentHolder = assistantBubble.querySelector('.ai-bubble-body');
  contentHolder.innerHTML = `<div class="flex items-center gap-2"><span class="spinner spinner-sm"></span><span>Running AI ${action}...</span></div>`;

  setGenerating(true);

  const provider = document.getElementById('ai-provider-select')?.value || 'OPENAI';
  const model = document.getElementById('ai-model-select')?.value || null;

  const payload = {
    projectId: editorBridge?.getProjectId ? editorBridge.getProjectId() : null,
    fileId: activeFile ? activeFile.id : null,
    filePath: activeFile ? activeFile.path : null,
    fileContent: activeFile ? activeFile.content : null,
    selectedCode: selectedCode || (activeFile ? activeFile.content : ''),
    instruction: `Execute ${action} operation.`,
    language: activeFile ? activeFile.language : 'plaintext',
    provider: provider,
    model: model
  };

  try {
    const endpoint = `/ai/code/${action.toLowerCase()}`;
    const res = await apiRequest(endpoint, {
      method: 'POST',
      body: payload
    });

    const data = res.data;
    contentHolder.innerHTML = formatMarkdown(data.explanation || data.suggestedCode);
    setupCodeBlockCopyButtons(contentHolder);

    // If replacement suggested and diff generated, render interactive Diff Card
    if (data.suggestedCode && data.diff) {
      const diffCard = renderDiffCard(data, activeFile);
      contentHolder.appendChild(diffCard);
    }

  } catch (err) {
    contentHolder.innerHTML = `<span class="text-danger">⚠️ Action failed: ${escapeHtml(err.message)}</span>`;
    showToast('AI Action Failed', err.message, 'error');
  } finally {
    setGenerating(false);
    scrollToBottom();
  }
}

/**
 * Render Interactive Diff Review Card (Apply / Reject)
 */
function renderDiffCard(data, activeFile) {
  const card = document.createElement('div');
  card.className = 'ai-diff-card';

  const fileName = activeFile ? activeFile.name : 'Selection';

  let diffLinesHtml = '';
  const lines = data.diff.split('\n');
  lines.forEach((l) => {
    if (l.startsWith('+') && !l.startsWith('+++')) {
      diffLinesHtml += `<div class="diff-line-add">${escapeHtml(l)}</div>`;
    } else if (l.startsWith('-') && !l.startsWith('---')) {
      diffLinesHtml += `<div class="diff-line-remove">${escapeHtml(l)}</div>`;
    } else {
      diffLinesHtml += `<div style="padding: 1px 8px; color: var(--color-text-muted);">${escapeHtml(l)}</div>`;
    }
  });

  card.innerHTML = `
    <div class="diff-header">
      <span><strong>Suggested Changes:</strong> ${escapeHtml(fileName)}</span>
      <span class="badge badge-primary">${escapeHtml(data.provider)} / ${escapeHtml(data.model)}</span>
    </div>
    <div class="diff-content" style="max-height: 220px; overflow-y: auto;">
      ${diffLinesHtml}
    </div>
    <div class="diff-actions">
      <button class="btn btn-xs btn-primary apply-diff-btn">
        <span>✓ Apply Change</span>
      </button>
      <button class="btn btn-xs btn-ghost text-danger reject-diff-btn">
        <span>✕ Reject</span>
      </button>
    </div>
  `;

  const applyBtn = card.querySelector('.apply-diff-btn');
  const rejectBtn = card.querySelector('.reject-diff-btn');

  applyBtn.addEventListener('click', () => {
    if (editorBridge?.replaceSelection) {
      editorBridge.replaceSelection(data.suggestedCode);
      if (editorBridge.setDirty) editorBridge.setDirty(true);
      showToast('Change Applied', 'Updated editor buffer. Press Ctrl+S to save changes.', 'success');
      card.querySelector('.diff-actions').innerHTML = '<span class="text-xs text-success">✓ Applied to editor buffer (Unsaved: Press Ctrl+S)</span>';
    }
  });

  rejectBtn.addEventListener('click', () => {
    card.remove();
    showToast('Suggestion Dismissed', 'Proposed diff was rejected.', 'info');
  });

  return card;
}

/**
 * Format Markdown Content Safely with Code Fences & Syntax Tags
 */
function formatMarkdown(raw) {
  if (!raw) return '';

  // 1. Extract and replace code fences ```lang ... ```
  const codeBlocks = [];
  const withTokens = raw.replace(/```([a-zA-Z0-9_\-#+]*)\n([\s\S]*?)```/g, (match, lang, code) => {
    const idx = codeBlocks.length;
    codeBlocks.push({ lang: lang || 'code', code });
    return `___CODE_BLOCK_${idx}___`;
  });

  // 2. Escape HTML on prose
  let safeHtml = escapeHtml(withTokens);

  // 3. Inline code `...`
  safeHtml = safeHtml.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

  // 4. Bold **...**
  safeHtml = safeHtml.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

  // 5. Italic *...*
  safeHtml = safeHtml.replace(/\*([^*]+)\*/g, '<em>$1</em>');

  // 6. Headers ### / ## / #
  safeHtml = safeHtml.replace(/^### (.*$)/gim, '<h4 style="margin: 6px 0; font-size: 0.85rem;">$1</h4>');
  safeHtml = safeHtml.replace(/^## (.*$)/gim, '<h3 style="margin: 8px 0; font-size: 0.9rem;">$1</h3>');
  safeHtml = safeHtml.replace(/^# (.*$)/gim, '<h2 style="margin: 10px 0; font-size: 0.95rem;">$1</h2>');

  // 7. Bullet lists
  safeHtml = safeHtml.replace(/^\s*-\s+(.*$)/gim, '• $1<br>');

  // 8. Newlines to <br>
  safeHtml = safeHtml.replace(/\n/g, '<br>');

  // 9. Re-insert code blocks
  safeHtml = safeHtml.replace(/___CODE_BLOCK_(\d+)___/g, (_, index) => {
    const block = codeBlocks[Number(index)];
    const escapedCode = escapeHtml(block.code);
    return `
      <div class="ai-code-block">
        <div class="ai-code-block-header">
          <span>${escapeHtml(block.lang.toUpperCase())}</span>
          <button class="ai-copy-btn" data-code="${escapeHtml(block.code)}">Copy</button>
        </div>
        <pre><code>${escapedCode}</code></pre>
      </div>
    `;
  });

  return safeHtml;
}

/**
 * Setup Copy Buttons for Code Blocks
 */
function setupCodeBlockCopyButtons(container) {
  const btns = container.querySelectorAll('.ai-copy-btn');
  btns.forEach((btn) => {
    btn.addEventListener('click', async () => {
      const code = btn.getAttribute('data-code');
      if (code) {
        await navigator.clipboard.writeText(code);
        const originalText = btn.textContent;
        btn.textContent = 'Copied!';
        setTimeout(() => {
          btn.textContent = originalText;
        }, 1500);
      }
    });
  });
}

/**
 * Append Chat Bubble to Messages Box
 */
function appendChatMessage(role, text) {
  const box = document.getElementById('ai-messages-container');
  if (!box) return null;

  const bubble = document.createElement('div');
  bubble.className = `ai-chat-bubble ${role}`;

  const header = document.createElement('strong');
  header.textContent = role === 'user' ? 'Developer' : 'DevPilot AI';

  const body = document.createElement('div');
  body.className = 'ai-bubble-body';
  body.innerHTML = formatMarkdown(text);

  bubble.appendChild(header);
  bubble.appendChild(body);
  box.appendChild(bubble);

  setupCodeBlockCopyButtons(body);
  scrollToBottom();
  return bubble;
}

/**
 * Update UI Generating State
 */
function setGenerating(generating) {
  isGenerating = generating;
  const sendBtn = document.getElementById('ai-send-btn');
  const stopBtn = document.getElementById('ai-stop-btn');

  if (sendBtn) sendBtn.disabled = generating;
  if (stopBtn) stopBtn.style.display = generating ? 'inline-flex' : 'none';
}

/**
 * Scroll Messages to Bottom
 */
function scrollToBottom() {
  const box = document.getElementById('ai-messages-container');
  if (box) {
    box.scrollTop = box.scrollHeight;
  }
}

// ==========================================================================
// Phase 9: AI Context Engine, Changeset Review & Version History UI
// ==========================================================================

let activeReviewChangeset = null;

function initPhase9Features() {
  // Context Preview Modal bindings
  const aiCtxBtn = document.getElementById('ai-context-btn');
  const statusCtxBtn = document.getElementById('status-context-btn');
  const ctxModal = document.getElementById('context-preview-modal');
  const ctxCloseBtn = document.getElementById('context-modal-close-btn');
  const ctxOkBtn = document.getElementById('context-modal-ok-btn');

  const openContext = () => openContextPreviewModal();
  if (aiCtxBtn) aiCtxBtn.addEventListener('click', openContext);
  if (statusCtxBtn) statusCtxBtn.addEventListener('click', openContext);
  if (ctxCloseBtn) ctxCloseBtn.addEventListener('click', () => { if (ctxModal) ctxModal.style.display = 'none'; });
  if (ctxOkBtn) ctxOkBtn.addEventListener('click', () => { if (ctxModal) ctxModal.style.display = 'none'; });

  // Changeset Review Modal bindings
  const aiChangesetsBtn = document.getElementById('ai-changesets-btn');
  const csModal = document.getElementById('changeset-review-modal');
  const csCloseBtn = document.getElementById('changeset-modal-close-btn');
  const csCancelBtn = document.getElementById('changeset-cancel-btn');
  const csApplyBtn = document.getElementById('changeset-apply-selected-btn');
  const csRejectBtn = document.getElementById('changeset-reject-btn');
  const csSelectAll = document.getElementById('changeset-select-all');

  if (aiChangesetsBtn) aiChangesetsBtn.addEventListener('click', () => openLatestChangesetModal());
  if (csCloseBtn) csCloseBtn.addEventListener('click', () => { if (csModal) csModal.style.display = 'none'; });
  if (csCancelBtn) csCancelBtn.addEventListener('click', () => { if (csModal) csModal.style.display = 'none'; });
  if (csSelectAll) {
    csSelectAll.addEventListener('change', (e) => {
      const cbs = document.querySelectorAll('.changeset-file-cb');
      cbs.forEach(cb => { cb.checked = e.target.checked; });
    });
  }
  if (csApplyBtn) csApplyBtn.addEventListener('click', () => handleApplyChangeset());
  if (csRejectBtn) csRejectBtn.addEventListener('click', () => handleRejectChangeset());

  // Version History Modal bindings
  const statusHistoryBtn = document.getElementById('status-history-btn');
  const verModal = document.getElementById('file-version-modal');
  const verCloseBtn = document.getElementById('version-modal-close-btn');
  const verOkBtn = document.getElementById('version-modal-ok-btn');

  if (statusHistoryBtn) statusHistoryBtn.addEventListener('click', () => openVersionHistoryModal());
  if (verCloseBtn) verCloseBtn.addEventListener('click', () => { if (verModal) verModal.style.display = 'none'; });
  if (verOkBtn) verOkBtn.addEventListener('click', () => { if (verModal) verModal.style.display = 'none'; });

  // Diff Mode Banner bindings
  const diffToggleBtn = document.getElementById('diff-toggle-mode-btn');
  const diffExitBtn = document.getElementById('diff-exit-btn');
  if (diffToggleBtn) diffToggleBtn.addEventListener('click', () => toggleDiffSideBySide());
  if (diffExitBtn) diffExitBtn.addEventListener('click', () => hideDiff());
}

async function openContextPreviewModal() {
  const modal = document.getElementById('context-preview-modal');
  const projectId = editorBridge?.getProjectId ? editorBridge.getProjectId() : null;
  if (!projectId) {
    showToast('No Project Selected', 'Open a project to preview workspace context.', 'warning');
    return;
  }

  const activeFile = editorBridge?.getActiveFile ? editorBridge.getActiveFile() : null;
  const selectedCode = editorBridge?.getSelectedCode ? editorBridge.getSelectedCode() : null;
  const inputEl = document.getElementById('ai-input-box');
  const query = inputEl ? inputEl.value.trim() : '';

  try {
    const res = await apiRequest('/ai/context/preview', {
      method: 'POST',
      body: JSON.stringify({
        projectId,
        fileId: activeFile ? activeFile.id : null,
        selectedCode: selectedCode || null,
        query: query || null
      })
    });

    const data = res.data;
    const statFiles = document.getElementById('ctx-stat-files');
    const statTokens = document.getElementById('ctx-stat-tokens');
    const statChars = document.getElementById('ctx-stat-chars');
    const listEl = document.getElementById('context-files-list');

    if (statFiles) statFiles.textContent = `${data.selectedFiles?.length || 0} / ${data.totalProjectFiles || 0}`;
    if (statTokens) statTokens.textContent = data.estimatedTokens || 0;
    if (statChars) statChars.textContent = data.totalCharactersUsed || 0;

    if (listEl) {
      listEl.innerHTML = '';
      if (!data.selectedFiles || data.selectedFiles.length === 0) {
        listEl.innerHTML = '<div style="padding: 1rem; text-align: center; color: var(--color-text-muted);">No relevant files scored for context.</div>';
      } else {
        data.selectedFiles.forEach(f => {
          const item = document.createElement('div');
          item.className = 'context-file-item';
          item.innerHTML = `
            <div class="context-file-item-left">
              <span class="context-score-badge">+${f.score}</span>
              <span class="context-file-path">${escapeHtml(f.path)}</span>
            </div>
            <span class="context-file-reason">${escapeHtml(f.reason || '')}</span>
          `;
          listEl.appendChild(item);
        });
      }
    }

    if (modal) modal.style.display = 'flex';
  } catch (err) {
    showToast('Context Preview Failed', err.message, 'error');
  }
}

async function openLatestChangesetModal() {
  const projectId = editorBridge?.getProjectId ? editorBridge.getProjectId() : null;
  if (!projectId) {
    showToast('No Project Selected', 'Open a project to view changesets.', 'warning');
    return;
  }

  try {
    const res = await apiRequest(`/projects/${projectId}/changesets`);
    const list = res.data || [];
    if (list.length === 0) {
      showToast('No Changesets', 'There are no AI proposed changesets for this project yet.', 'info');
      return;
    }

    // Default to the newest changeset
    const cs = list[0];
    renderChangesetModal(cs);
  } catch (err) {
    showToast('Failed to Load Changesets', err.message, 'error');
  }
}

export function renderChangesetModal(cs) {
  activeReviewChangeset = cs;
  const modal = document.getElementById('changeset-review-modal');
  const title = document.getElementById('changeset-modal-title');
  const status = document.getElementById('changeset-modal-status');
  const summary = document.getElementById('changeset-modal-summary');
  const tbody = document.getElementById('changeset-files-tbody');

  if (title) title.textContent = `Review Changeset: ${cs.summary || 'AI Proposed Edits'}`;
  if (status) {
    status.textContent = cs.status || 'PROPOSED';
    status.className = `badge badge-${cs.status === 'APPLIED' ? 'success' : (cs.status === 'REJECTED' ? 'neutral' : 'primary')}`;
  }
  if (summary) summary.textContent = `Proposed ${cs.filesCount || cs.files?.length || 0} file modification(s). Check files to review diffs and apply:`;

  if (tbody) {
    tbody.innerHTML = '';
    (cs.files || []).forEach(f => {
      const tr = document.createElement('tr');
      const op = (f.operation || 'UPDATE').toLowerCase();
      tr.innerHTML = `
        <td><input type="checkbox" class="changeset-file-cb" value="${f.id}" checked></td>
        <td><code>${escapeHtml(f.newPath || f.oldPath || 'unknown')}</code></td>
        <td><span class="operation-badge ${op}">${escapeHtml(op)}</span></td>
        <td><span class="diff-stat-add">+${f.additions || 0}</span> <span class="diff-stat-del">-${f.deletions || 0}</span></td>
        <td><button class="btn btn-xs btn-secondary btn-inspect-diff" data-file-id="${f.id}">Inspect Diff</button></td>
      `;

      const inspectBtn = tr.querySelector('.btn-inspect-diff');
      if (inspectBtn) {
        inspectBtn.addEventListener('click', () => {
          if (modal) modal.style.display = 'none';
          const targetPath = f.newPath || f.oldPath || 'file';
          const bannerFileName = document.getElementById('diff-banner-filename');
          if (bannerFileName) {
            bannerFileName.textContent = `${targetPath} (${op.toUpperCase()}) — Original vs Proposed`;
          }
          showDiff(f.originalContent || '', f.proposedContent || '', null, targetPath);
          showToast('Diff Review Active', `Viewing side-by-side comparison for ${targetPath}.`, 'info');
        });
      }

      tbody.appendChild(tr);
    });
  }

  if (modal) modal.style.display = 'flex';
}

async function handleApplyChangeset() {
  if (!activeReviewChangeset) return;
  const projectId = editorBridge?.getProjectId ? editorBridge.getProjectId() : null;
  const cbs = document.querySelectorAll('.changeset-file-cb:checked');
  const selectedIds = Array.from(cbs).map(cb => cb.value);

  if (selectedIds.length === 0) {
    showToast('No Files Selected', 'Please select at least one file to apply.', 'warning');
    return;
  }

  try {
    const res = await apiRequest(`/projects/${projectId}/changesets/${activeReviewChangeset.id}/apply`, {
      method: 'POST',
      body: JSON.stringify({ fileChangeIds: selectedIds })
    });

    showToast('Changes Applied', `Successfully applied ${selectedIds.length} file change(s).`, 'success');
    const modal = document.getElementById('changeset-review-modal');
    if (modal) modal.style.display = 'none';
    hideDiff();

    if (editorBridge?.refreshExplorer) editorBridge.refreshExplorer();
    const activeFile = editorBridge?.getActiveFile ? editorBridge.getActiveFile() : null;
    if (activeFile && editorBridge?.openFile) {
      editorBridge.openFile(activeFile.id);
    }
  } catch (err) {
    if (err.status === 409 || err.data?.errorCode === 'CONCURRENCY_CONFLICT') {
      showToast('Concurrency Conflict (409)', err.message || 'File has been modified since changeset was proposed. Application aborted.', 'error');
    } else {
      showToast('Apply Failed', err.message, 'error');
    }
  }
}

async function handleRejectChangeset() {
  if (!activeReviewChangeset) return;
  const projectId = editorBridge?.getProjectId ? editorBridge.getProjectId() : null;

  try {
    await apiRequest(`/projects/${projectId}/changesets/${activeReviewChangeset.id}/reject`, {
      method: 'POST'
    });
    showToast('Changeset Rejected', 'Proposed changes were discarded.', 'info');
    const modal = document.getElementById('changeset-review-modal');
    if (modal) modal.style.display = 'none';
  } catch (err) {
    showToast('Reject Failed', err.message, 'error');
  }
}

async function openVersionHistoryModal() {
  const projectId = editorBridge?.getProjectId ? editorBridge.getProjectId() : null;
  const activeFile = editorBridge?.getActiveFile ? editorBridge.getActiveFile() : null;

  if (!projectId || !activeFile) {
    showToast('No File Open', 'Please open a file to view its version history.', 'warning');
    return;
  }

  const modal = document.getElementById('file-version-modal');
  const pathLabel = document.getElementById('version-file-path');
  const listEl = document.getElementById('version-history-list');

  if (pathLabel) pathLabel.textContent = activeFile.path || activeFile.name;

  try {
    const res = await apiRequest(`/projects/${projectId}/files/${activeFile.id}/versions`);
    const versions = res.data || [];

    if (listEl) {
      listEl.innerHTML = '';
      if (versions.length === 0) {
        listEl.innerHTML = '<div style="padding: 1rem; text-align: center; color: var(--color-text-muted);">No version snapshots recorded yet. Versions are created on manual saves, AI patches, and rollbacks.</div>';
      } else {
        versions.forEach(v => {
          const card = document.createElement('div');
          card.className = 'version-card';
          const src = (v.source || 'MANUAL').toLowerCase();
          const dateStr = v.createdAt ? new Date(v.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '';

          card.innerHTML = `
            <div class="version-info-left">
              <span class="version-tag">v${v.versionNumber}</span>
              <span class="version-source-badge ${src}">${escapeHtml(src)}</span>
              <span class="version-meta">${dateStr}</span>
            </div>
            <div class="flex items-center gap-2">
              <button class="btn btn-xs btn-secondary btn-version-diff" data-v="${v.versionNumber}">Diff with Current</button>
              <button class="btn btn-xs btn-primary btn-version-restore" data-id="${v.id}" data-v="${v.versionNumber}">Restore</button>
            </div>
          `;

          const diffBtn = card.querySelector('.btn-version-diff');
          if (diffBtn) {
            diffBtn.addEventListener('click', () => {
              if (modal) modal.style.display = 'none';
              const bannerFileName = document.getElementById('diff-banner-filename');
              if (bannerFileName) {
                bannerFileName.textContent = `${activeFile.path} (v${v.versionNumber} vs Current)`;
              }
              showDiff(v.content || '', activeFile.content || '', null, activeFile.path);
              showToast('Version Diff Active', `Comparing snapshot v${v.versionNumber} with current editor content.`, 'info');
            });
          }

          const restoreBtn = card.querySelector('.btn-version-restore');
          if (restoreBtn) {
            restoreBtn.addEventListener('click', async () => {
              if (confirm(`Restore ${activeFile.name} to version v${v.versionNumber}? A rollback snapshot will be recorded.`)) {
                try {
                  await apiRequest(`/projects/${projectId}/files/${activeFile.id}/versions/${v.id}/restore`, {
                    method: 'POST'
                  });
                  showToast('Version Restored', `File restored to version v${v.versionNumber}.`, 'success');
                  if (modal) modal.style.display = 'none';
                  if (editorBridge?.openFile) {
                    editorBridge.openFile(activeFile.id);
                  }
                } catch (rErr) {
                  showToast('Restore Failed', rErr.message, 'error');
                }
              }
            });
          }

          listEl.appendChild(card);
        });
      }
    }

    if (modal) modal.style.display = 'flex';
  } catch (err) {
    showToast('Failed to Load Versions', err.message, 'error');
  }
}
