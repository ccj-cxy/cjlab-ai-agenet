const AUTH_KEY = "cjlab-agent-auth";
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
    roleCards: [],
    selectedRoleId: localStorage.getItem(SELECTED_ROLE_CARD_KEY) || "default",
    dirty: false
};

const els = {
    pageStatus: document.querySelector("#rolePageStatus"),
    signedInBox: document.querySelector("#roleSignedInBox"),
    currentUserName: document.querySelector("#roleCurrentUserName"),
    currentUserEmail: document.querySelector("#roleCurrentUserEmail"),
    logoutBtn: document.querySelector("#roleLogoutBtn"),
    authForm: document.querySelector("#roleAuthForm"),
    loginModeBtn: document.querySelector("#roleLoginModeBtn"),
    registerModeBtn: document.querySelector("#roleRegisterModeBtn"),
    emailInput: document.querySelector("#roleEmailInput"),
    passwordInput: document.querySelector("#rolePasswordInput"),
    displayNameGroup: document.querySelector("#roleDisplayNameGroup"),
    displayNameInput: document.querySelector("#roleDisplayNameInput"),
    authSubmitBtn: document.querySelector("#roleAuthSubmitBtn"),
    authStatus: document.querySelector("#roleAuthStatus"),
    reloadBtn: document.querySelector("#roleReloadBtn"),
    resetDefaultsBtn: document.querySelector("#roleResetDefaultsBtn"),
    newBtn: document.querySelector("#roleNewBtn"),
    duplicateBtn: document.querySelector("#roleDuplicateBtn"),
    deleteBtn: document.querySelector("#roleDeleteBtn"),
    saveBtn: document.querySelector("#roleSaveBtn"),
    countBadge: document.querySelector("#roleCountBadge"),
    list: document.querySelector("#roleCardList"),
    idInput: document.querySelector("#roleIdInput"),
    nameInput: document.querySelector("#roleNameInput"),
    avatarInput: document.querySelector("#roleAvatarInput"),
    avatarFileInput: document.querySelector("#roleAvatarFileInput"),
    clearAvatarBtn: document.querySelector("#roleClearAvatarBtn"),
    avatarPreview: document.querySelector("#roleAvatarPreview"),
    descriptionInput: document.querySelector("#roleDescriptionInput"),
    instructionInput: document.querySelector("#roleInstructionInput"),
    editorStatus: document.querySelector("#roleEditorStatus"),
    updatedAt: document.querySelector("#roleUpdatedAt"),
    previewId: document.querySelector("#rolePreviewId"),
    previewBox: document.querySelector("#rolePreviewBox"),
    workspaceSubtitle: document.querySelector("#roleWorkspaceSubtitle")
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

function renderAvatarPreview(card) {
    const avatar = String(card?.avatar || "").trim();
    if (avatar) {
        els.avatarPreview.innerHTML = `<img src="${escapeHtml(avatar)}" alt="">`;
    } else {
        els.avatarPreview.textContent = roleInitials(card);
    }
}

function setBusy(button, busy) {
    if (button) {
        button.disabled = busy;
    }
}

function setStatus(target, message, type = "") {
    target.textContent = message;
    target.classList.toggle("ok", type === "ok");
    target.classList.toggle("error", type === "error");
}

async function requestJson(method, path, body, options = {}) {
    const headers = {
        Accept: "application/json",
        ...(options.auth === false ? {} : authHeaders())
    };
    const init = { method, headers };
    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
        init.body = JSON.stringify(body);
    }
    const response = await fetch(path, init);
    const text = await response.text();
    const payload = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(payload?.message || payload?.error || `HTTP ${response.status}`);
    }
    return payload;
}

function switchAuthMode(mode) {
    state.authMode = mode;
    els.loginModeBtn.classList.toggle("active", mode === "login");
    els.registerModeBtn.classList.toggle("active", mode === "register");
    els.displayNameGroup.classList.toggle("hidden", mode !== "register");
    els.authSubmitBtn.textContent = mode === "login" ? "登录" : "注册";
    setStatus(els.authStatus, mode === "login" ? "使用邮箱和密码登录" : "注册后会自动登录");
}

