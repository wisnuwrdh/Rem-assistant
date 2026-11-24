(function () {
  'use strict';

  const MONTH_NAMES = ['Januari','Februari','Maret','April','Mei','Juni','Juli','Agustus','September','Oktober','November','Desember'];
  const MONTH_SHORT = ['Jan','Feb','Mar','Apr','Mei','Jun','Jul','Agu','Sep','Okt','Nov','Des'];
  const DONUT_COLORS = ['#00BFA5','#7C8FF5','#FF5370','#FFAB00','#00B0FF','#7C4DFF','#FF6D00','#64DD17','#FF4081','#26C6DA'];
  const ACCOUNT_TYPES = [
    { id: 'cash', label: 'Tunai', icon: '💵', group: 'Tunai' },
    { id: 'bank', label: 'Bank', icon: '🏦', group: 'Bank' },
    { id: 'ewallet', label: 'E-Wallet', icon: '📱', group: 'Akun Virtual' },
    { id: 'credit', label: 'Kartu Kredit', icon: '💳', group: 'Kredit' },
    { id: 'invest', label: 'Investasi', icon: '📈', group: 'Investasi' },
    { id: 'other', label: 'Lainnya', icon: '📦', group: 'Lainnya' },
  ];
  const EMOJIS = ['🍔','☕','🛒','🚗','⛽','🏠','💡','📱','🎮','🎬','💊','📚','✈️','👕','🎁','💼','💵','🏦','📈','🏷️','📌','💸','😄','👩','👨','👦','👧','🧑','🐶','🐱','🛍️','📦','🎧','⌚','👟','💻','📷','🏖️'];
  const SV_ICONS = [
    { key: 'bag', emoji: '🛍️' }, { key: 'phone', emoji: '📱' }, { key: 'laptop', emoji: '💻' },
    { key: 'shoe', emoji: '👟' }, { key: 'plane', emoji: '✈️' }, { key: 'home', emoji: '🏠' },
    { key: 'watch', emoji: '⌚' }, { key: 'cam', emoji: '📷' }, { key: 'game', emoji: '🎮' },
    { key: 'car', emoji: '🚗' },
  ];

  const DEFAULT_BOOKS = [{ id: 1, name: 'Personal', accountIds: [1, 2] }];
  const DEFAULT_ACCOUNTS = [
    { id: 1, name: 'Tunai', type: 'cash', icon: '💵', balance: 0, notes: '', useInBook: true, includeAsset: true, isSavings: false },
    { id: 2, name: 'Tabungan', type: 'other', icon: '🎯', balance: 0, notes: 'Akun tabungan wishlist', useInBook: true, includeAsset: true, isSavings: true },
  ];
  const DEFAULT_CATEGORIES = [
    { id: 1, name: 'Makan & Minum', type: 'expense', emoji: '🍔' },
    { id: 2, name: 'Transportasi', type: 'expense', emoji: '🚗' },
    { id: 3, name: 'Belanja', type: 'expense', emoji: '🛒' },
    { id: 4, name: 'Tagihan', type: 'expense', emoji: '💡' },
    { id: 5, name: 'Hiburan', type: 'expense', emoji: '🎬' },
    { id: 6, name: 'Kesehatan', type: 'expense', emoji: '💊' },
    { id: 7, name: 'Pendidikan', type: 'expense', emoji: '📚' },
    { id: 8, name: 'Lainnya', type: 'expense', emoji: '📌' },
    { id: 9, name: 'Gaji', type: 'income', emoji: '💼' },
    { id: 10, name: 'Bonus', type: 'income', emoji: '🎁' },
    { id: 11, name: 'Investasi', type: 'income', emoji: '📈' },
    { id: 12, name: 'Transfer', type: 'both', emoji: '⇄' },
    { id: 13, name: 'Lainnya', type: 'income', emoji: '💸' },
  ];
  const DEFAULT_MEMBERS = [{ id: 'm1', name: 'Diri Sendiri', emoji: '😄' }];

  const clone = (o) => JSON.parse(JSON.stringify(o));
  const genId = () => Date.now() + Math.floor(Math.random() * 10000);
  const pad2 = (n) => String(n).padStart(2, '0');
  const todayStr = () => {
    const d = new Date();
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  };
  const esc = (s) => String(s || '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  function defaultState() {
    return {
      books: clone(DEFAULT_BOOKS),
      accounts: clone(DEFAULT_ACCOUNTS),
      categories: clone(DEFAULT_CATEGORIES),
      members: clone(DEFAULT_MEMBERS),
      transactions: [],
      masterBudgets: [],
      wishlists: [],
      prefs: { currentBookId: 1, visibility: true, budgetCollapsed: false, lastAccByBook: {} },
    };
  }

  function normalizeState(raw) {
    const base = defaultState();
    if (!raw || typeof raw !== 'object') return base;
    const s = {
      books: Array.isArray(raw.books) && raw.books.length ? raw.books : base.books,
      accounts: Array.isArray(raw.accounts) && raw.accounts.length ? raw.accounts : base.accounts,
      categories: Array.isArray(raw.categories) && raw.categories.length ? raw.categories : base.categories,
      members: Array.isArray(raw.members) && raw.members.length ? raw.members : base.members,
      transactions: Array.isArray(raw.transactions) ? raw.transactions : [],
      masterBudgets: Array.isArray(raw.masterBudgets) ? raw.masterBudgets : [],
      wishlists: Array.isArray(raw.wishlists) ? raw.wishlists : [],
      prefs: { ...base.prefs, ...(raw.prefs || {}) },
    };
    if (!s.accounts.some((a) => a.isSavings)) {
      s.accounts.push({ id: genId(), name: 'Tabungan', type: 'other', icon: '🎯', balance: 0, notes: 'Akun tabungan wishlist', useInBook: true, includeAsset: true, isSavings: true });
    }
    if (!s.books.some((b) => b.id === s.prefs.currentBookId)) s.prefs.currentBookId = s.books[0].id;
    return s;
  }

  let hooks = null;
  let root = null;
  const nowD = new Date();
  const ui = {
    view: 'buku',
    month: nowD.getMonth(),
    year: nowD.getFullYear(),
    anaMonth: nowD.getMonth(),
    anaYear: nowD.getFullYear(),
    anaPeriod: 'month',
    anaTab: 'expense',
    accDetailId: null,
    accMonth: nowD.getMonth(),
    accYear: nowD.getFullYear(),
    accTab: 'analysis',
    trxModal: { editId: null, type: 'out', amountExpr: '0', catId: 1, accId: 1, toAccId: null, date: todayStr(), memberId: 'm1', note: '' },
    accEditId: null,
    accSelectedType: 'cash',
    accSelectedEmoji: '💵',
    catMgrType: 'expense',
    catEditId: null,
    memberEditId: null,
    svEditId: null,
    svDetailId: null,
    svDepId: null,
    svDepAccId: null,
    svWdId: null,
    svWdAccId: null,
    detailTrxId: null,
    budgetEditId: null,
    confirmCb: null,
    promptCb: null,
  };

  const cm = () => normalizeState(hooks?.getDb()?.cucimoney);
  function saveCm(nextState) {
    const db = hooks.getDb();
    db.cucimoney = normalizeState(nextState);
    hooks.saveDb();
    hooks.onUpdate?.();
  }
  const fmtRp = (n, forceVis) => {
    const s = cm();
    const vis = forceVis !== undefined ? forceVis : s.prefs.visibility;
    if (!vis) return 'Rp ••••••';
    const num = Math.round(Number(n) || 0);
    return (num < 0 ? '-Rp ' : 'Rp ') + Math.abs(num).toLocaleString('id-ID');
  };
  const activeBook = (s = cm()) => s.books.find((b) => b.id === s.prefs.currentBookId) || s.books[0];
  const bookAccounts = (s, bookId) => {
    const b = s.books.find((x) => x.id === bookId) || s.books[0];
    const ids = new Set(b?.accountIds || []);
    const matched = s.accounts.filter((a) => !a.isSavings && (ids.has(a.id) || a.useInBook));
    return matched.length ? matched : s.accounts.filter((a) => !a.isSavings);
  };
  const savingsAcc = (s = cm()) => s.accounts.find((a) => a.isSavings);
  const $q = (sel) => root?.querySelector(sel);

  function showCmToast(msg, type = 'ok') {
    const el = $q('#cm-toast');
    if (!el) return;
    el.textContent = msg;
    el.className = `cm-toast show ${type}`;
    clearTimeout(showCmToast._t);
    showCmToast._t = setTimeout(() => el.classList.remove('show'), 2600);
  }

  function fmtDateId(iso) {
    try {
      const d = new Date(iso + 'T00:00:00');
      return d.toLocaleDateString('id-ID', { weekday: 'short', day: 'numeric', month: 'short' });
    } catch (_) { return iso; }
  }

  function evalKbAmount(expr) {
    try {
      const clean = String(expr || '0').replace(/×/g, '*').replace(/÷/g, '/').replace(/−/g, '-').replace(/[^0-9+\-*/.]/g, '');
      if (!clean) return 0;
      const val = Function(`"use strict"; return (${clean})`)();
      return Number.isFinite(val) && val > 0 ? Math.round(val) : 0;
    } catch (_) { return 0; }
  }

  function buildShellHtml() {
    return `
      <!-- BUKU -->
      <div id="cm-view-buku" class="cm-view active">
        <div class="cm-header">
          <div class="cm-header-left">
            <button type="button" class="cm-icon-btn" id="cm-exit-btn" title="Kembali ke Apps">←</button>
            <button type="button" class="cm-icon-btn" id="cm-search-btn" title="Cari">🔍</button>
          </div>
          <button type="button" class="cm-book-sel" id="cm-book-btn">
            <span>📖</span><span id="cm-book-name">Personal</span><span>▾</span>
          </button>
          <button type="button" class="cm-icon-btn" id="cm-eye-btn" title="Tampil/Sembunyikan Saldo">👁</button>
        </div>
        <div class="cm-hero-card">
          <div class="cm-month-nav">
            <button type="button" class="cm-nav-arrow" id="cm-m-prev">‹</button>
            <span class="cm-month-label" id="cm-buku-month">NOVEMBER 2026</span>
            <button type="button" class="cm-nav-arrow" id="cm-m-next">›</button>
          </div>
          <div class="cm-hero-saldo-label">Total Saldo</div>
          <div class="cm-hero-saldo" id="cm-buku-total">Rp 0</div>
          <div class="cm-hero-stats">
            <div class="cm-hero-stat">
              <div class="cm-hero-stat-lbl"><span class="cm-dot" style="background:var(--cm-income)"></span> Pemasukan</div>
              <div class="cm-hero-stat-amt cm-in" id="cm-buku-in">Rp 0</div>
            </div>
            <div class="cm-hero-stat-div"></div>
            <div class="cm-hero-stat">
              <div class="cm-hero-stat-lbl"><span class="cm-dot" style="background:var(--cm-expense)"></span> Pengeluaran</div>
              <div class="cm-hero-stat-amt cm-out" id="cm-buku-out">Rp 0</div>
            </div>
          </div>
        </div>
        <div id="cm-buku-budget-wrap"></div>
        <div class="cm-sec-title">Transaksi</div>
        <div id="cm-trx-list"></div>
      </div>

      <!-- DOMPET -->
      <div id="cm-view-dompet" class="cm-view">
        <div class="cm-header">
          <div class="cm-header-left">
            <button type="button" class="cm-icon-btn" data-cm-exit>←</button>
            <div class="cm-title">Dompet</div>
          </div>
          <button type="button" class="cm-icon-btn" data-cm-eye>👁</button>
        </div>
        <div class="cm-wallet-hero">
          <div class="cm-hero-saldo-label">Aset Bersih</div>
          <div class="cm-hero-saldo" style="margin-bottom:0" id="cm-dompet-net">Rp 0</div>
          <div class="cm-wallet-sub">
            <div class="cm-wallet-sub-box">
              <div class="cm-hero-stat-lbl" style="justify-content:center">Aset</div>
              <div class="cm-hero-stat-amt cm-in" id="cm-dompet-asset">Rp 0</div>
            </div>
            <div class="cm-wallet-sub-box">
              <div class="cm-hero-stat-lbl" style="justify-content:center">Hutang</div>
              <div class="cm-hero-stat-amt cm-out" id="cm-dompet-liab">Rp 0</div>
            </div>
          </div>
        </div>
        <div class="cm-sec-title">Akun Saya</div>
        <div id="cm-account-list"></div>
        <button type="button" class="cm-add-dashed" id="cm-add-acc-btn">+ Tambah Akun</button>
      </div>

      <!-- ANALISIS -->
      <div id="cm-view-analisis" class="cm-view">
        <div class="cm-header">
          <div class="cm-header-left">
            <button type="button" class="cm-icon-btn" data-cm-exit>←</button>
            <div class="cm-title">Analisis</div>
          </div>
          <button type="button" class="cm-icon-btn" data-cm-eye>👁</button>
        </div>
        <div class="cm-ana-wrap">
          <div class="cm-month-nav" style="margin-bottom:10px">
            <button type="button" class="cm-nav-arrow" id="cm-ana-prev">‹</button>
            <span class="cm-month-label" id="cm-ana-month-lbl">NOVEMBER 2026</span>
            <button type="button" class="cm-nav-arrow" id="cm-ana-next">›</button>
          </div>
          <div class="cm-seg-tabs" id="cm-ana-periods">
            <button type="button" class="cm-seg-tab active" data-period="month">Bulan</button>
            <button type="button" class="cm-seg-tab" data-period="year">Tahun</button>
            <button type="button" class="cm-seg-tab" data-period="all">Semua</button>
          </div>
          <div class="cm-sum3">
            <div class="cm-sum-card"><div class="cm-sum-lbl">Masuk</div><div class="cm-sum-val cm-in" id="cm-ana-in">Rp 0</div><div class="cm-sum-delta" id="cm-ana-in-d"></div></div>
            <div class="cm-sum-card"><div class="cm-sum-lbl">Keluar</div><div class="cm-sum-val cm-out" id="cm-ana-out">Rp 0</div><div class="cm-sum-delta" id="cm-ana-out-d"></div></div>
            <div class="cm-sum-card"><div class="cm-sum-lbl">Selisih</div><div class="cm-sum-val" id="cm-ana-net">Rp 0</div><div class="cm-sum-delta" id="cm-ana-net-d"></div></div>
          </div>
          <div class="cm-seg-tabs" id="cm-ana-tabs">
            <button type="button" class="cm-seg-tab active" data-atab="expense">Keluar</button>
            <button type="button" class="cm-seg-tab" data-atab="income">Masuk</button>
            <button type="button" class="cm-seg-tab" data-atab="budget">Budget</button>
            <button type="button" class="cm-seg-tab" data-atab="tren">Tren</button>
          </div>
        </div>
        <div id="cm-ana-body"></div>
      </div>

      <!-- LAINNYA (LEBIH) -->
      <div id="cm-view-lebih" class="cm-view">
        <div class="cm-header">
          <div class="cm-header-left">
            <button type="button" class="cm-icon-btn" data-cm-exit>←</button>
            <div class="cm-title">Lainnya</div>
          </div>
        </div>
        <div class="cm-lebih-grid" id="cm-lebih-grid"></div>
      </div>

      <!-- TABUNGAN -->
      <div id="cm-view-tabungan" class="cm-view">
        <div class="cm-header">
          <div class="cm-header-left">
            <button type="button" class="cm-icon-btn" id="cm-sv-back">←</button>
            <div class="cm-title">🎯 Tabungan</div>
          </div>
          <button type="button" class="cm-icon-btn" id="cm-sv-add">+</button>
        </div>
        <div id="cm-sv-hero"></div>
        <div id="cm-sv-list"></div>
        <button type="button" class="cm-add-dashed" id="cm-sv-add2">+ Tambah Wishlist</button>
      </div>

      <!-- FAB -->
      <button type="button" class="cm-fab visible" id="cm-fab" title="Catat Transaksi">+</button>

      <!-- BOTTOM NAV -->
      <nav class="cm-bottom-nav">
        <button type="button" class="cm-nav-item active" data-cmview="buku"><span>📖</span><span class="cm-nav-label">Buku</span></button>
        <button type="button" class="cm-nav-item" data-cmview="dompet"><span>💳</span><span class="cm-nav-label">Dompet</span></button>
        <button type="button" class="cm-nav-item" data-cmview="analisis"><span>📊</span><span class="cm-nav-label">Analisis</span></button>
        <button type="button" class="cm-nav-item" data-cmview="lebih"><span>⋯</span><span class="cm-nav-label">Lainnya</span></button>
      </nav>

      <!-- BACKDROP & DYNAMIC SHEETS/MODALS -->
      <div class="cm-backdrop" id="cm-backdrop"></div>
      <div class="cm-sheet" id="cm-sheet"></div>
      <div class="cm-modal-fs" id="cm-modal-fs"></div>
      <div class="cm-modal-fs" id="cm-modal-sub" style="z-index:85"></div>
      <div class="cm-dialog-center" id="cm-dialog"></div>
      <input type="file" id="cm-restore-input" accept=".json" style="display:none" />
      <div class="cm-toast" id="cm-toast"></div>
    `;
  }

  function renderAll() {
    if (!root) return;
    const s = cm();
    root.querySelectorAll('.cm-view').forEach((v) => v.classList.remove('active'));
    $q(`#cm-view-${ui.view}`)?.classList.add('active');
    root.querySelectorAll('.cm-nav-item').forEach((b) => {
      const target = ui.view === 'tabungan' ? 'lebih' : ui.view;
      b.classList.toggle('active', b.dataset.cmview === target);
    });
    $q('#cm-fab')?.classList.toggle('visible', ui.view === 'buku');

    if (ui.view === 'buku') renderBuku(s);
    else if (ui.view === 'dompet') renderDompet(s);
    else if (ui.view === 'analisis') renderAnalisis(s);
    else if (ui.view === 'lebih') renderLebih(s);
    else if (ui.view === 'tabungan') renderTabungan(s);
  }

  function renderBuku(s) {
    const book = activeBook(s);
    $q('#cm-book-name').textContent = book.name;
    $q('#cm-buku-month').textContent = `${MONTH_NAMES[ui.month].toUpperCase()} ${ui.year}`;
    const accs = bookAccounts(s, book.id);
    const totalBal = accs.reduce((sum, a) => sum + (Number(a.balance) || 0), 0);
    $q('#cm-buku-total').textContent = fmtRp(totalBal, s.prefs.visibility);

    const prefix = `${ui.year}-${pad2(ui.month + 1)}`;
    const monthTrxs = s.transactions.filter((t) => t.bookId === book.id && String(t.date || '').startsWith(prefix));
    let mIn = 0, mOut = 0;
    monthTrxs.forEach((t) => {
      if (t.transferPair != null) return;
      if (t.direction === 'in') mIn += t.amount;
      else mOut += t.amount;
    });
    $q('#cm-buku-in').textContent = fmtRp(mIn, s.prefs.visibility);
    $q('#cm-buku-out').textContent = fmtRp(mOut, s.prefs.visibility);

    // Master Budget mini card
    const bWrap = $q('#cm-buku-budget-wrap');
    const mb = s.masterBudgets.find((b) => b.bookId === book.id && b.month === ui.month && b.year === ui.year);
    if (mb && mb.limit > 0) {
      const spent = s.transactions
        .filter((t) => t.bookId === book.id && t.direction === 'out' && t.transferPair == null && t.date >= mb.startDate && t.date <= mb.endDate)
        .reduce((a, b) => a + b.amount, 0);
      const pct = Math.min(100, Math.round((spent / mb.limit) * 100));
      const over = spent > mb.limit;
      const color = over ? 'var(--cm-expense)' : pct >= 80 ? '#FFAB00' : 'var(--cm-primary)';
      bWrap.innerHTML = `
        <div class="cm-buku-budget ${s.prefs.budgetCollapsed ? 'collapsed' : ''}" id="cm-buku-budget-card">
          <div class="cm-buku-budget-head" id="cm-buku-budget-toggle">
            <span style="font-size:12px;font-weight:800">🎯 Budget Bulan Ini</span>
            <span style="font-size:11.5px;font-weight:800;color:${color}">${pct}% ${s.prefs.budgetCollapsed ? '▾' : '▴'}</span>
          </div>
          <div class="cm-buku-budget-body">
            <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:6px">
              <span style="color:var(--cm-muted)">Terpakai: <strong style="color:var(--cm-text)">${fmtRp(spent)}</strong></span>
              <span style="color:var(--cm-muted)">Batas: <strong>${fmtRp(mb.limit)}</strong></span>
            </div>
            <div class="cm-bar-track"><div class="cm-bar-fill" style="width:${pct}%;background:${color}"></div></div>
          </div>
        </div>`;
      $q('#cm-buku-budget-toggle')?.addEventListener('click', () => {
        const next = cm();
        next.prefs.budgetCollapsed = !next.prefs.budgetCollapsed;
        saveCm(next);
        renderBuku(cm());
      });
    } else {
      bWrap.innerHTML = '';
    }

    // Group transactions by date, merge transfer pairs
    const listEl = $q('#cm-trx-list');
    if (!monthTrxs.length) {
      listEl.innerHTML = `<div class="cm-empty">Belum ada transaksi di ${MONTH_NAMES[ui.month]} ${ui.year}.<br>Ketuk + untuk mencatat atau minta Rem!</div>`;
      return;
    }

    const seenPair = new Set();
    const displayRows = [];
    const sorted = [...monthTrxs].sort((a, b) => (b.date || '').localeCompare(a.date || '') || (b.id - a.id));
    sorted.forEach((t) => {
      if (t.transferPair != null) {
        if (seenPair.has(t.transferPair)) return;
        seenPair.add(t.transferPair);
        const pair = monthTrxs.filter((x) => x.transferPair === t.transferPair);
        const outT = pair.find((x) => x.direction === 'out') || t;
        const inT = pair.find((x) => x.direction === 'in') || t;
        displayRows.push({ ...outT, _isTransfer: true, _fromAcc: outT.accountId, _toAcc: inT.accountId });
      } else {
        displayRows.push(t);
      }
    });

    const groups = new Map();
    displayRows.forEach((t) => {
      if (!groups.has(t.date)) groups.set(t.date, []);
      groups.get(t.date).push(t);
    });

    const catById = new Map(s.categories.map((c) => [c.id, c]));
    const accById = new Map(s.accounts.map((a) => [a.id, a]));
    listEl.innerHTML = Array.from(groups.entries()).map(([date, items]) => {
      let dIn = 0, dOut = 0;
      items.forEach((x) => {
        if (x._isTransfer) return;
        if (x.direction === 'in') dIn += x.amount; else dOut += x.amount;
      });
      const rowsHtml = items.map((t) => {
        const cat = catById.get(t.categoryId);
        const acc = accById.get(t.accountId);
        const toAcc = t._toAcc ? accById.get(t._toAcc) : null;
        const cls = t._isTransfer ? 'transfer' : t.direction === 'in' ? 'in' : 'out';
        const ico = t._isTransfer ? '⇄' : (cat?.emoji || '📌');
        const sub = t._isTransfer ? `${acc?.name || '?'} → ${toAcc?.name || '?'}` : `${cat?.name || 'Lainnya'} · ${acc?.name || ''}`;
        const amt = `${t._isTransfer ? '' : (t.direction === 'in' ? '+ ' : '− ')}${fmtRp(t.amount)}`;
        return `
          <div class="cm-trx-item" data-trxid="${t.id}">
            <div class="cm-trx-left">
              <div class="cm-trx-ico ${cls}">${ico}</div>
              <div class="cm-trx-info">
                <div class="cm-trx-title">${esc(t.title)}</div>
                <div class="cm-trx-sub">${esc(sub)}</div>
              </div>
            </div>
            <div class="cm-trx-amt ${cls === 'in' ? 'cm-in' : cls === 'out' ? 'cm-out' : 'cm-trf'}">${amt}</div>
          </div>`;
      }).join('');
      return `
        <div class="cm-trx-group">
          <div class="cm-trx-group-head">
            <span class="cm-trx-date">${esc(fmtDateId(date))}</span>
            <div class="cm-trx-sum">
              ${dIn > 0 ? `<div class="cm-in">+ ${fmtRp(dIn)}</div>` : ''}
              ${dOut > 0 ? `<div class="cm-out">− ${fmtRp(dOut)}</div>` : ''}
            </div>
          </div>
          <div class="cm-trx-group-items">${rowsHtml}</div>
        </div>`;
    }).join('');

    listEl.querySelectorAll('.cm-trx-group-head').forEach((h) => {
      h.addEventListener('click', () => h.parentElement.classList.toggle('collapsed'));
    });
    listEl.querySelectorAll('.cm-trx-item').forEach((row) => {
      row.addEventListener('click', () => openTrxDetail(Number(row.dataset.trxid)));
    });
  }

  function renderDompet(s) {
    let assets = 0, liab = 0;
    s.accounts.forEach((a) => {
      if (!a.includeAsset) return;
      if (a.type === 'credit') liab += Math.max(0, -a.balance);
      else assets += a.balance;
    });
    $q('#cm-dompet-net').textContent = fmtRp(assets - liab);
    $q('#cm-dompet-asset').textContent = fmtRp(assets);
    $q('#cm-dompet-liab').textContent = fmtRp(liab);

    const groups = new Map();
    s.accounts.forEach((a) => {
      if (a.isSavings && a.balance <= 0) return;
      const def = ACCOUNT_TYPES.find((t) => t.id === a.type);
      const grp = a.isSavings ? 'Tabungan Wishlist' : (def?.group || 'Lainnya');
      if (!groups.has(grp)) groups.set(grp, []);
      groups.get(grp).push(a);
    });

    const el = $q('#cm-account-list');
    el.innerHTML = Array.from(groups.entries()).map(([grp, accs]) => {
      const grpTotal = accs.reduce((sum, a) => sum + a.balance, 0);
      return `
        <div class="cm-acc-group">
          <div class="cm-acc-group-head">
            <span>${esc(grp)}</span><span>${fmtRp(grpTotal)}</span>
          </div>
          ${accs.map((a) => `
            <div class="cm-acc-row" data-accid="${a.id}">
              <div class="cm-trx-left">
                <div class="cm-acc-ico">${esc(a.icon || '💵')}</div>
                <div class="cm-trx-info">
                  <div class="cm-trx-title">${esc(a.name)}</div>
                  ${a.notes ? `<div class="cm-trx-sub">${esc(a.notes)}</div>` : ''}
                </div>
              </div>
              <div class="cm-trx-amt ${a.balance < 0 ? 'cm-out' : ''}">${fmtRp(a.balance)}</div>
            </div>`).join('')}
        </div>`;
    }).join('');
    el.querySelectorAll('.cm-acc-row').forEach((r) => {
      r.addEventListener('click', () => openAccountDetail(Number(r.dataset.accid)));
    });
  }

  function renderAnalisis(s) {
    const book = activeBook(s);
    const lbl = ui.anaPeriod === 'all' ? 'SEMUA PERIODE' : ui.anaPeriod === 'year' ? `TAHUN ${ui.anaYear}` : `${MONTH_NAMES[ui.anaMonth].toUpperCase()} ${ui.anaYear}`;
    $q('#cm-ana-month-lbl').textContent = lbl;
    root.querySelectorAll('#cm-ana-periods .cm-seg-tab').forEach((b) => b.classList.toggle('active', b.dataset.period === ui.anaPeriod));
    root.querySelectorAll('#cm-ana-tabs .cm-seg-tab').forEach((b) => b.classList.toggle('active', b.dataset.atab === ui.anaTab));

    const filterByPeriod = (m, y, period) => s.transactions.filter((t) => {
      if (t.bookId !== book.id || t.transferPair != null) return false;
      if (period === 'all') return true;
      if (period === 'year') return String(t.date || '').startsWith(String(y));
      return String(t.date || '').startsWith(`${y}-${pad2(m + 1)}`);
    });

    const cur = filterByPeriod(ui.anaMonth, ui.anaYear, ui.anaPeriod);
    const inc = cur.filter((t) => t.direction === 'in').reduce((a, b) => a + b.amount, 0);
    const exp = cur.filter((t) => t.direction === 'out').reduce((a, b) => a + b.amount, 0);
    const net = inc - exp;
    $q('#cm-ana-in').textContent = fmtRp(inc);
    $q('#cm-ana-out').textContent = fmtRp(exp);
    const netEl = $q('#cm-ana-net');
    netEl.textContent = fmtRp(net);
    netEl.className = `cm-sum-val ${net >= 0 ? 'cm-in' : 'cm-out'}`;

    const body = $q('#cm-ana-body');
    if (ui.anaTab === 'expense' || ui.anaTab === 'income') {
      const dir = ui.anaTab === 'income' ? 'in' : 'out';
      const total = dir === 'in' ? inc : exp;
      const catMap = new Map();
      cur.filter((t) => t.direction === dir).forEach((t) => {
        catMap.set(t.categoryId, (catMap.get(t.categoryId) || 0) + t.amount);
      });
      const catById = new Map(s.categories.map((c) => [c.id, c]));
      const rows = Array.from(catMap.entries()).sort((a, b) => b[1] - a[1]);
      if (!rows.length) {
        body.innerHTML = `<div class="cm-empty">Belum ada data ${dir === 'in' ? 'pemasukan' : 'pengeluaran'} pada periode ini.</div>`;
        return;
      }
      const circ = 2 * Math.PI * 46;
      let offset = 0;
      const arcs = rows.map(([cid, amt], i) => {
        const pct = total > 0 ? (amt / total) : 0;
        const len = pct * circ;
        const col = DONUT_COLORS[i % DONUT_COLORS.length];
        const seg = `<circle cx="60" cy="60" r="46" fill="none" stroke="${col}" stroke-width="16" stroke-dasharray="${len} ${circ}" stroke-dashoffset="${-offset}" transform="rotate(-90 60 60)"/>`;
        offset += len;
        return seg;
      }).join('');
      const topPct = total > 0 ? Math.round((rows[0][1] / total) * 100) : 0;
      const legend = rows.map(([cid, amt], i) => {
        const c = catById.get(cid);
        const pct = total > 0 ? Math.round((amt / total) * 100) : 0;
        return `<div class="cm-legend-row"><span class="cm-dot" style="background:${DONUT_COLORS[i % DONUT_COLORS.length]}"></span><span style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${esc(c?.name || 'Lainnya')}</span><strong>${pct}%</strong></div>`;
      }).join('');
      const bars = rows.map(([cid, amt], i) => {
        const c = catById.get(cid);
        const pct = total > 0 ? Math.round((amt / total) * 100) : 0;
        const col = DONUT_COLORS[i % DONUT_COLORS.length];
        return `
          <div class="cm-cat-bar-card" data-cid="${cid}" data-dir="${dir}">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-size:13px;font-weight:700">${esc(c?.emoji || '📌')} ${esc(c?.name || 'Lainnya')}</span>
              <span style="font-size:13px;font-weight:800;color:${col}">${fmtRp(amt)} (${pct}%)</span>
            </div>
            <div class="cm-bar-track"><div class="cm-bar-fill" style="width:${pct}%;background:${col}"></div></div>
          </div>`;
      }).join('');
      body.innerHTML = `
        <div class="cm-donut-card">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span style="font-size:11.5px;font-weight:800;color:var(--cm-muted);text-transform:uppercase">Total ${dir === 'in' ? 'Pemasukan' : 'Pengeluaran'}</span>
            <strong class="${dir === 'in' ? 'cm-in' : 'cm-out'}">${fmtRp(total)}</strong>
          </div>
          <div class="cm-donut-wrap">
            <div class="cm-donut-svg-box">
              <svg viewBox="0 0 120 120"><circle cx="60" cy="60" r="46" fill="none" stroke="var(--cm-bg-card2)" stroke-width="16"/>${arcs}</svg>
              <div class="cm-donut-center"><strong style="font-size:18px">${topPct}%</strong><span style="font-size:10px;color:var(--cm-muted)">terbesar</span></div>
            </div>
            <div class="cm-donut-legend">${legend}</div>
          </div>
        </div>
        <div class="cm-sec-title">Per Kategori</div>${bars}`;
      body.querySelectorAll('.cm-cat-bar-card').forEach((el) => {
        el.addEventListener('click', () => openCatDrilldown(Number(el.dataset.cid), el.dataset.dir));
      });
    } else if (ui.anaTab === 'budget') {
      const mb = s.masterBudgets.find((b) => b.bookId === book.id && b.month === ui.anaMonth && b.year === ui.anaYear);
      if (!mb) {
        body.innerHTML = `
          <div class="cm-empty">
            <div style="font-size:32px;margin-bottom:8px">🎯</div>
            Belum ada budget untuk ${MONTH_NAMES[ui.anaMonth]} ${ui.anaYear}.
            <div style="margin-top:14px"><button type="button" class="cm-btn cm-btn-pri" id="cm-setup-budget-btn" style="padding:10px 18px">+ Atur Budget Bulanan</button></div>
          </div>`;
        $q('#cm-setup-budget-btn')?.addEventListener('click', () => openBudgetForm(null));
        return;
      }
      const spent = s.transactions
        .filter((t) => t.bookId === book.id && t.direction === 'out' && t.transferPair == null && t.date >= mb.startDate && t.date <= mb.endDate)
        .reduce((a, b) => a + b.amount, 0);
      const pct = mb.limit > 0 ? Math.min(100, Math.round((spent / mb.limit) * 100)) : 0;
      const rem = mb.limit - spent;
      const col = spent > mb.limit ? 'var(--cm-expense)' : pct >= 80 ? '#FFAB00' : 'var(--cm-primary)';
      body.innerHTML = `
        <div class="cm-donut-card">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
            <div>
              <div style="font-size:14px;font-weight:800">Budget ${MONTH_NAMES[mb.month]} ${mb.year}</div>
              <div style="font-size:11px;color:var(--cm-muted)">${esc(mb.startDate)} s/d ${esc(mb.endDate)}</div>
            </div>
            <div style="display:flex;gap:6px">
              <button type="button" class="cm-icon-btn" id="cm-edit-budget-btn">✎</button>
              <button type="button" class="cm-icon-btn" id="cm-del-budget-btn" style="color:var(--cm-expense)">✕</button>
            </div>
          </div>
          <div style="font-size:22px;font-weight:800;color:${col}">${fmtRp(spent)} <span style="font-size:13px;color:var(--cm-muted);font-weight:600">/ ${fmtRp(mb.limit)}</span></div>
          <div class="cm-bar-track" style="height:8px;margin:10px 0"><div class="cm-bar-fill" style="width:${pct}%;background:${col}"></div></div>
          <div style="font-size:12px;color:var(--cm-muted)">${rem >= 0 ? `Sisa budget: <strong class="cm-in">${fmtRp(rem)}</strong>` : `Melebihi budget: <strong class="cm-out">${fmtRp(-rem)}</strong>`}</div>
          ${mb.note ? `<div style="margin-top:8px;font-size:12px;color:var(--cm-muted)">Catatan: ${esc(mb.note)}</div>` : ''}
        </div>`;
      $q('#cm-edit-budget-btn')?.addEventListener('click', () => openBudgetForm(mb));
      $q('#cm-del-budget-btn')?.addEventListener('click', () => {
        const next = cm();
        next.masterBudgets = next.masterBudgets.filter((x) => x.id !== mb.id);
        saveCm(next);
        renderAnalisis(cm());
        showCmToast('Budget dihapus');
      });
    } else {
      // Tren 6 bulan
      const months = [];
      for (let i = 5; i >= 0; i--) {
        const d = new Date(ui.anaYear, ui.anaMonth - i, 1);
        const pref = `${d.getFullYear()}-${pad2(d.getMonth() + 1)}`;
        const mT = s.transactions.filter((t) => t.bookId === book.id && t.transferPair == null && String(t.date || '').startsWith(pref));
        months.push({
          label: MONTH_SHORT[d.getMonth()],
          in: mT.filter((t) => t.direction === 'in').reduce((a, b) => a + b.amount, 0),
          out: mT.filter((t) => t.direction === 'out').reduce((a, b) => a + b.amount, 0),
        });
      }
      const maxV = Math.max(1, ...months.flatMap((m) => [m.in, m.out]));
      const barsSvg = months.map((m, i) => {
        const x = 18 + i * 46;
        const hIn = Math.max(2, Math.round((m.in / maxV) * 80));
        const hOut = Math.max(2, Math.round((m.out / maxV) * 80));
        return `
          <rect x="${x}" y="${95 - hIn}" width="12" height="${hIn}" rx="3" fill="#00BFA5"/>
          <rect x="${x + 15}" y="${95 - hOut}" width="12" height="${hOut}" rx="3" fill="#FF5370"/>
          <text x="${x + 13}" y="112" text-anchor="middle" fill="#7A8099" font-size="9">${m.label}</text>`;
      }).join('');
      body.innerHTML = `
        <div class="cm-donut-card">
          <div style="font-size:11.5px;font-weight:800;color:var(--cm-muted);text-transform:uppercase;margin-bottom:10px">Arus Kas 6 Bulan</div>
          <svg viewBox="0 0 300 120" style="width:100%;height:auto;display:block">${barsSvg}</svg>
          <div style="display:flex;justify-content:center;gap:14px;margin-top:8px;font-size:11px;color:var(--cm-muted)">
            <span><span style="color:var(--cm-income)">■</span> Pemasukan</span>
            <span><span style="color:var(--cm-expense)">■</span> Pengeluaran</span>
          </div>
        </div>`;
    }
  }

  function renderLebih(s) {
    const items = [
      { id: 'books', icon: '📖', bg: 'rgba(0,191,165,0.14)', label: 'Buku', badge: s.books.length },
      { id: 'acc', icon: '💳', bg: 'rgba(124,143,245,0.14)', label: 'Akun', badge: s.accounts.filter((a) => !a.isSavings).length },
      { id: 'alltrx', icon: '🧾', bg: 'rgba(0,176,255,0.14)', label: 'Transaksi', badge: s.transactions.filter((t) => t.bookId === activeBook(s).id).length },
      { id: 'search', icon: '🔍', bg: 'rgba(255,83,112,0.14)', label: 'Cari' },
      { id: 'budget', icon: '💰', bg: 'rgba(100,221,23,0.14)', label: 'Budget' },
      { id: 'tabungan', icon: '🎯', bg: 'rgba(255,193,7,0.14)', label: 'Tabungan', badge: s.wishlists.filter((w) => w.status !== 'Selesai').length },
      { id: 'cat', icon: '🏷️', bg: 'rgba(124,77,255,0.14)', label: 'Kategori', badge: s.categories.length },
      { id: 'member', icon: '👥', bg: 'rgba(255,109,0,0.14)', label: 'Anggota', badge: s.members.length },
      { id: 'csv', icon: '📥', bg: 'rgba(0,191,165,0.12)', label: 'Export CSV' },
      { id: 'backup', icon: '💾', bg: 'rgba(99,179,237,0.14)', label: 'Backup' },
      { id: 'restore', icon: '📤', bg: 'rgba(246,173,85,0.14)', label: 'Restore' },
      { id: 'reset', icon: '🗑️', bg: 'rgba(255,83,112,0.14)', label: 'Reset Data' },
    ];
    const grid = $q('#cm-lebih-grid');
    grid.innerHTML = items.map((it) => `
      <div class="cm-lebih-item" data-act="${it.id}">
        ${it.badge != null ? `<span class="cm-lebih-badge">${it.badge}</span>` : ''}
        <div class="cm-lebih-ico" style="background:${it.bg};font-size:20px">${it.icon}</div>
        <div class="cm-lebih-lbl">${it.label}</div>
      </div>`).join('');
    grid.querySelectorAll('.cm-lebih-item').forEach((el) => {
      el.addEventListener('click', () => handleLebihAction(el.dataset.act));
    });
  }

  function handleLebihAction(act) {
    if (act === 'books') openBookSheet();
    else if (act === 'acc') openAccountForm(null);
    else if (act === 'alltrx') openAllTrxSheet();
    else if (act === 'search') openSearchModal();
    else if (act === 'budget') { ui.view = 'analisis'; ui.anaTab = 'budget'; renderAll(); }
    else if (act === 'tabungan') { ui.view = 'tabungan'; renderAll(); }
    else if (act === 'cat') openCategoryManager();
    else if (act === 'member') openMemberManager();
    else if (act === 'csv') exportCsv();
    else if (act === 'backup') exportBackup();
    else if (act === 'restore') $q('#cm-restore-input')?.click();
    else if (act === 'reset') {
      openConfirm('Reset CuciMoney+', 'Semua transaksi, dompet, budget, dan tabungan akan dikembalikan ke awal.', true, () => {
        saveCm(defaultState());
        renderAll();
        showCmToast('Data CuciMoney+ direset');
      });
    }
  }

  function renderTabungan(s) {
    const acc = savingsAcc(s);
    const totalSaved = s.wishlists.filter((w) => w.status !== 'Selesai').reduce((a, b) => a + (Number(b.saved) || 0), 0);
    const totalTarget = s.wishlists.filter((w) => w.status !== 'Selesai').reduce((a, b) => a + (Number(b.price) || 0), 0);
    const pct = totalTarget > 0 ? Math.min(100, Math.round((totalSaved / totalTarget) * 100)) : 0;
    $q('#cm-sv-hero').innerHTML = `
      <div class="cm-sv-hero">
        <div class="cm-hero-saldo-label">Saldo Tabungan Wishlist</div>
        <div class="cm-hero-saldo" style="margin-bottom:8px">${fmtRp(acc?.balance ?? totalSaved)}</div>
        <div style="font-size:11.5px;color:var(--cm-muted);margin-bottom:8px">Terkumpul ${fmtRp(totalSaved)} dari target ${fmtRp(totalTarget)} (${pct}%)</div>
        <div class="cm-bar-track"><div class="cm-bar-fill" style="width:${pct}%;background:var(--cm-primary)"></div></div>
      </div>`;

    const priRank = { 'Penting': 0, 'Nanti': 1, 'Kapan-kapan': 2 };
    const sorted = [...s.wishlists].sort((a, b) => {
      if ((a.status === 'Selesai') !== (b.status === 'Selesai')) return a.status === 'Selesai' ? 1 : -1;
      return (priRank[a.priority] ?? 1) - (priRank[b.priority] ?? 1);
    });
    const list = $q('#cm-sv-list');
    if (!sorted.length) {
      list.innerHTML = `<div class="cm-empty">Belum ada wishlist tabungan.<br>Ketuk + untuk membuat target tabunganmu!</div>`;
      return;
    }
    list.innerHTML = sorted.map((w) => {
      const ico = SV_ICONS.find((i) => i.key === w.image)?.emoji || '🛍️';
      const p = w.price > 0 ? Math.min(100, Math.round((w.saved / w.price) * 100)) : 0;
      const done = w.status === 'Selesai';
      return `
        <div class="cm-sv-card" data-wid="${w.id}">
          <div style="display:flex;justify-content:space-between;align-items:center;gap:10px">
            <div style="display:flex;align-items:center;gap:10px;min-width:0">
              <div class="cm-acc-ico">${ico}</div>
              <div style="min-width:0">
                <div class="cm-trx-title">${esc(w.title)}</div>
                <div class="cm-trx-sub">${fmtRp(w.saved)} / ${fmtRp(w.price)}</div>
              </div>
            </div>
            <span class="cm-sv-tag" style="background:${done ? 'var(--cm-income-dim)' : 'var(--cm-bg-card2)'};color:${done ? 'var(--cm-income)' : 'var(--cm-muted)'}">${done ? 'Selesai' : esc(w.priority)}</span>
          </div>
          <div class="cm-bar-track"><div class="cm-bar-fill" style="width:${p}%;background:${done ? 'var(--cm-income)' : '#FFC107'}"></div></div>
        </div>`;
    }).join('');
    list.querySelectorAll('.cm-sv-card').forEach((c) => {
      c.addEventListener('click', () => openSvDetail(c.dataset.wid));
    });
  }

  // ── Sheets & Modals Helpers ───────────────────────────────────────
  function openSheet(html) {
    $q('#cm-backdrop').classList.add('active');
    const sh = $q('#cm-sheet');
    sh.innerHTML = html;
    sh.classList.add('active');
  }
  function closeSheet() {
    $q('#cm-sheet').classList.remove('active');
    if (!$q('#cm-dialog').classList.contains('active')) $q('#cm-backdrop').classList.remove('active');
  }
  function openConfirm(title, msg, isDanger, onOk) {
    ui.confirmCb = onOk;
    $q('#cm-backdrop').classList.add('active');
    const d = $q('#cm-dialog');
    d.innerHTML = `
      <div style="padding:18px">
        <div style="font-size:15px;font-weight:800;margin-bottom:6px">${esc(title)}</div>
        <div style="font-size:12.5px;color:var(--cm-muted);line-height:1.45">${esc(msg)}</div>
      </div>
      <div class="cm-btn-row" style="padding:10px 14px">
        <button type="button" class="cm-btn cm-btn-sec" id="cm-cfm-no">Batal</button>
        <button type="button" class="cm-btn ${isDanger ? 'cm-btn-dan' : 'cm-btn-pri'}" id="cm-cfm-yes">Ya, Lanjutkan</button>
      </div>`;
    d.classList.add('active');
    $q('#cm-cfm-no').onclick = closeDialog;
    $q('#cm-cfm-yes').onclick = () => { const fn = ui.confirmCb; closeDialog(); fn?.(); };
  }
  function openPrompt(title, placeholder, initial, onOk) {
    ui.promptCb = onOk;
    $q('#cm-backdrop').classList.add('active');
    const d = $q('#cm-dialog');
    d.innerHTML = `
      <div style="padding:18px">
        <div style="font-size:15px;font-weight:800;margin-bottom:10px">${esc(title)}</div>
        <input type="text" class="cm-input" id="cm-pmp-inp" placeholder="${esc(placeholder)}" value="${esc(initial || '')}" />
      </div>
      <div class="cm-btn-row" style="padding:10px 14px">
        <button type="button" class="cm-btn cm-btn-sec" id="cm-pmp-no">Batal</button>
        <button type="button" class="cm-btn cm-btn-pri" id="cm-pmp-yes">Simpan</button>
      </div>`;
    d.classList.add('active');
    $q('#cm-pmp-no').onclick = closeDialog;
    $q('#cm-pmp-yes').onclick = () => { const val = $q('#cm-pmp-inp').value.trim(); closeDialog(); if (val) onOk?.(val); };
    setTimeout(() => $q('#cm-pmp-inp')?.focus(), 50);
  }
  function closeDialog() {
    $q('#cm-dialog').classList.remove('active');
    if (!$q('#cm-sheet').classList.contains('active')) $q('#cm-backdrop').classList.remove('active');
  }

  // ── Transaction Add/Edit Calculator Modal ─────────────────────────
  function openTrxForm(editTrx = null) {
    const s = cm();
    const book = activeBook(s);
    const accs = bookAccounts(s, book.id);
    if (editTrx) {
      if (editTrx.savingsPot) { showCmToast('Transaksi tabungan dikelola di menu Tabungan', 'err'); return; }
      const isTrf = editTrx.transferPair != null;
      const pair = isTrf ? s.transactions.filter((x) => x.transferPair === editTrx.transferPair) : [];
      const outT = pair.find((x) => x.direction === 'out') || editTrx;
      const inT = pair.find((x) => x.direction === 'in');
      ui.trxModal = {
        editId: outT.id,
        type: isTrf ? 'transfer' : (outT.direction === 'in' ? 'in' : 'out'),
        amountExpr: String(outT.amount),
        catId: outT.categoryId,
        accId: outT.accountId,
        toAccId: inT?.accountId || accs.find((a) => a.id !== outT.accountId)?.id || accs[0]?.id,
        date: outT.date || todayStr(),
        memberId: outT.memberId || s.members[0]?.id || 'm1',
        note: outT.title || '',
      };
    } else {
      const defAcc = s.prefs.lastAccByBook?.[book.id] || accs[0]?.id || 1;
      const defTo = accs.find((a) => a.id !== defAcc)?.id || defAcc;
      ui.trxModal = {
        editId: null, type: 'out', amountExpr: '0',
        catId: s.categories.find((c) => c.type === 'expense')?.id || 1,
        accId: defAcc, toAccId: defTo, date: todayStr(),
        memberId: s.members[0]?.id || 'm1', note: '',
      };
    }
    renderTrxForm();
  }

  function renderTrxForm() {
    const s = cm();
    const m = ui.trxModal;
    const accs = s.accounts.filter((a) => !a.isSavings);
    const fromAcc = accs.find((a) => a.id === m.accId) || accs[0];
    const toAcc = accs.find((a) => a.id === m.toAccId) || accs[0];
    const mem = s.members.find((x) => x.id === m.memberId) || s.members[0];
    const cats = s.categories.filter((c) => m.type === 'in' ? (c.type === 'income' || c.type === 'both') : (c.type === 'expense' || c.type === 'both'));
    const amtNum = evalKbAmount(m.amountExpr);
    const clrCls = m.type === 'in' ? 'cm-in' : m.type === 'transfer' ? 'cm-trf' : 'cm-out';

    const fs = $q('#cm-modal-fs');
    fs.innerHTML = `
      <div class="cm-trx-tabs">
        <button type="button" class="cm-kb-btn" id="cm-trx-close" style="width:44px;height:46px;font-size:18px">✕</button>
        <div class="cm-trx-tab ${m.type === 'out' ? 'active-out' : ''}" data-tt="out">Pengeluaran</div>
        <div class="cm-trx-tab ${m.type === 'in' ? 'active-in' : ''}" data-tt="in">Pemasukan</div>
        <div class="cm-trx-tab ${m.type === 'transfer' ? 'active-transfer' : ''}" data-tt="transfer">Transfer</div>
      </div>
      <div style="flex:1;overflow-y:auto;min-height:0">
        ${m.type === 'transfer' ? `
          <div style="display:flex;gap:8px;align-items:center;padding:12px 14px 4px">
            <button type="button" class="cm-btn cm-btn-sec" id="cm-trf-from">${esc(fromAcc?.icon || '💵')} ${esc(fromAcc?.name || 'Dari')}</button>
            <span style="color:var(--cm-muted)">→</span>
            <button type="button" class="cm-btn cm-btn-sec" id="cm-trf-to">${esc(toAcc?.icon || '🏦')} ${esc(toAcc?.name || 'Ke')}</button>
          </div>` : `
          <div class="cm-sec-title" style="padding-bottom:2px">Kategori</div>
          <div class="cm-cat-grid">
            ${cats.map((c) => `
              <div class="cm-cat-cell ${m.catId === c.id ? (m.type === 'in' ? 'sel-in' : 'sel-out') : ''}" data-cid="${c.id}">
                <span style="font-size:20px">${esc(c.emoji)}</span>
                <span class="cm-cat-cell-name">${esc(c.name)}</span>
              </div>`).join('')}
          </div>`}
      </div>
      <div class="cm-kb">
        <div class="cm-kb-row1">
          ${m.type !== 'transfer' ? `<button type="button" class="cm-kb-btn" id="cm-kb-acc" style="max-width:110px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(fromAcc?.icon || '💵')} ${esc(fromAcc?.name || 'Akun')}</button>` : ''}
          <div style="flex:1;padding:0 8px"><input type="text" class="cm-input" id="cm-kb-note" placeholder="Catatan..." value="${esc(m.note)}" style="padding:6px 9px;font-size:12.5px" /></div>
          <div style="padding:0 10px;font-size:15px;font-weight:800" class="${clrCls}">${amtNum ? fmtRp(amtNum, true) : m.amountExpr}</div>
        </div>
        <div class="cm-kb-row2">
          <button type="button" class="cm-kb-btn" id="cm-kb-member">${esc(mem?.emoji || '😄')} ${esc(mem?.name?.split(' ')[0] || 'Saya')}</button>
          <button type="button" class="cm-kb-btn" id="cm-kb-date" style="color:var(--cm-primary)">📅 ${esc(m.date.slice(5))}</button>
          <button type="button" class="cm-kb-btn" id="cm-kb-save-next" style="color:var(--cm-income);font-size:18px;font-weight:800">+</button>
          <button type="button" class="cm-kb-btn done" id="cm-kb-save-done">✓</button>
        </div>
        <div class="cm-kb-grid">
          ${['7','8','9','÷','4','5','6','×','1','2','3','−','.','0','⌫','+'].map((k) => {
            const cls = ['÷','×','−','+'].includes(k) ? 'op' : k === '⌫' ? 'bs' : '';
            return `<button type="button" class="cm-kb-key ${cls}" data-k="${k}">${k}</button>`;
          }).join('')}
        </div>
      </div>`;
    fs.classList.add('active');

    $q('#cm-trx-close').onclick = () => fs.classList.remove('active');
    fs.querySelectorAll('.cm-trx-tab').forEach((t) => {
      t.onclick = () => {
        m.type = t.dataset.tt;
        if (m.type !== 'transfer') {
          const valid = s.categories.filter((c) => m.type === 'in' ? c.type === 'income' : c.type === 'expense');
          if (!valid.some((c) => c.id === m.catId)) m.catId = valid[0]?.id || 1;
        }
        renderTrxForm();
      };
    });
    fs.querySelectorAll('.cm-cat-cell').forEach((c) => {
      c.onclick = () => { m.catId = Number(c.dataset.cid); renderTrxForm(); };
    });
    $q('#cm-kb-note').oninput = (e) => { m.note = e.target.value; };
    $q('#cm-kb-acc')?.addEventListener('click', () => pickAccount('Dari Akun', (id) => { m.accId = id; renderTrxForm(); }));
    $q('#cm-trf-from')?.addEventListener('click', () => pickAccount('Akun Sumber', (id) => { m.accId = id; renderTrxForm(); }));
    $q('#cm-trf-to')?.addEventListener('click', () => pickAccount('Akun Tujuan', (id) => { m.toAccId = id; renderTrxForm(); }));
    $q('#cm-kb-member').onclick = () => {
      openSheet(`<div class="cm-sheet-handle"></div><div class="cm-sheet-title">Pilih Anggota</div><div class="cm-sheet-body">${s.members.map((mb) => `<div class="cm-acc-row" data-mid="${mb.id}"><span>${esc(mb.emoji)} ${esc(mb.name)}</span></div>`).join('')}</div>`);
      $q('#cm-sheet').querySelectorAll('.cm-acc-row').forEach((r) => { r.onclick = () => { m.memberId = r.dataset.mid; closeSheet(); renderTrxForm(); }; });
    };
    $q('#cm-kb-date').onclick = () => {
      $q('#cm-backdrop').classList.add('active');
      const d = $q('#cm-dialog');
      d.innerHTML = `<div style="padding:18px"><div style="font-size:15px;font-weight:800;margin-bottom:10px">Pilih Tanggal</div><input type="date" class="cm-input" id="cm-date-inp" value="${esc(m.date)}" /></div><div class="cm-btn-row"><button type="button" class="cm-btn cm-btn-sec" id="cm-dt-no">Batal</button><button type="button" class="cm-btn cm-btn-pri" id="cm-dt-ok">Pilih</button></div>`;
      d.classList.add('active');
      $q('#cm-dt-no').onclick = closeDialog;
      $q('#cm-dt-ok').onclick = () => { m.date = $q('#cm-date-inp').value || todayStr(); closeDialog(); renderTrxForm(); };
    };
    fs.querySelectorAll('.cm-kb-key').forEach((btn) => {
      btn.onclick = () => {
        const k = btn.dataset.k;
        if (k === '⌫') m.amountExpr = m.amountExpr.length > 1 ? m.amountExpr.slice(0, -1) : '0';
        else if (['÷','×','−','+'].includes(k)) {
          if (/[÷×−+]$/.test(m.amountExpr)) m.amountExpr = m.amountExpr.slice(0, -1) + k;
          else m.amountExpr += k;
        } else {
          m.amountExpr = m.amountExpr === '0' && k !== '.' ? k : m.amountExpr + k;
        }
        renderTrxForm();
      };
    });
    const commit = (closeAfter) => {
      const amt = evalKbAmount(m.amountExpr);
      if (amt <= 0) { showCmToast('Nominal harus lebih dari 0', 'err'); return; }
      const next = cm();
      const book = activeBook(next);
      if (m.editId) removeTrxById(next, m.editId);
      if (m.type === 'transfer') {
        if (!m.toAccId || m.toAccId === m.accId) { showCmToast('Pilih akun tujuan yang berbeda', 'err'); return; }
        const pair = genId();
        const title = m.note.trim() || 'Transfer Saldo';
        next.transactions.unshift(
          { id: pair, bookId: book.id, title, amount: amt, direction: 'out', categoryId: 12, accountId: m.accId, date: m.date, memberId: m.memberId, transferPair: pair, createdAt: Date.now() },
          { id: pair + 1, bookId: book.id, title, amount: amt, direction: 'in', categoryId: 12, accountId: m.toAccId, date: m.date, memberId: m.memberId, transferPair: pair, createdAt: Date.now() + 1 }
        );
        adjBal(next, m.accId, -amt);
        adjBal(next, m.toAccId, amt);
      } else {
        const dir = m.type === 'in' ? 'in' : 'out';
        const cat = next.categories.find((c) => c.id === m.catId);
        const title = m.note.trim() || cat?.name || (dir === 'in' ? 'Pemasukan' : 'Pengeluaran');
        next.transactions.unshift({ id: genId(), bookId: book.id, title, amount: amt, direction: dir, categoryId: m.catId, accountId: m.accId, date: m.date, memberId: m.memberId, transferPair: null, createdAt: Date.now() });
        adjBal(next, m.accId, dir === 'in' ? amt : -amt);
      }
      next.prefs.lastAccByBook[book.id] = m.accId;
      saveCm(next);
      showCmToast('Transaksi tersimpan');
      if (closeAfter) fs.classList.remove('active');
      else { m.amountExpr = '0'; m.note = ''; m.editId = null; renderTrxForm(); }
      renderAll();
    };
    $q('#cm-kb-save-done').onclick = () => commit(true);
    $q('#cm-kb-save-next').onclick = () => commit(false);
  }

  function pickAccount(title, cb) {
    const s = cm();
    const accs = s.accounts.filter((a) => !a.isSavings);
    openSheet(`<div class="cm-sheet-handle"></div><div class="cm-sheet-title">${esc(title)}</div><div class="cm-sheet-body">${accs.map((a) => `<div class="cm-acc-row" data-aid="${a.id}"><span>${esc(a.icon || '💵')} ${esc(a.name)}</span><span style="font-size:12px;color:var(--cm-muted)">${fmtRp(a.balance)}</span></div>`).join('')}</div>`);
    $q('#cm-sheet').querySelectorAll('.cm-acc-row').forEach((r) => { r.onclick = () => { closeSheet(); cb(Number(r.dataset.aid)); }; });
  }

  function adjBal(s, accId, delta) {
    const a = s.accounts.find((x) => x.id === accId);
    if (a) a.balance = (Number(a.balance) || 0) + delta;
  }

  function removeTrxById(s, trxId) {
    const t = s.transactions.find((x) => x.id === trxId);
    if (!t) return;
    if (t.transferPair != null) {
      const pairs = s.transactions.filter((x) => x.transferPair === t.transferPair);
      pairs.forEach((p) => adjBal(s, p.accountId, p.direction === 'in' ? -p.amount : p.amount));
      s.transactions = s.transactions.filter((x) => x.transferPair !== t.transferPair);
    } else {
      adjBal(s, t.accountId, t.direction === 'in' ? -t.amount : t.amount);
      s.transactions = s.transactions.filter((x) => x.id !== t.id);
    }
  }

  function openTrxDetail(trxId) {
    const s = cm();
    const t = s.transactions.find((x) => x.id === trxId);
    if (!t) return;
    const cat = s.categories.find((c) => c.id === t.categoryId);
    const acc = s.accounts.find((a) => a.id === t.accountId);
    const mem = s.members.find((m) => m.id === t.memberId);
    const cls = t.transferPair != null ? 'cm-trf' : t.direction === 'in' ? 'cm-in' : 'cm-out';
    $q('#cm-backdrop').classList.add('active');
    const d = $q('#cm-dialog');
    d.innerHTML = `
      <div style="display:flex;justify-content:space-between;padding:12px 14px 0">
        <button type="button" class="cm-icon-btn" id="cm-det-x">✕</button>
        <div style="display:flex;gap:8px">
          ${t.savingsPot ? '' : `<button type="button" class="cm-icon-btn" id="cm-det-del" style="color:var(--cm-expense)">🗑</button><button type="button" class="cm-icon-btn" id="cm-det-edit" style="color:var(--cm-primary)">✎</button>`}
        </div>
      </div>
      <div style="text-align:center;padding:12px 18px 18px">
        <div style="font-size:34px;margin-bottom:6px">${t.transferPair != null ? '⇄' : esc(cat?.emoji || '📌')}</div>
        <div class="${cls}" style="font-size:24px;font-weight:800;margin-bottom:4px">${fmtRp(t.amount)}</div>
        <div style="font-size:14px;font-weight:700;margin-bottom:12px">${esc(t.title)}</div>
        <div style="background:var(--cm-bg-card2);border-radius:12px;padding:10px 12px;font-size:12px;text-align:left;display:flex;flex-direction:column;gap:6px">
          <div style="display:flex;justify-content:space-between"><span style="color:var(--cm-muted)">Kategori</span><strong>${esc(cat?.name || 'Transfer')}</strong></div>
          <div style="display:flex;justify-content:space-between"><span style="color:var(--cm-muted)">Akun</span><strong>${esc(acc?.name || '-')}</strong></div>
          <div style="display:flex;justify-content:space-between"><span style="color:var(--cm-muted)">Tanggal</span><strong>${esc(t.date)}</strong></div>
          <div style="display:flex;justify-content:space-between"><span style="color:var(--cm-muted)">Anggota</span><strong>${esc(mem?.name || 'Diri Sendiri')}</strong></div>
        </div>
      </div>`;
    d.classList.add('active');
    $q('#cm-det-x').onclick = closeDialog;
    $q('#cm-det-del')?.addEventListener('click', () => {
      openConfirm('Hapus Transaksi', `Hapus "${t.title}"? Saldo akun akan disesuaikan otomatis.`, true, () => {
        const next = cm();
        removeTrxById(next, t.id);
        saveCm(next);
        renderAll();
        showCmToast('Transaksi dihapus');
      });
    });
    $q('#cm-det-edit')?.addEventListener('click', () => { closeDialog(); openTrxForm(t); });
  }

  // ── Account Detail & Form ─────────────────────────────────────────
  function openAccountDetail(accId) {
    ui.accDetailId = accId;
    const s = cm();
    const acc = s.accounts.find((a) => a.id === accId);
    if (!acc) return;
    const trxs = s.transactions.filter((t) => t.accountId === accId).sort((a, b) => (b.date || '').localeCompare(a.date || '')).slice(0, 20);
    const fs = $q('#cm-modal-fs');
    fs.innerHTML = `
      <div class="cm-header">
        <button type="button" class="cm-icon-btn" id="cm-acd-back">←</button>
        <div class="cm-title">Detail Akun</div>
        ${acc.isSavings ? '<div style="width:36px"></div>' : '<button type="button" class="cm-icon-btn" id="cm-acd-edit">✎</button>'}
      </div>
      <div style="flex:1;overflow-y:auto;padding-bottom:20px">
        <div class="cm-wallet-hero">
          <div style="font-size:32px;margin-bottom:6px">${esc(acc.icon || '💵')}</div>
          <div style="font-size:16px;font-weight:800">${esc(acc.name)}</div>
          <div style="font-size:11.5px;color:var(--cm-muted);margin:3px 0 10px">${esc(acc.notes || 'Akun keuangan')}</div>
          <div class="cm-hero-saldo" style="margin-bottom:0">${fmtRp(acc.balance)}</div>
        </div>
        <div class="cm-sec-title">Mutasi Terakhir (${trxs.length})</div>
        ${trxs.length ? `<div class="cm-trx-group">${trxs.map((t) => `<div class="cm-trx-item"><div><div class="cm-trx-title">${esc(t.title)}</div><div class="cm-trx-sub">${esc(t.date)}</div></div><div class="cm-trx-amt ${t.direction === 'in' ? 'cm-in' : 'cm-out'}">${t.direction === 'in' ? '+ ' : '− '}${fmtRp(t.amount)}</div></div>`).join('')}</div>` : '<div class="cm-empty">Belum ada mutasi di akun ini</div>'}
      </div>`;
    fs.classList.add('active');
    $q('#cm-acd-back').onclick = () => fs.classList.remove('active');
    $q('#cm-acd-edit')?.addEventListener('click', () => openAccountForm(acc));
  }

  function openAccountForm(acc = null) {
    ui.accEditId = acc?.id || null;
    ui.accSelectedType = acc?.type || 'cash';
    ui.accSelectedEmoji = acc?.icon || '💵';
    const fs = $q('#cm-modal-sub');
    fs.innerHTML = `
      <div class="cm-header">
        <button type="button" class="cm-icon-btn" id="cm-acf-x">✕</button>
        <div class="cm-title">${acc ? 'Edit Akun' : 'Tambah Akun'}</div>
        ${acc ? '<button type="button" class="cm-icon-btn" id="cm-acf-del" style="color:var(--cm-expense)">🗑</button>' : '<div style="width:36px"></div>'}
      </div>
      <div style="flex:1;overflow-y:auto;padding:14px">
        <div class="cm-form-group"><label>Nama Akun</label><input class="cm-input" id="cm-acf-name" value="${esc(acc?.name || '')}" placeholder="Contoh: BCA / GoPay" /></div>
        <div class="cm-form-group"><label>Tipe</label>
          <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:6px" id="cm-acf-types">
            ${ACCOUNT_TYPES.map((t) => `<button type="button" class="cm-btn ${ui.accSelectedType === t.id ? 'cm-btn-pri' : 'cm-btn-sec'}" data-tid="${t.id}" style="padding:8px 4px;font-size:11.5px">${t.icon} ${t.label}</button>`).join('')}
          </div>
        </div>
        <div class="cm-form-group"><label>Ikon</label>
          <div class="cm-emoji-grid" id="cm-acf-emojis">${EMOJIS.slice(0, 18).map((em) => `<div class="cm-emoji-cell ${ui.accSelectedEmoji === em ? 'sel' : ''}" data-em="${em}">${em}</div>`).join('')}</div>
        </div>
        <div class="cm-form-group"><label>Saldo Saat Ini (Rp)</label><input type="number" class="cm-input" id="cm-acf-bal" value="${acc?.balance ?? 0}" /></div>
        <div class="cm-form-group"><label>Catatan</label><input class="cm-input" id="cm-acf-note" value="${esc(acc?.notes || '')}" placeholder="Opsional..." /></div>
        <label style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;font-size:13px"><span>Tampilkan di Buku aktif</span><input type="checkbox" id="cm-acf-book" ${acc ? (acc.useInBook ? 'checked' : '') : 'checked'} /></label>
        <label style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;font-size:13px"><span>Hitung ke Total Aset</span><input type="checkbox" id="cm-acf-asset" ${acc ? (acc.includeAsset ? 'checked' : '') : 'checked'} /></label>
      </div>
      <div class="cm-btn-row"><button type="button" class="cm-btn cm-btn-sec" id="cm-acf-cancel">Batal</button><button type="button" class="cm-btn cm-btn-pri" id="cm-acf-save">Simpan</button></div>`;
    fs.classList.add('active');
    $q('#cm-acf-x').onclick = $q('#cm-acf-cancel').onclick = () => fs.classList.remove('active');
    fs.querySelectorAll('#cm-acf-types button').forEach((b) => {
      b.onclick = () => {
        ui.accSelectedType = b.dataset.tid;
        fs.querySelectorAll('#cm-acf-types button').forEach((x) => { x.className = `cm-btn ${x.dataset.tid === ui.accSelectedType ? 'cm-btn-pri' : 'cm-btn-sec'}`; });
      };
    });
    fs.querySelectorAll('#cm-acf-emojis .cm-emoji-cell').forEach((c) => {
      c.onclick = () => {
        ui.accSelectedEmoji = c.dataset.em;
        fs.querySelectorAll('#cm-acf-emojis .cm-emoji-cell').forEach((x) => x.classList.toggle('sel', x.dataset.em === ui.accSelectedEmoji));
      };
    });
    $q('#cm-acf-del')?.addEventListener('click', () => {
      openConfirm('Hapus Akun', `Hapus akun "${acc.name}"?`, true, () => {
        const next = cm();
        next.accounts = next.accounts.filter((a) => a.id !== acc.id);
        next.books.forEach((b) => { b.accountIds = (b.accountIds || []).filter((id) => id !== acc.id); });
        saveCm(next);
        fs.classList.remove('active');
        $q('#cm-modal-fs').classList.remove('active');
        renderAll();
      });
    });
    $q('#cm-acf-save').onclick = () => {
      const name = $q('#cm-acf-name').value.trim();
      if (!name) { showCmToast('Nama akun wajib diisi', 'err'); return; }
      const next = cm();
      const obj = {
        id: acc?.id || genId(),
        name,
        type: ui.accSelectedType,
        icon: ui.accSelectedEmoji,
        balance: Math.round(Number($q('#cm-acf-bal').value) || 0),
        notes: $q('#cm-acf-note').value.trim(),
        useInBook: $q('#cm-acf-book').checked,
        includeAsset: $q('#cm-acf-asset').checked,
        isSavings: false,
      };
      if (acc) next.accounts = next.accounts.map((a) => a.id === acc.id ? obj : a);
      else {
        next.accounts.push(obj);
        const ab = activeBook(next);
        if (obj.useInBook && !ab.accountIds.includes(obj.id)) ab.accountIds.push(obj.id);
      }
      saveCm(next);
      fs.classList.remove('active');
      if (ui.accDetailId === obj.id) openAccountDetail(obj.id);
      renderAll();
      showCmToast('Akun disimpan');
    };
  }

  // ── Budget Setup & Category Drilldown ─────────────────────────────
  function openBudgetForm(existing = null) {
    const y = ui.anaYear, m = ui.anaMonth;
    const defStart = `${y}-${pad2(m + 1)}-01`;
    const lastD = new Date(y, m + 1, 0).getDate();
    const defEnd = `${y}-${pad2(m + 1)}-${pad2(lastD)}`;
    const fs = $q('#cm-modal-fs');
    fs.innerHTML = `
      <div class="cm-header">
        <button type="button" class="cm-icon-btn" id="cm-bf-x">✕</button>
        <div class="cm-title">Atur Budget ${MONTH_NAMES[m]} ${y}</div>
        <div style="width:36px"></div>
      </div>
      <div style="flex:1;overflow-y:auto;padding:16px">
        <div class="cm-form-group"><label>Total Limit (Rp)</label><input type="number" class="cm-input" id="cm-bf-lim" value="${existing?.limit || 3000000}" /></div>
        <div class="cm-form-group"><label>Mulai</label><input type="date" class="cm-input" id="cm-bf-s" value="${existing?.startDate || defStart}" /></div>
        <div class="cm-form-group"><label>Selesai</label><input type="date" class="cm-input" id="cm-bf-e" value="${existing?.endDate || defEnd}" /></div>
        <div class="cm-form-group"><label>Catatan</label><input class="cm-input" id="cm-bf-n" value="${esc(existing?.note || '')}" placeholder="Opsional..." /></div>
      </div>
      <div class="cm-btn-row"><button type="button" class="cm-btn cm-btn-sec" id="cm-bf-no">Batal</button><button type="button" class="cm-btn cm-btn-pri" id="cm-bf-ok">Simpan</button></div>`;
    fs.classList.add('active');
    $q('#cm-bf-x').onclick = $q('#cm-bf-no').onclick = () => fs.classList.remove('active');
    $q('#cm-bf-ok').onclick = () => {
      const limit = Math.round(Number($q('#cm-bf-lim').value) || 0);
      if (limit <= 0) { showCmToast('Limit harus lebih dari 0', 'err'); return; }
      const next = cm();
      const book = activeBook(next);
      next.masterBudgets = next.masterBudgets.filter((b) => !(b.bookId === book.id && b.month === m && b.year === y));
      next.masterBudgets.push({ id: existing?.id || genId(), bookId: book.id, month: m, year: y, limit, startDate: $q('#cm-bf-s').value || defStart, endDate: $q('#cm-bf-e').value || defEnd, note: $q('#cm-bf-n').value.trim() });
      saveCm(next);
      fs.classList.remove('active');
      renderAll();
      showCmToast('Budget disimpan');
    };
  }

  function openCatDrilldown(catId, dir) {
    const s = cm();
    const book = activeBook(s);
    const cat = s.categories.find((c) => c.id === catId);
    const trxs = s.transactions.filter((t) => t.bookId === book.id && t.categoryId === catId && t.direction === dir && t.transferPair == null && (ui.anaPeriod === 'all' ? true : ui.anaPeriod === 'year' ? t.date.startsWith(String(ui.anaYear)) : t.date.startsWith(`${ui.anaYear}-${pad2(ui.anaMonth + 1)}`)));
    const total = trxs.reduce((a, b) => a + b.amount, 0);
    const fs = $q('#cm-modal-fs');
    fs.innerHTML = `
      <div class="cm-header" style="background:var(--cm-primary);color:#061412">
        <button type="button" class="cm-icon-btn" id="cm-cd-x" style="background:rgba(0,0,0,0.18);color:#fff;border:none">←</button>
        <div class="cm-title" style="color:#fff">${esc(cat?.emoji || '📌')} ${esc(cat?.name || 'Kategori')}</div>
        <div style="width:36px"></div>
      </div>
      <div style="background:var(--cm-primary);padding:6px 16px 16px;color:#fff">
        <div style="font-size:11px;opacity:0.8">TOTAL</div>
        <div style="font-size:24px;font-weight:800">${fmtRp(total)}</div>
      </div>
      <div style="flex:1;overflow-y:auto;padding:12px 0">
        <div class="cm-trx-group">${trxs.map((t) => `<div class="cm-trx-item" data-id="${t.id}"><div><div class="cm-trx-title">${esc(t.title)}</div><div class="cm-trx-sub">${esc(t.date)}</div></div><div class="cm-trx-amt ${dir === 'in' ? 'cm-in' : 'cm-out'}">${fmtRp(t.amount)}</div></div>`).join('')}</div>
      </div>`;
    fs.classList.add('active');
    $q('#cm-cd-x').onclick = () => fs.classList.remove('active');
  }

  // ── Books, Search, AllTrx, Categories, Members ────────────────────
  function openBookSheet() {
    const s = cm();
    const cur = activeBook(s);
    openSheet(`
      <div class="cm-sheet-handle"></div>
      <div class="cm-sheet-title">Pilih Buku</div>
      <div class="cm-sheet-body">
        ${s.books.map((b) => `
          <div class="cm-acc-row" data-bid="${b.id}">
            <div><div class="cm-trx-title">${esc(b.name)} ${b.id === cur.id ? '✓' : ''}</div><div class="cm-trx-sub">${s.transactions.filter((t) => t.bookId === b.id).length} transaksi</div></div>
            ${s.books.length > 1 && b.id !== cur.id ? `<button type="button" class="cm-icon-btn" data-bdel="${b.id}" style="color:var(--cm-expense)">🗑</button>` : ''}
          </div>`).join('')}
        <button type="button" class="cm-btn cm-btn-pri" id="cm-bk-new" style="width:100%;margin-top:10px">+ Buku Baru</button>
      </div>`);
    const sh = $q('#cm-sheet');
    sh.querySelectorAll('[data-bid]').forEach((r) => {
      r.onclick = (e) => {
        if (e.target.closest('[data-bdel]')) return;
        const next = cm(); next.prefs.currentBookId = Number(r.dataset.bid); saveCm(next); closeSheet(); renderAll();
      };
    });
    sh.querySelectorAll('[data-bdel]').forEach((b) => {
      b.onclick = () => {
        const bid = Number(b.dataset.bdel);
        openConfirm('Hapus Buku', 'Hapus buku ini beserta transaksinya?', true, () => {
          const next = cm();
          next.transactions = next.transactions.filter((t) => t.bookId !== bid);
          next.masterBudgets = next.masterBudgets.filter((x) => x.bookId !== bid);
          next.books = next.books.filter((x) => x.id !== bid);
          saveCm(next);
          openBookSheet();
          renderAll();
        });
      };
    });
    $q('#cm-bk-new').onclick = () => {
      closeSheet();
      openPrompt('Buat Buku Baru', 'Nama buku (mis. Bisnis)', '', (name) => {
        const next = cm();
        const nb = { id: genId(), name, accountIds: next.accounts.filter((a) => !a.isSavings).map((a) => a.id) };
        next.books.push(nb);
        next.prefs.currentBookId = nb.id;
        saveCm(next);
        renderAll();
        showCmToast(`Buku "${name}" dibuat`);
      });
    };
  }

  function openSearchModal() {
    const fs = $q('#cm-modal-fs');
    fs.innerHTML = `
      <div class="cm-header">
        <button type="button" class="cm-icon-btn" id="cm-sr-x">←</button>
        <input class="cm-input" id="cm-sr-inp" placeholder="Cari transaksi atau nominal..." style="flex:1;padding:8px 12px" />
      </div>
      <div style="flex:1;overflow-y:auto;padding-top:10px" id="cm-sr-res"><div class="cm-empty">Ketik kata kunci pencarian</div></div>`;
    fs.classList.add('active');
    $q('#cm-sr-x').onclick = () => fs.classList.remove('active');
    const inp = $q('#cm-sr-inp');
    inp.oninput = () => {
      const q = inp.value.trim().toLowerCase();
      const s = cm();
      const book = activeBook(s);
      const hits = q ? s.transactions.filter((t) => t.bookId === book.id && (String(t.title).toLowerCase().includes(q) || String(t.amount).includes(q))).slice(0, 40) : [];
      $q('#cm-sr-res').innerHTML = hits.length
        ? `<div class="cm-trx-group">${hits.map((t) => `<div class="cm-trx-item" data-id="${t.id}"><div><div class="cm-trx-title">${esc(t.title)}</div><div class="cm-trx-sub">${esc(t.date)}</div></div><div class="cm-trx-amt ${t.direction === 'in' ? 'cm-in' : 'cm-out'}">${fmtRp(t.amount)}</div></div>`).join('')}</div>`
        : '<div class="cm-empty">Tidak ditemukan transaksi yang cocok</div>';
      $q('#cm-sr-res').querySelectorAll('.cm-trx-item').forEach((r) => { r.onclick = () => openTrxDetail(Number(r.dataset.id)); });
    };
    setTimeout(() => inp.focus(), 50);
  }

  function openAllTrxSheet() {
    const s = cm();
    const book = activeBook(s);
    const trxs = s.transactions.filter((t) => t.bookId === book.id).sort((a, b) => (b.date || '').localeCompare(a.date || '')).slice(0, 60);
    openSheet(`
      <div class="cm-sheet-handle"></div>
      <div class="cm-sheet-title">Semua Transaksi (${trxs.length})</div>
      <div class="cm-sheet-body">${trxs.length ? trxs.map((t) => `<div class="cm-acc-row" data-tid="${t.id}"><div><div class="cm-trx-title">${esc(t.title)}</div><div class="cm-trx-sub">${esc(t.date)}</div></div><div class="cm-trx-amt ${t.direction === 'in' ? 'cm-in' : 'cm-out'}">${fmtRp(t.amount)}</div></div>`).join('') : '<div class="cm-empty">Belum ada transaksi</div>'}</div>`);
    $q('#cm-sheet').querySelectorAll('[data-tid]').forEach((r) => { r.onclick = () => { closeSheet(); openTrxDetail(Number(r.dataset.tid)); }; });
  }

  function openCategoryManager() {
    const s = cm();
    const list = s.categories.filter((c) => c.type === ui.catMgrType || c.type === 'both');
    const fs = $q('#cm-modal-fs');
    fs.innerHTML = `
      <div class="cm-header">
        <button type="button" class="cm-icon-btn" id="cm-cm-x">←</button>
        <div class="cm-title">Kelola Kategori</div>
        <button type="button" class="cm-icon-btn" id="cm-cm-add">+</button>
      </div>
      <div style="padding:10px 14px"><div class="cm-seg-tabs"><button type="button" class="cm-seg-tab ${ui.catMgrType === 'expense' ? 'active' : ''}" data-ct="expense">Pengeluaran</button><button type="button" class="cm-seg-tab ${ui.catMgrType === 'income' ? 'active' : ''}" data-ct="income">Pemasukan</button></div></div>
      <div style="flex:1;overflow-y:auto">
        ${list.map((c) => `<div class="cm-acc-row"><span>${esc(c.emoji)} ${esc(c.name)}</span><div style="display:flex;gap:6px"><button type="button" class="cm-icon-btn" data-cedit="${c.id}">✎</button><button type="button" class="cm-icon-btn" data-cdel="${c.id}" style="color:var(--cm-expense)">🗑</button></div></div>`).join('')}
      </div>`;
    fs.classList.add('active');
    $q('#cm-cm-x').onclick = () => { fs.classList.remove('active'); renderAll(); };
    fs.querySelectorAll('[data-ct]').forEach((b) => { b.onclick = () => { ui.catMgrType = b.dataset.ct; openCategoryManager(); }; });
    $q('#cm-cm-add').onclick = () => openPrompt('Tambah Kategori', 'Nama kategori (contoh: Kopi)', '', (name) => {
      const next = cm(); next.categories.push({ id: genId(), name, type: ui.catMgrType, emoji: '🏷️' }); saveCm(next); openCategoryManager();
    });
    fs.querySelectorAll('[data-cedit]').forEach((b) => {
      b.onclick = () => {
        const cid = Number(b.dataset.cedit);
        const cur = cm().categories.find((x) => x.id === cid);
        openPrompt('Ubah Kategori', 'Nama kategori', cur?.name || '', (name) => {
          const next = cm(); const t = next.categories.find((x) => x.id === cid); if (t) t.name = name; saveCm(next); openCategoryManager();
        });
      };
    });
    fs.querySelectorAll('[data-cdel]').forEach((b) => {
      b.onclick = () => {
        const cid = Number(b.dataset.cdel);
        const next = cm(); next.categories = next.categories.filter((x) => x.id !== cid); saveCm(next); openCategoryManager();
      };
    });
  }

  function openMemberManager() {
    const s = cm();
    const fs = $q('#cm-modal-fs');
    fs.innerHTML = `
      <div class="cm-header">
        <button type="button" class="cm-icon-btn" id="cm-mm-x">←</button>
        <div class="cm-title">Kelola Anggota</div>
        <button type="button" class="cm-icon-btn" id="cm-mm-add">+</button>
      </div>
      <div style="flex:1;overflow-y:auto">
        ${s.members.map((m) => `<div class="cm-acc-row"><span>${esc(m.emoji)} ${esc(m.name)}</span>${s.members.length > 1 ? `<button type="button" class="cm-icon-btn" data-mdel="${m.id}" style="color:var(--cm-expense)">🗑</button>` : ''}</div>`).join('')}
      </div>`;
    fs.classList.add('active');
    $q('#cm-mm-x').onclick = () => { fs.classList.remove('active'); renderAll(); };
    $q('#cm-mm-add').onclick = () => openPrompt('Tambah Anggota', 'Nama anggota', '', (name) => {
      const next = cm(); next.members.push({ id: `m_${genId()}`, name, emoji: '😄' }); saveCm(next); openMemberManager();
    });
    fs.querySelectorAll('[data-mdel]').forEach((b) => {
      b.onclick = () => { const next = cm(); next.members = next.members.filter((x) => x.id !== b.dataset.mdel); saveCm(next); openMemberManager(); };
    });
  }

  // ── Tabungan Wishlist Flows ───────────────────────────────────────
  function openSvDetail(wid) {
    const s = cm();
    const w = s.wishlists.find((x) => x.id === wid);
    if (!w) return;
    const ico = SV_ICONS.find((i) => i.key === w.image)?.emoji || '🛍️';
    const done = w.status === 'Selesai';
    openSheet(`
      <div class="cm-sheet-handle"></div>
      <div class="cm-sheet-title">${ico} ${esc(w.title)}</div>
      <div class="cm-sheet-body">
        <div style="text-align:center;margin-bottom:14px">
          <div style="font-size:22px;font-weight:800" class="cm-in">${fmtRp(w.saved)}</div>
          <div style="font-size:12px;color:var(--cm-muted)">dari target ${fmtRp(w.price)} · ${esc(w.priority)}</div>
        </div>
        <div style="display:flex;gap:8px;margin-bottom:14px">
          ${done ? '' : `<button type="button" class="cm-btn cm-btn-pri" id="cm-sv-dep">+ Nabung</button>`}
          ${w.saved > 0 ? `<button type="button" class="cm-btn cm-btn-sec" id="cm-sv-wd">${done ? 'Cairkan' : 'Realisasi / Cairkan'}</button>` : ''}
          <button type="button" class="cm-btn cm-btn-sec" id="cm-sv-ed" style="flex:0 0 auto">✎</button>
        </div>
        <div class="cm-sec-title" style="padding:4px 0 8px">Riwayat</div>
        ${(w.history || []).length ? w.history.slice().reverse().map((h) => `<div class="cm-acc-row"><div><div class="cm-trx-title">${h.type === 'in' ? 'Setoran' : 'Pencairan'} · ${esc(h.accountName)}</div><div class="cm-trx-sub">${esc(h.date)}</div></div><div class="cm-trx-amt ${h.type === 'in' ? 'cm-in' : 'cm-out'}">${fmtRp(h.amount)}</div></div>`).join('') : '<div class="cm-empty" style="padding:14px">Belum ada riwayat setoran</div>'}
      </div>`);
    $q('#cm-sv-dep')?.addEventListener('click', () => { closeSheet(); openSvDeposit(wid); });
    $q('#cm-sv-wd')?.addEventListener('click', () => { closeSheet(); openSvWithdraw(wid); });
    $q('#cm-sv-ed')?.addEventListener('click', () => { closeSheet(); openSvForm(w); });
  }

  function openSvForm(w = null) {
    let selPri = w?.priority || 'Penting';
    let selIcon = w?.image || 'bag';
    openSheet(`
      <div class="cm-sheet-handle"></div>
      <div class="cm-sheet-title">${w ? 'Edit Wishlist' : 'Wishlist Baru'}</div>
      <div class="cm-sheet-body">
        <div class="cm-form-group"><label>Nama Barang / Target</label><input class="cm-input" id="cm-svf-t" value="${esc(w?.title || '')}" placeholder="Contoh: Laptop Baru" /></div>
        <div class="cm-form-group"><label>Target Harga (Rp)</label><input type="number" class="cm-input" id="cm-svf-p" value="${w?.price || 1000000}" /></div>
        <div class="cm-form-group"><label>Prioritas</label>
          <div class="cm-seg-tabs" id="cm-svf-pri">${['Penting','Nanti','Kapan-kapan'].map((p) => `<button type="button" class="cm-seg-tab ${selPri === p ? 'active' : ''}" data-p="${p}">${p}</button>`).join('')}</div>
        </div>
        <div class="cm-form-group"><label>Ikon</label>
          <div class="cm-emoji-grid" id="cm-svf-ico">${SV_ICONS.map((i) => `<div class="cm-emoji-cell ${selIcon === i.key ? 'sel' : ''}" data-ik="${i.key}">${i.emoji}</div>`).join('')}</div>
        </div>
        <div style="display:flex;gap:8px;margin-top:8px">
          ${w && (w.saved || 0) <= 0 ? `<button type="button" class="cm-btn cm-btn-dan" id="cm-svf-del">Hapus</button>` : ''}
          <button type="button" class="cm-btn cm-btn-pri" id="cm-svf-ok">Simpan</button>
        </div>
      </div>`);
    const sh = $q('#cm-sheet');
    sh.querySelectorAll('[data-p]').forEach((b) => { b.onclick = () => { selPri = b.dataset.p; sh.querySelectorAll('[data-p]').forEach((x) => x.classList.toggle('active', x.dataset.p === selPri)); }; });
    sh.querySelectorAll('[data-ik]').forEach((b) => { b.onclick = () => { selIcon = b.dataset.ik; sh.querySelectorAll('[data-ik]').forEach((x) => x.classList.toggle('sel', x.dataset.ik === selIcon)); }; });
    $q('#cm-svf-del')?.addEventListener('click', () => {
      const next = cm(); next.wishlists = next.wishlists.filter((x) => x.id !== w.id); saveCm(next); closeSheet(); renderAll(); showCmToast('Wishlist dihapus');
    });
    $q('#cm-svf-ok').onclick = () => {
      const title = $q('#cm-svf-t').value.trim();
      const price = Math.round(Number($q('#cm-svf-p').value) || 0);
      if (!title || price <= 0) { showCmToast('Isi nama dan harga target', 'err'); return; }
      const next = cm();
      if (w) {
        const idx = next.wishlists.findIndex((x) => x.id === w.id);
        if (idx >= 0) next.wishlists[idx] = { ...next.wishlists[idx], title, price, priority: selPri, image: selIcon };
      } else {
        next.wishlists.unshift({ id: `wish_${genId()}`, title, price, priority: selPri, image: selIcon, status: 'Belum', saved: 0, realizedAt: null, realizedAmount: 0, history: [], createdAt: Date.now() });
      }
      saveCm(next);
      closeSheet();
      renderAll();
      showCmToast('Wishlist disimpan');
    };
  }

  function openSvDeposit(wid) {
    const s = cm();
    const w = s.wishlists.find((x) => x.id === wid);
    const accs = s.accounts.filter((a) => !a.isSavings);
    if (!w || !accs.length) return;
    let selAcc = accs[0].id;
    const rem = Math.max(0, w.price - w.saved);
    openSheet(`
      <div class="cm-sheet-handle"></div>
      <div class="cm-sheet-title">Nabung · ${esc(w.title)}</div>
      <div class="cm-sheet-body">
        <div class="cm-form-group"><label>Nominal (Rp)</label><input type="number" class="cm-input" id="cm-svd-amt" value="${Math.min(100000, rem || 100000)}" /></div>
        <div class="cm-sv-chips">${[25000, 50000, 100000, 250000, rem].filter((v) => v > 0).map((v) => `<button type="button" class="cm-sv-chip" data-v="${v}">${fmtRp(v, true)}</button>`).join('')}</div>
        <div class="cm-form-group"><label>Ambil Dari Akun</label>
          <select class="cm-select" id="cm-svd-acc">${accs.map((a) => `<option value="${a.id}">${esc(a.icon || '💵')} ${esc(a.name)} (${fmtRp(a.balance)})</option>`).join('')}</select>
        </div>
        <button type="button" class="cm-btn cm-btn-pri" id="cm-svd-go" style="width:100%">Nabung Sekarang</button>
      </div>`);
    $q('#cm-sheet').querySelectorAll('.cm-sv-chip').forEach((c) => { c.onclick = () => { $q('#cm-svd-amt').value = c.dataset.v; }; });
    $q('#cm-svd-go').onclick = () => {
      const amt = Math.round(Number($q('#cm-svd-amt').value) || 0);
      selAcc = Number($q('#cm-svd-acc').value);
      if (amt <= 0) { showCmToast('Nominal tidak valid', 'err'); return; }
      const next = cm();
      const wish = next.wishlists.find((x) => x.id === wid);
      const src = next.accounts.find((a) => a.id === selAcc);
      const svAcc = savingsAcc(next);
      const book = activeBook(next);
      if (!wish || !src || !svAcc) return;
      const pair = genId();
      const today = todayStr();
      next.transactions.unshift(
        { id: pair, bookId: book.id, title: `Nabung: ${wish.title}`, amount: amt, direction: 'out', categoryId: 12, accountId: src.id, date: today, memberId: 'm1', transferPair: pair, savingsPot: wid, createdAt: Date.now() },
        { id: pair + 1, bookId: book.id, title: `Nabung: ${wish.title}`, amount: amt, direction: 'in', categoryId: 12, accountId: svAcc.id, date: today, memberId: 'm1', transferPair: pair, savingsPot: wid, createdAt: Date.now() + 1 }
      );
      adjBal(next, src.id, -amt);
      adjBal(next, svAcc.id, amt);
      wish.saved = (Number(wish.saved) || 0) + amt;
      wish.history = [...(wish.history || []), { id: genId(), type: 'in', amount: amt, date: today, accountId: src.id, accountName: src.name, ts: Date.now() }];
      saveCm(next);
      closeSheet();
      renderAll();
      showCmToast(`Berhasil menabung ${fmtRp(amt, true)}`);
    };
  }

  function openSvWithdraw(wid) {
    const s = cm();
    const w = s.wishlists.find((x) => x.id === wid);
    const accs = s.accounts.filter((a) => !a.isSavings);
    if (!w || !accs.length) return;
    openSheet(`
      <div class="cm-sheet-handle"></div>
      <div class="cm-sheet-title">Cairkan · ${esc(w.title)}</div>
      <div class="cm-sheet-body">
        <div style="margin-bottom:10px;font-size:13px">Saldo terkumpul: <strong class="cm-in">${fmtRp(w.saved)}</strong></div>
        <div class="cm-form-group"><label>Kembalikan Ke Akun</label>
          <select class="cm-select" id="cm-svw-acc">${accs.map((a) => `<option value="${a.id}">${esc(a.icon || '💵')} ${esc(a.name)}</option>`).join('')}</select>
        </div>
        <div style="display:flex;gap:8px">
          <button type="button" class="cm-btn cm-btn-sec" id="cm-svw-cancel">Batal Target</button>
          <button type="button" class="cm-btn cm-btn-pri" id="cm-svw-realize">Tandai Selesai</button>
        </div>
      </div>`);
    const doWd = (markDone) => {
      const dstId = Number($q('#cm-svw-acc').value);
      const next = cm();
      const wish = next.wishlists.find((x) => x.id === wid);
      const dst = next.accounts.find((a) => a.id === dstId);
      const svAcc = savingsAcc(next);
      const book = activeBook(next);
      const amt = Number(wish?.saved) || 0;
      if (!wish || !dst || !svAcc || amt <= 0) return;
      const pair = genId();
      const today = todayStr();
      next.transactions.unshift(
        { id: pair, bookId: book.id, title: `${markDone ? 'Realisasi' : 'Batal'}: ${wish.title}`, amount: amt, direction: 'out', categoryId: 12, accountId: svAcc.id, date: today, memberId: 'm1', transferPair: pair, savingsPot: wid, createdAt: Date.now() },
        { id: pair + 1, bookId: book.id, title: `${markDone ? 'Realisasi' : 'Batal'}: ${wish.title}`, amount: amt, direction: 'in', categoryId: 12, accountId: dst.id, date: today, memberId: 'm1', transferPair: pair, savingsPot: wid, createdAt: Date.now() + 1 }
      );
      adjBal(next, svAcc.id, -amt);
      adjBal(next, dst.id, amt);
      wish.saved = 0;
      if (markDone) { wish.status = 'Selesai'; wish.realizedAt = Date.now(); wish.realizedAmount = amt; }
      wish.history = [...(wish.history || []), { id: genId(), type: 'out', kind: markDone ? 'realize' : 'cancel', amount: amt, date: today, accountId: dst.id, accountName: dst.name, ts: Date.now() }];
      saveCm(next);
      closeSheet();
      renderAll();
      showCmToast(markDone ? 'Wishlist terealisasi!' : 'Tabungan dicairkan');
    };
    $q('#cm-svw-cancel').onclick = () => doWd(false);
    $q('#cm-svw-realize').onclick = () => doWd(true);
  }

  function exportCsv() {
    const s = cm();
    const book = activeBook(s);
    const catMap = new Map(s.categories.map((c) => [c.id, c.name]));
    const accMap = new Map(s.accounts.map((a) => [a.id, a.name]));
    const rows = ['Tanggal,Judul,Tipe,Nominal,Kategori,Akun'];
    s.transactions.filter((t) => t.bookId === book.id).forEach((t) => {
      rows.push(`${t.date},"${String(t.title).replace(/"/g, '""')}",${t.direction},${t.amount},"${catMap.get(t.categoryId) || ''}","${accMap.get(t.accountId) || ''}"`);
    });
    const blob = new Blob([rows.join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `cucimoney-${book.name.toLowerCase()}-${todayStr()}.csv`;
    a.click();
    showCmToast('CSV diunduh');
  }

  function exportBackup() {
    const s = cm();
    const payload = { _version: 1, _app: 'cucimoney', _exportedAt: new Date().toISOString(), db: s };
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `cucimoney-backup-${todayStr()}.json`;
    a.click();
    showCmToast('Backup JSON diunduh');
  }

  function wireEvents() {
    $q('#cm-exit-btn').onclick = () => window.CuciMoneyModule.close();
    root.querySelectorAll('[data-cm-exit]').forEach((b) => { b.onclick = () => window.CuciMoneyModule.close(); });
    $q('#cm-eye-btn').onclick = () => window.CuciMoneyModule.toggleVisibility();
    root.querySelectorAll('[data-cm-eye]').forEach((b) => { b.onclick = () => window.CuciMoneyModule.toggleVisibility(); });
    $q('#cm-book-btn').onclick = openBookSheet;
    $q('#cm-search-btn').onclick = openSearchModal;
    $q('#cm-fab').onclick = () => openTrxForm(null);
    $q('#cm-add-acc-btn').onclick = () => openAccountForm(null);
    $q('#cm-sv-back').onclick = () => { ui.view = 'lebih'; renderAll(); };
    $q('#cm-sv-add').onclick = $q('#cm-sv-add2').onclick = () => openSvForm(null);
    $q('#cm-backdrop').onclick = () => { closeSheet(); closeDialog(); };

    $q('#cm-m-prev').onclick = () => { ui.month--; if (ui.month < 0) { ui.month = 11; ui.year--; } renderAll(); };
    $q('#cm-m-next').onclick = () => { ui.month++; if (ui.month > 11) { ui.month = 0; ui.year++; } renderAll(); };
    $q('#cm-ana-prev').onclick = () => {
      if (ui.anaPeriod === 'year') ui.anaYear--;
      else { ui.anaMonth--; if (ui.anaMonth < 0) { ui.anaMonth = 11; ui.anaYear--; } }
      renderAll();
    };
    $q('#cm-ana-next').onclick = () => {
      if (ui.anaPeriod === 'year') ui.anaYear++;
      else { ui.anaMonth++; if (ui.anaMonth > 11) { ui.anaMonth = 0; ui.anaYear++; } }
      renderAll();
    };
    root.querySelectorAll('#cm-ana-periods .cm-seg-tab').forEach((b) => { b.onclick = () => { ui.anaPeriod = b.dataset.period; renderAll(); }; });
    root.querySelectorAll('#cm-ana-tabs .cm-seg-tab').forEach((b) => { b.onclick = () => { ui.anaTab = b.dataset.atab; renderAll(); }; });
    root.querySelectorAll('.cm-nav-item').forEach((b) => { b.onclick = () => { ui.view = b.dataset.cmview; renderAll(); }; });

    $q('#cm-restore-input').onchange = (e) => {
      const file = e.target.files?.[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const parsed = JSON.parse(String(reader.result || '{}'));
          const src = parsed.db && parsed.db.accounts ? parsed.db : parsed;
          saveCm(normalizeState(src));
          renderAll();
          showCmToast('Backup berhasil dipulihkan');
        } catch (_) {
          showCmToast('File backup tidak valid', 'err');
        }
      };
      reader.readAsText(file);
      e.target.value = '';
    };
  }

  window.CuciMoneyModule = {
    defaultState,
    normalizeState,
    init(initHooks) {
      hooks = initHooks;
      root = document.getElementById('cucimoney-overlay');
      if (!root) return;
      root.innerHTML = buildShellHtml();
      wireEvents();
      renderAll();
    },
    open() {
      if (!root) return;
      root.classList.add('open');
      renderAll();
    },
    close() {
      if (!root) return;
      root.classList.remove('open');
      hooks?.onUpdate?.();
    },
    toggleVisibility() {
      const next = cm();
      next.prefs.visibility = !next.prefs.visibility;
      saveCm(next);
      renderAll();
    },
    resetData() {
      saveCm(defaultState());
      renderAll();
    },
    getSummary() {
      const s = cm();
      const book = activeBook(s);
      const accs = bookAccounts(s, book.id);
      const balance = accs.reduce((a, b) => a + (Number(b.balance) || 0), 0);
      const today = todayStr();
      const todayExp = s.transactions
        .filter((t) => t.bookId === book.id && t.date === today && t.direction === 'out' && t.transferPair == null)
        .reduce((a, b) => a + b.amount, 0);
      return {
        bookName: book.name,
        visibility: s.prefs.visibility,
        balance,
        todayExpense: todayExp,
        formattedBalance: fmtRp(balance, s.prefs.visibility),
        formattedTodayExpense: fmtRp(todayExp, s.prefs.visibility),
      };
    },
    buildContextForAssistant() {
      const s = cm();
      const actBook = activeBook(s);
      const today = todayStr();
      const curYear = nowD.getFullYear();
      const curMonth = nowD.getMonth();
      const pref = `${curYear}-${pad2(curMonth + 1)}`;

      const cats = Array.isArray(s.categories) ? s.categories : [];
      const expenseCats = cats.filter((c) => c.type === 'expense' || c.type === 'both').map((c) => c.name);
      const incomeCats = cats.filter((c) => c.type === 'income' || c.type === 'both').map((c) => c.name);
      const catById = new Map(cats.map((c) => [c.id, c.name]));

      let blk = `\n\n## Ringkasan Keuangan CuciMoney+ (Buku aktif di app: ${actBook.name})\n`;
      if (expenseCats.length) blk += `- Kategori Pengeluaran tersedia: ${expenseCats.join(', ')}\n`;
      if (incomeCats.length) blk += `- Kategori Pemasukan tersedia: ${incomeCats.join(', ')}\n`;

      s.books.forEach((book) => {
        const nonTrf = s.transactions.filter((t) => t.bookId === book.id && t.transferPair == null);
        const monthTrx = nonTrf.filter((t) => String(t.date || '').startsWith(pref));
        const todayExp = nonTrf.filter((t) => t.date === today && t.direction === 'out').reduce((a, b) => a + b.amount, 0);
        const mIn = monthTrx.filter((t) => t.direction === 'in').reduce((a, b) => a + b.amount, 0);
        const mOut = monthTrx.filter((t) => t.direction === 'out').reduce((a, b) => a + b.amount, 0);
        const allIn = nonTrf.filter((t) => t.direction === 'in').reduce((a, b) => a + b.amount, 0);
        const allOut = nonTrf.filter((t) => t.direction === 'out').reduce((a, b) => a + b.amount, 0);

        blk += `- KEUANGAN BUKU "${book.name}" (bookId=${book.id}): hari ini keluar ${fmtRp(todayExp, true)}; bulan ini pemasukan ${fmtRp(mIn, true)}, pengeluaran ${fmtRp(mOut, true)} (${monthTrx.length} transaksi); sepanjang waktu pemasukan ${fmtRp(allIn, true)}, pengeluaran ${fmtRp(allOut, true)}.\n`;

        const expMap = new Map();
        const incMap = new Map();
        monthTrx.forEach((t) => {
          const cName = catById.get(t.categoryId) || 'Lainnya';
          const targetMap = t.direction === 'in' ? incMap : expMap;
          targetMap.set(cName, (targetMap.get(cName) || 0) + (Number(t.amount) || 0));
        });
        const topExp = [...expMap.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6);
        if (topExp.length) {
          blk += `  Breakdown Pengeluaran bulan ini (${book.name}): ${topExp.map(([k, v]) => `${k} ${fmtRp(v, true)}`).join(', ')}\n`;
        }
        const topInc = [...incMap.entries()].sort((a, b) => b[1] - a[1]).slice(0, 5);
        if (topInc.length) {
          blk += `  Breakdown Pemasukan bulan ini (${book.name}): ${topInc.map(([k, v]) => `${k} ${fmtRp(v, true)}`).join(', ')}\n`;
        }

        const mb = (s.masterBudgets || []).find((b) => b.bookId === book.id && b.month === curMonth && b.year === curYear);
        if (mb && Number(mb.limit) > 0) {
          const spent = (mb.startDate && mb.endDate)
            ? nonTrf.filter((t) => t.direction === 'out' && t.date >= mb.startDate && t.date <= mb.endDate).reduce((a, b) => a + b.amount, 0)
            : mOut;
          const pct = Math.round((spent / mb.limit) * 100);
          const diff = mb.limit - spent;
          const remTxt = diff >= 0 ? `sisa ${fmtRp(diff, true)}` : `melebihi batas ${fmtRp(-diff, true)}`;
          blk += `  Budget bulan ini (${book.name}): batas ${fmtRp(mb.limit, true)}, terpakai ${fmtRp(spent, true)} (${pct}%), ${remTxt}.\n`;
        }

        const accs = bookAccounts(s, book.id).filter((a) => !a.isSavings);
        if (accs.length) {
          const assets = accs.filter((a) => a.type !== 'credit');
          const debts = accs.filter((a) => a.type === 'credit');
          const totAsset = assets.reduce((a, b) => a + (Number(b.balance) || 0), 0);
          const totDebt = debts.reduce((a, b) => a + Math.abs(Number(b.balance) || 0), 0);
          const names = assets.map((a) => `${a.name} ${fmtRp(a.balance, true)}`).join(', ');
          const debtInfo = totDebt > 0 ? ` (aset ${fmtRp(totAsset, true)}, hutang ${fmtRp(totDebt, true)})` : '';
          blk += `  Akun buku "${book.name}": ${names || 'tidak ada aset'}; aset bersih ${fmtRp(totAsset - totDebt, true)}${debtInfo}.\n`;
        }

        if (book.id === actBook.id) {
          const rec = s.transactions.filter((t) => t.bookId === book.id).slice(0, 3);
          if (rec.length) {
            blk += `  Transaksi terakhir (${book.name}): ` + rec.map((t) => `${t.title} (${t.direction === 'in' ? '+' : '-'}${fmtRp(t.amount, true)}, ${t.date})`).join('; ') + '\n';
          }
        }
      });

      const savTotal = s.accounts.filter((a) => a.isSavings).reduce((a, b) => a + (Number(b.balance) || 0), 0);
      const activeWishes = (s.wishlists || []).filter((w) => w.status !== 'Selesai');
      if (savTotal > 0 || activeWishes.length) {
        blk += `- Tabungan Wishlist (Total saldo pot tabungan: ${fmtRp(savTotal, true)}):\n`;
        activeWishes.slice(0, 10).forEach((w) => {
          const savedAmt = Number(w.saved) || 0;
          const priceAmt = Number(w.price) || 0;
          const remAmt = Math.max(0, priceAmt - savedAmt);
          blk += `  • "${w.title}": terkumpul ${fmtRp(savedAmt, true)} / target ${fmtRp(priceAmt, true)} (kurang ${fmtRp(remAmt, true)}) [${w.priority || 'Nanti'}]\n`;
        });
      }
      return blk;
    },
    applyTagAdd(parts) {
      const p1 = (parts[0] || '').toLowerCase();
      const explicitDir = ['out', 'in', 'expense', 'income', 'keluar', 'masuk'].includes(p1);
      const dir = explicitDir && ['in', 'income', 'masuk'].includes(p1) ? 'in' : 'out';
      const amtStr = (explicitDir ? parts[1] : parts[0]) || '';
      const amt = parseInt(String(amtStr).replace(/[^0-9]/g, ''), 10);
      if (!amt || amt <= 0) return null;
      const title = ((explicitDir ? parts[2] : parts[1]) || '').trim() || (dir === 'in' ? 'Pemasukan' : 'Pengeluaran');
      const catName = ((explicitDir ? parts[3] : parts[2]) || '').trim().toLowerCase();
      const accName = ((explicitDir ? parts[4] : parts[3]) || '').trim().toLowerCase();

      const next = cm();
      const book = activeBook(next);
      const cat = next.categories.find((c) => catName && c.name.toLowerCase() === catName && (c.type === 'both' || (dir === 'in' ? c.type === 'income' : c.type === 'expense')))
        || next.categories.find((c) => dir === 'in' ? c.type === 'income' : c.type === 'expense')
        || next.categories[0];
      const bookAccs = bookAccounts(next, book.id);
      const acc = next.accounts.find((a) => !a.isSavings && accName && a.name.toLowerCase().includes(accName))
        || bookAccs.find((a) => a.id === next.prefs.lastAccByBook?.[book.id])
        || bookAccs[0]
        || next.accounts[0];
      if (!acc) return null;

      next.transactions.unshift({
        id: genId(), bookId: book.id, title, amount: amt, direction: dir,
        categoryId: cat?.id || 1, accountId: acc.id, date: todayStr(),
        memberId: next.members[0]?.id || 'm1', transferPair: null, createdAt: Date.now(),
      });
      adjBal(next, acc.id, dir === 'in' ? amt : -amt);
      next.prefs.lastAccByBook[book.id] = acc.id;
      saveCm(next);
      if (root?.classList.contains('open')) renderAll();
      return `${title} (${fmtRp(amt, true)})`;
    },
    applyTagSave(parts) {
      const p0 = String(parts[0] || '').trim();
      const p1 = String(parts[1] || '').trim();
      const p0Digits = p0.replace(/[^0-9]/g, '');
      const p1Digits = p1.replace(/[^0-9]/g, '');
      const flipped = Boolean(p0Digits) && /^[0-9.,\s]+$/.test(p0) && /[a-zA-Z]/.test(p1);
      const wishQuery = (flipped ? p1 : p0).trim();
      const amt = parseInt(flipped ? p0Digits : p1Digits, 10);
      if (!wishQuery || !amt || amt <= 0) return null;

      const accName = String(parts[2] || '').trim().toLowerCase();
      const bookName = String(parts[3] || '').trim().toLowerCase();
      const next = cm();
      const book = (bookName && (next.books.find((b) => b.name.toLowerCase() === bookName) || next.books.find((b) => b.name.toLowerCase().includes(bookName))))
        || activeBook(next);
      const bookAccs = bookAccounts(next, book.id).filter((a) => !a.isSavings);
      const acc = (accName && (bookAccs.find((a) => a.name.toLowerCase().includes(accName)) || next.accounts.find((a) => !a.isSavings && a.name.toLowerCase().includes(accName))))
        || bookAccs.find((a) => a.id === next.prefs.lastAccByBook?.[book.id])
        || bookAccs[0]
        || next.accounts.find((a) => !a.isSavings);
      const svAcc = savingsAcc(next);
      if (!acc || !svAcc) return null;

      const qLow = wishQuery.toLowerCase();
      let wish = next.wishlists.find((w) => w.status !== 'Selesai' && w.title.toLowerCase() === qLow)
        || next.wishlists.find((w) => w.status !== 'Selesai' && w.title.toLowerCase().includes(qLow));
      if (!wish) {
        wish = {
          id: `wish_${genId()}`, title: wishQuery, price: amt, priority: 'Nanti',
          image: 'bag', status: 'Belum', saved: 0, realizedAt: null, realizedAmount: 0, history: [], createdAt: Date.now(),
        };
        next.wishlists.unshift(wish);
      }

      const pair = genId();
      const today = todayStr();
      next.transactions.unshift(
        { id: pair, bookId: book.id, title: `Nabung: ${wish.title}`, amount: amt, direction: 'out', categoryId: 12, accountId: acc.id, date: today, memberId: 'm1', transferPair: pair, savingsPot: wish.id, createdAt: Date.now() },
        { id: pair + 1, bookId: book.id, title: `Nabung: ${wish.title}`, amount: amt, direction: 'in', categoryId: 12, accountId: svAcc.id, date: today, memberId: 'm1', transferPair: pair, savingsPot: wish.id, createdAt: Date.now() + 1 }
      );
      adjBal(next, acc.id, -amt);
      adjBal(next, svAcc.id, amt);
      wish.saved = (Number(wish.saved) || 0) + amt;
      wish.history = [...(wish.history || []), { id: genId(), type: 'in', amount: amt, date: today, accountId: acc.id, accountName: acc.name, ts: Date.now() }];
      next.prefs.lastAccByBook[book.id] = acc.id;
      saveCm(next);
      if (root?.classList.contains('open')) renderAll();
      return `Tabungan ${wish.title} (+${fmtRp(amt, true)})`;
    },
    applyTagRealize(parts) {
      const wishQuery = String(parts[0] || '').trim();
      if (!wishQuery) return null;
      const p1 = String(parts[1] || '').trim();
      const p2 = String(parts[2] || '').trim();
      const p1IsNum = Boolean(p1) && /^[0-9.,\s]+$/.test(p1);
      const accName = (p1IsNum ? p2 : p1).toLowerCase();
      const paidAmt = parseInt((p1IsNum ? p1 : p2).replace(/[^0-9]/g, ''), 10) || 0;
      const bookName = String(parts[3] || '').trim().toLowerCase();

      const next = cm();
      const book = (bookName && (next.books.find((b) => b.name.toLowerCase() === bookName) || next.books.find((b) => b.name.toLowerCase().includes(bookName))))
        || activeBook(next);
      const bookAccs = bookAccounts(next, book.id).filter((a) => !a.isSavings);
      const dst = (accName && (bookAccs.find((a) => a.name.toLowerCase().includes(accName)) || next.accounts.find((a) => !a.isSavings && a.name.toLowerCase().includes(accName))))
        || bookAccs.find((a) => a.id === next.prefs.lastAccByBook?.[book.id])
        || bookAccs[0]
        || next.accounts.find((a) => !a.isSavings);
      const svAcc = savingsAcc(next);

      const qLow = wishQuery.toLowerCase();
      const wish = next.wishlists.find((w) => w.status !== 'Selesai' && w.title.toLowerCase() === qLow)
        || next.wishlists.find((w) => w.status !== 'Selesai' && w.title.toLowerCase().includes(qLow))
        || next.wishlists.find((w) => w.title.toLowerCase().includes(qLow));

      const today = todayStr();
      const wishTitle = wish?.title || wishQuery;
      const savedAmt = Number(wish?.saved) || 0;
      if (wish) {
        if (savedAmt > 0 && dst && svAcc) {
          const pair = genId();
          next.transactions.unshift(
            { id: pair, bookId: book.id, title: `Realisasi: ${wish.title}`, amount: savedAmt, direction: 'out', categoryId: 12, accountId: svAcc.id, date: today, memberId: 'm1', transferPair: pair, savingsPot: wish.id, createdAt: Date.now() },
            { id: pair + 1, bookId: book.id, title: `Realisasi: ${wish.title}`, amount: savedAmt, direction: 'in', categoryId: 12, accountId: dst.id, date: today, memberId: 'm1', transferPair: pair, savingsPot: wish.id, createdAt: Date.now() + 1 }
          );
          adjBal(next, svAcc.id, -savedAmt);
          adjBal(next, dst.id, savedAmt);
          wish.saved = 0;
          wish.status = 'Selesai';
          wish.realizedAt = Date.now();
          wish.realizedAmount = savedAmt;
          wish.history = [...(wish.history || []), { id: genId(), type: 'out', kind: 'realize', amount: savedAmt, date: today, accountId: dst.id, accountName: dst.name, ts: Date.now() }];
        } else {
          wish.status = 'Selesai';
          wish.realizedAt = Date.now();
          wish.realizedAmount = paidAmt > 0 ? paidAmt : (Number(wish.price) || 0);
        }
      }
      if (savedAmt <= 0 && paidAmt > 0 && dst) {
        const buyCat = next.categories.find((c) => c.name.toLowerCase() === 'belanja')
          || next.categories.find((c) => c.type === 'expense')
          || next.categories[0];
        next.transactions.unshift({
          id: genId(), bookId: book.id, title: `Beli Wishlist: ${wishTitle}`, amount: paidAmt, direction: 'out',
          categoryId: buyCat?.id || 4, accountId: dst.id, date: today, memberId: next.members[0]?.id || 'm1', transferPair: null, createdAt: Date.now(),
        });
        adjBal(next, dst.id, -paidAmt);
        next.prefs.lastAccByBook[book.id] = dst.id;
      }
      saveCm(next);
      if (root?.classList.contains('open')) renderAll();
      return { label: `Wishlist Selesai: ${wishTitle}`, wishTitle, wishId: wish?.id || '' };
    },
  };
})();
