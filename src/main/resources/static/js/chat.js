/**
 * 站内聊天客户端：原生 WebSocket + JSON 协议，无第三方依赖。
 * 协议见 ChatWebSocketHandler（send / history / read / presence / chat / error）。
 */
(function () {
    'use strict';

    var data = window.chatData || {};
    var me = data.me || '';
    var conversations = Array.isArray(data.conversations) ? data.conversations : [];
    var groups = Array.isArray(data.groups) ? data.groups : [];
    var depts = Array.isArray(data.depts) ? data.depts : [];

    var socket = null;
    var currentPartner = null;      // { id, name }
    var onlineUsers = [];           // 在线用户 id 集合（来自 presence）

    var convListEl = document.getElementById('chatConvList');
    var convEmptyEl = document.getElementById('chatConvEmpty');
    var headerEl = document.getElementById('chatWindowHeader');
    var msgsEl = document.getElementById('chatMsgs');
    var inputEl = document.getElementById('chatInput');
    var sendBtn = document.getElementById('chatSend');
    var stateEl = document.getElementById('chatConnState');
    var searchEl = document.getElementById('chatSearch');
    var dropdownEl = document.getElementById('chatUserDropdown');
    var clearBtn = document.getElementById('chatClearBtn');

    // ---------- 连接管理 ----------

    function connect() {
        var proto = location.protocol === 'https:' ? 'wss://' : 'ws://';
        socket = new WebSocket(proto + location.host + '/ws/chat');
        socket.onopen = function () { setOnline(true); };
        socket.onclose = function () { setOnline(false); setTimeout(connect, 3000); };
        socket.onerror = function () { /* 触发 onclose 后自动重连 */ };
        socket.onmessage = function (ev) {
            try { handleMessage(JSON.parse(ev.data)); } catch (e) { /* 忽略坏帧 */ }
        };
    }

    function setOnline(online) {
        stateEl.textContent = online ? '已连接' : '已断开，重连中…';
        stateEl.className = 'badge ' + (online ? 'text-bg-success' : 'text-bg-warning');
        refreshInputState();
    }

    function send(obj) {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify(obj));
        }
    }

    // ---------- 消息处理 ----------

    function handleMessage(msg) {
        if (msg.type === 'chat') {
            upsertConversation(msg);
            // 当前会话内：我发给对方的消息回执（msg.from===me）与对方发来的消息都渲染
            if (currentPartner && isInCurrentConversation(msg)) {
                appendMessage(msg);
                // 仅对"对方发来"的消息回执已读（自己的消息无需标记）
                if (msg.from !== me) {
                    send({ type: 'read', with: msg.from });
                }
            }
        } else if (msg.type === 'history') {
            if (currentPartner && currentPartner.id === msg.with) {
                msgsEl.innerHTML = '';
                (msg.messages || []).forEach(appendMessage);
                msgsEl.scrollTop = msgsEl.scrollHeight;
            }
        } else if (msg.type === 'presence') {
            onlineUsers = msg.users || [];
            renderConversations();
            renderHeader();
        } else if (msg.type === 'cleared') {
            // 本端清空确认，或对方清空了与我的会话 → 移除会话并清空消息区
            var partner = msg.with;
            conversations = conversations.filter(function (c) { return c.partnerId !== partner; });
            if (currentPartner && currentPartner.id === partner) {
                currentPartner = null;
                msgsEl.innerHTML = '';
                renderHeader();
                refreshInputState();
                if (clearBtn) { clearBtn.style.display = 'none'; }
            }
            renderConversations();
        } else if (msg.type === 'error') {
            var hint = document.getElementById('chatErrorHint');
            if (!hint) {
                hint = document.createElement('div');
                hint.id = 'chatErrorHint';
                hint.className = 'alert alert-danger py-1 px-2 small mb-2';
                msgsEl.insertAdjacentElement('afterend', hint);
            }
            hint.textContent = msg.message || '出错了';
        }
    }

    /** 消息是否属于当前打开会话：我发给对方 或 对方发给我 */
    function isInCurrentConversation(msg) {
        if (!currentPartner) {
            return false;
        }
        return (msg.from === me && msg.to === currentPartner.id)
            || (msg.from === currentPartner.id);
    }

    /** 收到新消息：更新（或新建）会话摘要并刷新列表 */
    function upsertConversation(msg) {
        var other = msg.from === me ? msg.to : msg.from;
        var otherName = msg.from === me ? msg.toName : msg.fromName;
        var conv = conversations.filter(function (c) { return c.partnerId === other; })[0];
        if (!conv) {
            conv = { partnerId: other, partnerName: otherName, lastContent: '', lastTimeText: '', unread: 0 };
            conversations.unshift(conv);
        } else {
            conversations.splice(conversations.indexOf(conv), 1);
            conversations.unshift(conv);
        }
        conv.lastContent = msg.content;
        conv.lastTimeText = msg.time;
        // 别人发来的且当前不在该会话中 → 未读 +1
        if (msg.from !== me && !(currentPartner && currentPartner.id === other)) {
            conv.unread = (conv.unread || 0) + 1;
        }
        renderConversations();
    }

    // ---------- 渲染 ----------

    function renderConversations() {
        convListEl.innerHTML = '';
        var hasAny = false;
        conversations.forEach(function (conv) {
            hasAny = true;
            var item = document.createElement('div');
            item.className = 'conv-item' + (currentPartner && currentPartner.id === conv.partnerId ? ' active' : '');
            item.addEventListener('click', function () {
                openConversation(conv.partnerId, conv.partnerName);
            });

            var head = document.createElement('div');
            head.className = 'conv-item-head';
            var name = document.createElement('span');
            name.className = 'conv-item-name';
            name.textContent = conv.partnerName || conv.partnerId;
            var dot = document.createElement('span');
            dot.className = 'online-dot' + (onlineUsers.indexOf(conv.partnerId) >= 0 ? ' on' : '');
            dot.title = onlineUsers.indexOf(conv.partnerId) >= 0 ? '在线' : '离线';
            head.appendChild(name);
            head.appendChild(dot);

            var last = document.createElement('div');
            last.className = 'conv-item-last';
            last.textContent = conv.lastContent || '';

            var foot = document.createElement('div');
            foot.className = 'conv-item-foot';
            var time = document.createElement('span');
            time.className = 'conv-item-time';
            time.textContent = conv.lastTimeText || '';
            var badge = document.createElement('span');
            badge.className = 'badge rounded-pill text-bg-petrol';
            badge.style.display = (conv.unread && conv.unread > 0) ? '' : 'none';
            badge.textContent = conv.unread || 0;
            foot.appendChild(time);
            foot.appendChild(badge);

            item.appendChild(head);
            item.appendChild(last);
            item.appendChild(foot);
            convListEl.appendChild(item);
        });
        convEmptyEl.style.display = hasAny ? 'none' : '';
    }

    function renderHeader() {
        headerEl.innerHTML = '';
        if (!currentPartner) {
            headerEl.appendChild(document.createTextNode('选择一个会话开始聊天'));
            return;
        }
        var name = document.createElement('span');
        name.className = 'fw-semibold';
        name.textContent = currentPartner.name || currentPartner.id;
        var dot = document.createElement('span');
        dot.className = 'online-dot ms-2' + (onlineUsers.indexOf(currentPartner.id) >= 0 ? ' on' : '');
        dot.title = onlineUsers.indexOf(currentPartner.id) >= 0 ? '在线' : '离线';
        headerEl.appendChild(name);
        headerEl.appendChild(dot);
    }

    function appendMessage(msg) {
        var mine = msg.from === me;
        var wrap = document.createElement('div');
        wrap.className = 'chat-msg ' + (mine ? 'mine' : 'theirs');

        var bubble = document.createElement('div');
        bubble.className = 'chat-bubble';
        bubble.textContent = msg.content;   // textContent 渲染，防 XSS

        var meta = document.createElement('div');
        meta.className = 'chat-msg-meta';
        var who = mine ? '我' : (msg.fromName || msg.from);
        meta.textContent = (mine ? '' : who + ' · ') + (msg.time || '');
        if (mine) { meta.textContent = (msg.time || '') + ' · 我'; }

        wrap.appendChild(bubble);
        wrap.appendChild(meta);
        msgsEl.appendChild(wrap);
        msgsEl.scrollTop = msgsEl.scrollHeight;
    }

    // ---------- 交互 ----------

    function openConversation(partnerId, partnerName) {
        currentPartner = { id: partnerId, name: partnerName };
        if (clearBtn) { clearBtn.style.display = ''; }
        conversations.forEach(function (conv) {
            if (conv.partnerId === partnerId) {
                conv.unread = 0;
            }
        });
        renderConversations();
        renderHeader();
        msgsEl.innerHTML = '';
        refreshInputState();
        inputEl.focus();
        // 拉取历史（服务端同时把对方发来的未读标记为已读）
        send({ type: 'history', with: partnerId, limit: 100 });
        send({ type: 'read', with: partnerId });
    }

    function sendMessage() {
        var content = inputEl.value.trim();
        if (!content || !currentPartner) {
            return;
        }
        send({ type: 'send', to: currentPartner.id, content: content });
        inputEl.value = '';
        inputEl.focus();
        // 发送回执由服务端 push 回来后渲染（保证与服务端一致）
    }

    function refreshInputState() {
        var ready = socket && socket.readyState === WebSocket.OPEN && currentPartner;
        inputEl.disabled = !ready;
        sendBtn.disabled = !ready;
    }

    // ---------- 联系人搜索（服务端关键字检索，避免全量下拉） ----------

    function doSearch(q) {
        fetch('/chat/users?q=' + encodeURIComponent(q), { headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                renderDropdown(res && res.code === 0 ? (res.data || []) : []);
            })
            .catch(function () { hideDropdown(); });
    }

    function renderDropdown(users) {
        dropdownEl.innerHTML = '';
        if (!users.length) {
            var empty = document.createElement('div');
            empty.className = 'chat-user-empty';
            empty.textContent = '未找到匹配的用户';
            dropdownEl.appendChild(empty);
        } else {
            users.forEach(function (u) {
                var item = document.createElement('div');
                item.className = 'chat-user-item';
                item.addEventListener('click', function () {
                    openConversation(u.userId, u.userName || u.userId);
                    hideDropdown();
                    searchEl.value = '';
                    searchEl.blur();
                });
                var name = document.createElement('span');
                name.className = 'chat-user-name';
                name.textContent = u.userName || u.userId;
                var id = document.createElement('span');
                id.className = 'chat-user-id';
                id.textContent = u.userId;
                var type = document.createElement('span');
                type.className = 'badge text-bg-secondary ms-auto';
                type.textContent = u.userType === 'INSTRUCTOR' ? '教师' : (u.userType === 'STUDENT' ? '学生' : u.userType);
                item.appendChild(name);
                item.appendChild(id);
                item.appendChild(type);
                dropdownEl.appendChild(item);
            });
        }
        dropdownEl.style.display = '';
    }

    function hideDropdown() {
        dropdownEl.style.display = 'none';
        dropdownEl.innerHTML = '';
    }

    var searchTimer = null;
    searchEl.addEventListener('input', function () {
        clearTimeout(searchTimer);
        var q = searchEl.value.trim();
        if (!q) {
            hideDropdown();
            return;
        }
        // 防抖：停止输入 250ms 后再请求
        searchTimer = setTimeout(function () { doSearch(q); }, 250);
    });
    searchEl.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            hideDropdown();
            searchEl.blur();
        }
    });
    document.addEventListener('click', function (e) {
        if (!searchEl.contains(e.target) && !dropdownEl.contains(e.target)) {
            hideDropdown();
        }
    });

    // ---------- 联系人模态框（常用分组 + 按院系浏览） ----------

    var contactsBtn = document.getElementById('chatContactsBtn');
    var contactModalEl = document.getElementById('contactModal');
    var tabSmartBtn = document.getElementById('tabSmartBtn');
    var tabDeptBtn = document.getElementById('tabDeptBtn');
    var tabSmartPane = document.getElementById('tabSmartPane');
    var tabDeptPane = document.getElementById('tabDeptPane');
    var smartGroupsEl = document.getElementById('smartGroups');
    var smartGroupEmptyEl = document.getElementById('smartGroupEmpty');
    var deptSelectEl = document.getElementById('deptSelect');
    var deptSearchEl = document.getElementById('deptSearch');
    var deptUsersEl = document.getElementById('deptUsers');

    function showContactModal() {
        if (!contactModalEl) {
            return;
        }
        if (window.bootstrap) {
            window.bootstrap.Modal.getOrCreateInstance(contactModalEl).show();
            return;
        }
        // 兜底：Bootstrap JS 未加载时手动显示（含半透明遮罩）
        contactModalEl.classList.add('show', 'd-block');
        contactModalEl.style.display = 'block';
        var backdrop = document.createElement('div');
        backdrop.className = 'modal-backdrop fade show';
        backdrop.id = 'contactModalBackdrop';
        document.body.appendChild(backdrop);
    }

    function hideContactModal() {
        if (!contactModalEl) {
            return;
        }
        if (window.bootstrap) {
            window.bootstrap.Modal.getInstance(contactModalEl).hide();
            return;
        }
        contactModalEl.classList.remove('show', 'd-block');
        contactModalEl.style.display = '';
        var backdrop = document.getElementById('contactModalBackdrop');
        if (backdrop) {
            backdrop.remove();
        }
    }

    function switchTab(tab) {
        var smart = tab === 'smart';
        tabSmartBtn.classList.toggle('active', smart);
        tabDeptBtn.classList.toggle('active', !smart);
        tabSmartPane.style.display = smart ? '' : 'none';
        tabDeptPane.style.display = smart ? 'none' : '';
    }

    /** 渲染一个可点击的用户条目（供分组/院系列表共用） */
    function appendContactUser(container, u) {
        var item = document.createElement('div');
        item.className = 'contact-user-item';
        item.addEventListener('click', function () {
            openConversation(u.userId, u.userName || u.userId);
            hideContactModal();
        });
        var name = document.createElement('span');
        name.className = 'contact-user-name';
        name.textContent = u.userName || u.userId;
        var id = document.createElement('span');
        id.className = 'contact-user-id';
        id.textContent = u.userId;
        var type = document.createElement('span');
        type.className = 'badge text-bg-secondary';
        type.textContent = u.userType === 'INSTRUCTOR' ? '教师' : (u.userType === 'STUDENT' ? '学生' : u.userType);
        var dot = document.createElement('span');
        dot.className = 'online-dot ms-auto' + (onlineUsers.indexOf(u.userId) >= 0 ? ' on' : '');
        dot.title = onlineUsers.indexOf(u.userId) >= 0 ? '在线' : '离线';
        item.appendChild(name);
        item.appendChild(id);
        item.appendChild(type);
        item.appendChild(dot);
        container.appendChild(item);
    }

    function renderSmartGroups() {
        if (!smartGroupsEl || !smartGroupEmptyEl) {
            return;
        }
        smartGroupsEl.innerHTML = '';
        var hasAny = false;
        groups.forEach(function (g) {
            if (!g.users || !g.users.length) {
                return;
            }
            hasAny = true;
            var head = document.createElement('div');
            head.className = 'contact-group-title';
            head.textContent = g.title + '（' + g.users.length + '）';
            smartGroupsEl.appendChild(head);
            var list = document.createElement('div');
            list.className = 'contact-user-list';
            g.users.forEach(function (u) { appendContactUser(list, u); });
            smartGroupsEl.appendChild(list);
        });
        smartGroupEmptyEl.style.display = hasAny ? 'none' : '';
    }

    function renderDeptSelect() {
        if (!deptSelectEl) {
            return;
        }
        deptSelectEl.innerHTML = '<option value="">— 选择院系 —</option>';
        depts.forEach(function (d) {
            var opt = document.createElement('option');
            opt.value = d.deptName;
            opt.textContent = d.deptName + '（学生 ' + d.studentCount + ' · 教师 ' + d.instructorCount + '）';
            deptSelectEl.appendChild(opt);
        });
    }

    function fetchDeptUsers() {
        var dept = deptSelectEl.value;
        if (!dept) {
            deptUsersEl.innerHTML = '';
            var hint = document.createElement('p');
            hint.className = 'hint mb-0';
            hint.textContent = '请先选择院系。';
            deptUsersEl.appendChild(hint);
            return;
        }
        var q = deptSearchEl.value.trim();
        var url = '/chat/users?dept=' + encodeURIComponent(dept) + '&limit=100';
        if (q) {
            url += '&q=' + encodeURIComponent(q);
        }
        fetch(url, { headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                deptUsersEl.innerHTML = '';
                var users = res && res.code === 0 ? (res.data || []) : [];
                if (!users.length) {
                    var empty = document.createElement('p');
                    empty.className = 'hint mb-0';
                    empty.textContent = q ? '该院系下没有匹配的用户。' : '该院系暂无有账号的师生。';
                    deptUsersEl.appendChild(empty);
                    return;
                }
                var list = document.createElement('div');
                list.className = 'contact-user-list';
                users.forEach(function (u) { appendContactUser(list, u); });
                deptUsersEl.appendChild(list);
            })
            .catch(function () {
                deptUsersEl.innerHTML = '';
                var err = document.createElement('p');
                err.className = 'hint mb-0';
                err.textContent = '加载失败，请重试。';
                deptUsersEl.appendChild(err);
            });
    }

    var deptSearchTimer = null;
    // 元素缺失时仅跳过对应功能，不阻塞聊天主体（防御性处理）
    if (contactsBtn) { contactsBtn.addEventListener('click', showContactModal); }
    if (tabSmartBtn) { tabSmartBtn.addEventListener('click', function () { switchTab('smart'); }); }
    if (tabDeptBtn) { tabDeptBtn.addEventListener('click', function () { switchTab('dept'); }); }
    if (deptSelectEl) { deptSelectEl.addEventListener('change', function () { fetchDeptUsers(); }); }
    if (deptSearchEl) {
        deptSearchEl.addEventListener('input', function () {
            clearTimeout(deptSearchTimer);
            deptSearchTimer = setTimeout(fetchDeptUsers, 250);
        });
    }
    if (contactModalEl) {
        // 模态框每次打开时刷新在线状态渲染
        contactModalEl.addEventListener('shown.bs.modal', function () {
            renderSmartGroups();
        });
    }

    // ---------- 事件绑定与启动 ----------

    sendBtn.addEventListener('click', sendMessage);
    inputEl.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
    if (clearBtn) {
        clearBtn.addEventListener('click', function () {
            if (!currentPartner) {
                return;
            }
            var name = currentPartner.name || currentPartner.id;
            if (!confirm('确定清空与 ' + name + ' 的全部聊天记录吗？此操作双方都不可恢复。')) {
                return;
            }
            send({ type: 'clear', with: currentPartner.id });
        });
    }

    renderConversations();
    renderSmartGroups();
    renderDeptSelect();
    connect();
})();
