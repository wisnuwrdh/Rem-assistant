(function () {
  'use strict';

  const CATEGORY_PRESETS = [
    'General', 'Kesehatan', 'Olahraga', 'Produktivitas',
    'Karir & Bisnis', 'Belajar', 'Kreativitas', 'Sosial',
    'Keuangan', 'Spiritual',
  ];

  const HABIT_COLORS = [
    '#FFFFFF', '#1A1917', '#C0392B', '#E8735A',
    '#F59E0B', '#EAB308', '#22C55E', '#2C7A7B',
    '#3B82F6', '#6366F1', '#6B4FBB', '#A855F7',
    '#EC4899', '#F472B6', '#7C6F4A', '#64748B',
  ];

  const HABIT_ICONS = [
    { key: 'star', label: 'Default', svg: '<svg viewBox="0 0 24 24"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>' },
    { key: 'dumbbell', label: 'Olahraga', svg: '<svg viewBox="0 0 24 24"><path d="M6.5 6.5h-2a1 1 0 0 0-1 1v9a1 1 0 0 0 1 1h2"/><path d="M17.5 6.5h2a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1h-2"/><path d="M6.5 9.5h11v5h-11z"/></svg>' },
    { key: 'book', label: 'Baca', svg: '<svg viewBox="0 0 24 24"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>' },
    { key: 'briefcase', label: 'Kerja', svg: '<svg viewBox="0 0 24 24"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>' },
    { key: 'film', label: 'Hiburan', svg: '<svg viewBox="0 0 24 24"><rect x="2" y="2" width="20" height="20" rx="2"/><line x1="7" y1="2" x2="7" y2="22"/><line x1="17" y1="2" x2="17" y2="22"/><line x1="2" y1="12" x2="22" y2="12"/></svg>' },
    { key: 'apple', label: 'Makan/Diet', svg: '<svg viewBox="0 0 24 24"><path d="M12 20.94c1.5 0 2.75 1.06 4 1.06 3 0 6-8 6-12.22A4.91 4.91 0 0 0 17 5c-2.22 0-4 1.44-5 2-1-.56-2.78-2-5-2a4.9 4.9 0 0 0-5 4.78C2 14 5 22 8 22c1.25 0 2.5-1.06 4-1.06Z"/><path d="M10 2c1 .5 2 2 2 5"/></svg>' },
    { key: 'moon', label: 'Tidur', svg: '<svg viewBox="0 0 24 24"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>' },
    { key: 'feather', label: 'Menulis', svg: '<svg viewBox="0 0 24 24"><path d="M20.24 12.24a6 6 0 0 0-8.49-8.49L5 10.5V19h8.5z"/><line x1="16" y1="8" x2="2" y2="22"/></svg>' },
    { key: 'dollar', label: 'Keuangan', svg: '<svg viewBox="0 0 24 24"><line x1="12" y1="2" x2="12" y2="22"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>' },
    { key: 'gamepad', label: 'Gaming', svg: '<svg viewBox="0 0 24 24"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><line x1="8" y1="10" x2="8" y2="14"/></svg>' },
    { key: 'walk', label: 'Jalan', svg: '<svg viewBox="0 0 24 24"><circle cx="12" cy="4" r="2"/><path d="M10.5 8l-2.5 5 3 2-1 6"/><path d="M13 8l2 4 3 1"/></svg>' },
    { key: 'heart', label: 'Kesehatan', svg: '<svg viewBox="0 0 24 24"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>' },
    { key: 'music', label: 'Musik', svg: '<svg viewBox="0 0 24 24"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>' },
    { key: 'coffee', label: 'Kopi', svg: '<svg viewBox="0 0 24 24"><path d="M17 8h1a4 4 0 1 1 0 8h-1"/><path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z"/></svg>' },
    { key: 'sun', label: 'Pagi', svg: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2"/></svg>' },
    { key: 'target', label: 'Target', svg: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>' },
    { key: 'zap', label: 'Energi', svg: '<svg viewBox="0 0 24 24"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>' },
    { key: 'droplet', label: 'Minum Air', svg: '<svg viewBox="0 0 24 24"><path d="M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z"/></svg>' },
    { key: 'globe', label: 'Bahasa', svg: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>' },
    { key: 'smile', label: 'Mood', svg: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/></svg>' },
    { key: 'phone', label: 'Detox', svg: '<svg viewBox="0 0 24 24"><rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>' },
    { key: 'flame', label: 'Semangat', svg: '<svg viewBox="0 0 24 24"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>' },
    { key: 'pencil', label: 'Belajar', svg: '<svg viewBox="0 0 24 24"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>' },
    { key: 'camera', label: 'Foto/Vlog', svg: '<svg viewBox="0 0 24 24"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/></svg>' },
  ];

  const DAY_NAMES_SHORT = ['Min', 'Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab'];
  const DAY_NAMES_FULL = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];
  const DAY_BTN_ORDER = [[1, 'Sen'], [2, 'Sel'], [3, 'Rab'], [4, 'Kam'], [5, 'Jum'], [6, 'Sab'], [0, 'Min']];
  const MONTH_NAMES_ID = ['Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni', 'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'];

  const esc = (s) => String(s || '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const pad2 = (n) => String(n).padStart(2, '0');
  const getToday = () => {
    const d = new Date();
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  };
  const getDateOffset = (days) => {
    const d = new Date();
    d.setDate(d.getDate() + days);
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  };
  const fmtTopDate = () => new Date().toLocaleDateString('id-ID', { weekday: 'long', day: 'numeric', month: 'long' });
  const fmtFullDate = (ds) => {
    if (!ds) return '';
    const d = new Date(`${ds}T12:00:00`);
    return `${DAY_NAMES_FULL[d.getDay()]}, ${d.getDate()} ${MONTH_NAMES_ID[d.getMonth()]} ${d.getFullYear()}`;
  };
  const fmtShortDate = (ds) => {
    if (!ds) return '';
    try { return new Date(`${ds}T12:00:00`).toLocaleDateString('id-ID', { day: 'numeric', month: 'short' }); }
    catch (_) { return ds; }
  };

  const isLightHex = (hex) => {
    const c = String(hex || '').replace('#', '');
    if (c.length < 6) return false;
    const r = parseInt(c.slice(0, 2), 16), g = parseInt(c.slice(2, 4), 16), b = parseInt(c.slice(4, 6), 16);
    return (0.299 * r + 0.587 * g + 0.114 * b) > 200;
  };
  const isDarkHex = (hex) => {
    const c = String(hex || '').replace('#', '');
    if (c.length < 6) return false;
    const r = parseInt(c.slice(0, 2), 16), g = parseInt(c.slice(2, 4), 16), b = parseInt(c.slice(4, 6), 16);
    return (0.299 * r + 0.587 * g + 0.114 * b) < 80;
  };

  function iconSvg(key) {
    return (HABIT_ICONS.find((i) => i.key === key) || HABIT_ICONS[0]).svg;
  }

  function defaultState() {
    return {
      habits: [],
      todos: [],
      coachMemory: null,
      memoryDate: '',
      notifHabitTime: '07:00',
      notifTodoTime: '15:00',
      theme: 'light',
      doneOpen: true,
    };
  }

  function normalizeHabit(raw, idx) {
    if (!raw || !raw.name) return null;
    const hist = {};
    if (raw.history && typeof raw.history === 'object') {
      Object.entries(raw.history).forEach(([k, arr]) => {
        if (!Array.isArray(arr)) return;
        hist[k] = arr.map((item) => (typeof item === 'string'
          ? { id: item, text: '', completedAt: '' }
          : { id: String(item?.id || 'main'), text: String(item?.text || ''), completedAt: String(item?.completedAt || '') }));
      });
    }
    return {
      id: String(raw.id || `hab_${Date.now()}_${idx || 0}`),
      name: String(raw.name).trim(),
      category: String(raw.category || 'General').trim() || 'General',
      color: String(raw.color || '#1A1917'),
      icon: String(raw.icon || 'star'),
      activeDays: Array.isArray(raw.activeDays) ? raw.activeDays.filter((n) => Number.isInteger(n) && n >= 0 && n <= 6) : [],
      subTasks: Array.isArray(raw.subTasks) ? raw.subTasks.map((s, si) => ({ id: String(s?.id || `s_${si}`), text: String(s?.text || '') })).filter((s) => s.text) : [],
      history: hist,
      collapsed: Boolean(raw.collapsed),
      archived: Boolean(raw.archived),
      createdAt: String(raw.createdAt || getToday()),
    };
  }

  function normalizeTodo(raw, idx) {
    if (!raw || !raw.title) return null;
    const pri = ['urgent', 'soon', 'normal'].includes(raw.priority) ? raw.priority : 'normal';
    return {
      id: String(raw.id || `todo_${Date.now()}_${idx || 0}`),
      title: String(raw.title).trim(),
      priority: pri,
      deadline: String(raw.deadline || ''),
      done: Boolean(raw.done),
      completedAt: raw.completedAt ? String(raw.completedAt) : null,
      subtasks: Array.isArray(raw.subtasks)
        ? raw.subtasks.map((s, si) => ({ id: String(s?.id || `ts_${si}`), text: String(s?.text || ''), done: Boolean(s?.done) })).filter((s) => s.text)
        : [],
    };
  }

  function normalizeState(raw) {
    const def = defaultState();
    if (!raw || typeof raw !== 'object') return def;
    let rawTodos = Array.isArray(raw.todos) ? raw.todos : (Array.isArray(raw.tasks) ? raw.tasks : []);
    let rawHabits = Array.isArray(raw.habits) ? raw.habits : [];

    // Migrate legacy standalone keys if ecoDb.dailyos is empty
    if (rawHabits.length === 0 && rawTodos.length === 0) {
      try {
        const lh = JSON.parse(localStorage.getItem('dailyos_habits') || 'null');
        const lt = JSON.parse(localStorage.getItem('dailyos_todos') || 'null');
        if (Array.isArray(lh) && lh.length) rawHabits = lh;
        if (Array.isArray(lt) && lt.length) rawTodos = lt;
      } catch (_) {}
    }

    return {
      habits: rawHabits.map(normalizeHabit).filter(Boolean),
      todos: rawTodos.map(normalizeTodo).filter(Boolean),
      coachMemory: raw.coachMemory && typeof raw.coachMemory === 'object' ? raw.coachMemory : null,
      memoryDate: typeof raw.memoryDate === 'string' ? raw.memoryDate : '',
      notifHabitTime: typeof raw.notifHabitTime === 'string' ? raw.notifHabitTime : '07:00',
      notifTodoTime: typeof raw.notifTodoTime === 'string' ? raw.notifTodoTime : '15:00',
      theme: raw.theme === 'dark' ? 'dark' : 'light',
      doneOpen: raw.doneOpen !== undefined ? Boolean(raw.doneOpen) : true,
    };
  }

  // Module refs & UI state
  let deps = { getDb: () => null, saveDb: () => {}, onUpdate: () => {}, showMainToast: () => {} };
  let root = null;
  const ui = {
    view: 'habits',
    chartRange: 7,
    calMonth: new Date(),
    selectedCalDate: getToday(),
    openTodoSubs: {},
    editHabitId: null,
    editTodoId: null,
    hFormColor: '#1A1917',
    hFormIcon: 'star',
    hFormDays: [],
    hFormSubs: [],
    hIconGridOpen: false,
    tFormPri: 'normal',
    tFormSubs: [],
    coachText: null,
    coachLoading: false,
    coachError: null,
    parsedTasks: null,
    rekomList: null,
    confirmCallback: null,
  };

  function state() {
    const db = deps.getDb();
    if (!db) return defaultState();
    db.dailyos = normalizeState(db.dailyos);
    return db.dailyos;
  }

  function persist() {
    deps.saveDb();
    deps.onUpdate?.();
  }

  // ── Calculations ──
  function isHabitScheduled(h, ds) {
    if (!h.activeDays || h.activeDays.length === 0) return true;
    const d = new Date(`${ds}T12:00:00`);
    return h.activeDays.includes(d.getDay());
  }

  function recalculateXp(habits) {
    let total = 0;
    habits.forEach((h) => {
      Object.values(h.history || {}).forEach((arr) => {
        if (Array.isArray(arr)) total += arr.length * 10;
      });
    });
    return total;
  }

  function calcHabitStreak(h) {
    const today = getToday();
    let streak = 0;
    const cur = new Date(`${today}T12:00:00`);
    for (let i = 0; i < 365; i++) {
      const ds = `${cur.getFullYear()}-${pad2(cur.getMonth() + 1)}-${pad2(cur.getDate())}`;
      const logs = (h.history && h.history[ds]) || [];
      if (logs.length > 0) {
        streak++;
        cur.setDate(cur.getDate() - 1);
      } else if (ds === today || !isHabitScheduled(h, ds)) {
        cur.setDate(cur.getDate() - 1);
      } else {
        break;
      }
    }
    return streak;
  }

  function calcOverallStreak(habits) {
    const active = habits.filter((h) => !h.archived);
    if (!active.length) return 0;
    const today = getToday();
    let streak = 0;
    const cur = new Date(`${today}T12:00:00`);
    for (let i = 0; i < 3650; i++) {
      const ds = `${cur.getFullYear()}-${pad2(cur.getMonth() + 1)}-${pad2(cur.getDate())}`;
      const sched = active.filter((h) => isHabitScheduled(h, ds));
      const hasAct = sched.some((h) => ((h.history && h.history[ds]) || []).length > 0);
      if (hasAct) streak++;
      else if (ds === today || sched.length === 0) { /* continue */ }
      else break;
      cur.setDate(cur.getDate() - 1);
    }
    return streak;
  }

  function calcTodayProgress(habits) {
    const today = getToday();
    const active = habits.filter((h) => !h.archived);
    let doneSlots = 0, totalSlots = 0, completedHabits = 0, scheduledHabits = 0;
    active.forEach((h) => {
      if (!isHabitScheduled(h, today)) return;
      scheduledHabits++;
      const tc = h.subTasks.length || 1;
      const done = ((h.history && h.history[today]) || []).length;
      totalSlots += tc;
      doneSlots += Math.min(done, tc);
      if (done >= tc) completedHabits++;
    });
    const pct = totalSlots ? Math.round((doneSlots / totalSlots) * 100) : 0;
    return { doneSlots, totalSlots, pct, completedHabits, scheduledHabits };
  }

  function calc7DayAvg(habits) {
    const active = habits.filter((h) => !h.archived);
    if (!active.length) return 0;
    let total = 0, done = 0;
    for (let i = 0; i < 7; i++) {
      const ds = getDateOffset(-i);
      active.forEach((h) => {
        if (!isHabitScheduled(h, ds)) return;
        total += h.subTasks.length || 1;
        done += ((h.history && h.history[ds]) || []).length;
      });
    }
    return total ? Math.min(100, Math.round((done / total) * 100)) : 0;
  }

  function getHabitStats30Days(habits) {
    return habits.filter((h) => !h.archived).map((h) => {
      const today = getToday();
      const tc = h.subTasks.length || 1;
      const sched = [], comp = [], skip = [];
      for (let i = 0; i < 30; i++) {
        const ds = getDateOffset(-i);
        if (!isHabitScheduled(h, ds)) continue;
        sched.push(ds);
        const cnt = ((h.history && h.history[ds]) || []).length;
        if (cnt >= tc) comp.push(ds);
        else if (ds !== today) skip.push(ds);
      }
      const skipDow = new Array(7).fill(0), schedDow = new Array(7).fill(0);
      sched.forEach((ds) => schedDow[new Date(`${ds}T12:00:00`).getDay()]++);
      skip.forEach((ds) => skipDow[new Date(`${ds}T12:00:00`).getDay()]++);
      const weakDays = [];
      for (let d = 0; d < 7; d++) {
        if (schedDow[d] >= 3 && Math.round((skipDow[d] / schedDow[d]) * 100) >= 50) weakDays.push(DAY_NAMES_FULL[d]);
      }
      return {
        name: h.name,
        category: h.category || 'General',
        scheduledCount: sched.length,
        completedCount: comp.length,
        skipCount: skip.length,
        completionRate: sched.length ? Math.round((comp.length / sched.length) * 100) : 0,
        currentStreak: calcHabitStreak(h),
        weakDays,
      };
    });
  }

  function getIdekuProses() {
    const db = deps.getDb();
    const list = db?.ideku?.ideas;
    if (!Array.isArray(list)) return [];
    return list.filter((i) => i.type === 'Idea' && i.status === 'Proses')
      .map((i) => ({ title: i.title, category: i.category || '', notes: i.notes || '' }));
  }

  // ── DOM Scaffolding inside #dailyos-overlay ──
  function mountDom() {
    root = document.getElementById('dailyos-overlay');
    if (!root || root.dataset.mounted) return;
    root.dataset.mounted = '1';

    root.innerHTML = `
      <header class="do-header">
        <div class="do-header-l">
          <button type="button" class="do-icon-btn" id="do-exit-btn" aria-label="Kembali">
            <svg viewBox="0 0 24 24"><polyline points="15 18 9 12 15 6"/></svg>
          </button>
          <div>
            <div class="do-title">Daily OS</div>
            <div class="do-date" id="do-top-date"></div>
          </div>
        </div>
        <button type="button" class="do-icon-btn" id="do-theme-btn" aria-label="Ganti Tema"></button>
      </header>

      <div class="do-body" id="do-body">
        <div class="do-view active" id="do-view-habits"></div>
        <div class="do-view" id="do-view-todos"></div>
        <div class="do-view" id="do-view-stats"></div>
        <div class="do-view" id="do-view-ai"></div>
        <div class="do-view" id="do-view-settings"></div>
      </div>

      <button type="button" class="do-fab" id="do-fab" aria-label="Tambah">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      </button>

      <nav class="do-nav">
        <button type="button" class="do-nav-btn active" data-do-tab="habits">
          <svg viewBox="0 0 24 24"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
          <span>Habit</span>
        </button>
        <button type="button" class="do-nav-btn" data-do-tab="todos">
          <svg viewBox="0 0 24 24"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><circle cx="3" cy="6" r="1"/><circle cx="3" cy="12" r="1"/><circle cx="3" cy="18" r="1"/></svg>
          <span>To-Do</span>
        </button>
        <button type="button" class="do-nav-btn" data-do-tab="stats">
          <svg viewBox="0 0 24 24"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
          <span>Statistik</span>
        </button>
        <button type="button" class="do-nav-btn" data-do-tab="ai">
          <svg viewBox="0 0 24 24"><path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/></svg>
          <span>AI</span>
        </button>
        <button type="button" class="do-nav-btn" data-do-tab="settings">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9c.26.604.852.997 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          <span>Setting</span>
        </button>
      </nav>

      <!-- Habit Modal -->
      <div class="do-modal-ov" id="do-habit-modal">
        <div class="do-modal">
          <div class="do-modal-hdr">
            <span class="do-modal-title" id="do-hm-title">Tambah Habit</span>
            <button type="button" class="do-mini-btn" id="do-hm-close"><svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
          </div>
          <div class="do-modal-body">
            <div>
              <label class="do-fld-lbl">Nama Habit</label>
              <input class="do-input" id="do-hm-name" type="text" placeholder="Contoh: Olahraga Pagi" />
            </div>
            <div>
              <label class="do-fld-lbl">Kategori</label>
              <select class="do-input" id="do-hm-cat">
                ${CATEGORY_PRESETS.map((c) => `<option value="${esc(c)}">${esc(c)}</option>`).join('')}
                <option value="__other__">Lainnya...</option>
              </select>
              <input class="do-input" id="do-hm-cat-custom" type="text" placeholder="Ketik kategori custom..." style="display:none; margin-top:6px" />
            </div>
            <div>
              <label class="do-fld-lbl">Warna Badge</label>
              <div class="do-color-grid" id="do-hm-colors"></div>
            </div>
            <div>
              <label class="do-fld-lbl">Ikon</label>
              <button type="button" class="do-input" id="do-hm-icon-toggle" style="display:flex;align-items:center;justify-content:space-between;cursor:pointer;text-align:left"></button>
              <div class="do-icon-grid" id="do-hm-icons" style="display:none;margin-top:6px"></div>
            </div>
            <div>
              <label class="do-fld-lbl">Jadwal Hari Aktif</label>
              <div class="do-day-row" id="do-hm-days"></div>
              <div class="do-preset-row" id="do-hm-presets">
                <button type="button" class="do-preset-btn" data-days="">Setiap Hari</button>
                <button type="button" class="do-preset-btn" data-days="1,2,3,4,5">Senin–Jumat</button>
                <button type="button" class="do-preset-btn" data-days="1,3,5">Sen, Rab, Jum</button>
                <button type="button" class="do-preset-btn" data-days="0,6">Akhir Pekan</button>
              </div>
              <div style="font-size:11px;color:var(--do-text3);margin-top:5px" id="do-hm-sched-hint"></div>
            </div>
            <div>
              <label class="do-fld-lbl">Checklist (opsional — kosong = 1 aktivitas)</label>
              <div id="do-hm-subs" style="display:flex;flex-direction:column;gap:6px"></div>
              <button type="button" class="do-btn-sec" id="do-hm-add-sub" style="width:100%;margin-top:6px">+ Tambah Checklist</button>
            </div>
          </div>
          <div class="do-modal-ftr">
            <button type="button" class="do-btn-sec" id="do-hm-cancel">Batal</button>
            <button type="button" class="do-btn-pri" id="do-hm-save">Simpan</button>
          </div>
        </div>
      </div>

      <!-- To-Do Modal -->
      <div class="do-modal-ov" id="do-todo-modal">
        <div class="do-modal">
          <div class="do-modal-hdr">
            <span class="do-modal-title" id="do-tm-title">Tambah To-Do</span>
            <button type="button" class="do-mini-btn" id="do-tm-close"><svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
          </div>
          <div class="do-modal-body">
            <div>
              <label class="do-fld-lbl">Task</label>
              <input class="do-input" id="do-tm-input" type="text" placeholder="Apa yang perlu diselesaikan?" />
            </div>
            <div>
              <label class="do-fld-lbl">Prioritas</label>
              <div class="do-pri-row" id="do-tm-pris">
                <button type="button" class="do-pri-opt" data-pri="urgent">Hari Ini</button>
                <button type="button" class="do-pri-opt" data-pri="soon">Minggu Ini</button>
                <button type="button" class="do-pri-opt" data-pri="normal">Nanti</button>
              </div>
            </div>
            <div>
              <label class="do-fld-lbl">Deadline (opsional)</label>
              <input class="do-input" id="do-tm-dl" type="date" />
            </div>
            <div>
              <label class="do-fld-lbl">Subtask (opsional)</label>
              <div id="do-tm-subs" style="display:flex;flex-direction:column;gap:6px"></div>
              <button type="button" class="do-btn-sec" id="do-tm-add-sub" style="width:100%;margin-top:6px">+ Tambah Subtask</button>
            </div>
          </div>
          <div class="do-modal-ftr">
            <button type="button" class="do-btn-sec" id="do-tm-cancel">Batal</button>
            <button type="button" class="do-btn-pri" id="do-tm-save">Simpan</button>
          </div>
        </div>
      </div>

      <!-- Custom Confirm/Alert Modal -->
      <div class="do-alert-ov" id="do-alert-modal">
        <div class="do-modal" style="max-width:340px">
          <div class="do-modal-hdr">
            <span class="do-modal-title" id="do-am-title">Konfirmasi</span>
          </div>
          <div class="do-modal-body">
            <div id="do-am-msg" style="font-size:13px;line-height:1.5;color:var(--do-text2)"></div>
          </div>
          <div class="do-modal-ftr" id="do-am-ftr"></div>
        </div>
      </div>

      <input type="file" id="do-restore-file" accept=".json,application/json" style="display:none" />
    `;

    // Bind Header & Nav events
    root.querySelector('#do-exit-btn').addEventListener('click', close);
    root.querySelector('#do-theme-btn').addEventListener('click', () => {
      const s = state();
      s.theme = s.theme === 'dark' ? 'light' : 'dark';
      persist();
      applyTheme();
      render();
    });
    root.querySelector('#do-fab').addEventListener('click', () => {
      if (ui.view === 'habits') openHabitModal(null);
      else if (ui.view === 'todos') openTodoModal(null);
    });
    root.querySelectorAll('[data-do-tab]').forEach((btn) => {
      btn.addEventListener('click', () => switchTab(btn.dataset.doTab));
    });

    // Habit Modal Events
    root.querySelector('#do-hm-close').addEventListener('click', () => closeModal('#do-habit-modal'));
    root.querySelector('#do-hm-cancel').addEventListener('click', () => closeModal('#do-habit-modal'));
    root.querySelector('#do-hm-cat').addEventListener('change', (e) => {
      root.querySelector('#do-hm-cat-custom').style.display = e.target.value === '__other__' ? 'block' : 'none';
    });
    root.querySelector('#do-hm-icon-toggle').addEventListener('click', () => {
      ui.hIconGridOpen = !ui.hIconGridOpen;
      root.querySelector('#do-hm-icons').style.display = ui.hIconGridOpen ? 'grid' : 'none';
    });
    root.querySelectorAll('#do-hm-presets .do-preset-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        ui.hFormDays = btn.dataset.days ? btn.dataset.days.split(',').map(Number) : [];
        renderHabitModalDays();
      });
    });
    root.querySelector('#do-hm-add-sub').addEventListener('click', () => {
      ui.hFormSubs.push({ id: `s_${Date.now()}_${ui.hFormSubs.length}`, text: '' });
      renderHabitModalSubs();
    });
    root.querySelector('#do-hm-save').addEventListener('click', saveHabitFromModal);

    // Todo Modal Events
    root.querySelector('#do-tm-close').addEventListener('click', () => closeModal('#do-todo-modal'));
    root.querySelector('#do-tm-cancel').addEventListener('click', () => closeModal('#do-todo-modal'));
    root.querySelectorAll('#do-tm-pris .do-pri-opt').forEach((btn) => {
      btn.addEventListener('click', () => {
        ui.tFormPri = btn.dataset.pri;
        renderTodoModalPri();
      });
    });
    root.querySelector('#do-tm-add-sub').addEventListener('click', () => {
      ui.tFormSubs.push('');
      renderTodoModalSubs();
    });
    root.querySelector('#do-tm-save').addEventListener('click', saveTodoFromModal);

    // Restore File Handler
    root.querySelector('#do-restore-file').addEventListener('change', (e) => {
      const f = e.target.files?.[0];
      e.target.value = '';
      if (!f) return;
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const parsed = JSON.parse(String(reader.result || '{}'));
          if (!Array.isArray(parsed.habits) && !Array.isArray(parsed.todos)) {
            showAlert('Format Tidak Valid', 'File JSON tidak sesuai format backup Daily OS.');
            return;
          }
          showConfirm('Restore Backup', 'Data saat ini akan diganti dengan data backup. Lanjutkan?', () => {
            const db = deps.getDb();
            db.dailyos = normalizeState({
              ...state(),
              habits: parsed.habits || [],
              todos: parsed.todos || [],
              coachMemory: parsed.coachMemory || null,
              memoryDate: parsed.coachMemory ? getToday() : '',
            });
            persist();
            render();
            showAlert('Restore Berhasil', `${db.dailyos.habits.length} habit dan ${db.dailyos.todos.length} task dipulihkan.`);
          }, 'Restore');
        } catch (_) {
          showAlert('Error', 'Gagal membaca file JSON.');
        }
      };
      reader.readAsText(f);
    });

    // Global dropdown close inside overlay
    root.addEventListener('click', (e) => {
      if (!e.target.closest('.do-dropdown-wrap')) {
        root.querySelectorAll('.do-dropdown.open').forEach((el) => el.classList.remove('open'));
      }
    });
  }

  function applyTheme() {
    if (!root) return;
    const isDark = state().theme === 'dark';
    root.classList.toggle('do-dark', isDark);
    const btn = root.querySelector('#do-theme-btn');
    if (btn) {
      btn.innerHTML = isDark
        ? '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2"/></svg>'
        : '<svg viewBox="0 0 24 24"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>';
    }
  }

  function switchTab(tab) {
    ui.view = tab;
    root.querySelectorAll('[data-do-tab]').forEach((b) => b.classList.toggle('active', b.dataset.doTab === tab));
    root.querySelectorAll('.do-view').forEach((v) => v.classList.toggle('active', v.id === `do-view-${tab}`));
    const fab = root.querySelector('#do-fab');
    if (fab) fab.classList.toggle('hidden', tab !== 'habits' && tab !== 'todos');
    render();
    if (tab === 'ai' && !ui.coachText && !ui.coachLoading) fetchCoachBriefing();
  }

  function showAlert(title, msg) {
    const ov = root.querySelector('#do-alert-modal');
    root.querySelector('#do-am-title').textContent = title;
    root.querySelector('#do-am-msg').textContent = msg;
    const ftr = root.querySelector('#do-am-ftr');
    ftr.innerHTML = '<button type="button" class="do-btn-pri" id="do-am-ok">OK</button>';
    ftr.querySelector('#do-am-ok').addEventListener('click', () => ov.classList.remove('open'));
    ov.classList.add('open');
  }

  function showConfirm(title, msg, onConfirm, confirmLbl = 'Hapus', isDanger = false) {
    const ov = root.querySelector('#do-alert-modal');
    root.querySelector('#do-am-title').textContent = title;
    root.querySelector('#do-am-msg').textContent = msg;
    const ftr = root.querySelector('#do-am-ftr');
    ftr.innerHTML = `
      <button type="button" class="do-btn-sec" id="do-am-cancel">Batal</button>
      <button type="button" class="${isDanger ? 'do-btn-danger' : 'do-btn-pri'}" style="flex:1" id="do-am-confirm">${esc(confirmLbl)}</button>
    `;
    ftr.querySelector('#do-am-cancel').addEventListener('click', () => ov.classList.remove('open'));
    ftr.querySelector('#do-am-confirm').addEventListener('click', () => {
      ov.classList.remove('open');
      onConfirm();
    });
    ov.classList.add('open');
  }

  function closeModal(sel) {
    root.querySelector(sel)?.classList.remove('open');
  }

  // ── Render Views ──
  function render() {
    if (!root) return;
    const topDate = root.querySelector('#do-top-date');
    if (topDate) topDate.textContent = fmtTopDate();
    applyTheme();
    if (ui.view === 'habits') renderHabitsView();
    else if (ui.view === 'todos') renderTodosView();
    else if (ui.view === 'stats') renderStatsView();
    else if (ui.view === 'ai') renderAiView();
    else if (ui.view === 'settings') renderSettingsView();
  }

  function renderHabitsView() {
    const wrap = root.querySelector('#do-view-habits');
    const s = state();
    const xp = recalculateXp(s.habits);
    const lvl = Math.floor(xp / 100) + 1;
    const fill = xp % 100;
    const today = getToday();
    const active = s.habits.filter((h) => !h.archived);

    let html = `
      <div class="do-xp-banner">
        <div class="do-xp-top">
          <span class="do-xp-lbl">Consistency XP · Lvl ${lvl}</span>
          <span class="do-xp-pts">${xp} pts</span>
        </div>
        <div class="do-xp-track"><div class="do-xp-fill" style="width:${fill}%"></div></div>
      </div>
    `;

    if (!active.length) {
      html += '<div class="do-empty"><p>Belum ada habit.</p><p>Tap + untuk mulai.</p></div>';
      wrap.innerHTML = html;
      return;
    }

    const isDark = s.theme === 'dark';
    active.forEach((h) => {
      const sched = isHabitScheduled(h, today);
      const todayLogs = ((h.history && h.history[today]) || []).map((l) => l.id);
      const invLight = !isDark && isLightHex(h.color);
      const invDark = isDark && isDarkHex(h.color);
      const bg = invLight ? 'var(--do-surface2)' : (invDark ? '#F0F0F0' : (h.color || '#1A1917'));
      const fg = invLight ? 'var(--do-text)' : (invDark ? '#0F0F0F' : (isLightHex(h.color) ? '#1A1917' : '#FFFFFF'));
      const schedLbl = !h.activeDays.length ? 'Setiap hari' : h.activeDays.slice().sort().map((d) => DAY_NAMES_SHORT[d]).join(', ');
      const rows = h.subTasks.length ? h.subTasks : [{ id: 'main', text: 'Selesaikan Habit Ini' }];

      html += `
        <div class="do-habit-card ${sched ? '' : 'off-schedule'}" data-hid="${esc(h.id)}">
          <div class="do-habit-header" data-toggle-collapse="${esc(h.id)}">
            <span class="do-drag-handle" data-drag-hid="${esc(h.id)}">
              <svg viewBox="0 0 16 16"><circle cx="5" cy="4" r="1.3"/><circle cx="11" cy="4" r="1.3"/><circle cx="5" cy="8" r="1.3"/><circle cx="11" cy="8" r="1.3"/><circle cx="5" cy="12" r="1.3"/><circle cx="11" cy="12" r="1.3"/></svg>
            </span>
            <div class="do-habit-badge" style="background:${bg};color:${fg};${invLight ? 'border:1px solid var(--do-border);' : ''}">${iconSvg(h.icon)}</div>
            <div class="do-habit-info">
              <div class="do-habit-name">${esc(h.name)}</div>
              <div class="do-habit-meta">${esc(h.category || 'General')} · ${esc(schedLbl)}</div>
              ${sched ? '' : '<div class="do-habit-off">Tidak dijadwalkan hari ini</div>'}
            </div>
            <div class="do-habit-actions do-dropdown-wrap">
              <span class="do-mini-btn"><svg viewBox="0 0 24 24">${h.collapsed ? '<polyline points="9 18 15 12 9 6"/>' : '<polyline points="6 9 12 15 18 9"/>'}</svg></span>
              <button type="button" class="do-mini-btn" data-h-menu="${esc(h.id)}" aria-label="Menu">
                <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></svg>
              </button>
              <div class="do-dropdown" id="do-dd-${esc(h.id)}">
                <button type="button" data-h-edit="${esc(h.id)}">Edit</button>
                <button type="button" data-h-move="${esc(h.id)}:-1">Geser Naik</button>
                <button type="button" data-h-move="${esc(h.id)}:1">Geser Turun</button>
                <button type="button" class="danger" data-h-del="${esc(h.id)}">Hapus</button>
              </div>
            </div>
          </div>
          ${h.collapsed ? '' : `
            <div class="do-subtasks">
              ${rows.map((r) => {
                const chk = todayLogs.includes(r.id);
                return `
                  <div class="do-subtask-row" ${sched ? `data-h-chk="${esc(h.id)}|${esc(r.id)}"` : ''}>
                    <div class="do-check-box ${chk ? 'checked' : ''}">${chk ? '<svg viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg>' : ''}</div>
                    <span class="do-subtask-lbl ${chk ? 'checked' : ''}">${esc(r.text)}</span>
                  </div>
                `;
              }).join('')}
            </div>
          `}
        </div>
      `;
    });

    wrap.innerHTML = html;

    // Event bindings
    wrap.querySelectorAll('[data-toggle-collapse]').forEach((el) => {
      el.addEventListener('click', (e) => {
        if (e.target.closest('.do-dropdown-wrap') || e.target.closest('.do-drag-handle')) return;
        const h = state().habits.find((x) => x.id === el.dataset.toggleCollapse);
        if (h) { h.collapsed = !h.collapsed; persist(); renderHabitsView(); }
      });
    });
    wrap.querySelectorAll('[data-h-menu]').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const dd = root.querySelector(`#do-dd-${btn.dataset.hMenu}`);
        const wasOpen = dd?.classList.contains('open');
        root.querySelectorAll('.do-dropdown.open').forEach((d) => d.classList.remove('open'));
        if (dd && !wasOpen) dd.classList.add('open');
      });
    });
    wrap.querySelectorAll('[data-h-edit]').forEach((b) => b.addEventListener('click', () => openHabitModal(b.dataset.hEdit)));
    wrap.querySelectorAll('[data-h-move]').forEach((b) => {
      b.addEventListener('click', () => {
        const [hid, d] = b.dataset.hMove.split(':');
        moveHabitOrder(hid, Number(d));
      });
    });
    wrap.querySelectorAll('[data-h-del]').forEach((b) => {
      b.addEventListener('click', () => {
        const h = state().habits.find((x) => x.id === b.dataset.hDel);
        if (!h) return;
        showConfirm('Hapus Habit', `Yakin ingin menghapus "${h.name}"? XP dan riwayatnya tetap tersimpan.`, () => {
          h.archived = true;
          persist();
          renderHabitsView();
        }, 'Hapus', true);
      });
    });
    wrap.querySelectorAll('[data-h-chk]').forEach((row) => {
      row.addEventListener('click', () => {
        const [hid, sid] = row.dataset.hChk.split('|');
        toggleHabitCheck(hid, sid);
      });
    });
  }

  function moveHabitOrder(hid, delta) {
    const s = state();
    const active = s.habits.filter((h) => !h.archived);
    const archived = s.habits.filter((h) => h.archived);
    const idx = active.findIndex((h) => h.id === hid);
    const target = idx + delta;
    if (idx !== -1 && target >= 0 && target < active.length) {
      const [moved] = active.splice(idx, 1);
      active.splice(target, 0, moved);
      s.habits = [...active, ...archived];
      persist();
      renderHabitsView();
    }
  }

  function toggleHabitCheck(hid, sid) {
    const s = state();
    const h = s.habits.find((x) => x.id === hid);
    const today = getToday();
    if (!h || !isHabitScheduled(h, today)) return;
    if (!h.history) h.history = {};
    if (!Array.isArray(h.history[today])) h.history[today] = [];
    const labelText = sid === 'main' ? h.name : (h.subTasks.find((x) => x.id === sid)?.text || sid);
    const idx = h.history[today].findIndex((x) => x.id === sid);
    if (idx >= 0) h.history[today].splice(idx, 1);
    else h.history[today].push({ id: sid, text: labelText, completedAt: new Date().toISOString() });
    persist();
    renderHabitsView();
  }

  function renderTodosView() {
    const wrap = root.querySelector('#do-view-todos');
    const s = state();
    const today = getToday();
    const pending = s.todos.filter((t) => !t.done);
    const doneList = s.todos.filter((t) => t.done);

    if (!s.todos.length) {
      wrap.innerHTML = '<div class="do-empty"><p>Belum ada task.</p><p>Tap + atau gunakan AI Smart To-Do.</p></div>';
      return;
    }

    const renderCard = (t) => {
      const overdue = !t.done && t.deadline && t.deadline < today;
      const priLbl = t.priority === 'urgent' ? 'Hari Ini' : (t.priority === 'soon' ? 'Minggu Ini' : 'Nanti');
      const subOpen = Boolean(ui.openTodoSubs[t.id]);
      const doneSubs = t.subtasks.filter((x) => x.done).length;
      return `
        <div class="do-todo-item ${t.done ? 'done' : ''}" data-tid="${esc(t.id)}">
          <div class="do-todo-main">
            <div class="do-check-box ${t.done ? 'checked' : ''}" data-t-chk="${esc(t.id)}">
              ${t.done ? '<svg viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg>' : ''}
            </div>
            <div class="do-todo-content">
              <div class="do-todo-title">${esc(t.title)}</div>
              <div class="do-todo-meta">
                <span class="do-pri-tag do-pri-${t.priority}">${priLbl}</span>
                ${t.deadline ? `<span class="do-dl-tag ${overdue ? 'overdue' : ''}">${overdue ? '! ' : ''}${fmtShortDate(t.deadline)}</span>` : ''}
                ${t.subtasks.length ? `<button type="button" class="do-sub-toggle" data-t-sub-tog="${esc(t.id)}">${doneSubs}/${t.subtasks.length} subtask ${subOpen ? '▾' : '▸'}</button>` : ''}
              </div>
            </div>
            <div class="do-dropdown-wrap">
              <button type="button" class="do-mini-btn" data-t-menu="${esc(t.id)}" aria-label="Menu">
                <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></svg>
              </button>
              <div class="do-dropdown" id="do-tdd-${esc(t.id)}">
                <button type="button" data-t-edit="${esc(t.id)}">Edit</button>
                <button type="button" data-t-move="${esc(t.id)}:-1">Geser Naik</button>
                <button type="button" data-t-move="${esc(t.id)}:1">Geser Turun</button>
                <button type="button" class="danger" data-t-del="${esc(t.id)}">Hapus</button>
              </div>
            </div>
          </div>
          ${t.subtasks.length && subOpen ? `
            <div class="do-todo-subs">
              ${t.subtasks.map((st) => `
                <div class="do-todo-sub-row ${st.done ? 'done' : ''}" data-t-sub-chk="${esc(t.id)}|${esc(st.id)}">
                  <div class="do-check-box ${st.done ? 'checked' : ''}" style="width:15px;height:15px;border-radius:4px">
                    ${st.done ? '<svg viewBox="0 0 24 24" style="width:10px;height:10px"><polyline points="20 6 9 17 4 12"/></svg>' : ''}
                  </div>
                  <span>${esc(st.text)}</span>
                </div>
              `).join('')}
            </div>
          ` : ''}
        </div>
      `;
    };

    let html = '';
    [['urgent', 'Hari Ini'], ['soon', 'Minggu Ini'], ['normal', 'Nanti']].forEach(([pri, lbl]) => {
      const grp = pending.filter((t) => t.priority === pri);
      if (grp.length) {
        html += `<div class="do-sec-title">${lbl}</div>`;
        html += grp.map(renderCard).join('');
      }
    });

    if (doneList.length) {
      html += `<div class="do-done-hdr" id="do-done-hdr">${s.doneOpen ? '▾' : '▸'} SELESAI (${doneList.length})</div>`;
      if (s.doneOpen) html += doneList.map(renderCard).join('');
    }

    wrap.innerHTML = html;

    wrap.querySelector('#do-done-hdr')?.addEventListener('click', () => {
      s.doneOpen = !s.doneOpen;
      persist();
      renderTodosView();
    });
    wrap.querySelectorAll('[data-t-chk]').forEach((b) => {
      b.addEventListener('click', () => {
        const t = state().todos.find((x) => x.id === b.dataset.tChk);
        if (t) { t.done = !t.done; t.completedAt = t.done ? new Date().toISOString() : null; persist(); renderTodosView(); }
      });
    });
    wrap.querySelectorAll('[data-t-sub-tog]').forEach((b) => {
      b.addEventListener('click', () => {
        ui.openTodoSubs[b.dataset.tSubTog] = !ui.openTodoSubs[b.dataset.tSubTog];
        renderTodosView();
      });
    });
    wrap.querySelectorAll('[data-t-sub-chk]').forEach((row) => {
      row.addEventListener('click', () => {
        const [tid, sid] = row.dataset.tSubChk.split('|');
        const t = state().todos.find((x) => x.id === tid);
        const st = t?.subtasks.find((x) => x.id === sid);
        if (st) { st.done = !st.done; persist(); renderTodosView(); }
      });
    });
    wrap.querySelectorAll('[data-t-menu]').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const dd = root.querySelector(`#do-tdd-${btn.dataset.tMenu}`);
        const wasOpen = dd?.classList.contains('open');
        root.querySelectorAll('.do-dropdown.open').forEach((d) => d.classList.remove('open'));
        if (dd && !wasOpen) dd.classList.add('open');
      });
    });
    wrap.querySelectorAll('[data-t-edit]').forEach((b) => b.addEventListener('click', () => openTodoModal(b.dataset.tEdit)));
    wrap.querySelectorAll('[data-t-move]').forEach((b) => {
      b.addEventListener('click', () => {
        const [tid, d] = b.dataset.tMove.split(':');
        moveTodoOrder(tid, Number(d));
      });
    });
    wrap.querySelectorAll('[data-t-del]').forEach((b) => {
      b.addEventListener('click', () => {
        const t = state().todos.find((x) => x.id === b.dataset.tDel);
        if (!t) return;
        showConfirm('Hapus To-Do', `Yakin ingin menghapus task "${t.title}"?`, () => {
          state().todos = state().todos.filter((x) => x.id !== t.id);
          persist();
          renderTodosView();
        }, 'Hapus', true);
      });
    });
  }

  function moveTodoOrder(tid, delta) {
    const s = state();
    const target = s.todos.find((t) => t.id === tid);
    if (!target) return;
    const sameGroup = s.todos.filter((t) => t.done === target.done && (t.done || t.priority === target.priority));
    const idx = sameGroup.findIndex((t) => t.id === tid);
    const swapIdx = idx + delta;
    if (idx === -1 || swapIdx < 0 || swapIdx >= sameGroup.length) return;
    const gA = s.todos.indexOf(sameGroup[idx]);
    const gB = s.todos.indexOf(sameGroup[swapIdx]);
    [s.todos[gA], s.todos[gB]] = [s.todos[gB], s.todos[gA]];
    persist();
    renderTodosView();
  }

  function buildTrendSvg(habits, days) {
    const active = habits.filter((h) => !h.archived);
    const pts = [];
    for (let i = days - 1; i >= 0; i--) {
      const ds = getDateOffset(-i);
      let dt = 0, dd = 0;
      active.forEach((h) => {
        if (!isHabitScheduled(h, ds)) return;
        dt += h.subTasks.length || 1;
        dd += ((h.history && h.history[ds]) || []).length;
      });
      const pct = dt ? Math.min(100, Math.round((dd / dt) * 100)) : 0;
      pts.push({ label: fmtShortDate(ds), pct });
    }
    const W = 320, H = 120, padX = 8, padY = 10;
    const cw = W - padX * 2, ch = H - padY * 2;
    const stepX = pts.length > 1 ? cw / (pts.length - 1) : cw;
    const coords = pts.map((p, idx) => ({
      x: padX + idx * stepX,
      y: padY + ch * (1 - p.pct / 100),
      ...p,
    }));
    const lineD = coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(' ');
    const areaD = `${lineD} L${coords[coords.length - 1].x.toFixed(1)},${(padY + ch).toFixed(1)} L${coords[0].x.toFixed(1)},${(padY + ch).toFixed(1)} Z`;
    return `
      <svg viewBox="0 0 ${W} ${H}" style="width:100%;height:125px;display:block">
        <line x1="${padX}" y1="${padY}" x2="${W - padX}" y2="${padY}" stroke="var(--do-border)" stroke-width="1"/>
        <line x1="${padX}" y1="${padY + ch / 2}" x2="${W - padX}" y2="${padY + ch / 2}" stroke="var(--do-border)" stroke-width="1"/>
        <line x1="${padX}" y1="${padY + ch}" x2="${W - padX}" y2="${padY + ch}" stroke="var(--do-border)" stroke-width="1"/>
        <path d="${areaD}" fill="var(--do-accent)" fill-opacity="0.1"/>
        <path d="${lineD}" fill="none" stroke="var(--do-accent)" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
        ${pts.length <= 14 ? coords.map((c) => `<circle cx="${c.x.toFixed(1)}" cy="${c.y.toFixed(1)}" r="2.8" fill="var(--do-accent)"/>`).join('') : ''}
      </svg>
      <div style="display:flex;justify-content:space-between;font-size:10px;color:var(--do-text3);margin-top:4px">
        <span>${esc(pts[0]?.label || '')}</span>
        <span>${esc(pts[Math.floor(pts.length / 2)]?.label || '')}</span>
        <span style="color:var(--do-text2)">${esc(pts[pts.length - 1]?.label || '')} (${pts[pts.length - 1]?.pct || 0}%)</span>
      </div>
    `;
  }

  function renderStatsView() {
    const wrap = root.querySelector('#do-view-stats');
    const s = state();
    const today = getToday();
    const activeHabits = s.habits.filter((h) => !h.archived);
    const streak = calcOverallStreak(s.habits);
    const prog = calcTodayProgress(s.habits);
    const avg7 = calc7DayAvg(s.habits);

    const year = ui.calMonth.getFullYear(), month = ui.calMonth.getMonth();
    const firstDow = (new Date(year, month, 1).getDay() + 6) % 7;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    let calCells = '';
    for (let i = 0; i < firstDow; i++) calCells += '<div></div>';
    for (let d = 1; d <= daysInMonth; d++) {
      const ds = `${year}-${pad2(month + 1)}-${pad2(d)}`;
      const isToday = ds === today, isSel = ds === ui.selectedCalDate;
      const dots = s.habits.filter((h) => ((h.history && h.history[ds]) || []).length > 0).slice(0, 3);
      calCells += `
        <div class="do-cal-day ${isToday ? 'today' : ''} ${isSel ? 'selected' : ''}" data-cal-ds="${ds}">
          <span>${d}</span>
          <div class="do-cal-dots">
            ${dots.map((h) => `<span class="do-cal-dot" style="background:${isSel ? 'var(--do-accent-fg)' : esc(h.color || '#1A1917')}"></span>`).join('')}
          </div>
        </div>
      `;
    }

    const selLogs = s.habits.map((h) => ({ h, logs: (h.history && h.history[ui.selectedCalDate]) || [] })).filter((x) => x.logs.length > 0);
    const doneTodos = s.todos.filter((t) => t.done).length;
    const totalTodos = s.todos.length;
    const todoPct = totalTodos ? Math.round((doneTodos / totalTodos) * 100) : 0;
    const overdue = s.todos.filter((t) => !t.done && t.deadline && t.deadline < today).length;

    wrap.innerHTML = `
      <div class="do-stats-div"><span>HABIT</span></div>
      <div class="do-stat-grid">
        <div class="do-stat-card"><div class="do-stat-val">${activeHabits.length}</div><div class="do-stat-lbl">Total Habit</div></div>
        <div class="do-stat-card"><div class="do-stat-val">${streak}</div><div class="do-stat-lbl">Streak Hari</div></div>
        <div class="do-stat-card"><div class="do-stat-val">${prog.pct}%</div><div class="do-stat-lbl">Hari Ini</div></div>
        <div class="do-stat-card"><div class="do-stat-val">${avg7}%</div><div class="do-stat-lbl">Rata-rata 7H</div></div>
      </div>

      <div class="do-card">
        <div class="do-card-hdr">
          <span class="do-card-title">Konsistensi</span>
          <div class="do-range-row">
            ${[[7, '7 Hari'], [30, '30 Hari'], [90, '3 Bulan']].map(([d, l]) =>
              `<button type="button" class="do-range-btn ${ui.chartRange === d ? 'active' : ''}" data-range="${d}">${l}</button>`
            ).join('')}
          </div>
        </div>
        ${buildTrendSvg(s.habits, ui.chartRange)}
      </div>

      <div class="do-card">
        <div class="do-card-hdr">
          <span class="do-card-title">Kalender Bulanan</span>
          <div class="do-cal-nav">
            <button type="button" class="do-cal-btn" id="do-cal-prev">‹</button>
            <span style="font-size:12px;font-weight:600">${MONTH_NAMES_ID[month]} ${year}</span>
            <button type="button" class="do-cal-btn" id="do-cal-next">›</button>
          </div>
        </div>
        <div class="do-cal-grid">
          ${['S', 'S', 'R', 'K', 'J', 'S', 'M'].map((d) => `<div class="do-cal-dow">${d}</div>`).join('')}
          ${calCells}
        </div>
        <div class="do-cal-detail">
          <div class="do-cal-detail-hdr">
            <span style="font-size:12px;font-weight:600">${fmtFullDate(ui.selectedCalDate)}</span>
            ${selLogs.length ? '<button type="button" class="do-btn-clear-day" id="do-clear-day-btn">Hapus Data Hari Ini</button>' : ''}
          </div>
          ${!selLogs.length
            ? '<div style="font-size:12px;color:var(--do-text3)">Tidak ada aktivitas.</div>'
            : selLogs.map(({ h, logs }) => `
              <div style="display:flex;gap:8px;margin-bottom:6px;align-items:flex-start">
                <span style="width:8px;height:8px;border-radius:50%;background:${esc(h.color || '#1A1917')};margin-top:5px;flex-shrink:0"></span>
                <div>
                  <div style="font-size:12.5px;font-weight:600">${esc(h.name)}${h.archived ? ' <span style="font-size:11px;color:var(--do-text3)">(dihapus)</span>' : ''}</div>
                  ${logs.some((l) => l.id !== 'main')
                    ? logs.map((l) => `<div style="font-size:11px;color:var(--do-text2)">· ${esc(l.text || h.subTasks.find((st) => st.id === l.id)?.text || l.id)}</div>`).join('')
                    : '<div style="font-size:11px;color:var(--do-text2)">Selesai</div>'}
                </div>
              </div>
            `).join('')}
        </div>
      </div>

      <div class="do-stats-div"><span>TO-DO</span></div>
      <div class="do-stat-grid">
        <div class="do-stat-card"><div class="do-stat-val">${totalTodos}</div><div class="do-stat-lbl">Total Task</div></div>
        <div class="do-stat-card"><div class="do-stat-val">${doneTodos}</div><div class="do-stat-lbl">Selesai</div></div>
        <div class="do-stat-card"><div class="do-stat-val">${todoPct}%</div><div class="do-stat-lbl">Completion</div></div>
        <div class="do-stat-card"><div class="do-stat-val ${overdue ? 'danger' : ''}">${overdue}</div><div class="do-stat-lbl">Overdue</div></div>
      </div>

      <div class="do-card">
        <div class="do-card-title" style="margin-bottom:10px">Prioritas</div>
        ${[['urgent', 'Hari Ini (Urgent)', 'var(--do-urgent)'], ['soon', 'Minggu Ini (Soon)', 'var(--do-soon)'], ['normal', 'Nanti (Normal)', 'var(--do-normal)']].map(([k, l, col]) => {
          const tot = s.todos.filter((t) => t.priority === k).length;
          const dn = s.todos.filter((t) => t.priority === k && t.done).length;
          const pct = tot ? Math.round((dn / tot) * 100) : 0;
          return `
            <div class="do-prog-row">
              <div class="do-prog-top"><span>${l}</span><strong style="color:var(--do-text)">${dn}/${tot}</strong></div>
              <div class="do-prog-track"><div class="do-prog-fill" style="width:${pct}%;background:${col}"></div></div>
            </div>
          `;
        }).join('')}
      </div>
    `;

    wrap.querySelectorAll('[data-range]').forEach((b) => b.addEventListener('click', () => {
      ui.chartRange = Number(b.dataset.range);
      renderStatsView();
    }));
    wrap.querySelector('#do-cal-prev')?.addEventListener('click', () => {
      ui.calMonth = new Date(year, month - 1, 1);
      renderStatsView();
    });
    wrap.querySelector('#do-cal-next')?.addEventListener('click', () => {
      ui.calMonth = new Date(year, month + 1, 1);
      renderStatsView();
    });
    wrap.querySelectorAll('[data-cal-ds]').forEach((c) => c.addEventListener('click', () => {
      ui.selectedCalDate = c.dataset.calDs;
      renderStatsView();
    }));
    wrap.querySelector('#do-clear-day-btn')?.addEventListener('click', () => {
      const ds = ui.selectedCalDate;
      showConfirm('Hapus Data Hari Ini', `Hapus semua catatan habit pada ${fmtFullDate(ds)}? XP akan dihitung ulang.`, () => {
        s.habits.forEach((h) => { if (h.history) delete h.history[ds]; });
        persist();
        renderStatsView();
      }, 'Hapus', true);
    });
  }

  async function refreshCoachMemoryInBackground() {
    const s = state();
    const active = s.habits.filter((h) => !h.archived);
    if (!active.length) return;
    const today = getToday();
    const diffDays = s.memoryDate ? Math.floor((new Date(today) - new Date(s.memoryDate)) / 86400000) : 99;
    if (s.coachMemory && diffDays < 3) return;
    const stats = getHabitStats30Days(s.habits);
    if (!stats.length) return;
    const statsText = stats.map((st) => `- "${st.name}" (${st.category}): ${st.completedCount}/${st.scheduledCount} (${st.completionRate}%), streak ${st.currentStreak}h${st.weakDays.length ? `, sering skip ${st.weakDays.join(', ')}` : ''}`).join('\n');
    try {
      const res = await fetch('/api/dailyos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode: 'memory', todayLabel: fmtFullDate(today), statsText }),
      });
      const data = await res.json();
      if (data?.memory) {
        s.coachMemory = { ...data.memory, lastUpdated: today, habitCount: stats.length };
        s.memoryDate = today;
        persist();
      }
    } catch (_) {}
  }

  async function fetchCoachBriefing() {
    ui.coachLoading = true;
    ui.coachError = null;
    renderAiView();
    refreshCoachMemoryInBackground();
    const s = state();
    const today = getToday();
    const active = s.habits.filter((h) => !h.archived);
    const habitSummary = !active.length ? 'Belum ada habit.' : active.map((h) => {
      const sched = isHabitScheduled(h, today);
      const tc = h.subTasks.length || 1;
      const done = ((h.history && h.history[today]) || []).length;
      const str = calcHabitStreak(h);
      const st = !sched ? 'tidak dijadwalkan hari ini' : (done >= tc ? 'SELESAI' : (done > 0 ? `sebagian (${done}/${tc})` : 'belum'));
      return `- ${h.name} (${h.category}): ${st}${str > 0 ? `, streak ${str} hari` : ''}`;
    }).join('\n');
    const pending = s.todos.filter((t) => !t.done);
    const todoSummary = !pending.length ? 'Tidak ada task pending.' : pending.map((t) => {
      const ov = t.deadline && t.deadline < today ? ' [OVERDUE]' : '';
      return `- [${t.priority}] ${t.title}${t.deadline ? ` (${t.deadline})` : ''}${ov}`;
    }).join('\n');

    try {
      const res = await fetch('/api/dailyos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          mode: 'coach',
          todayLabel: fmtFullDate(today),
          habitSummary,
          todoSummary,
          memory: s.coachMemory,
          idekuIdeas: getIdekuProses(),
        }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data?.error || 'Server error');
      ui.coachText = data.text || 'Tetap semangat menjalani hari ini.';
    } catch (err) {
      ui.coachError = err.message || 'Gagal menghubungi AI Coach.';
    } finally {
      ui.coachLoading = false;
      renderAiView();
    }
  }

  function renderAiView() {
    const wrap = root.querySelector('#do-view-ai');
    const prevInput = wrap.querySelector('#do-smart-input')?.value || '';
    const s = state();
    const today = getToday();
    const active = s.habits.filter((h) => !h.archived);

    const previewRows = active.map((h) => {
      const sched = isHabitScheduled(h, today);
      const tc = h.subTasks.length || 1;
      const done = ((h.history && h.history[today]) || []).length;
      const str = calcHabitStreak(h);
      const stLbl = !sched ? '—' : (done >= tc ? 'Done' : (done > 0 ? `${done}/${tc}` : 'Belum'));
      const stCol = !sched ? 'var(--do-text3)' : (done >= tc ? 'var(--do-ai-accent)' : 'var(--do-text2)');
      return `
        <div class="do-preview-row">
          <span style="font-weight:500">${esc(h.name)}</span>
          <span style="display:flex;gap:8px;align-items:center">
            ${str > 0 ? `<span style="font-size:11px;color:var(--do-soon)">${str}h</span>` : ''}
            <strong style="font-size:11px;color:${stCol}">${stLbl}</strong>
          </span>
        </div>
      `;
    }).join('');

    let parsedHtml = '';
    if (ui.parsedTasks && ui.parsedTasks.length) {
      parsedHtml = `
        <div class="do-ai-preview">
          <div style="font-size:12px;font-weight:600;color:var(--do-text2);margin-bottom:7px">Ditemukan ${ui.parsedTasks.length} task:</div>
          ${ui.parsedTasks.map((t) => `
            <div class="do-ai-item">
              <div>
                <div style="font-size:13px;font-weight:600">${esc(t.judul)}</div>
                <div style="font-size:11px;color:var(--do-text2);margin-top:2px">${esc(t.priority)}${t.deadline ? ` · ${esc(t.deadline)}` : ''}</div>
                ${Array.isArray(t.subtasks) && t.subtasks.length ? `<div style="font-size:11px;color:var(--do-text3);margin-top:2px">Subtasks: ${esc(t.subtasks.join(', '))}</div>` : ''}
              </div>
            </div>
          `).join('')}
          <button type="button" class="do-btn-pri" id="do-confirm-parsed" style="width:100%">+ Tambahkan ${ui.parsedTasks.length} Task ke To-Do</button>
        </div>
      `;
    }

    let rekomHtml = '';
    if (ui.rekomList && ui.rekomList.length) {
      rekomHtml = `
        <div class="do-ai-preview">
          <div style="font-size:12px;font-weight:600;color:var(--do-text2);margin-bottom:7px">${ui.rekomList.length} Rekomendasi To-Do:</div>
          ${ui.rekomList.map((r, i) => `
            <div class="do-ai-item">
              <div>
                <div style="font-size:13px;font-weight:600">${esc(r.judul)}</div>
                <div style="font-size:11px;color:var(--do-text2);margin-top:2px">${esc(r.priority)}${r.deadline ? ` · ${esc(r.deadline)}` : ''}</div>
                ${r.alasan ? `<div style="font-size:11px;color:var(--do-text2);margin-top:2px">↳ ${esc(r.alasan)}</div>` : ''}
              </div>
              <button type="button" class="do-ai-dismiss" data-rekom-del="${i}">×</button>
            </div>
          `).join('')}
          <button type="button" class="do-btn-pri" id="do-confirm-rekom" style="width:100%">+ Tambahkan Rekomendasi ke To-Do</button>
        </div>
      `;
    }

    wrap.innerHTML = `
      <div class="do-card">
        <div class="do-ai-card-title">AI Coach</div>
        <div class="do-ai-card-sub">Briefing harian berdasarkan progres habit, to-do & fokus IdeKu.</div>
        ${active.length ? `<div class="do-preview-box">${previewRows}</div>` : ''}
        <div class="do-coach-box">
          <div class="do-coach-hdr">
            <span class="do-coach-lbl">AI COACH BRIEFING</span>
            <button type="button" class="do-mini-btn" id="do-coach-refresh" style="color:var(--do-ai-accent)" title="Refresh">
              <svg viewBox="0 0 24 24"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
            </button>
          </div>
          <div class="do-coach-body">${
            ui.coachLoading
              ? 'Menganalisis data…'
              : (ui.coachError ? esc(ui.coachError) : esc(ui.coachText || 'Tap refresh untuk meminta briefing.'))
          }</div>
        </div>
      </div>

      <div class="do-card">
        <div class="do-ai-card-title">Smart To-Do</div>
        <div class="do-ai-card-sub">Tulis natural — AI ekstrak menjadi daftar task lengkap.</div>
        <textarea class="do-textarea" id="do-smart-input" placeholder="Contoh: besok kerjakan laporan bulanan urgent sama beli kopi...">${esc(prevInput)}</textarea>
        <div class="do-btn-row">
          <button type="button" class="do-btn-pri" id="do-smart-parse">Parse dengan AI</button>
          <button type="button" class="do-btn-sec" id="do-smart-rekom">Rekomendasi AI</button>
        </div>
        ${parsedHtml}
        ${rekomHtml}
      </div>
    `;

    wrap.querySelector('#do-coach-refresh')?.addEventListener('click', fetchCoachBriefing);
    wrap.querySelector('#do-smart-parse')?.addEventListener('click', async () => {
      const inp = wrap.querySelector('#do-smart-input')?.value?.trim() || '';
      if (!inp) { showAlert('Input Kosong', 'Ketik task terlebih dahulu.'); return; }
      const btn = wrap.querySelector('#do-smart-parse');
      btn.textContent = 'Memproses…'; btn.disabled = true;
      try {
        const res = await fetch('/api/dailyos', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ mode: 'parse_todo', todayLabel: fmtFullDate(today), input: inp }),
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data?.error || 'Gagal parse');
        ui.parsedTasks = Array.isArray(data.tasks) ? data.tasks : [];
        if (!ui.parsedTasks.length) showAlert('Tidak Ditemukan', 'AI tidak menemukan task dalam teks tersebut.');
        renderAiView();
      } catch (err) {
        showAlert('Error AI', err.message);
        renderAiView();
      }
    });

    wrap.querySelector('#do-confirm-parsed')?.addEventListener('click', () => {
      if (!ui.parsedTasks?.length) return;
      const base = Date.now();
      ui.parsedTasks.forEach((t, i) => {
        s.todos.push({
          id: `todo_${base}_${i}`,
          title: String(t.judul || 'Task').trim(),
          priority: ['urgent', 'soon', 'normal'].includes(t.priority) ? t.priority : 'normal',
          deadline: String(t.deadline || ''),
          done: false,
          completedAt: null,
          subtasks: (t.subtasks || []).map((st, si) => ({ id: `ts_${base}_${i}_${si}`, text: String(st), done: false })),
        });
      });
      const added = ui.parsedTasks.length;
      ui.parsedTasks = null;
      const inpEl = wrap.querySelector('#do-smart-input');
      if (inpEl) inpEl.value = '';
      persist();
      renderAiView();
      deps.showMainToast?.(`${added} task ditambahkan ke To-Do`, true);
    });

    wrap.querySelector('#do-smart-rekom')?.addEventListener('click', async () => {
      const btn = wrap.querySelector('#do-smart-rekom');
      btn.textContent = 'Menganalisis…'; btn.disabled = true;
      try {
        const stats = getHabitStats30Days(s.habits);
        const res = await fetch('/api/dailyos', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            mode: 'recommend_todo',
            todayLabel: fmtFullDate(today),
            existingHabitsText: s.habits.filter((h) => !h.archived).map((h) => `${h.name} (${h.category})`).join(', ') || 'Belum ada.',
            habitGapSummary: stats.map((st) => `- "${st.name}": ${st.completedCount}/${st.scheduledCount} (${st.completionRate}%), skip ${st.skipCount}x`).join('\n') || 'Belum ada.',
            doneTodosText: s.todos.filter((t) => t.done).slice(0, 15).map((t) => t.title).join(', ') || 'Belum ada.',
            activeTodosText: s.todos.filter((t) => !t.done).map((t) => `- [${t.priority}] ${t.title}`).join('\n') || 'Tidak ada.',
            memory: s.coachMemory,
            idekuIdeas: getIdekuProses(),
          }),
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data?.error || 'Gagal rekomendasi');
        ui.rekomList = Array.isArray(data.recommendations) ? data.recommendations : [];
        renderAiView();
      } catch (err) {
        showAlert('Error AI', err.message);
        renderAiView();
      }
    });

    wrap.querySelectorAll('[data-rekom-del]').forEach((b) => {
      b.addEventListener('click', () => {
        ui.rekomList.splice(Number(b.dataset.rekomDel), 1);
        if (!ui.rekomList.length) ui.rekomList = null;
        renderAiView();
      });
    });

    wrap.querySelector('#do-confirm-rekom')?.addEventListener('click', () => {
      if (!ui.rekomList?.length) return;
      const base = Date.now();
      ui.rekomList.forEach((r, i) => {
        s.todos.push({
          id: `todo_${base}_${i}`,
          title: String(r.judul || 'Task').trim(),
          priority: ['urgent', 'soon', 'normal'].includes(r.priority) ? r.priority : 'soon',
          deadline: String(r.deadline || ''),
          done: false,
          completedAt: null,
          subtasks: [],
        });
      });
      const added = ui.rekomList.length;
      ui.rekomList = null;
      persist();
      renderAiView();
      deps.showMainToast?.(`${added} rekomendasi ditambahkan`, true);
    });
  }

  function downloadFile(filename, content, mime) {
    const blob = new Blob([content], { type: mime });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  function buildSummaryMarkdown(kind, offset) {
    const s = state();
    const isWeek = kind === 'week';
    const today = getToday();
    const now = new Date(`${today}T12:00:00`);
    let start, end;
    if (isWeek) {
      const dow = (now.getDay() + 6) % 7;
      start = new Date(now); start.setDate(now.getDate() - dow - offset * 7);
      end = new Date(start); end.setDate(start.getDate() + 6);
    } else {
      start = new Date(now.getFullYear(), now.getMonth() - offset, 1, 12);
      end = new Date(start.getFullYear(), start.getMonth() + 1, 0, 12);
    }
    const fmtIso = (d) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
    const startStr = fmtIso(start), endStr = fmtIso(end);
    const dates = [];
    for (let c = new Date(start); c <= end; c.setDate(c.getDate() + 1)) dates.push(fmtIso(c));
    const pctOf = (d, t) => (t ? `${Math.round((d / t) * 100)}%` : '–');

    const rows = s.habits.filter((h) => !h.archived).map((h) => {
      const slots = h.subTasks.length || 1;
      let scheduled = 0, full = 0, partial = 0, miss = 0, sDone = 0, sTotal = 0;
      const cells = dates.map((ds) => {
        if (ds > today) return { ds, status: 'future', done: 0, slots: 0 };
        if (ds < (h.createdAt || '0000-00-00') || !isHabitScheduled(h, ds)) return { ds, status: 'off', done: 0, slots: 0 };
        const d = Math.min(slots, ((h.history && h.history[ds]) || []).length);
        const st = d >= slots ? 'full' : (d > 0 ? 'partial' : 'miss');
        scheduled++; if (st === 'full') full++; else if (st === 'partial') partial++; else miss++;
        sDone += d; sTotal += slots;
        return { ds, status: st, done: d, slots };
      });
      return scheduled ? { habit: h, cells, scheduled, full, partial, miss, sDone, sTotal } : null;
    }).filter(Boolean);

    const totDone = rows.reduce((a, r) => a + r.sDone, 0);
    const totTotal = rows.reduce((a, r) => a + r.sTotal, 0);
    const doneTodos = s.todos.filter((t) => t.done && t.completedAt && t.completedAt.slice(0, 10) >= startStr && t.completedAt.slice(0, 10) <= endStr);

    const lines = [
      `# ${isWeek ? 'Summary Mingguan' : 'Summary Bulanan'} — ${fmtShortDate(startStr)} – ${fmtShortDate(endStr)}`,
      '',
      `> **Periode:** ${fmtFullDate(startStr)} → ${fmtFullDate(endStr)}`,
      `> **Dibuat:** ${fmtFullDate(today)}`,
      '',
      '## 📊 Ringkasan',
      '',
      '| Metrik | Hasil |',
      '|---|---|',
      `| Capaian habit | **${pctOf(totDone, totTotal)}** (${totDone} dari ${totTotal} aktivitas) |`,
      `| To-do selesai | **${doneTodos.length}** task |`,
      '',
      '## 📝 To-Do Selesai',
      '',
      ...(doneTodos.length ? doneTodos.map((t) => `- [x] ${t.title}`) : ['_Belum ada to-do yang diselesaikan pada periode ini._']),
      '',
      '## 🧭 Evaluasi Diri',
      '',
      '- **Yang berjalan baik:** ',
      '- **Yang belum berjalan / hambatan:** ',
      '- **Pelajaran:** ',
    ];
    const fname = isWeek ? `summary-mingguan_${startStr}_${endStr}.md` : `summary-bulanan_${startStr.slice(0, 7)}.md`;
    return { filename: fname, content: lines.join('\n') };
  }

  function renderSettingsView() {
    const wrap = root.querySelector('#do-view-settings');
    const s = state();
    wrap.innerHTML = `
      <div class="do-card">
        <div class="do-card-title" style="margin-bottom:10px">Notifikasi Harian</div>
        <div class="do-time-grid">
          <div>
            <label class="do-fld-lbl">Jam Habit</label>
            <input type="time" class="do-input" id="do-notif-h" value="${esc(s.notifHabitTime)}" />
          </div>
          <div>
            <label class="do-fld-lbl">Jam To-Do</label>
            <input type="time" class="do-input" id="do-notif-t" value="${esc(s.notifTodoTime)}" />
          </div>
        </div>
        <button type="button" class="do-btn-pri" id="do-save-notif" style="width:100%">Simpan Pengaturan Notifikasi</button>
      </div>

      <div class="do-card">
        <div class="do-card-title" style="margin-bottom:4px">Summary & Evaluasi</div>
        <div style="font-size:12px;color:var(--do-text2);margin-bottom:10px">Unduh laporan capaian habit & to-do (.md).</div>
        <div style="font-size:10.5px;font-weight:700;color:var(--do-text3);margin-bottom:5px">MINGGUAN</div>
        <div class="do-btn-row" style="margin-top:0;margin-bottom:10px">
          <button type="button" class="do-btn-sec" data-sum="week:0">Minggu ini</button>
          <button type="button" class="do-btn-sec" data-sum="week:1">Minggu lalu</button>
        </div>
        <div style="font-size:10.5px;font-weight:700;color:var(--do-text3);margin-bottom:5px">BULANAN</div>
        <div class="do-btn-row" style="margin-top:0">
          <button type="button" class="do-btn-sec" data-sum="month:0">Bulan ini</button>
          <button type="button" class="do-btn-sec" data-sum="month:1">Bulan lalu</button>
        </div>
      </div>

      <div class="do-card">
        <div class="do-card-title" style="margin-bottom:10px">Data Habit</div>
        <div style="display:flex;flex-direction:column;gap:8px">
          <button type="button" class="do-btn-sec" id="do-backup-json">Backup Data (JSON v3)</button>
          <button type="button" class="do-btn-sec" id="do-restore-json">Restore Data (JSON)</button>
          <button type="button" class="do-btn-sec" id="do-export-csv">Ekspor CSV</button>
        </div>
      </div>

      <div class="do-card" style="border-color:rgba(192,57,43,0.35)">
        <div class="do-card-title" style="color:var(--do-urgent);margin-bottom:10px">Danger Zone</div>
        <button type="button" class="do-btn-danger" id="do-reset-app">Reset Aplikasi</button>
      </div>
    `;

    wrap.querySelector('#do-save-notif').addEventListener('click', () => {
      s.notifHabitTime = wrap.querySelector('#do-notif-h').value || '07:00';
      s.notifTodoTime = wrap.querySelector('#do-notif-t').value || '15:00';
      persist();
      deps.showMainToast?.('Pengaturan notifikasi disimpan', true);
    });
    wrap.querySelectorAll('[data-sum]').forEach((b) => {
      b.addEventListener('click', () => {
        const [k, off] = b.dataset.sum.split(':');
        const { filename, content } = buildSummaryMarkdown(k, Number(off));
        downloadFile(filename, content, 'text/markdown');
      });
    });
    wrap.querySelector('#do-backup-json').addEventListener('click', () => {
      const payload = {
        version: 3,
        habits: s.habits,
        todos: s.todos,
        totalPoints: recalculateXp(s.habits),
        coachMemory: s.coachMemory,
        exportedAt: new Date().toISOString(),
      };
      downloadFile('dailyos-backup.json', JSON.stringify(payload, null, 2), 'application/json');
    });
    wrap.querySelector('#do-restore-json').addEventListener('click', () => root.querySelector('#do-restore-file').click());
    wrap.querySelector('#do-export-csv').addEventListener('click', () => {
      const active = s.habits.filter((h) => !h.archived);
      if (!active.length) { showAlert('Kosong', 'Belum ada habit untuk diekspor.'); return; }
      let csv = 'Tanggal,Habit,Selesai,Total,Persentase\n';
      const dates = [...new Set(active.flatMap((h) => Object.keys(h.history || {})))].sort();
      dates.forEach((ds) => {
        active.forEach((h) => {
          const tc = h.subTasks.length || 1;
          const dn = ((h.history && h.history[ds]) || []).length;
          csv += `"${ds}","${h.name.replace(/"/g, '""')}","${dn}","${tc}","${Math.round((dn / tc) * 100)}%"\n`;
        });
      });
      downloadFile('dailyos-habits.csv', csv, 'text/csv');
    });
    wrap.querySelector('#do-reset-app').addEventListener('click', () => {
      showConfirm('Reset Aplikasi', 'Semua data habit, to-do, riwayat, dan XP akan dihapus permanen. Yakin?', () => {
        resetData();
        deps.showMainToast?.('Data Daily OS berhasil direset', true);
      }, 'Reset Semua', true);
    });
  }

  // ── Modals ──
  function openHabitModal(hid) {
    const h = hid ? state().habits.find((x) => x.id === hid) : null;
    ui.editHabitId = h?.id || null;
    root.querySelector('#do-hm-title').textContent = h ? 'Edit Habit' : 'Tambah Habit';
    root.querySelector('#do-hm-name').value = h?.name || '';
    const cat = h?.category || 'General';
    const selEl = root.querySelector('#do-hm-cat');
    const custEl = root.querySelector('#do-hm-cat-custom');
    if (CATEGORY_PRESETS.includes(cat)) { selEl.value = cat; custEl.style.display = 'none'; custEl.value = ''; }
    else { selEl.value = '__other__'; custEl.style.display = 'block'; custEl.value = cat; }
    ui.hFormColor = h?.color || '#1A1917';
    ui.hFormIcon = h?.icon || 'star';
    ui.hFormDays = h ? [...h.activeDays] : [];
    ui.hFormSubs = h ? h.subTasks.map((x) => ({ ...x })) : [];
    ui.hIconGridOpen = false;
    root.querySelector('#do-hm-icons').style.display = 'none';
    renderHabitModalPickers();
    renderHabitModalDays();
    renderHabitModalSubs();
    root.querySelector('#do-habit-modal').classList.add('open');
  }

  function renderHabitModalPickers() {
    const cWrap = root.querySelector('#do-hm-colors');
    cWrap.innerHTML = HABIT_COLORS.map((hex) => `
      <div class="do-color-dot ${ui.hFormColor.toLowerCase() === hex.toLowerCase() ? 'selected' : ''}"
           style="background:${hex};${isLightHex(hex) ? 'border:1px solid #D0CFC9;' : ''}"
           data-h-col="${hex}"></div>
    `).join('');
    cWrap.querySelectorAll('[data-h-col]').forEach((el) => el.addEventListener('click', () => {
      ui.hFormColor = el.dataset.hCol;
      renderHabitModalPickers();
    }));

    const curIc = HABIT_ICONS.find((i) => i.key === ui.hFormIcon) || HABIT_ICONS[0];
    root.querySelector('#do-hm-icon-toggle').innerHTML = `
      <span style="display:flex;align-items:center;gap:8px">${curIc.svg.replace('<svg ', '<svg style="width:17px;height:17px;stroke:currentColor;fill:none;stroke-width:1.9" ')}<span>Pilih Ikon (${curIc.label})</span></span>
      <span style="font-size:11px;color:var(--do-text2)">${ui.hIconGridOpen ? '▲' : '▼'}</span>
    `;
    const iWrap = root.querySelector('#do-hm-icons');
    iWrap.innerHTML = HABIT_ICONS.map((i) => `
      <button type="button" class="do-icon-cell ${ui.hFormIcon === i.key ? 'selected' : ''}" data-h-ic="${i.key}" title="${i.label}">${i.svg}</button>
    `).join('');
    iWrap.querySelectorAll('[data-h-ic]').forEach((b) => b.addEventListener('click', () => {
      ui.hFormIcon = b.dataset.hIc;
      ui.hIconGridOpen = false;
      iWrap.style.display = 'none';
      renderHabitModalPickers();
    }));
  }

  function renderHabitModalDays() {
    const dWrap = root.querySelector('#do-hm-days');
    dWrap.innerHTML = DAY_BTN_ORDER.map(([num, lbl]) => `
      <button type="button" class="do-day-btn ${ui.hFormDays.includes(num) ? 'active' : ''}" data-h-day="${num}">${lbl}</button>
    `).join('');
    dWrap.querySelectorAll('[data-h-day]').forEach((b) => b.addEventListener('click', () => {
      const n = Number(b.dataset.hDay);
      if (ui.hFormDays.includes(n)) ui.hFormDays = ui.hFormDays.filter((x) => x !== n);
      else ui.hFormDays.push(n);
      renderHabitModalDays();
    }));
    root.querySelector('#do-hm-sched-hint').textContent = ui.hFormDays.length
      ? `Aktif: ${ui.hFormDays.slice().sort().map((n) => DAY_NAMES_SHORT[n]).join(', ')}`
      : 'Semua hari aktif (default: setiap hari)';
  }

  function renderHabitModalSubs() {
    const sWrap = root.querySelector('#do-hm-subs');
    sWrap.innerHTML = ui.hFormSubs.map((sub, idx) => `
      <div class="do-sub-input-row">
        <input class="do-input" type="text" value="${esc(sub.text)}" placeholder="Sub-aktivitas..." data-h-sub-i="${idx}" />
        <button type="button" class="do-icon-btn" data-h-sub-del="${idx}">×</button>
      </div>
    `).join('');
    sWrap.querySelectorAll('[data-h-sub-i]').forEach((inp) => inp.addEventListener('input', () => {
      ui.hFormSubs[Number(inp.dataset.hSubI)].text = inp.value;
    }));
    sWrap.querySelectorAll('[data-h-sub-del]').forEach((b) => b.addEventListener('click', () => {
      ui.hFormSubs.splice(Number(b.dataset.hSubDel), 1);
      renderHabitModalSubs();
    }));
  }

  function saveHabitFromModal() {
    const name = root.querySelector('#do-hm-name').value.trim();
    if (!name) { showAlert('Wajib Diisi', 'Nama habit tidak boleh kosong.'); return; }
    const catSel = root.querySelector('#do-hm-cat').value;
    const category = catSel === '__other__' ? (root.querySelector('#do-hm-cat-custom').value.trim() || 'General') : catSel;
    const cleanSubs = ui.hFormSubs.map((x) => ({ ...x, text: x.text.trim() })).filter((x) => x.text);
    const s = state();
    if (ui.editHabitId) {
      const h = s.habits.find((x) => x.id === ui.editHabitId);
      if (h) {
        // Freeze snapshot texts on historical logs before mutating
        Object.values(h.history || {}).forEach((arr) => {
          arr.forEach((log) => {
            if (!log.text) log.text = log.id === 'main' ? h.name : (h.subTasks.find((st) => st.id === log.id)?.text || log.id);
          });
        });
        h.name = name;
        h.category = category;
        h.color = ui.hFormColor;
        h.icon = ui.hFormIcon;
        h.activeDays = [...ui.hFormDays];
        h.subTasks = cleanSubs;
      }
    } else {
      s.habits.push({
        id: `hab_${Date.now()}`,
        name,
        category,
        color: ui.hFormColor,
        icon: ui.hFormIcon,
        activeDays: [...ui.hFormDays],
        subTasks: cleanSubs,
        history: {},
        collapsed: false,
        archived: false,
        createdAt: getToday(),
      });
    }
    persist();
    closeModal('#do-habit-modal');
    renderHabitsView();
  }

  function openTodoModal(tid) {
    const t = tid ? state().todos.find((x) => x.id === tid) : null;
    ui.editTodoId = t?.id || null;
    root.querySelector('#do-tm-title').textContent = t ? 'Edit To-Do' : 'Tambah To-Do';
    root.querySelector('#do-tm-input').value = t?.title || '';
    root.querySelector('#do-tm-dl').value = t?.deadline || '';
    ui.tFormPri = t?.priority || 'normal';
    ui.tFormSubs = t ? t.subtasks.map((x) => x.text) : [];
    renderTodoModalPri();
    renderTodoModalSubs();
    root.querySelector('#do-todo-modal').classList.add('open');
  }

  function renderTodoModalPri() {
    root.querySelectorAll('#do-tm-pris .do-pri-opt').forEach((b) => {
      b.classList.remove('selected-urgent', 'selected-soon', 'selected-normal');
      if (b.dataset.pri === ui.tFormPri) b.classList.add(`selected-${ui.tFormPri}`);
    });
  }

  function renderTodoModalSubs() {
    const sWrap = root.querySelector('#do-tm-subs');
    sWrap.innerHTML = ui.tFormSubs.map((txt, idx) => `
      <div class="do-sub-input-row">
        <input class="do-input" type="text" value="${esc(txt)}" placeholder="Subtask..." data-t-sub-i="${idx}" />
        <button type="button" class="do-icon-btn" data-t-sub-del="${idx}">×</button>
      </div>
    `).join('');
    sWrap.querySelectorAll('[data-t-sub-i]').forEach((inp) => inp.addEventListener('input', () => {
      ui.tFormSubs[Number(inp.dataset.tSubI)] = inp.value;
    }));
    sWrap.querySelectorAll('[data-t-sub-del]').forEach((b) => b.addEventListener('click', () => {
      ui.tFormSubs.splice(Number(b.dataset.tSubDel), 1);
      renderTodoModalSubs();
    }));
  }

  function saveTodoFromModal() {
    const title = root.querySelector('#do-tm-input').value.trim();
    if (!title) { showAlert('Wajib Diisi', 'Judul task tidak boleh kosong.'); return; }
    const deadline = root.querySelector('#do-tm-dl').value.trim();
    const cleanSubs = ui.tFormSubs.map((x) => x.trim()).filter(Boolean);
    const s = state();
    const base = Date.now();
    if (ui.editTodoId) {
      const t = s.todos.find((x) => x.id === ui.editTodoId);
      if (t) {
        const oldSubs = t.subtasks || [];
        t.title = title;
        t.priority = ui.tFormPri;
        t.deadline = deadline;
        t.subtasks = cleanSubs.map((txt, i) => {
          const prev = oldSubs.find((os) => os.text.toLowerCase() === txt.toLowerCase());
          return { id: prev?.id || `ts_${base}_${i}`, text: txt, done: prev?.done || false };
        });
      }
    } else {
      s.todos.push({
        id: `todo_${base}`,
        title,
        priority: ui.tFormPri,
        deadline,
        done: false,
        completedAt: null,
        subtasks: cleanSubs.map((txt, i) => ({ id: `ts_${base}_${i}`, text: txt, done: false })),
      });
    }
    persist();
    closeModal('#do-todo-modal');
    renderTodosView();
  }

  // ── Public Module API ──
  function init(config) {
    deps = { ...deps, ...config };
    mountDom();
    state();
    render();
  }

  function open() {
    mountDom();
    root?.classList.add('open');
    render();
  }

  function close() {
    root?.classList.remove('open');
  }

  function resetData() {
    const db = deps.getDb();
    if (!db) return;
    db.dailyos = defaultState();
    ui.coachText = null;
    ui.parsedTasks = null;
    ui.rekomList = null;
    persist();
    render();
  }

  function getSummary() {
    const s = state();
    const streak = calcOverallStreak(s.habits);
    const prog = calcTodayProgress(s.habits);
    const pendingTodos = s.todos.filter((t) => !t.done).length;
    return {
      streakText: `${streak} Hari Streak`,
      progressLabel: prog.scheduledHabits === 0 ? 'Belum ada habit hari ini' : `${prog.completedHabits}/${prog.scheduledHabits} Habit Selesai`,
      pct: prog.pct,
      pendingTodosText: `${pendingTodos} To-Do Pending`,
    };
  }

  function buildContextForAssistant() {
    const s = state();
    const active = s.habits.filter((h) => !h.archived);
    if (!active.length && !s.todos.length) return '';
    const prog = calcTodayProgress(s.habits);
    const streak = calcOverallStreak(s.habits);
    const pending = s.todos.filter((t) => !t.done);
    const lines = [`\n\n## Konteks Daily OS (${prog.completedHabits}/${prog.scheduledHabits} habit selesai [${prog.pct}%], Streak ${streak}h)`];
    if (pending.length) {
      lines.push(`To-Do Pending (${pending.length}): ` + pending.slice(0, 4).map((t) => `${t.title} [${t.priority}]`).join('; '));
    }
    return lines.join('\n');
  }

  function applyTagAction(parts) {
    if (!parts || parts.length < 2) return null;
    const cmd = String(parts[0] || '').trim().toUpperCase();
    const s = state();
    const today = getToday();
    if (cmd === 'HABIT') {
      const name = String(parts[1] || '').trim();
      if (!name) return null;
      const category = String(parts[2] || 'General').trim() || 'General';
      const icon = String(parts[3] || 'star').trim() || 'star';
      s.habits.push({
        id: `hab_${Date.now()}`,
        name,
        category,
        color: '#1A1917',
        icon,
        activeDays: [],
        subTasks: [],
        history: {},
        collapsed: false,
        archived: false,
        createdAt: today,
      });
      persist();
      if (root?.classList.contains('open')) render();
      return `Habit "${name}"`;
    }
    if (cmd === 'TODO' || cmd === 'TASK') {
      const title = String(parts[1] || '').trim();
      if (!title) return null;
      const pRaw = String(parts[2] || 'urgent').trim().toLowerCase();
      const priority = ['urgent', 'soon', 'normal'].includes(pRaw) ? pRaw : 'urgent';
      const dlRaw = String(parts[3] || '').trim();
      const deadline = /^\d{4}-\d{2}-\d{2}$/.test(dlRaw) ? dlRaw : '';
      s.todos.push({
        id: `todo_${Date.now()}`,
        title,
        priority,
        deadline,
        done: false,
        completedAt: null,
        subtasks: [],
      });
      persist();
      if (root?.classList.contains('open')) render();
      return `To-Do "${title}"`;
    }
    if (cmd === 'CHECK' || cmd === 'DONE' || cmd === 'COMPLETE') {
      const q = String(parts[1] || '').trim().toLowerCase();
      if (!q) return null;
      const hab = s.habits.find((h) => !h.archived && h.name.toLowerCase().includes(q));
      if (hab) {
        const sid = hab.subTasks[0]?.id || 'main';
        const txt = hab.subTasks[0]?.text || hab.name;
        if (!hab.history) hab.history = {};
        if (!Array.isArray(hab.history[today])) hab.history[today] = [];
        if (!hab.history[today].some((l) => l.id === sid)) {
          hab.history[today].push({ id: sid, text: txt, completedAt: new Date().toISOString() });
          persist();
          if (root?.classList.contains('open')) render();
          return `Checklist "${hab.name}"`;
        }
        return null;
      }
      const td = s.todos.find((t) => !t.done && t.title.toLowerCase().includes(q));
      if (td) {
        td.done = true;
        td.completedAt = new Date().toISOString();
        persist();
        if (root?.classList.contains('open')) render();
        return `Selesai "${td.title}"`;
      }
    }
    return null;
  }

  window.DailyOsModule = {
    defaultState,
    normalizeState,
    init,
    open,
    close,
    render,
    resetData,
    getSummary,
    buildContextForAssistant,
    applyTagAction,
  };
})();
