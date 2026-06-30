const AUTH_KEY = "cjlab-agent-auth";
const CONVERSATION_KEY = "cjlab-agent-conversations";
const ROLE_CARD_KEY = "cjlab-agent-role-cards";
const SELECTED_ROLE_CARD_KEY = "cjlab-agent-selected-role-card";

const DEFAULT_ROLE_CARDS = [
    {
        id: "default",
        name: "默认助手",
        description: "平衡、直接、可执行",
        instruction: "用清晰、简洁、务实的方式回答。优先给结论和可执行步骤。",
        avatar: ""
    },
    {
        id: "engineer",
        name: "后端工程师",
        description: "Java/Spring/MyBatis 工程实现优先",
        instruction: "以资深 Java 后端工程师风格回答。优先指出模块边界、数据模型、接口契约、异常路径和验证方式。代码建议要贴近现有工程。",
        avatar: ""
    },
    {
        id: "product",
        name: "产品分析师",
        description: "目标、用户价值、流程和优先级",
        instruction: "以产品分析师风格回答。先澄清目标和用户场景，再按优先级拆解方案，关注体验闭环、指标和风险。",
        avatar: ""
    },
    {
        id: "teacher",
        name: "讲解老师",
        description: "循序渐进、示例驱动",
        instruction: "以耐心讲解老师风格回答。把复杂概念拆成小步骤，用示例解释关键点，并在最后给出简短总结。",
        avatar: ""
    }
];

