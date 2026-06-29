const AUTH_KEY = "cjlab-agent-auth";
const CONVERSATION_KEY = "cjlab-agent-conversations";

const state = {
    authMode: "login",
    auth: loadAuth(),
    conversations: [],
    tools: [],
    sending: false,
    lastLog: null
};

const els = {
    runtimeBadge: document.querySelector("#runtimeBadge"),
    runtimeValue: document.querySelector("#runtimeValue"),
    chatModelValue: document.querySelector("#chatModelValue"),
    profilesValue: document.querySelector("#profilesValue"),
    refreshRuntimeBtn: document.querySelector("#refreshRuntimeBtn"),
    authForm: document.querySelector("#authForm"),
    loginModeBtn: document.querySelector("#loginModeBtn"),
    registerModeBtn: document.querySelector("#registerModeBtn"),
    emailInput: document.querySelector("#emailInput"),
    passwordInput: document.querySelector("#passwordInput"),
    displayNameGroup: document.querySelector("#displayNameGroup"),
    displayNameInput: document.querySelector("#displayNameInput"),
    authSubmitBtn: document.querySelector("#authSubmitBtn"),
    authStatus: document.querySelector("#authStatus"),
    signedInBox: document.querySelector("#signedInBox"),
    currentUserName: document.querySelector("#currentUserName"),
    currentUserEmail: document.querySelector("#currentUserEmail"),
    logoutBtn: document.querySelector("#logoutBtn"),
    newChatBtn: document.querySelector("#newChatBtn"),
    conversationId: document.querySelector("#conversationId"),
    conversationList: document.querySelector("#conversationList"),
    chatTitle: document.querySelector("#chatTitle"),
    chatSubtitle: document.querySelector("#chatSubtitle"),
    chatStream: document.querySelector("#chatStream"),
    chatForm: document.querySelector("#chatForm"),
    chatMessage: document.querySelector("#chatMessage"),
    sendChatBtn: document.querySelector("#sendChatBtn"),
    clearChatBtn: document.querySelector("#clearChatBtn"),
    loadMemoryBtn: document.querySelector("#loadMemoryBtn"),
    reloadMemoryBtn: document.querySelector("#reloadMemoryBtn"),
    memoryLimit: document.querySelector("#memoryLimit"),
    memoryList: document.querySelector("#memoryList"),
    knowledgeTitle: document.querySelector("#knowledgeTitle"),
    knowledgeContent: document.querySelector("#knowledgeContent"),
    saveKnowledgeBtn: document.querySelector("#saveKnowledgeBtn"),
    listKnowledgeBtn: document.querySelector("#listKnowledgeBtn"),
    searchQuery: document.querySelector("#searchQuery"),
    searchKnowledgeBtn: document.querySelector("#searchKnowledgeBtn"),
    knowledgeList: document.querySelector("#knowledgeList"),
    loadToolsBtn: document.querySelector("#loadToolsBtn"),
    toolSelect: document.querySelector("#toolSelect"),
    toolInput: document.querySelector("#toolInput"),
    toolArguments: document.querySelector("#toolArguments"),
    executeToolBtn: document.querySelector("#executeToolBtn"),
    toolResult: document.querySelector("#toolResult"),
    clearLogBtn: document.querySelector("#clearLogBtn"),
    requestLog: document.querySelector("#requestLog"),
    tabButtons: document.querySelectorAll(".tab-button"),
    tabPanels: document.querySelectorAll(".tab-panel")
};

function loadAuth() {
    try {
        return JSON.parse(localStorage.getItem(AUTH_KEY) || "null");
    } catch {
        return null;
    }
}

function saveAuth(auth) {
    state.auth = auth;
    if (auth) {
        localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
    } else {
        localStorage.removeItem(AUTH_KEY);
    }
    renderAuth();
}

function authHeaders() {
    return state.auth?.accessToken ? { Authorization: `Bearer ${state.auth.accessToken}` } : {};
}

