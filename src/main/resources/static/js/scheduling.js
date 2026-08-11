/**
 * 排课看板交互：
 *  - 拖拽小方块到「教室 x 时间段」表格完成排课，校验不通过时方块回到原位
 *  - 拖动时实时预判目标教师的时间冲突，冲突格子标红
 *  - 待安排课程支持关键词搜索过滤
 *  - 支持切换到「周课表」视图（按教室/教师筛选）
 * 全部事件挂在 document 上，看板被替换后依然有效；不依赖全局 bootstrap 对象。
 */
(function () {
    'use strict';

    var PAGE = '/admin/scheduling';

    function $(id) {
        return document.getElementById(id);
    }

    function hasBootstrap() {
        return typeof window.bootstrap !== 'undefined' && window.bootstrap &&
               typeof window.bootstrap.Toast !== 'undefined';
    }

    function termQuery() {
        return 'semester=' + encodeURIComponent($('semester').value) +
               '&year=' + encodeURIComponent($('year').value);
    }

    function showToast(message, type) {
        var wrap = $('schedToastWrap');
        if (!wrap) { return; }
        try {
            var toast = document.createElement('div');
            toast.className = 'toast align-items-center border-0 show text-bg-' + (type || 'info');
            toast.setAttribute('role', 'alert');
            toast.innerHTML = '<div class="d-flex"><div class="toast-body">' + message + '</div>' +
                '<button type="button" class="btn-close btn-close-white me-2 m-auto"></button></div>';
            toast.style.display = 'block';
            wrap.appendChild(toast);
            function remove() {
                toast.classList.remove('show');
                setTimeout(function () {
                    if (toast.parentNode) { toast.parentNode.removeChild(toast); }
                }, 200);
            }
            var closeBtn = toast.querySelector('.btn-close');
            if (closeBtn) { closeBtn.addEventListener('click', remove); }
            setTimeout(remove, 3200);
        } catch (err) { /* 提示组件失败不影响主流程 */ }
    }

    /* ---------- 看板内置数据（由服务端渲染进页面） ---------- */
    function boardData() {
        return window._schedBoardData || { timeSlotDays: {}, teacherLoad: {} };
    }

    /* "M 08:00-08:50" -> {day:'M', start:480, end:530} */
    function periodOf(str) {
        var sp = str.indexOf(' ');
        var day = str.substring(0, sp);
        var range = str.substring(sp + 1);
        var dash = range.indexOf('-');
        function toMin(t) {
            var p = t.split(':');
            return parseInt(p[0], 10) * 60 + parseInt(p[1], 10);
        }
        return { day: day, start: toMin(range.substring(0, dash)), end: toMin(range.substring(dash + 1)) };
    }

    function daysOf(timeSlotId) {
        var list = boardData().timeSlotDays[timeSlotId] || [];
        var out = [];
        for (var i = 0; i < list.length; i++) {
            out.push(periodOf(list[i]));
        }
        return out;
    }

    /* 两个时间段是否存在“同一天且有重叠” */
    function overlaps(a, b) {
        for (var i = 0; i < a.length; i++) {
            for (var j = 0; j < b.length; j++) {
                if (a[i].day === b[j].day && a[i].start < b[j].end && b[j].start < a[i].end) {
                    return true;
                }
            }
        }
        return false;
    }

    /* 某教师本学期已占用时段 vs 每个目标时段，返回冲突的时间段标识集合 */
    function computeTeacherConflicts(instructorId, excludeKey) {
        var data = boardData();
        var load = data.teacherLoad[instructorId] || [];
        var occupied = [];
        for (var i = 0; i < load.length; i++) {
            var parts = load[i].split('|');
            if ((parts[0] + '|' + parts[1]) === excludeKey) { continue; }
            occupied.push(parts[2]);
        }
        var conflicts = {};
        var targets = Object.keys(data.timeSlotDays);
        for (var t = 0; t < targets.length; t++) {
            var tid = targets[t];
            var targetDays = daysOf(tid);
            for (var o = 0; o < occupied.length; o++) {
                if (overlaps(targetDays, daysOf(occupied[o]))) {
                    conflicts[tid] = true;
                    break;
                }
            }
        }
        return conflicts;
    }

    function refreshBoard() {
        window._currentView = 'board';
        return fetch(PAGE + '/board?' + termQuery(), { headers: { 'Accept': 'text/html' } })
            .then(function (r) { return r.text(); })
            .then(function (html) {
                var holder = $('schedBoard');
                var view = $('viewArea');
                if (holder) { holder.outerHTML = html; }
                else if (view) { view.innerHTML = html; }
                applyPoolFilter();
                syncTabs('board');
            })
            .catch(function () {
                showToast('看板刷新失败，请手动重新加载', 'danger');
            });
    }

    function refreshWeek() {
        window._currentView = 'week';
        var q = termQuery();
        var typeEl = $('weekType');
        var keyEl = $('weekKey');
        var type = typeEl ? typeEl.value : 'all';
        var key = keyEl ? keyEl.value : '';
        q += '&type=' + encodeURIComponent(type) + '&key=' + encodeURIComponent(key);
        return fetch(PAGE + '/week?' + q, { headers: { 'Accept': 'text/html' } })
            .then(function (r) { return r.text(); })
            .then(function (html) {
                var view = $('viewArea');
                if (view) { view.innerHTML = html; }
                syncTabs('week');
            })
            .catch(function () {
                showToast('周课表加载失败，请重试', 'danger');
            });
    }

    function syncTabs(view) {
        var tb = $('tabBoard');
        var tw = $('tabWeek');
        if (tb) { tb.classList.toggle('active', view === 'board'); }
        if (tw) { tw.classList.toggle('active', view === 'week'); }
    }

    function switchView(view) {
        if (view === 'week') { return refreshWeek(); }
        return refreshBoard();
    }

    /* ---------- 待排课搜索过滤 ---------- */
    function applyPoolFilter() {
        var q = window._poolSearch || '';
        var input = $('poolSearch');
        if (input) { input.value = q; }
        var cards = document.querySelectorAll('#poolArea .sched-card');
        var shown = 0;
        for (var i = 0; i < cards.length; i++) {
            var hit = !q || cards[i].textContent.toLowerCase().indexOf(q.toLowerCase()) !== -1;
            cards[i].style.display = hit ? '' : 'none';
            if (hit) { shown++; }
        }
        var noMatch = $('poolNoMatch');
        if (noMatch) {
            noMatch.style.display = (cards.length > 0 && shown === 0) ? '' : 'none';
        }
    }

    function shake(el) {
        if (!el) { return; }
        el.classList.remove('shake');
        void el.offsetWidth;
        el.classList.add('shake');
    }

    /* 读取页面 meta 中的 CSRF token */
    function csrfToken() {
        var meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
    }

    /* 发送表单请求：非 JSON 响应视为失败（如未登录被重定向、服务器异常页） */
    function postForm(url, params) {
        return fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-TOKEN': csrfToken() },
            body: params
        }).then(function (r) {
            var ct = r.headers.get('content-type') || '';
            if (ct.indexOf('application/json') !== -1) {
                return r.json();
            }
            throw new Error('服务器返回异常（可能未登录或会话已过期）');
        });
    }

    function clearDragOver() {
        var cells = document.querySelectorAll('.drag-over, .cell-teacher-conflict');
        for (var i = 0; i < cells.length; i++) {
            cells[i].classList.remove('drag-over');
            cells[i].classList.remove('cell-teacher-conflict');
        }
    }

    /* ---------- 新建待排课班弹窗：优先 Bootstrap，缺失时手动降级 ---------- */

    function showModal(id) {
        var modal = $(id);
        if (!modal) { return; }
        if (hasBootstrap()) {
            new window.bootstrap.Modal(modal).show();
            return;
        }
        modal.style.display = 'block';
        modal.classList.add('show');
        modal.setAttribute('aria-hidden', 'false');
        var backdrop = document.createElement('div');
        backdrop.className = 'modal-backdrop fade show';
        backdrop.setAttribute('data-sched-backdrop', '1');
        document.body.appendChild(backdrop);
        document.body.classList.add('modal-open');
    }

    function hideModal(id) {
        var modal = $(id);
        if (!modal) { return; }
        if (hasBootstrap() && window.bootstrap.Modal && window.bootstrap.Modal.getInstance(modal)) {
            window.bootstrap.Modal.getInstance(modal).hide();
            return;
        }
        modal.classList.remove('show');
        modal.style.display = 'none';
        modal.setAttribute('aria-hidden', 'true');
        var backdrops = document.querySelectorAll('.modal-backdrop');
        for (var i = 0; i < backdrops.length; i++) {
            if (backdrops[i].getAttribute('data-sched-backdrop') === '1') {
                backdrops[i].remove();
            }
        }
        document.body.classList.remove('modal-open');
        document.body.style.removeProperty('padding-right');
    }

    /* 无 Bootstrap 时手动接管弹窗开关；有 Bootstrap 时交给 data-* 属性处理 */
    document.addEventListener('click', function (e) {
        if (hasBootstrap()) { return; }
        var openBtn = e.target.closest('#openCreateModalBtn');
        if (openBtn) {
            e.preventDefault();
            showModal('createSectionModal');
            return;
        }
        var dismiss = e.target.closest('[data-bs-dismiss="modal"]');
        if (dismiss) {
            hideModal('createSectionModal');
            return;
        }
        if (e.target.closest('.modal-backdrop')) {
            hideModal('createSectionModal');
        }
    });

    /* ---------- 学期筛选 / 周课表查询 / 视图切换 ---------- */
    document.addEventListener('submit', function (e) {
        if (e.target.id === 'termForm') {
            e.preventDefault();
            if (window._currentView === 'week') { refreshWeek(); } else { refreshBoard(); }
        } else if (e.target.id === 'weekForm') {
            e.preventDefault();
            refreshWeek();
        }
    });

    document.addEventListener('click', function (e) {
        if (e.target.closest('#tabBoard')) {
            e.preventDefault();
            switchView('board');
        } else if (e.target.closest('#tabWeek')) {
            e.preventDefault();
            switchView('week');
        }
    });

    document.addEventListener('change', function (e) {
        if (e.target && e.target.id === 'weekType') {
            var col = $('weekKeyCol');
            if (col) { col.style.display = e.target.value === 'all' ? 'none' : ''; }
        }
    });

    document.addEventListener('input', function (e) {
        if (e.target && e.target.id === 'poolSearch') {
            window._poolSearch = e.target.value;
            applyPoolFilter();
        }
    });

    /* ---------- 拖拽开始：记录来源 + 计算教师冲突预判 ---------- */
    document.addEventListener('dragstart', function (e) {
        var el = e.target.closest('[draggable="true"][data-course-id]');
        if (!el || !$('schedBoard')) { return; }
        var d = {
            courseId: el.getAttribute('data-course-id'),
            secId: el.getAttribute('data-sec-id'),
            semester: el.getAttribute('data-semester'),
            year: el.getAttribute('data-year'),
            instructorId: el.getAttribute('data-instructor-id') || '',
            building: el.getAttribute('data-building') || '',
            room: el.getAttribute('data-room') || ''
        };
        window._schedDrag = { data: d, from: el };
        window._schedConflicts = d.instructorId
            ? computeTeacherConflicts(d.instructorId, d.courseId + '|' + d.secId)
            : null;
        e.dataTransfer.setData('text/plain', JSON.stringify(d));
        e.dataTransfer.effectAllowed = 'move';
        el.classList.add('dragging');
    });

    document.addEventListener('dragend', function () {
        var el = window._schedDrag && window._schedDrag.from;
        if (el) { el.classList.remove('dragging'); }
        window._schedDrag = null;
        window._schedConflicts = null;
        clearDragOver();
    });

    /* ---------- 拖拽经过：格子高亮（含教师冲突标红） ---------- */
    document.addEventListener('dragover', function (e) {
        var cell = e.target.closest('.sched-cell');
        if (cell) {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            cell.classList.add('drag-over');
            if (window._schedConflicts && window._schedConflicts[cell.getAttribute('data-timeslot')]) {
                cell.classList.add('cell-teacher-conflict');
            }
            return;
        }
        var pool = e.target.closest('#poolArea');
        if (pool) {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            pool.classList.add('drag-over');
        }
    });

    document.addEventListener('dragleave', function (e) {
        var cell = e.target.closest('.sched-cell');
        if (cell) {
            cell.classList.remove('drag-over');
            return;
        }
        var pool = e.target.closest('#poolArea');
        if (pool) { pool.classList.remove('drag-over'); }
    });

    /* ---------- 拖拽放下：放入格子 = 排课 ---------- */
    document.addEventListener('drop', function (e) {
        var cell = e.target.closest('.sched-cell');
        if (!cell) { return; }
        e.preventDefault();
        cell.classList.remove('drag-over');
        var drag = window._schedDrag;
        if (!drag) { return; }
        var d = drag.data;
        var building = cell.getAttribute('data-building');
        var room = cell.getAttribute('data-room');
        var ts = cell.getAttribute('data-timeslot');

        /* 目标格子已被其他班占用：直接拒绝，方块回到原位 */
        var occupied = cell.querySelector('.sched-block');
        if (occupied && occupied !== drag.from) {
            showToast('无法排课：' + building + ' ' + room + ' 在 ' + ts + ' 时段已被其他开课班占用', 'danger');
            shake(drag.from);
            return;
        }

        var params = new URLSearchParams();
        params.set('courseId', d.courseId);
        params.set('secId', d.secId);
        params.set('semester', d.semester);
        params.set('year', d.year);
        params.set('building', building);
        params.set('roomNumber', room);
        params.set('timeSlotId', ts);
        params.set('instructorId', d.instructorId);

        postForm(PAGE + '/assign', params).then(function (res) {
            if (res.code === 0) {
                showToast('排课成功：' + d.courseId + ' 第 ' + d.secId + ' 班 → ' +
                    building + ' ' + room + '（' + ts + '）', 'success');
                refreshBoard();
            } else {
                showToast('排课失败：' + res.message, 'danger');
                shake(drag.from);
            }
        }).catch(function (err) {
            showToast(err && err.message ? err.message : '网络异常，请重试', 'danger');
            shake(drag.from);
        });
    });

    /* ---------- 拖拽放下：拖回待排课区 = 取消排课 ---------- */
    document.addEventListener('drop', function (e) {
        var pool = e.target.closest('#poolArea');
        if (!pool) { return; }
        e.preventDefault();
        pool.classList.remove('drag-over');
        var drag = window._schedDrag;
        if (!drag) { return; }
        var d = drag.data;
        if (!d.building) { return; } /* 本来就是待排课班，忽略 */

        if (!confirm('确定将 ' + d.courseId + ' 第 ' + d.secId + ' 班移出课表（取消排课）吗？')) { return; }

        var params = new URLSearchParams();
        params.set('courseId', d.courseId);
        params.set('secId', d.secId);
        params.set('semester', d.semester);
        params.set('year', d.year);

        postForm(PAGE + '/unassign', params).then(function (res) {
            if (res.code === 0) {
                showToast('已取消排课：' + d.courseId + ' 第 ' + d.secId + ' 班回到待安排区', 'success');
                refreshBoard();
            } else {
                showToast('操作失败：' + res.message, 'danger');
            }
        }).catch(function (err) {
            showToast(err && err.message ? err.message : '网络异常，请重试', 'danger');
        });
    });

    /* ---------- 新建待排课班 ---------- */
    var createBtn = $('createSectionBtn');
    if (createBtn) {
        createBtn.addEventListener('click', function () {
            var courseId = $('newCourseId').value;
            var secId = $('newSecId').value.trim();
            if (!courseId || !secId) {
                showToast('请选择课程并填写班号', 'warning');
                return;
            }
            var params = new URLSearchParams();
            params.set('courseId', courseId);
            params.set('secId', secId);
            params.set('semester', $('semester').value);
            params.set('year', $('year').value);
            params.set('instructorId', $('newInstructorId').value || '');

            postForm(PAGE + '/create', params).then(function (res) {
                if (res.code === 0) {
                    hideModal('createSectionModal');
                    $('newSecId').value = '';
                    showToast('已创建待排课班：' + courseId + ' 第 ' + secId + ' 班', 'success');
                    refreshBoard();
                } else {
                    showToast('创建失败：' + res.message, 'danger');
                }
            }).catch(function (err) {
                showToast(err && err.message ? err.message : '网络异常，请重试', 'danger');
            });
        });
    }
})();