const state = {
    authMode: "login",
    auth: loadAuth(),
    conversations: [],
    roleCards: loadRoleCards(),
    selectedRoleCardId: localStorage.getItem(SELECTED_ROLE_CARD_KEY) || "default",
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
    roleChatSelect: document.querySelector("#roleChatSelect"),
    activeRoleAvatar: document.querySelector("#activeRoleAvatar"),
    activeRoleLabel: document.querySelector("#activeRoleLabel"),
    streamState: document.querySelector("#streamState"),
    chatStream: document.querySelector("#chatStream"),
    chatForm: document.querySelector("#chatForm"),
    chatMessage: document.querySelector("#chatMessage"),
    sendChatBtn: document.querySelector("#sendChatBtn"),
    clearChatBtn: document.querySelector("#clearChatBtn"),
    loadMemoryBtn: document.querySelector("#loadMemoryBtn"),
    reloadMemoryBtn: document.querySelector("#reloadMemoryBtn"),
    loadSummaryBtn: document.querySelector("#loadSummaryBtn"),
    memoryLimit: document.querySelector("#memoryLimit"),
    memoryList: document.querySelector("#memoryList"),
    summaryBox: document.querySelector("#summaryBox"),
    roleCardSelect: document.querySelector("#roleCardSelect"),
    roleCardName: document.querySelector("#roleCardName"),
    roleCardDescription: document.querySelector("#roleCardDescription"),
    roleCardAvatar: document.querySelector("#roleCardAvatar"),
    roleCardInstruction: document.querySelector("#roleCardInstruction"),
    saveRoleCardBtn: document.querySelector("#saveRoleCardBtn"),
    newRoleCardBtn: document.querySelector("#newRoleCardBtn"),
    resetRoleCardsBtn: document.querySelector("#resetRoleCardsBtn"),
    roleCardPreview: document.querySelector("#roleCardPreview"),
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

function loadRoleCards() {
    try {
        const cards = JSON.parse(localStorage.getItem(ROLE_CARD_KEY) || "null");
        if (Array.isArray(cards) && cards.length > 0) {
            return cards;
        }
    } catch {
        // Fall back to built-in role cards.
    }
    return DEFAULT_ROLE_CARDS.map(card => ({ ...card }));
}

function saveRoleCards() {
    localStorage.setItem(ROLE_CARD_KEY, JSON.stringify(state.roleCards));
}

function mergeRoleCards(baseCards, persistedCards) {
    const merged = new Map();
    baseCards.forEach(card => merged.set(card.id, { ...card }));
    (persistedCards || []).forEach(card => {
        if (card?.id) {
            merged.set(card.id, {
                id: card.id,
                name: card.name,
                description: card.description || "",
                instruction: card.instruction || "",
                avatar: card.avatar || "",
                updatedAt: card.updatedAt || null
            });
        }
    });
    return Array.from(merged.values());
}

async function loadPersistedRoleCards() {
    if (!isSignedIn()) {
        return;
    }
    const data = await requestJson("GET", "/api/role-cards");
    state.roleCards = mergeRoleCards(DEFAULT_ROLE_CARDS, data);
    saveRoleCards();
    renderRoleCards();
}

async function persistRoleCard(card) {
    if (!isSignedIn()) {
        return card;
    }
    return await requestJson("POST", "/api/role-cards", {
        id: card.id,
        name: card.name,
        description: card.description,
        instruction: card.instruction,
        avatar: card.avatar
    });
}

async function persistDefaultRoleCards() {
    if (!isSignedIn()) {
        return DEFAULT_ROLE_CARDS.map(card => ({ ...card }));
    }
    return await requestJson("POST", "/api/role-cards/defaults");
}

function selectedRoleCard() {
    return state.roleCards.find(card => card.id === state.selectedRoleCardId)
        || state.roleCards[0]
        || DEFAULT_ROLE_CARDS[0];
}

function roleCardPayload() {
    const card = selectedRoleCard();
    if (!card) {
        return null;
    }
    return {
        id: card.id,
        name: card.name,
        description: card.description,
        instruction: card.instruction,
        avatar: card.avatar
    };
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

function roleInitials(card) {
    const source = String(card?.name || card?.id || "AI").trim();
    if (!source) {
        return "AI";
    }
    return source
        .split(/[\s_-]+/)
        .filter(Boolean)
        .slice(0, 2)
        .map(part => part.slice(0, 1))
        .join("")
        .toUpperCase();
}

function roleAvatarHtml(card, className = "") {
    const avatar = String(card?.avatar || "").trim();
    const label = roleInitials(card);
    if (avatar) {
        return `<span class="role-avatar ${className}"><img src="${escapeHtml(avatar)}" alt=""></span>`;
    }
    return `<span class="role-avatar ${className}">${escapeHtml(label)}</span>`;
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

function setStreamState(label, mode = "idle") {
    if (!els.streamState) {
        return;
    }
    els.streamState.textContent = label;
    els.streamState.className = `stream-state ${mode}`;
}

function renderLog(entry) {
    state.lastLog = entry;
    els.requestLog.textContent = pretty(entry);
    console.info("[cjlab-agent]", sanitizeConsoleLog(entry));
}

function sanitizeConsoleLog(entry) {
    const safeEntry = JSON.parse(JSON.stringify(entry ?? {}));
    if (String(safeEntry.path || "").startsWith("/api/users/")) {
        if (safeEntry.request?.password) {
            safeEntry.request.password = "***";
        }
        if (safeEntry.response?.accessToken) {
            safeEntry.response.accessToken = "***";
        }
    }
    return safeEntry;
}

function splitThinkContent(rawContent) {
    const content = String(rawContent ?? "");
    const lower = content.toLowerCase();
    const openTag = "<think>";
    const closeTag = "</think>";
    const thinkingParts = [];
    const answerParts = [];
    let cursor = 0;
    let inThink = false;

    while (cursor < content.length) {
        if (!inThink) {
            const openIndex = lower.indexOf(openTag, cursor);
            if (openIndex === -1) {
                answerParts.push(content.slice(cursor));
                break;
            }
            answerParts.push(content.slice(cursor, openIndex));
            cursor = openIndex + openTag.length;
            inThink = true;
        } else {
            const closeIndex = lower.indexOf(closeTag, cursor);
            if (closeIndex === -1) {
                thinkingParts.push(content.slice(cursor));
                cursor = content.length;
                break;
            }
            thinkingParts.push(content.slice(cursor, closeIndex));
            cursor = closeIndex + closeTag.length;
            inThink = false;
        }
    }

    const thinking = thinkingParts.join("").trim();
    const answer = answerParts.join("").replace(/^\s+/, "");
    return { thinking, answer, thinkingOpen: inThink };
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
    els.loadSummaryBtn.disabled = !signedIn;
    els.saveKnowledgeBtn.disabled = !signedIn;
    els.listKnowledgeBtn.disabled = !signedIn;
    els.searchKnowledgeBtn.disabled = !signedIn;
    els.loadToolsBtn.disabled = !signedIn;
    els.executeToolBtn.disabled = !signedIn;
    els.roleChatSelect.disabled = !signedIn;
    els.chatSubtitle.textContent = signedIn ? "ready" : "请先登录，然后开始 SSE 对话";
    if (!state.sending) {
        setStreamState(signedIn ? "ready" : "locked", signedIn ? "ready" : "idle");
    }
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
        await loadPersistedRoleCards();
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
        await loadPersistedRoleCards();
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

function renderRoleCards() {
    if (!state.roleCards.some(card => card.id === state.selectedRoleCardId)) {
        state.selectedRoleCardId = state.roleCards[0]?.id || "default";
    }
    const options = state.roleCards.map(card => `
        <option value="${escapeHtml(card.id)}" ${card.id === state.selectedRoleCardId ? "selected" : ""}>
            ${escapeHtml(card.name || card.id)}
        </option>
    `).join("");
    els.roleChatSelect.innerHTML = options;
    els.roleCardSelect.innerHTML = options;
    renderRoleCardEditor();
}

function renderRoleCardEditor() {
    const card = selectedRoleCard();
    els.roleCardName.value = card?.name || "";
    els.roleCardDescription.value = card?.description || "";
    els.roleCardAvatar.value = card?.avatar || "";
    els.roleCardInstruction.value = card?.instruction || "";
    els.activeRoleLabel.textContent = card?.name || "默认助手";
    if (card?.avatar) {
        els.activeRoleAvatar.innerHTML = `<img src="${escapeHtml(card.avatar)}" alt="">`;
    } else {
        els.activeRoleAvatar.textContent = roleInitials(card);
    }
    els.roleCardPreview.innerHTML = card ? `
        <div class="result-item">
            <div class="result-title">
                <span class="role-preview-heading">${roleAvatarHtml(card)}<span>${escapeHtml(card.name)}</span></span>
                <span>${escapeHtml(card.id)}</span>
            </div>
            <div class="result-meta">${escapeHtml(card.description)}</div>
            <div class="result-content">${escapeHtml(card.instruction)}</div>
        </div>
    ` : "暂无数据";
}

function selectRoleCard(id) {
    state.selectedRoleCardId = id;
    localStorage.setItem(SELECTED_ROLE_CARD_KEY, id);
    if (els.roleChatSelect.value !== id) {
        els.roleChatSelect.value = id;
    }
    if (els.roleCardSelect.value !== id) {
        els.roleCardSelect.value = id;
    }
    renderRoleCards();
}

function normalizeRoleId(value) {
    const normalized = String(value || "")
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9_-]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 48);
    return normalized || `role-${Date.now()}`;
}

async function saveCurrentRoleCard() {
    setBusy(els.saveRoleCardBtn, true);
    const current = selectedRoleCard();
    try {
        const name = els.roleCardName.value.trim() || "未命名角色";
        const id = current?.id || normalizeRoleId(name);
        const next = {
            id,
            name,
            description: els.roleCardDescription.value.trim(),
            instruction: els.roleCardInstruction.value.trim(),
            avatar: els.roleCardAvatar.value.trim()
        };
        const saved = await persistRoleCard(next);
        const normalized = {
            ...next,
            id: saved?.id || next.id,
            name: saved?.name || next.name,
            description: saved?.description || next.description,
            instruction: saved?.instruction || next.instruction,
            avatar: saved?.avatar || next.avatar,
            updatedAt: saved?.updatedAt || null
        };
        const index = state.roleCards.findIndex(card => card.id === id || card.id === normalized.id);
        if (index >= 0) {
            state.roleCards.splice(index, 1, normalized);
        } else {
            state.roleCards.push(normalized);
        }
        state.selectedRoleCardId = normalized.id;
        saveRoleCards();
        localStorage.setItem(SELECTED_ROLE_CARD_KEY, normalized.id);
        renderRoleCards();
    } finally {
        setBusy(els.saveRoleCardBtn, false);
    }
}

function newRoleCard() {
    const id = `role-${Date.now()}`;
    state.roleCards.push({
        id,
        name: "自定义角色",
        description: "",
        instruction: "",
        avatar: ""
    });
    state.selectedRoleCardId = id;
    saveRoleCards();
    localStorage.setItem(SELECTED_ROLE_CARD_KEY, id);
    renderRoleCards();
    els.roleCardName.focus();
}

async function resetRoleCards() {
    setBusy(els.resetRoleCardsBtn, true);
    state.roleCards = DEFAULT_ROLE_CARDS.map(card => ({ ...card }));
    state.selectedRoleCardId = "default";
    try {
        if (isSignedIn()) {
            const savedCards = await persistDefaultRoleCards();
            state.roleCards = mergeRoleCards(DEFAULT_ROLE_CARDS, savedCards);
        }
        saveRoleCards();
        localStorage.setItem(SELECTED_ROLE_CARD_KEY, state.selectedRoleCardId);
        renderRoleCards();
    } finally {
        setBusy(els.resetRoleCardsBtn, false);
    }
}

function setConversation(id) {
    els.conversationId.value = id;
    els.chatTitle.textContent = id;
    renderConversations();
    loadMemory(true);
    loadSummary();
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
    const parsed = role === "assistant" ? splitThinkContent(content) : { thinking: "", answer: content };
    row.innerHTML = role === "assistant" ? `
        <div class="message">
            <div class="think-panel ${parsed.thinking ? "" : "hidden"}">
                <button class="think-toggle" type="button" aria-expanded="false">
                    <span>思考</span>
                    <span class="think-state">${parsed.thinkingOpen ? "生成中" : "已收起"}</span>
                </button>
                <pre class="think-content hidden">${escapeHtml(parsed.thinking)}</pre>
            </div>
            <div class="bubble">${escapeHtml(parsed.answer)}</div>
            <div class="message-meta">
                <span>Agent</span>
                <span>${escapeHtml(options.time || nowLabel())}</span>
                ${options.pending ? '<span class="streaming-dot">SSE</span>' : '<button class="copy-message" type="button">复制</button>'}
            </div>
        </div>
    ` : `
        <div class="message">
            <div class="bubble">${escapeHtml(content)}</div>
            <div class="message-meta">
                <span>You</span>
                <span>${escapeHtml(options.time || nowLabel())}</span>
            </div>
        </div>
    `;
    els.chatStream.appendChild(row);
    scrollChat();
    return row;
}

function updateMessage(row, content, pending = false) {
    row.classList.toggle("pending", pending);
    if (row.classList.contains("assistant")) {
        renderAssistantMessage(row, content, pending);
    } else {
        row.querySelector(".bubble").textContent = content;
    }
    const meta = row.querySelector(".message-meta");
    if (!pending && !meta.querySelector(".copy-message")) {
        meta.querySelector(".streaming-dot")?.remove();
        const button = document.createElement("button");
        button.className = "copy-message";
        button.type = "button";
        button.textContent = "复制";
        meta.appendChild(button);
    }
    scrollChat();
}

function appendToMessage(row, content) {
    const current = row.dataset.rawContent || "";
    updateMessage(row, `${current}${content}`, true);
}

function renderAssistantMessage(row, rawContent, pending = false) {
    const parsed = splitThinkContent(rawContent);
    row.dataset.rawContent = rawContent;
    row.querySelector(".bubble").textContent = parsed.answer || (pending ? "正在接收回复..." : "");
    const thinkPanel = row.querySelector(".think-panel");
    const thinkContent = row.querySelector(".think-content");
    const thinkToggle = row.querySelector(".think-toggle");
    const thinkState = row.querySelector(".think-state");

    if (parsed.thinking) {
        thinkPanel.classList.remove("hidden");
        thinkContent.textContent = parsed.thinking;
        if (pending && parsed.thinkingOpen) {
            thinkContent.classList.remove("hidden");
            thinkToggle.setAttribute("aria-expanded", "true");
            thinkState.textContent = "生成中";
        } else {
            thinkContent.classList.add("hidden");
            thinkToggle.setAttribute("aria-expanded", "false");
            thinkState.textContent = "已收起";
        }
    } else {
        thinkPanel.classList.add("hidden");
    }
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
    setStreamState("connecting", "streaming");
    const id = currentConversationId();
    touchConversation(id, message.slice(0, 28) || id);
    appendMessage("user", message);
    els.chatMessage.value = "";
    autoResizeComposer();
    const pendingRow = appendMessage("assistant", "", { pending: true });

    try {
        let streamedContent = "";
        let deltaCount = 0;
        await requestChatStream({ conversationId: id, message, roleCard: roleCardPayload() }, {
            start: data => {
                setStreamState("connected", "streaming");
                if (data.conversationId) {
                    els.chatTitle.textContent = data.conversationId;
                }
            },
            delta: data => {
                streamedContent += data.content || "";
                deltaCount += 1;
                updateMessage(pendingRow, streamedContent, true);
                setStreamState(`stream ${deltaCount}`, "streaming");
            },
            done: data => {
                streamedContent = data.content || streamedContent;
                updateMessage(pendingRow, streamedContent, false);
                setStreamState("done", "ready");
            }
        });
        els.chatSubtitle.textContent = "ready";
        await loadMemory(false);
        await loadSummary();
    } catch (error) {
        updateMessage(pendingRow, `请求失败: ${error.message}`, false);
        els.chatSubtitle.textContent = "error";
        setStreamState("error", "error");
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
    if (!isSignedIn()) {
        return;
    }
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

async function loadSummary() {
    if (!isSignedIn()) {
        return;
    }
    setBusy(els.loadSummaryBtn, true);
    try {
        const data = await requestJson("GET", `/api/memory/${encodeURIComponent(currentConversationId())}/summary`);
        if (!data?.content) {
            els.summaryBox.textContent = "暂无摘要";
            return;
        }
        const updatedAt = data.updatedAt ? new Date(data.updatedAt).toLocaleString("zh-CN") : "-";
        els.summaryBox.innerHTML = `
            <div class="summary-meta">
                <span>${escapeHtml(data.conversationId)}</span>
                <span>${escapeHtml(data.messageCount)} messages</span>
                <span>${escapeHtml(updatedAt)}</span>
            </div>
            <div class="summary-content">${escapeHtml(data.content)}</div>
        `;
    } catch (error) {
        els.summaryBox.textContent = `读取失败: ${error.message}`;
    } finally {
        setBusy(els.loadSummaryBtn, false);
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
        applyToolPreset();
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

function applyToolPreset() {
    if (els.toolSelect.value === "web_search") {
        els.toolInput.value = "CJLab AI Agent";
        els.toolArguments.value = pretty({ query: "CJLab AI Agent", limit: 5 });
    } else if (els.toolSelect.value === "current_time") {
        els.toolInput.value = "now";
        els.toolArguments.value = "{}";
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
    const thinkToggle = event.target.closest(".think-toggle");
    if (thinkToggle) {
        const panel = thinkToggle.closest(".think-panel");
        const content = panel.querySelector(".think-content");
        const stateLabel = panel.querySelector(".think-state");
        const expanded = thinkToggle.getAttribute("aria-expanded") === "true";
        thinkToggle.setAttribute("aria-expanded", String(!expanded));
        content.classList.toggle("hidden", expanded);
        stateLabel.textContent = expanded ? "已收起" : "已展开";
        return;
    }

    const button = event.target.closest(".copy-message");
    if (!button) {
        return;
    }
    navigator.clipboard?.writeText(button.closest(".message").querySelector(".bubble").textContent);
});
els.clearChatBtn.addEventListener("click", showEmptyChat);
els.loadMemoryBtn.addEventListener("click", () => loadMemory(true));
els.reloadMemoryBtn.addEventListener("click", () => loadMemory(false));
els.loadSummaryBtn.addEventListener("click", loadSummary);
els.roleChatSelect.addEventListener("change", () => selectRoleCard(els.roleChatSelect.value));
els.roleCardSelect.addEventListener("change", () => selectRoleCard(els.roleCardSelect.value));
els.saveRoleCardBtn.addEventListener("click", saveCurrentRoleCard);
els.newRoleCardBtn.addEventListener("click", newRoleCard);
els.resetRoleCardsBtn.addEventListener("click", resetRoleCards);
els.saveKnowledgeBtn.addEventListener("click", saveKnowledge);
els.listKnowledgeBtn.addEventListener("click", listKnowledge);
els.searchKnowledgeBtn.addEventListener("click", searchKnowledge);
els.loadToolsBtn.addEventListener("click", loadTools);
els.toolSelect.addEventListener("change", applyToolPreset);
els.executeToolBtn.addEventListener("click", executeTool);
els.clearLogBtn.addEventListener("click", () => {
    els.requestLog.textContent = "等待请求";
});
els.tabButtons.forEach(button => button.addEventListener("click", () => switchTab(button.dataset.tab)));

switchAuthMode("login");
renderAuth();
loadConversations();
renderConversations();
renderRoleCards();
showEmptyChat();
autoResizeComposer();
loadRuntime();
verifyCurrentUser();