function isSignedIn() {
    return Boolean(state.auth?.accessToken);
}

function currentConversationId() {
    const value = els.conversationId.value.trim();
    return value || "demo";
}

function nowLabel() {
    return new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function pretty(value) {
    return JSON.stringify(value, null, 2);
}

function setBusy(button, busy) {
    if (button) {
        button.disabled = busy;
    }
}

function setStatus(message, type = "") {
    els.authStatus.textContent = message;
    els.authStatus.classList.toggle("ok", type === "ok");
    els.authStatus.classList.toggle("error", type === "error");
}

function renderLog(entry) {
    state.lastLog = entry;
    els.requestLog.textContent = pretty(entry);
}

async function requestJson(method, path, body, options = {}) {
    const startedAt = new Date().toISOString();
    const headers = {
        Accept: "application/json",
        ...(options.auth === false ? {} : authHeaders())
    };
    const init = { method, headers };
    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
        init.body = JSON.stringify(body);
    }

    let response;
    let payload;
    try {
        response = await fetch(path, init);
        const text = await response.text();
        payload = text ? JSON.parse(text) : null;
    } catch (error) {
        renderLog({ startedAt, method, path, status: "network-error", error: error.message });
        throw error;
    }

    renderLog({ startedAt, method, path, status: response.status, ok: response.ok, request: body ?? null, response: payload });

    if (!response.ok) {
        throw new Error(payload?.message || payload?.error || `HTTP ${response.status}`);
    }
    return payload;
}

function parseSseEvents(buffer) {
    const events = [];
    let cursor = 0;
    while (true) {
        const lf = buffer.indexOf("\n\n", cursor);
        const crlf = buffer.indexOf("\r\n\r\n", cursor);
        const boundaries = [lf, crlf].filter(index => index !== -1);
        if (boundaries.length === 0) {
            return { events, rest: buffer.slice(cursor) };
        }

        const boundary = Math.min(...boundaries);
        const delimiterLength = boundary === crlf ? 4 : 2;
        const rawEvent = buffer.slice(cursor, boundary);
        cursor = boundary + delimiterLength;

        let eventName = "message";
        const dataLines = [];
        rawEvent.split("\n").forEach(line => {
            const normalized = line.endsWith("\r") ? line.slice(0, -1) : line;
            if (normalized.startsWith("event:")) {
                eventName = normalized.slice(6).trim();
            } else if (normalized.startsWith("data:")) {
                dataLines.push(normalized.slice(5).trimStart());
            }
        });

        if (dataLines.length > 0) {
            events.push({ event: eventName, data: dataLines.join("\n") });
        }
    }
}