function renderAuth() {
    const signedIn = isSignedIn();
    const user = state.auth?.user;
    els.signedInBox.classList.toggle("hidden", !signedIn);
    els.logoutBtn.classList.toggle("hidden", !signedIn);
    els.authForm.classList.toggle("hidden", signedIn);
    els.currentUserName.textContent = user?.displayName || user?.email || "已登录";
    els.currentUserEmail.textContent = user?.email || "";
    els.pageStatus.textContent = signedIn ? "signed in" : "locked";
    els.workspaceSubtitle.textContent = signedIn ? "按当前登录人隔离保存" : "请先登录";
    [els.reloadBtn, els.resetDefaultsBtn, els.newBtn, els.duplicateBtn, els.deleteBtn, els.saveBtn]
        .forEach(button => button.disabled = !signedIn);
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
        setStatus(els.authStatus, "登录成功", "ok");
        await loadRoleCards();
    } catch (error) {
        setStatus(els.authStatus, `失败: ${error.message}`, "error");
    } finally {
        setBusy(els.authSubmitBtn, false);
    }
}

async function verifyCurrentUser() {
    if (!isSignedIn()) {
        renderAuth();
        renderRoleCards();
        return;
    }
    try {
        const user = await requestJson("GET", "/api/users/me");
        saveAuth({ ...state.auth, user });
        setStatus(els.authStatus, "登录态有效", "ok");
        await loadRoleCards();
    } catch {
        saveAuth(null);
        state.roleCards = [];
        setStatus(els.authStatus, "登录态已失效，请重新登录", "error");
        renderRoleCards();
    }
}

function logout() {
    saveAuth(null);
    state.roleCards = [];
    state.selectedRoleId = "default";
    setStatus(els.authStatus, "已退出");
    renderRoleCards();
}

function normalizeId(value) {
    const normalized = String(value || "")
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9_-]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 64);
    return normalized || `role-${Date.now()}`;
}

function mergeRoleCards(baseCards, persistedCards) {
    const merged = new Map();
    baseCards.forEach(card => merged.set(card.id, { ...card, source: "default" }));
    (persistedCards || []).forEach(card => {
        if (card?.id) {
            merged.set(card.id, {
                id: card.id,
                name: card.name,
                description: card.description || "",
                instruction: card.instruction || "",
                avatar: card.avatar || "",
                updatedAt: card.updatedAt || null,
                source: "database"
            });
        }
    });
    return Array.from(merged.values());
}

async function loadRoleCards() {
    if (!isSignedIn()) {
        state.roleCards = DEFAULT_ROLE_CARDS.map(card => ({ ...card, source: "default" }));
        renderRoleCards();
        return;
    }
    setBusy(els.reloadBtn, true);
    try {
        const data = await requestJson("GET", "/api/role-cards");
        state.roleCards = mergeRoleCards(DEFAULT_ROLE_CARDS, data);
        if (!state.roleCards.some(card => card.id === state.selectedRoleId)) {
            state.selectedRoleId = state.roleCards[0]?.id || "default";
        }
        localStorage.setItem(ROLE_CARD_KEY, JSON.stringify(state.roleCards));
        renderRoleCards();
        setStatus(els.editorStatus, "已加载角色卡", "ok");
    } catch (error) {
        setStatus(els.editorStatus, `加载失败: ${error.message}`, "error");
    } finally {
        setBusy(els.reloadBtn, false);
    }
}

function selectedRoleCard() {
    return state.roleCards.find(card => card.id === state.selectedRoleId) || state.roleCards[0] || null;
}

function renderRoleCards() {
    els.countBadge.textContent = String(state.roleCards.length);
    if (state.roleCards.length === 0) {
        els.list.textContent = isSignedIn() ? "暂无角色卡" : "登录后加载角色卡";
        renderEditor(null);
        return;
    }
    if (!state.roleCards.some(card => card.id === state.selectedRoleId)) {
        state.selectedRoleId = state.roleCards[0].id;
    }
    els.list.innerHTML = state.roleCards.map(card => `
        <button class="role-card-list-item ${card.id === state.selectedRoleId ? "active" : ""}" type="button" data-id="${escapeHtml(card.id)}">
            ${roleAvatarHtml(card)}
            <span class="role-card-list-text">
                <span class="role-card-name">${escapeHtml(card.name || card.id)}</span>
                <span class="role-card-id">${escapeHtml(card.id)} · ${escapeHtml(card.source || "local")}</span>
            </span>
        </button>
    `).join("");
    renderEditor(selectedRoleCard());
}