async function requestChatStream(body, handlers) {
    const startedAt = new Date().toISOString();
    renderLog({ startedAt, method: "POST", path: "/api/chat/stream", status: "connecting", request: body, response: { deltas: 0 } });

    const response = await fetch("/api/chat/stream", {
        method: "POST",
        headers: {
            Accept: "text/event-stream",
            "Content-Type": "application/json",
            ...authHeaders()
        },
        body: JSON.stringify(body)
    });

    if (!response.ok || !response.body) {
        const text = await response.text();
        renderLog({ startedAt, method: "POST", path: "/api/chat/stream", status: response.status, ok: false, response: text });
        throw new Error(`HTTP ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let deltaCount = 0;
    let finalContent = "";

    while (true) {
        const { value, done } = await reader.read();
        if (done) {
            break;
        }
        buffer += decoder.decode(value, { stream: true });
        const parsed = parseSseEvents(buffer);
        buffer = parsed.rest;

        for (const item of parsed.events) {
            const data = JSON.parse(item.data);
            if (item.event === "start") {
                handlers.start?.(data);
            } else if (item.event === "delta") {
                deltaCount += 1;
                handlers.delta?.(data);
                renderLog({ startedAt, method: "POST", path: "/api/chat/stream", status: "streaming", ok: true, response: { deltas: deltaCount, conversationId: data.conversationId } });
            } else if (item.event === "done") {
                finalContent = data.content || finalContent;
                handlers.done?.(data);
                renderLog({ startedAt, method: "POST", path: "/api/chat/stream", status: "done", ok: true, response: { deltas: deltaCount, conversationId: data.conversationId, content: finalContent } });
            } else if (item.event === "error") {
                renderLog({ startedAt, method: "POST", path: "/api/chat/stream", status: "stream-error", ok: false, response: data });
                throw new Error(data.content || "SSE stream error");
            }
        }
    }
}

function renderAuth() {
    const user = state.auth?.user;
    const signedIn = isSignedIn();
    els.signedInBox.classList.toggle("hidden", !signedIn);
    els.logoutBtn.classList.toggle("hidden", !signedIn);
    els.currentUserName.textContent = user?.displayName || user?.email || "已登录";
    els.currentUserEmail.textContent = user?.email || "";
    els.sendChatBtn.disabled = !signedIn || state.sending;
    els.loadMemoryBtn.disabled = !signedIn;
    els.reloadMemoryBtn.disabled = !signedIn;
    els.saveKnowledgeBtn.disabled = !signedIn;
    els.listKnowledgeBtn.disabled = !signedIn;
    els.searchKnowledgeBtn.disabled = !signedIn;
    els.loadToolsBtn.disabled = !signedIn;
    els.executeToolBtn.disabled = !signedIn;
    els.chatSubtitle.textContent = signedIn ? "ready" : "请先登录，然后开始 SSE 对话";
}

function switchAuthMode(mode) {
    state.authMode = mode;
    els.loginModeBtn.classList.toggle("active", mode === "login");
    els.registerModeBtn.classList.toggle("active", mode === "register");
    els.displayNameGroup.classList.toggle("hidden", mode !== "register");
    els.authSubmitBtn.textContent = mode === "login" ? "登录" : "注册";
    setStatus(mode === "login" ? "使用邮箱和密码登录" : "注册后会自动登录");
}

async function submitAuth(event) {
    event.preventDefault();
    setBusy(els.authSubmitBtn, true);
    try {
        const email = els.emailInput.value.trim();
        const password = els.passwordInput.value;
        if (state.authMode === "register") {
            await requestJson("POST", "/api/users/register", {
                email,
                password,
                displayName: els.displayNameInput.value.trim() || email
            }, { auth: false });
        }
        const login = await requestJson("POST", "/api/users/login", { email, password }, { auth: false });
        saveAuth(login);
        setStatus("登录成功", "ok");
        await loadTools();
    } catch (error) {
        setStatus(`失败: ${error.message}`, "error");
    } finally {
        setBusy(els.authSubmitBtn, false);
    }
}

async function verifyCurrentUser() {
    if (!isSignedIn()) {
        renderAuth();
        return;
    }
    try {
        const user = await requestJson("GET", "/api/users/me");
        saveAuth({ ...state.auth, user });
        setStatus("登录态有效", "ok");
        await loadTools();
    } catch {
        saveAuth(null);
        setStatus("登录态已失效，请重新登录", "error");
    }
}

function logout() {
    saveAuth(null);
    setStatus("已退出");
}

function loadConversations() {
    try {
        state.conversations = JSON.parse(localStorage.getItem(CONVERSATION_KEY) || "[]");
    } catch {
        state.conversations = [];
    }
    if (!state.conversations.some(item => item.id === "demo")) {
        state.conversations.unshift({ id: "demo", title: "demo", updatedAt: Date.now() });
    }
}

function saveConversations() {
    localStorage.setItem(CONVERSATION_KEY, JSON.stringify(state.conversations.slice(0, 24)));
}

function touchConversation(id, title = id) {
    const existing = state.conversations.find(item => item.id === id);
    if (existing) {
        existing.title = title || existing.title;
        existing.updatedAt = Date.now();
    } else {
        state.conversations.unshift({ id, title, updatedAt: Date.now() });
    }
    state.conversations.sort((a, b) => b.updatedAt - a.updatedAt);
    saveConversations();
    renderConversations();
}

function renderConversations() {
    const current = currentConversationId();
    els.conversationList.innerHTML = state.conversations.map(item => `
        <button class="conversation-item ${item.id === current ? "active" : ""}" type="button" data-id="${escapeHtml(item.id)}">
            <span class="conversation-name">${escapeHtml(item.title || item.id)}</span>
        </button>
    `).join("");
}

function setConversation(id) {
    els.conversationId.value = id;
    els.chatTitle.textContent = id;
    renderConversations();
    loadMemory(true);
}

function newConversation() {
    const id = `chat-${new Date().toISOString().replaceAll(/[-:.TZ]/g, "").slice(0, 14)}`;
    touchConversation(id, id);
    setConversation(id);
    showEmptyChat();
    els.chatMessage.focus();
}

function showEmptyChat() {
    els.chatStream.innerHTML = `
        <div class="empty-state">
            <div>
                <strong>CJLab Agent Console</strong>
                <span>登录后发送消息，右侧面板可验证 RAG、工具和记忆</span>
            </div>
        </div>
    `;
}

function appendMessage(role, content, options = {}) {
    els.chatStream.querySelector(".empty-state")?.remove();
    const row = document.createElement("div");
    row.className = `message-row ${role}${options.pending ? " pending" : ""}`;
    row.innerHTML = `
        <div class="message">
            <div class="bubble">${escapeHtml(content)}</div>
            <div class="message-meta">
                <span>${role === "user" ? "You" : "Agent"}</span>
                <span>${escapeHtml(options.time || nowLabel())}</span>
                ${role === "assistant" && !options.pending ? '<button class="copy-message" type="button">复制</button>' : ""}
            </div>
        </div>
    `;
    els.chatStream.appendChild(row);
    scrollChat();
    return row;
}

function updateMessage(row, content, pending = false) {
    row.classList.toggle("pending", pending);
    row.querySelector(".bubble").textContent = content;
    const meta = row.querySelector(".message-meta");
    if (!pending && !meta.querySelector(".copy-message")) {
        const button = document.createElement("button");
        button.className = "copy-message";
        button.type = "button";
        button.textContent = "复制";
        meta.appendChild(button);
    }
    scrollChat();
}

function appendToMessage(row, content) {
    const bubble = row.querySelector(".bubble");
    bubble.textContent = `${bubble.textContent}${content}`;
    scrollChat();
}

function scrollChat() {
    els.chatStream.scrollTop = els.chatStream.scrollHeight;
}

function autoResizeComposer() {
    els.chatMessage.style.height = "auto";
    els.chatMessage.style.height = `${Math.min(els.chatMessage.scrollHeight, 160)}px`;
}

async function loadRuntime() {
    setBusy(els.refreshRuntimeBtn, true);
    try {
        const data = await requestJson("GET", "/api/runtime", undefined, { auth: false });
        els.runtimeValue.textContent = data.runtime || "unknown";
        els.chatModelValue.textContent = data.chatModel || "none";
        els.profilesValue.textContent = data.activeProfiles?.join(", ") || "default";
        els.runtimeBadge.textContent = data.runtime || "unknown";
    } catch {
        els.runtimeValue.textContent = "unavailable";
        els.chatModelValue.textContent = "unavailable";
        els.profilesValue.textContent = "-";
        els.runtimeBadge.textContent = "server unavailable";
    } finally {
        setBusy(els.refreshRuntimeBtn, false);
    }
}

async function sendChat(event) {
    event?.preventDefault();
    if (!isSignedIn()) {
        setStatus("请先登录", "error");
        return;
    }
    if (state.sending) {
        return;
    }
    const message = els.chatMessage.value.trim();
    if (!message) {
        return;
    }

    state.sending = true;
    renderAuth();
    els.chatSubtitle.textContent = "streaming";
    const id = currentConversationId();
    touchConversation(id, message.slice(0, 28) || id);
    appendMessage("user", message);
    els.chatMessage.value = "";
    autoResizeComposer();
    const pendingRow = appendMessage("assistant", "", { pending: true });

    try {
        let streamedContent = "";
        await requestChatStream({ conversationId: id, message }, {
            delta: data => {
                streamedContent += data.content || "";
                appendToMessage(pendingRow, data.content || "");
            },
            done: data => {
                streamedContent = data.content || streamedContent;
                updateMessage(pendingRow, streamedContent, false);
            }
        });
        els.chatSubtitle.textContent = "ready";
        await loadMemory(false);
    } catch (error) {
        updateMessage(pendingRow, `请求失败: ${error.message}`, false);
        els.chatSubtitle.textContent = "error";
    } finally {
        state.sending = false;
        renderAuth();
    }
}

function renderResultList(target, items, renderer) {
    if (!items || items.length === 0) {
        target.textContent = "暂无数据";
        return;
    }
    target.innerHTML = items.map(renderer).join("");
}

function renderMemoryToChat(messages) {
    els.chatStream.innerHTML = "";
    if (!messages || messages.length === 0) {
        showEmptyChat();
        return;
    }
    messages.forEach(item => {
        appendMessage(item.role === "USER" ? "user" : "assistant", item.content, {
            time: new Date(item.createdAt).toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })
        });
    });
}

async function loadMemory(renderChat = false) {
    const button = renderChat ? els.loadMemoryBtn : els.reloadMemoryBtn;
    setBusy(button, true);
    try {
        const limit = Number(els.memoryLimit.value || 20);
        const data = await requestJson("GET", `/api/memory/${encodeURIComponent(currentConversationId())}?limit=${limit}`);
        renderResultList(els.memoryList, data, item => `
            <div class="result-item">
                <div class="result-title"><span>${escapeHtml(item.role)}</span><span>${escapeHtml(new Date(item.createdAt).toLocaleString("zh-CN"))}</span></div>
                <div class="result-content">${escapeHtml(item.content)}</div>
            </div>
        `);
        if (renderChat) {
            renderMemoryToChat(data);
        }
    } finally {
        setBusy(button, false);
    }
}

async function saveKnowledge() {
    setBusy(els.saveKnowledgeBtn, true);
    try {
        const data = await requestJson("POST", "/api/knowledge", {
            title: els.knowledgeTitle.value.trim(),
            content: els.knowledgeContent.value.trim(),
            metadata: { source: "console" }
        });
        els.knowledgeList.innerHTML = knowledgeItem(data, "saved");
    } finally {
        setBusy(els.saveKnowledgeBtn, false);
    }
}

function knowledgeItem(item, badge = "doc") {
    return `
        <div class="result-item">
            <div class="result-title"><span>${escapeHtml(item.title)}</span><span>${escapeHtml(badge)}</span></div>
            <div class="result-meta">${escapeHtml(item.id)}</div>
            <div class="result-content">${escapeHtml(item.content)}</div>
        </div>
    `;
}

async function listKnowledge() {
    setBusy(els.listKnowledgeBtn, true);
    try {
        const data = await requestJson("GET", "/api/knowledge");
        renderResultList(els.knowledgeList, data, item => knowledgeItem(item, item.metadata?.source || "doc"));
    } finally {
        setBusy(els.listKnowledgeBtn, false);
    }
}

async function searchKnowledge() {
    setBusy(els.searchKnowledgeBtn, true);
    try {
        const query = encodeURIComponent(els.searchQuery.value.trim());
        const data = await requestJson("GET", `/api/knowledge/search?query=${query}&limit=5`);
        renderResultList(els.knowledgeList, data, item => knowledgeItem(item.document, `score ${Number(item.score).toFixed(3)}`));
    } finally {
        setBusy(els.searchKnowledgeBtn, false);
    }
}

async function loadTools() {
    setBusy(els.loadToolsBtn, true);
    try {
        state.tools = await requestJson("GET", "/api/tools");
        els.toolSelect.innerHTML = state.tools.map(tool => `<option value="${escapeHtml(tool.name)}">${escapeHtml(tool.name)}</option>`).join("");
        renderResultList(els.toolResult, state.tools, tool => `
            <div class="result-item">
                <div class="result-title"><span>${escapeHtml(tool.name)}</span></div>
                <div class="result-content">${escapeHtml(tool.description)}</div>
            </div>
        `);
    } finally {
        setBusy(els.loadToolsBtn, false);
    }
}

async function executeTool() {
    setBusy(els.executeToolBtn, true);
    try {
        let args;
        try {
            args = JSON.parse(els.toolArguments.value || "{}");
        } catch {
            throw new Error("参数 JSON 格式不正确");
        }
        const data = await requestJson("POST", `/api/tools/${encodeURIComponent(els.toolSelect.value)}/execute`, {
            conversationId: currentConversationId(),
            input: els.toolInput.value.trim(),
            arguments: args
        });
        els.toolResult.innerHTML = `
            <div class="result-item">
                <div class="result-title"><span>${escapeHtml(data.toolName)}</span><span>done</span></div>
                <div class="result-content">${escapeHtml(data.content)}</div>
            </div>
        `;
    } catch (error) {
        els.toolResult.textContent = `执行失败: ${error.message}`;
    } finally {
        setBusy(els.executeToolBtn, false);
    }
}

function switchTab(tab) {
    els.tabButtons.forEach(button => button.classList.toggle("active", button.dataset.tab === tab));
    els.tabPanels.forEach(panel => panel.classList.toggle("active", panel.id === `${tab}Tab`));
}

els.loginModeBtn.addEventListener("click", () => switchAuthMode("login"));
els.registerModeBtn.addEventListener("click", () => switchAuthMode("register"));
els.authForm.addEventListener("submit", submitAuth);
els.logoutBtn.addEventListener("click", logout);
els.refreshRuntimeBtn.addEventListener("click", loadRuntime);
els.newChatBtn.addEventListener("click", newConversation);
els.conversationId.addEventListener("change", () => {
    touchConversation(currentConversationId(), currentConversationId());
    setConversation(currentConversationId());
});
els.conversationList.addEventListener("click", event => {
    const item = event.target.closest(".conversation-item");
    if (item) {
        setConversation(item.dataset.id);
    }
});
els.chatForm.addEventListener("submit", sendChat);
els.chatMessage.addEventListener("input", autoResizeComposer);
els.chatMessage.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        sendChat(event);
    }
});
els.chatStream.addEventListener("click", event => {
    const button = event.target.closest(".copy-message");
    if (!button) {
        return;
    }
    navigator.clipboard?.writeText(button.closest(".message").querySelector(".bubble").textContent);
});
els.clearChatBtn.addEventListener("click", showEmptyChat);
els.loadMemoryBtn.addEventListener("click", () => loadMemory(true));
els.reloadMemoryBtn.addEventListener("click", () => loadMemory(false));
els.saveKnowledgeBtn.addEventListener("click", saveKnowledge);
els.listKnowledgeBtn.addEventListener("click", listKnowledge);
els.searchKnowledgeBtn.addEventListener("click", searchKnowledge);
els.loadToolsBtn.addEventListener("click", loadTools);
els.executeToolBtn.addEventListener("click", executeTool);
els.clearLogBtn.addEventListener("click", () => {
    els.requestLog.textContent = "等待请求";
});
els.tabButtons.forEach(button => button.addEventListener("click", () => switchTab(button.dataset.tab)));

switchAuthMode("login");
renderAuth();
loadConversations();
renderConversations();
showEmptyChat();
autoResizeComposer();
loadRuntime();
verifyCurrentUser();