function renderEditor(card) {
    if (!card) {
        els.idInput.value = "";
        els.nameInput.value = "";
        els.avatarInput.value = "";
        els.descriptionInput.value = "";
        els.instructionInput.value = "";
        renderAvatarPreview(null);
        els.previewId.textContent = "-";
        els.previewBox.textContent = "暂无数据";
        els.updatedAt.textContent = "-";
        return;
    }
    els.idInput.value = card.id || "";
    els.nameInput.value = card.name || "";
    els.avatarInput.value = card.avatar || "";
    els.descriptionInput.value = card.description || "";
    els.instructionInput.value = card.instruction || "";
    renderAvatarPreview(card);
    els.updatedAt.textContent = card.updatedAt ? new Date(card.updatedAt).toLocaleString("zh-CN") : "未保存到数据库";
    renderPreview();
    state.dirty = false;
}

function renderPreview() {
    const id = normalizeId(els.idInput.value || els.nameInput.value);
    const name = els.nameInput.value.trim() || "未命名角色";
    const avatar = els.avatarInput.value.trim();
    const description = els.descriptionInput.value.trim();
    const instruction = els.instructionInput.value.trim();
    const card = { id, name, avatar };
    els.previewId.textContent = id;
    renderAvatarPreview(card);
    els.previewBox.innerHTML = `
        <div class="result-item">
            <div class="result-title">
                <span class="role-preview-heading">${roleAvatarHtml(card)}<span>${escapeHtml(name)}</span></span>
                <span>${escapeHtml(id)}</span>
            </div>
            <div class="result-meta">${escapeHtml(description || "-")}</div>
            <div class="result-content">${escapeHtml(instruction || "-")}</div>
        </div>
    `;
}

function selectRole(id) {
    state.selectedRoleId = id;
    localStorage.setItem(SELECTED_ROLE_CARD_KEY, id);
    renderRoleCards();
    setStatus(els.editorStatus, "已选择角色");
}

function editorCard() {
    return {
        id: normalizeId(els.idInput.value || els.nameInput.value),
        name: els.nameInput.value.trim() || "未命名角色",
        description: els.descriptionInput.value.trim(),
        instruction: els.instructionInput.value.trim(),
        avatar: els.avatarInput.value.trim()
    };
}

function newRoleCard() {
    const id = `role-${Date.now()}`;
    const card = {
        id,
        name: "自定义角色",
        description: "",
        instruction: "",
        avatar: "",
        source: "local"
    };
    state.roleCards.unshift(card);
    state.selectedRoleId = id;
    renderRoleCards();
    setStatus(els.editorStatus, "新角色未保存");
    els.nameInput.focus();
}

function duplicateRoleCard() {
    const current = selectedRoleCard();
    if (!current) {
        return;
    }
    const id = `${normalizeId(current.id)}-copy-${Date.now().toString().slice(-4)}`;
    state.roleCards.unshift({
        ...current,
        id,
        name: `${current.name || current.id} 副本`,
        updatedAt: null,
        source: "local"
    });
    state.selectedRoleId = id;
    renderRoleCards();
    setStatus(els.editorStatus, "副本未保存");
}

async function saveRoleCard() {
    if (!isSignedIn()) {
        setStatus(els.editorStatus, "请先登录", "error");
        return;
    }
    setBusy(els.saveBtn, true);
    try {
        const card = editorCard();
        const saved = await requestJson("POST", "/api/role-cards", card);
        const normalized = { ...card, ...saved, source: "database" };
        const existingIndex = state.roleCards.findIndex(item => item.id === state.selectedRoleId || item.id === normalized.id);
        if (existingIndex >= 0) {
            state.roleCards.splice(existingIndex, 1, normalized);
        } else {
            state.roleCards.unshift(normalized);
        }
        state.selectedRoleId = normalized.id;
        localStorage.setItem(SELECTED_ROLE_CARD_KEY, normalized.id);
        localStorage.setItem(ROLE_CARD_KEY, JSON.stringify(state.roleCards));
        renderRoleCards();
        setStatus(els.editorStatus, "保存成功", "ok");
    } catch (error) {
        setStatus(els.editorStatus, `保存失败: ${error.message}`, "error");
    } finally {
        setBusy(els.saveBtn, false);
    }
}

async function deleteRoleCard() {
    const current = selectedRoleCard();
    if (!current || !isSignedIn()) {
        return;
    }
    const confirmed = window.confirm(`删除角色卡 "${current.name || current.id}"？`);
    if (!confirmed) {
        return;
    }
    setBusy(els.deleteBtn, true);
    try {
        await requestJson("DELETE", `/api/role-cards/${encodeURIComponent(current.id)}`);
        state.roleCards = state.roleCards.filter(card => card.id !== current.id);
        state.selectedRoleId = state.roleCards[0]?.id || "default";
        localStorage.setItem(SELECTED_ROLE_CARD_KEY, state.selectedRoleId);
        localStorage.setItem(ROLE_CARD_KEY, JSON.stringify(state.roleCards));
        renderRoleCards();
        setStatus(els.editorStatus, "删除成功", "ok");
    } catch (error) {
        setStatus(els.editorStatus, `删除失败: ${error.message}`, "error");
    } finally {
        setBusy(els.deleteBtn, false);
    }
}

async function resetDefaults() {
    if (!isSignedIn()) {
        setStatus(els.editorStatus, "请先登录", "error");
        return;
    }
    setBusy(els.resetDefaultsBtn, true);
    try {
        const savedCards = await requestJson("POST", "/api/role-cards/defaults");
        state.roleCards = mergeRoleCards(DEFAULT_ROLE_CARDS, savedCards);
        state.selectedRoleId = "default";
        localStorage.setItem(SELECTED_ROLE_CARD_KEY, state.selectedRoleId);
        localStorage.setItem(ROLE_CARD_KEY, JSON.stringify(state.roleCards));
        renderRoleCards();
        setStatus(els.editorStatus, "默认角色已写入数据库", "ok");
    } catch (error) {
        setStatus(els.editorStatus, `恢复失败: ${error.message}`, "error");
    } finally {
        setBusy(els.resetDefaultsBtn, false);
    }
}

function markDirty() {
    state.dirty = true;
    renderPreview();
    setStatus(els.editorStatus, "有未保存修改");
}

function readAvatarFile(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result || ""));
        reader.onerror = () => reject(reader.error || new Error("头像读取失败"));
        reader.readAsDataURL(file);
    });
}

async function selectAvatarFile() {
    const file = els.avatarFileInput.files?.[0];
    if (!file) {
        return;
    }
    if (!file.type.startsWith("image/")) {
        setStatus(els.editorStatus, "请选择图片文件", "error");
        els.avatarFileInput.value = "";
        return;
    }
    if (file.size > 160 * 1024) {
        setStatus(els.editorStatus, "头像图片不能超过 160KB", "error");
        els.avatarFileInput.value = "";
        return;
    }
    try {
        els.avatarInput.value = await readAvatarFile(file);
        markDirty();
        setStatus(els.editorStatus, "头像已载入，保存后入库");
    } catch (error) {
        setStatus(els.editorStatus, `头像读取失败: ${error.message}`, "error");
    } finally {
        els.avatarFileInput.value = "";
    }
}

function clearAvatar() {
    els.avatarInput.value = "";
    markDirty();
    setStatus(els.editorStatus, "头像已清除，保存后生效");
}

els.loginModeBtn.addEventListener("click", () => switchAuthMode("login"));
els.registerModeBtn.addEventListener("click", () => switchAuthMode("register"));
els.authForm.addEventListener("submit", submitAuth);
els.logoutBtn.addEventListener("click", logout);
els.reloadBtn.addEventListener("click", loadRoleCards);
els.resetDefaultsBtn.addEventListener("click", resetDefaults);
els.newBtn.addEventListener("click", newRoleCard);
els.duplicateBtn.addEventListener("click", duplicateRoleCard);
els.deleteBtn.addEventListener("click", deleteRoleCard);
els.saveBtn.addEventListener("click", saveRoleCard);
els.avatarFileInput.addEventListener("change", selectAvatarFile);
els.clearAvatarBtn.addEventListener("click", clearAvatar);
els.list.addEventListener("click", event => {
    const item = event.target.closest(".role-card-list-item");
    if (item) {
        selectRole(item.dataset.id);
    }
});
[els.idInput, els.nameInput, els.avatarInput, els.descriptionInput, els.instructionInput]
    .forEach(input => input.addEventListener("input", markDirty));

switchAuthMode("login");
renderAuth();
loadRoleCards();
verifyCurrentUser();
