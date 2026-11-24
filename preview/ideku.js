(function () {
  'use strict';

  const FOCUS_LIMIT = 3;
  const STATUSES = ['Belum', 'Proses', 'Selesai', 'Parkir'];
  const PRIORITIES = ['Penting', 'Nanti', 'Kapan-kapan'];
  const ITEM_TYPES = ['Idea', 'Wishlist'];
  const DEFAULT_CATEGORIES = ['Dev', 'Bisnis', 'Konten', 'Belajar', 'Kreatif', 'Personal', 'Belanja'];

  const STATUS_DOT = { Belum: '#d1d5db', Proses: '#f59e0b', Selesai: '#10b981', Parkir: '#94a3b8' };
  const PRI_COLOR = { Penting: '#ef4444', Nanti: '#f59e0b', 'Kapan-kapan': '#10b981' };
  const CAT_PALETTE = ['#6366f1', '#ec4899', '#f59e0b', '#10b981', '#06b6d4', '#8b5cf6', '#ef4444', '#14b8a6', '#f97316', '#84cc16', '#3b82f6', '#d946ef'];

  const TYPE_ICON = {
    Idea: '<svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" style="display:inline-block;vertical-align:-1px;margin-right:4px"><circle cx="7" cy="5.5" r="3"/><line x1="7" y1="8.5" x2="7" y2="10"/><line x1="5.5" y1="10.5" x2="8.5" y2="10.5"/><line x1="6" y1="11.8" x2="8" y2="11.8"/></svg>',
    Wishlist: '<svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" style="display:inline-block;vertical-align:-1px;margin-right:4px"><path d="M1.5 2h1.2l1.6 6h6.4l1.3-4H4.5"/><circle cx="5.5" cy="11" r="0.9" fill="currentColor"/><circle cx="9.5" cy="11" r="0.9" fill="currentColor"/></svg>',
  };

  const CATEGORY_ICONS = {
    dev: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="8 6 2 12 8 18"/><polyline points="16 6 22 12 16 18"/></svg>',
    bisnis: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/></svg>',
    konten: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 11l18-5v12L3 14v-3z"/><path d="M11.6 16.8L13 21H9"/></svg>',
    belajar: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 10L12 5 2 10l10 5 10-5z"/><path d="M6 12v5c0 1.5 3 3 6 3s6-1.5 6-3v-5"/></svg>',
    kreatif: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2C6.477 2 2 6.477 2 12c0 5.523 4.477 10 10 10a2 2 0 002-2v-1a2 2 0 012-2h2a2 2 0 002-2c0-5.523-4.477-10-10-10z"/><circle cx="7.5" cy="10.5" r="1" fill="currentColor" stroke="none"/><circle cx="12" cy="7.5" r="1" fill="currentColor" stroke="none"/><circle cx="16.5" cy="10.5" r="1" fill="currentColor" stroke="none"/></svg>',
    personal: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.5-7 8-7s8 3 8 7"/></svg>',
    belanja: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>',
  };

  const GENERIC_CAT_ICONS = [
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"/></svg>',
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18h6"/><path d="M10 22h4"/><path d="M12 2a7 7 0 00-4 12.75V17h8v-2.25A7 7 0 0012 2z"/></svg>',
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>',
  ];

  const WISHLIST_ICONS = {
    bag: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>',
    phone: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>',
    laptop: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="2" y1="20" x2="22" y2="20"/></svg>',
    headphone: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 18v-6a9 9 0 0118 0v6"/><path d="M21 19a2 2 0 01-2 2h-1a2 2 0 01-2-2v-3a2 2 0 012-2h3z"/><path d="M3 19a2 2 0 002 2h1a2 2 0 002-2v-3a2 2 0 00-2-2H3z"/></svg>',
    watch: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="7"/><polyline points="12 9 12 12 13.5 13.5"/><path d="M16.51 17.35l-.35 3.83a2 2 0 01-2 1.82H9.83a2 2 0 01-2-1.82l-.35-3.83m.01-10.7l.35-3.83A2 2 0 019.83 1h4.35a2 2 0 012 1.82l.35 3.83"/></svg>',
    camera: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/><circle cx="12" cy="13" r="4"/></svg>',
    shirt: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20.38 3.46L16 2a4 4 0 01-8 0L3.62 3.46a2 2 0 00-1.34 2.23l.58 3.57a1 1 0 00.99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 002-2V10h2.15a1 1 0 00.99-.84l.58-3.57a2 2 0 00-1.34-2.23z"/></svg>',
    shoe: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M2 16l4-9 4 5 4-3 4 4"/><path d="M2 16h20v4H2z"/></svg>',
    book: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>',
    gamepad: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="2" y="6" width="20" height="12" rx="2"/><path d="M6 12h4M8 10v4M15 12h.01M18 12h.01"/></svg>',
    furniture: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20 9V7a2 2 0 00-2-2H6a2 2 0 00-2 2v2"/><path d="M2 11v5a2 2 0 002 2h16a2 2 0 002-2v-5a2 2 0 00-4 0v2H6v-2a2 2 0 00-4 0z"/><path d="M6 19v2M18 19v2"/></svg>',
    food: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 8h1a4 4 0 010 8h-1"/><path d="M2 8h16v9a4 4 0 01-4 4H6a4 4 0 01-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/></svg>',
    bike: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="5.5" cy="17.5" r="3.5"/><circle cx="18.5" cy="17.5" r="3.5"/><path d="M15 6a1 1 0 000-2h-3l-3 7-3 3"/><path d="M9 6L6 17"/></svg>',
    tool: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/></svg>',
    gift: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="20 12 20 22 4 22 4 12"/><rect x="2" y="7" width="20" height="5"/><line x1="12" y1="22" x2="12" y2="7"/><path d="M12 7H7.5a2.5 2.5 0 010-5C11 2 12 7 12 7z"/><path d="M12 7h4.5a2.5 2.5 0 000-5C13 2 12 7 12 7z"/></svg>',
    beauty: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>',
    sport: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M4.93 4.93l14.14 14.14"/></svg>',
    car: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M5 17H3a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v9a2 2 0 01-2 2h-2M14 17H9m6 0a2 2 0 11-4 0 2 2 0 014 0zm-9 0a2 2 0 11-4 0 2 2 0 014 0z"/></svg>',
    music: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>',
    health: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>',
    star: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
    other: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
    tv: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="2" y="3" width="20" height="15" rx="2"/><polyline points="8 21 12 17 16 21"/></svg>',
    glasses: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="6" cy="14" r="4"/><circle cx="18" cy="14" r="4"/><path d="M2 14h0M22 14h0M10 14h4"/><path d="M2 10c0-2 1-3 3-3h14c2 0 3 1 3 3"/></svg>',
    ring: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="14" r="7"/><path d="M8.5 8.5L9.5 4h5l1 4.5"/><path d="M9 4l3 3 3-3"/></svg>',
    toy: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 2a4 4 0 00-4 4v2H4l-2 14h20L20 8h-4V6a4 4 0 00-4-4z"/><line x1="8" y1="12" x2="8" y2="16"/><line x1="6" y1="14" x2="10" y2="14"/><line x1="15" y1="13" x2="17" y2="15"/><line x1="17" y1="13" x2="15" y2="15"/></svg>',
    plant: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 22V12"/><path d="M12 12C12 12 7 10 7 5a5 5 0 0110 0c0 5-5 7-5 7z"/><path d="M12 17c0 0-4-1-5-5"/></svg>',
    palette: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 2C6.477 2 2 6.477 2 12c0 5.523 4.477 10 10 10a2 2 0 002-2v-1a2 2 0 012-2h2a2 2 0 002-2c0-5.523-4.477-10-10-10z"/><circle cx="7.5" cy="10.5" r="1" fill="currentColor" stroke="none"/><circle cx="12" cy="7.5" r="1" fill="currentColor" stroke="none"/><circle cx="16.5" cy="10.5" r="1" fill="currentColor" stroke="none"/></svg>',
    pen: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="12" y1="19" x2="12" y2="3"/><path d="M8 7l4-4 4 4"/><path d="M9 19h6"/><path d="M8 22h8"/></svg>',
  };

  const WISHLIST_ICON_LABELS = {
    bag: 'Tas/Belanja', phone: 'HP', laptop: 'Laptop', headphone: 'Headphone',
    watch: 'Jam', camera: 'Kamera', shirt: 'Pakaian', shoe: 'Sepatu',
    book: 'Buku', gamepad: 'Gaming', furniture: 'Furnitur', food: 'Makanan',
    bike: 'Kendaraan', tool: 'Alat', gift: 'Hadiah', beauty: 'Kecantikan',
    sport: 'Olahraga', car: 'Mobil', music: 'Musik', health: 'Kesehatan',
    star: 'Lainnya', other: 'Umum', tv: 'TV/Monitor', glasses: 'Kacamata',
    ring: 'Perhiasan', toy: 'Mainan/Koleksi', plant: 'Tanaman', palette: 'Art Supply', pen: 'Stationery',
  };

  const esc = (s) => String(s || '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const clone = (o) => JSON.parse(JSON.stringify(o));

  function catColor(cat) {
    const name = cat || 'Lainnya';
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
    return CAT_PALETTE[Math.abs(hash) % CAT_PALETTE.length];
  }

  function catIcon(category) {
    const key = (category || '').trim().toLowerCase();
    if (CATEGORY_ICONS[key]) return CATEGORY_ICONS[key];
    let hash = 0;
    for (let i = 0; i < key.length; i++) hash = key.charCodeAt(i) + ((hash << 5) - hash);
    return GENERIC_CAT_ICONS[Math.abs(hash) % GENERIC_CAT_ICONS.length];
  }

  function getWishlistIcon(key) {
    if (!key || key.length > 30 || key.startsWith('data:') || key.startsWith('http')) {
      return WISHLIST_ICONS.bag;
    }
    return WISHLIST_ICONS[key] || WISHLIST_ICONS.bag;
  }

  function fmtDate(ts) {
    return new Date(Number(ts) || Date.now()).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  function defaultState() {
    return {
      ideas: [],
      categories: [...DEFAULT_CATEGORIES],
      viewMode: 'group',
      theme: 'light',
      collapsedCats: [],
    };
  }

  function normalizeItem(raw, idx) {
    const now = Date.now();
    const id = Number(raw?.id) || (now + (idx || 0));
    const type = raw?.type === 'Wishlist' ? 'Wishlist' : 'Idea';
    const status = STATUSES.includes(raw?.status) ? raw.status : 'Belum';
    const priority = PRIORITIES.includes(raw?.priority) ? raw.priority : 'Nanti';
    const title = String(raw?.title || '').trim();
    if (!title) return null;
    return {
      id,
      title,
      notes: String(raw?.notes || ''),
      category: String(raw?.category || (type === 'Wishlist' ? 'Belanja' : 'Personal')).trim() || 'Personal',
      jalur: String(raw?.jalur || ''),
      next: String(raw?.next || ''),
      priority,
      status,
      type,
      price: Math.max(0, Number(raw?.price) || 0),
      image: String(raw?.image || (type === 'Wishlist' ? 'bag' : '')),
    };
  }

  function normalizeState(raw) {
    const base = defaultState();
    if (!raw || typeof raw !== 'object') return base;
    let ideas = Array.isArray(raw.ideas)
      ? raw.ideas.map((x, i) => normalizeItem(x, i)).filter(Boolean)
      : [];
    // Legacy placeholder migration
    if (ideas.length === 0 && Array.isArray(raw.notes) && raw.notes.length > 0) {
      ideas = raw.notes
        .map((n, i) => normalizeItem(typeof n === 'string' ? { title: n } : n, i))
        .filter(Boolean);
    }
    const cats = Array.isArray(raw.categories) && raw.categories.length
      ? Array.from(new Set(raw.categories.map((c) => String(c || '').trim()).filter(Boolean)))
      : base.categories;
    return {
      ideas,
      categories: cats,
      viewMode: ['group', 'flat', 'grid'].includes(raw.viewMode) ? raw.viewMode : 'group',
      theme: raw.theme === 'dark' ? 'dark' : 'light',
      collapsedCats: Array.isArray(raw.collapsedCats) ? raw.collapsedCats : [],
    };
  }

  // ── Wishlist AI Decision System ──
  const aiDecisionSystem = {
    priorityWeights: { Penting: 3, Nanti: 2, 'Kapan-kapan': 1 },
    categoryContext: {
      Belanja: { type: 'purchase' },
      Personal: { type: 'lifestyle' },
      Kreatif: { type: 'investment' },
      Dev: { type: 'investment' },
      Bisnis: { type: 'investment' },
      Konten: { type: 'investment' },
      Belajar: { type: 'investment' },
    },
    priceThresholds: { low: 100000, medium: 500000, high: 2000000 },
    analyze(item) {
      if (!item || !item.title) {
        return { label: 'Pertimbangkan', recommendation: 'Tunda', reason: 'Data item tidak lengkap.' };
      }
      let score = 50;
      const w = this.priorityWeights[item.priority] || 1;
      score += w === 3 ? 20 : w === 2 ? 5 : -15;
      const price = Number(item.price) || 0;
      if (price > 0) {
        if (price <= this.priceThresholds.low) score += 10;
        else if (price <= this.priceThresholds.medium) score += 0;
        else if (price <= this.priceThresholds.high) score -= 12;
        else score -= 25;
      }
      const notes = (item.notes || '').trim();
      if (!notes) score -= 15;
      else if (notes.length < 20) score -= 5;
      else if (notes.length < 50) score += 5;
      else if (notes.split(/\s+/).length >= 10) score += 15;
      else score += 10;

      const ctx = this.categoryContext[item.category];
      if (ctx) {
        if (ctx.type === 'investment') score += price >= this.priceThresholds.medium ? 8 : 5;
        else if (ctx.type === 'lifestyle') score += 2;
        else if (ctx.type === 'purchase') score += 3;
      }
      score = Math.max(0, Math.min(100, score));

      const hasNotes = notes.length > 0;
      const pri = item.priority || 'Nanti';
      if (score >= 70) {
        const candidates = [
          pri === 'Penting' && `Prioritas tinggi dan ${hasNotes ? 'ada rencana jelas' : 'perlu segera ditindaklanjuti'}.`,
          price <= this.priceThresholds.low && 'Harga terjangkau, kerugian minimal.',
          hasNotes && 'Ada catatan detail yang menunjukkan keputusan matang.',
          ctx?.type === 'investment' && 'Investasi untuk kebutuhan jangka panjang.',
          score >= 85 && 'Semua faktor mendukung pembelian sekarang.',
        ].filter(Boolean);
        return { label: 'Layak', recommendation: 'Beli', reason: candidates[0] || 'Semua faktor mendukung untuk segera membeli.' };
      }
      if (score >= 50) {
        const candidates = [
          price > this.priceThresholds.medium && !hasNotes && 'Harga cukup tinggi, tapi catatan masih kurang detail.',
          pri === 'Nanti' && price > this.priceThresholds.low && 'Prioritas sedang + harga cukup tinggi, percepat riset atau tunda.',
          pri === 'Kapan-kapan' && price <= this.priceThresholds.medium && 'Prioritas rendah—cari yang lebih urgent dulu.',
          !hasNotes && 'Tanpa catatan detail, lebih baik tunda hingga yakin 100%.',
        ].filter(Boolean);
        return { label: 'Pertimbangkan', recommendation: 'Tunda', reason: candidates[0] || 'Ada beberapa faktor yang perlu dipertimbangkan lebih lanjut.' };
      }
      const candidates = [
        pri === 'Kapan-kapan' && price > this.priceThresholds.medium && 'Prioritas rendah tapi harga tinggi—bukan waktu yang tepat.',
        price > this.priceThresholds.high && pri !== 'Penting' && 'Harga sangat tinggi untuk prioritas tidak urgent.',
        !hasNotes && price > this.priceThresholds.medium && 'Tanpa rencana jelas + harga tinggi = impulsif.',
        pri === 'Kapan-kapan' && !hasNotes && 'Tidak urgent dan tidak ada pertimbangan matang.',
      ].filter(Boolean);
      return { label: 'Tidak Layak', recommendation: 'Jangan', reason: candidates[0] || 'Belum waktu yang tepat untuk membeli item ini.' };
    },
  };

  let hooks = null;
  let root = null;

  const ui = {
    filterType: 'Idea',
    filterStatus: null,
    filterCat: 'Semua',
    filterJalur: null,
    searchQuery: '',
    addingType: 'Idea',
    editingId: null,
    pendingDeleteId: null,
    detailId: null,
    menuOpen: false,
    perkayaIdeaId: null,
    perkayaResult: null,
    breakdownIdeaId: null,
    breakdownResult: null,
    aiDecisionId: null,
    focusFullTargetId: null,
    toastTimer: null,
    lastScrollTop: 0,
  };

  function getDb() {
    return hooks?.getDb?.();
  }

  function state() {
    const db = getDb();
    if (!db) return defaultState();
    db.ideku = normalizeState(db.ideku);
    return db.ideku;
  }

  function persist() {
    hooks?.saveDb?.();
    hooks?.onUpdate?.();
  }

  function syncWishlistToCuciMoney(idea) {
    if (!idea || idea.type !== 'Wishlist') return;
    const db = getDb();
    if (!db || !db.cucimoney) return;
    if (!Array.isArray(db.cucimoney.wishlists)) db.cucimoney.wishlists = [];
    const list = db.cucimoney.wishlists;
    const match = list.find((w) => String(w.id) === String(idea.id)) ||
      list.find((w) => String(w.title || '').trim().toLowerCase() === idea.title.trim().toLowerCase());
    const iconKey = WISHLIST_ICONS[idea.image] ? idea.image : 'bag';
    const nextStatus = idea.status === 'Selesai' ? 'Selesai' : 'Belum';
    if (match) {
      match.title = idea.title;
      match.price = Number(idea.price) || 0;
      match.priority = idea.priority || 'Nanti';
      match.image = iconKey;
      if (idea.status === 'Selesai') {
        match.status = 'Selesai';
        if (!match.realizedAt) match.realizedAt = Date.now();
      } else if (match.status === 'Selesai' && idea.status !== 'Selesai') {
        match.status = 'Belum';
      }
    } else {
      list.unshift({
        id: String(idea.id),
        title: idea.title,
        price: Number(idea.price) || 0,
        priority: idea.priority || 'Nanti',
        image: iconKey,
        status: nextStatus,
        saved: 0,
        realizedAt: idea.status === 'Selesai' ? Date.now() : null,
        realizedAmount: 0,
        history: [],
        createdAt: Number(idea.id) || Date.now(),
      });
    }
  }

  function syncFromCuciMoney() {
    const db = getDb();
    const cmWish = db?.cucimoney?.wishlists;
    if (!Array.isArray(cmWish) || !cmWish.length) return;
    const s = state();
    let changed = false;
    cmWish.forEach((w) => {
      const title = String(w.title || '').trim();
      if (!title) return;
      const existing = s.ideas.find((i) =>
        i.type === 'Wishlist' &&
        (String(i.id) === String(w.id) || i.title.trim().toLowerCase() === title.toLowerCase())
      );
      if (existing) {
        if (w.status === 'Selesai' && existing.status !== 'Selesai') {
          existing.status = 'Selesai';
          changed = true;
        }
        if ((Number(w.price) || 0) > 0 && existing.price !== Number(w.price)) {
          existing.price = Number(w.price);
          changed = true;
        }
      } else {
        const numericId = Number(String(w.id || '').replace(/\D/g, '')) || Number(w.createdAt) || Date.now();
        s.ideas.unshift({
          id: numericId,
          title,
          notes: '',
          category: 'Belanja',
          jalur: '',
          next: '',
          priority: PRIORITIES.includes(w.priority) ? w.priority : 'Nanti',
          status: w.status === 'Selesai' ? 'Selesai' : 'Belum',
          type: 'Wishlist',
          price: Math.max(0, Number(w.price) || 0),
          image: WISHLIST_ICONS[w.image] ? w.image : 'bag',
        });
        changed = true;
      }
    });
    if (changed) hooks?.saveDb?.();
  }

  function potFor(idea) {
    if (!idea || idea.type !== 'Wishlist') return null;
    const db = getDb();
    const list = db?.cucimoney?.wishlists;
    if (!Array.isArray(list)) return null;
    const match = list.find((w) => String(w.id) === String(idea.id)) ||
      list.find((w) => String(w.title || '').trim().toLowerCase() === idea.title.trim().toLowerCase());
    if (!match || !(Number(match.saved) > 0)) return null;
    return match;
  }

  function savingBlockHtml(idea, boxed) {
    if (!idea || idea.type !== 'Wishlist') return '';
    const pot = potFor(idea);
    if (!pot) return '';
    const price = Number(idea.price) || 0;
    const saved = Number(pot.saved) || 0;
    const full = price > 0 && saved >= price;
    const pct = price > 0 ? (full ? 100 : Math.min(99, Math.floor((saved / price) * 100))) : 0;
    const fmt = (n) => 'Rp ' + Number(n).toLocaleString('id-ID');
    return `<div class="ik-wishlist-saving${boxed ? ' boxed' : ''}">
      <div class="ik-ws-top"><span>Tabungan di CuciMoney+</span><span><b>${fmt(saved)}</b>${price > 0 ? ` / ${fmt(price)} · ${pct}%` : ''}</span></div>
      ${price > 0 ? `<div class="ik-ws-bar"><div class="ik-ws-fill${full ? ' full' : ''}" style="width:${pct}%"></div></div>` : ''}
      ${full ? '<div class="ik-ws-note">Target tercapai — cairkan lewat menu Tabungan di CuciMoney+</div>' : ''}
    </div>`;
  }

  function showToast(msg, duration = 2200) {
    if (!root) {
      hooks?.showMainToast?.(msg, true);
      return;
    }
    const t = root.querySelector('#ik-toast');
    if (!t) return;
    t.textContent = msg;
    t.style.display = 'block';
    clearTimeout(ui.toastTimer);
    ui.toastTimer = setTimeout(() => {
      t.style.display = 'none';
    }, duration);
  }

  function renderShell() {
    if (!root || root.dataset.ikBuilt) return;
    root.dataset.ikBuilt = '1';
    root.innerHTML = `
      <header class="ik-header">
        <div class="ik-header-inner">
          <div class="ik-header-left">
            <button type="button" class="ik-back-btn" id="ik-exit-btn" title="Kembali ke Apps">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
            </button>
            <div class="ik-brand">
              <div class="ik-brand-icon">
                <svg viewBox="0 0 18 18" fill="none">
                  <circle cx="9" cy="7.5" r="4" stroke="white" stroke-width="1.5"/>
                  <line x1="9" y1="11.5" x2="9" y2="14" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
                  <line x1="7" y1="15" x2="11" y2="15" stroke="white" stroke-width="1.4" stroke-linecap="round"/>
                  <line x1="7.5" y1="16.8" x2="10.5" y2="16.8" stroke="white" stroke-width="1.3" stroke-linecap="round"/>
                </svg>
              </div>
              <div>
                <div class="ik-brand-name">IdeKu</div>
                <div class="ik-brand-sub">Catat · Kelola · Eksekusi</div>
              </div>
            </div>
          </div>
          <div class="ik-header-btns">
            <button type="button" class="ik-btn ik-btn-ghost" id="ik-theme-btn" title="Ganti tema">☀︎</button>
            <button type="button" class="ik-btn ik-btn-ghost" id="ik-menu-btn" title="Menu">⋯</button>
            <button type="button" class="ik-btn ik-btn-dark" id="ik-add-btn" title="Tambah">+</button>
          </div>
        </div>
      </header>

      <div class="ik-scroll" id="ik-scroll">
        <div class="ik-autohide-bar" id="ik-autohide-bar">
          <div class="ik-stats" id="ik-stats"></div>
          <div class="ik-controls-inner">
            <div class="ik-search-row">
              <div class="ik-search-wrap">
                <span class="ik-search-icon">
                  <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="6.5" cy="6.5" r="4.5"/><line x1="10.5" y1="10.5" x2="14" y2="14"/></svg>
                </span>
                <input class="ik-search-input" id="ik-search-input" placeholder="Cari ide atau wishlist..." />
                <button type="button" class="ik-search-clear" id="ik-search-clear">✕</button>
              </div>
              <button type="button" class="ik-btn-viewmode" id="ik-viewmode-btn"></button>
            </div>
            <div class="ik-filters-wrap">
              <div class="ik-filters-group" id="ik-filters-status"></div>
            </div>
          </div>
        </div>

        <div class="ik-content">
          <div class="ik-cat-dash" id="ik-focus-panel" style="display:none"></div>
          <div class="ik-jalur-bar" id="ik-jalur-bar"></div>
          <datalist id="ik-jalur-list"></datalist>
          <div class="ik-cat-dash" id="ik-cat-dash" style="display:none"></div>
          <div id="ik-list"></div>
        </div>
      </div>

      <!-- Menu Dropdown -->
      <div class="ik-menu-dropdown" id="ik-menu-dropdown">
        <button type="button" class="ik-menu-btn" id="ik-m-cats">⊞ Kelola Kategori</button>
        <button type="button" class="ik-menu-btn" id="ik-m-exp-json">↑ Export JSON</button>
        <button type="button" class="ik-menu-btn" id="ik-m-exp-doc">📄 Export Dokumen</button>
        <button type="button" class="ik-menu-btn" id="ik-m-imp-json">↓ Import JSON</button>
        <button type="button" class="ik-menu-btn" id="ik-m-del-all" style="color:#ef4444">🗑 Hapus Semua Data</button>
      </div>
      <input type="file" id="ik-import-file" accept=".json" style="display:none" />

      <!-- Type Selector Modal -->
      <div class="ik-modal-backdrop" id="ik-type-modal">
        <div class="ik-sheet ik-sheet-pad">
          <div class="ik-modal-handle"></div>
          <div class="ik-modal-title">Pilih Tipe Catatan</div>
          <div class="ik-modal-sub">Catat ide proyek baru atau wishlist barang impianmu.</div>
          <div class="ik-modal-actions">
            <button type="button" class="ik-btn-cancel" id="ik-pick-idea" style="font-weight:600;color:var(--ik-text)">💡 Idea</button>
            <button type="button" class="ik-btn-submit" id="ik-pick-wishlist" style="flex:1">🛍️ Wishlist</button>
          </div>
        </div>
      </div>

      <!-- Add Modal -->
      <div class="ik-modal-backdrop" id="ik-add-modal">
        <div class="ik-sheet">
          <div class="ik-form-header">
            <span class="ik-form-title" id="ik-add-title">Idea Baru</span>
            <button type="button" class="ik-form-close" id="ik-add-close">×</button>
          </div>
          <div class="ik-form-body">
            <div class="ik-form-group">
              <label class="ik-form-label">Judul</label>
              <input class="ik-form-field" id="ik-f-title" placeholder="Tulis judul..." />
            </div>
            <div class="ik-form-group">
              <label class="ik-form-label">Catatan (opsional)</label>
              <textarea class="ik-form-field" id="ik-f-notes" rows="2" placeholder="Detail atau konteks tambahan..."></textarea>
            </div>
            <div class="ik-form-group" id="ik-f-jalur-wrap">
              <label class="ik-form-label">Jalur (opsional)</label>
              <input class="ik-form-field" id="ik-f-jalur" list="ik-jalur-list" placeholder="Nama jalur/ekosistem..." />
            </div>
            <div class="ik-form-row-2">
              <div class="ik-form-group">
                <label class="ik-form-label">Kategori</label>
                <select class="ik-form-field" id="ik-f-cat"></select>
              </div>
              <div class="ik-form-group">
                <label class="ik-form-label">Prioritas</label>
                <select class="ik-form-field" id="ik-f-pri">
                  <option>Penting</option><option>Nanti</option><option>Kapan-kapan</option>
                </select>
              </div>
            </div>
            <div id="ik-wishlist-fields" style="display:none">
              <div class="ik-form-group">
                <label class="ik-form-label">Harga (Rp)</label>
                <input class="ik-form-field" id="ik-f-price" type="text" inputmode="numeric" placeholder="Contoh: 250.000" />
              </div>
              <div class="ik-form-group">
                <label class="ik-form-label">Icon Produk</label>
                <button type="button" class="ik-wicon-toggle" id="ik-f-icon-toggle">
                  <div class="ik-wicon-preview" id="ik-f-icon-preview"></div>
                  <span class="ik-wicon-label" id="ik-f-icon-label">Tas/Belanja</span>
                </button>
                <div class="ik-wicon-body" id="ik-f-icon-body">
                  <div class="ik-wicon-grid" id="ik-f-icon-grid"></div>
                </div>
                <input type="hidden" id="ik-f-image" value="bag" />
              </div>
            </div>
          </div>
          <div class="ik-form-footer">
            <button type="button" class="ik-btn-cancel" id="ik-add-cancel">Batal</button>
            <button type="button" class="ik-btn-submit" id="ik-add-submit">Simpan</button>
          </div>
        </div>
      </div>

      <!-- Edit Idea Modal -->
      <div class="ik-modal-backdrop" id="ik-edit-modal">
        <div class="ik-sheet">
          <div class="ik-form-header">
            <span class="ik-form-title">Edit Ide</span>
            <button type="button" class="ik-form-close" id="ik-edit-close">×</button>
          </div>
          <div class="ik-form-body">
            <div class="ik-form-group">
              <label class="ik-form-label">Judul</label>
              <input class="ik-form-field" id="ik-e-title" />
            </div>
            <div class="ik-form-group">
              <label class="ik-form-label">Catatan (opsional)</label>
              <textarea class="ik-form-field" id="ik-e-notes" rows="2"></textarea>
            </div>
            <div class="ik-form-row-2">
              <div class="ik-form-group">
                <label class="ik-form-label">Kategori</label>
                <select class="ik-form-field" id="ik-e-cat"></select>
              </div>
              <div class="ik-form-group">
                <label class="ik-form-label">Prioritas</label>
                <select class="ik-form-field" id="ik-e-pri">
                  <option>Penting</option><option>Nanti</option><option>Kapan-kapan</option>
                </select>
              </div>
            </div>
          </div>
          <div class="ik-form-footer">
            <button type="button" class="ik-btn-cancel" id="ik-edit-cancel">Batal</button>
            <button type="button" class="ik-btn-submit" id="ik-edit-save">Simpan perubahan</button>
          </div>
        </div>
      </div>

      <!-- Edit Wishlist Modal -->
      <div class="ik-modal-backdrop" id="ik-edit-wishlist-modal">
        <div class="ik-sheet">
          <div class="ik-form-header">
            <span class="ik-form-title">Edit Wishlist</span>
            <button type="button" class="ik-form-close" id="ik-ew-close">×</button>
          </div>
          <div class="ik-form-body">
            <div class="ik-form-group">
              <label class="ik-form-label">Judul</label>
              <input class="ik-form-field" id="ik-ew-title" />
            </div>
            <div class="ik-form-group">
              <label class="ik-form-label">Catatan</label>
              <textarea class="ik-form-field" id="ik-ew-notes" rows="2"></textarea>
            </div>
            <div class="ik-form-row-2">
              <div class="ik-form-group">
                <label class="ik-form-label">Kategori</label>
                <select class="ik-form-field" id="ik-ew-cat"></select>
              </div>
              <div class="ik-form-group">
                <label class="ik-form-label">Prioritas</label>
                <select class="ik-form-field" id="ik-ew-pri">
                  <option>Penting</option><option>Nanti</option><option>Kapan-kapan</option>
                </select>
              </div>
            </div>
            <div class="ik-form-group">
              <label class="ik-form-label">Harga (Rp)</label>
              <input class="ik-form-field" id="ik-ew-price" type="text" inputmode="numeric" />
            </div>
            <div class="ik-form-group">
              <label class="ik-form-label">Icon Produk</label>
              <button type="button" class="ik-wicon-toggle" id="ik-ew-icon-toggle">
                <div class="ik-wicon-preview" id="ik-ew-icon-preview"></div>
                <span class="ik-wicon-label" id="ik-ew-icon-label">Tas/Belanja</span>
              </button>
              <div class="ik-wicon-body" id="ik-ew-icon-body">
                <div class="ik-wicon-grid" id="ik-ew-icon-grid"></div>
              </div>
              <input type="hidden" id="ik-ew-image" value="bag" />
            </div>
          </div>
          <div class="ik-form-footer">
            <button type="button" class="ik-btn-cancel" id="ik-ew-cancel">Batal</button>
            <button type="button" class="ik-btn-submit" id="ik-ew-save">Simpan perubahan</button>
          </div>
        </div>
      </div>

      <!-- Detail Modal -->
      <div class="ik-modal-backdrop" id="ik-detail-modal"></div>

      <!-- Focus Full Swap Modal -->
      <div class="ik-modal-backdrop" id="ik-focus-full-modal"></div>

      <!-- Manage Categories Modal -->
      <div class="ik-modal-backdrop" id="ik-cat-modal">
        <div class="ik-sheet">
          <div class="ik-form-header">
            <span class="ik-form-title">Kelola Kategori</span>
            <button type="button" class="ik-form-close" id="ik-cat-close">×</button>
          </div>
          <div class="ik-cat-list" id="ik-cat-list"></div>
          <div class="ik-cat-add-row">
            <input class="ik-form-field" id="ik-cat-add-input" placeholder="Nama kategori baru..." maxlength="24" />
            <button type="button" class="ik-btn-submit" id="ik-cat-add-btn" style="flex:0 0 auto;padding:9px 14px">Tambah</button>
          </div>
          <div style="padding: 4px 18px 22px">
            <button type="button" class="ik-btn-submit" id="ik-cat-done-btn" style="width:100%">Selesai</button>
          </div>
        </div>
      </div>

      <!-- Perkaya Modal -->
      <div class="ik-modal-backdrop" id="ik-perkaya-modal">
        <div class="ik-sheet">
          <div class="ik-perkaya-header">
            <div>
              <div class="ik-modal-title" style="margin:0">Perkaya Ide</div>
              <div id="ik-perkaya-idea-title" style="font-size:11.5px;color:var(--ik-sub)"></div>
            </div>
            <button type="button" class="ik-form-close" id="ik-perkaya-close">×</button>
          </div>
          <div class="ik-perkaya-body" id="ik-perkaya-body"></div>
          <div class="ik-perkaya-footer">
            <button type="button" class="ik-btn-cancel" id="ik-perkaya-cancel">Tutup</button>
            <button type="button" class="ik-btn-submit" id="ik-perkaya-save" disabled>Terapkan ke Catatan</button>
          </div>
        </div>
      </div>

      <!-- Breakdown Modal -->
      <div class="ik-modal-backdrop" id="ik-breakdown-modal">
        <div class="ik-sheet">
          <div class="ik-perkaya-header">
            <div>
              <div class="ik-modal-title" style="margin:0">Prompt Breakdown</div>
              <div id="ik-breakdown-idea-title" style="font-size:11.5px;color:var(--ik-sub)"></div>
            </div>
            <button type="button" class="ik-form-close" id="ik-breakdown-close">×</button>
          </div>
          <div class="ik-perkaya-body" id="ik-breakdown-body"></div>
          <div class="ik-perkaya-footer">
            <button type="button" class="ik-btn-cancel" id="ik-breakdown-cancel">Tutup</button>
            <button type="button" class="ik-btn-copy" id="ik-breakdown-copy" disabled>Salin Prompt</button>
          </div>
        </div>
      </div>

      <!-- AI Decision Modal -->
      <div class="ik-modal-backdrop" id="ik-ai-decision-modal"></div>

      <!-- Delete Confirm Modal -->
      <div class="ik-modal-backdrop" id="ik-del-modal">
        <div class="ik-sheet ik-sheet-pad">
          <div class="ik-modal-handle"></div>
          <div class="ik-modal-title">Hapus item ini?</div>
          <div class="ik-modal-sub" id="ik-del-sub">Item akan dihapus permanen.</div>
          <div class="ik-modal-actions">
            <button type="button" class="ik-btn-cancel" id="ik-del-cancel">Batal</button>
            <button type="button" class="ik-btn-danger" id="ik-del-confirm">Hapus</button>
          </div>
        </div>
      </div>

      <!-- Delete All Confirm Modal -->
      <div class="ik-modal-backdrop" id="ik-del-all-modal">
        <div class="ik-sheet ik-sheet-pad">
          <div class="ik-modal-handle"></div>
          <div class="ik-modal-title">Hapus semua data IdeKu?</div>
          <div class="ik-modal-sub">Semua ide dan wishlist di IdeKu akan dihapus permanen. Dana tabungan CuciMoney+ tetap aman.</div>
          <div class="ik-modal-actions">
            <button type="button" class="ik-btn-cancel" id="ik-del-all-cancel">Batal</button>
            <button type="button" class="ik-btn-danger" id="ik-del-all-confirm">Hapus Semua</button>
          </div>
        </div>
      </div>

      <div class="ik-toast" id="ik-toast"></div>
    `;

    bindEvents();
  }

  function formatRupiahInput(el) {
    const raw = el.value.replace(/\D/g, '');
    el.dataset.raw = raw;
    el.value = raw ? Number(raw).toLocaleString('id-ID') : '';
  }

  function getRupiahRaw(id) {
    const el = root.querySelector('#' + id);
    if (!el) return 0;
    const raw = el.dataset.raw || el.value.replace(/\./g, '').replace(/\D/g, '');
    return parseFloat(raw) || 0;
  }

  function populateWiconGrid(prefix, activeKey) {
    const grid = root.querySelector(`#ik-${prefix}-icon-grid`);
    if (!grid) return;
    grid.innerHTML = Object.keys(WISHLIST_ICONS).map((k) =>
      `<button type="button" class="ik-wicon-opt${k === activeKey ? ' active' : ''}" data-icon="${k}" title="${esc(WISHLIST_ICON_LABELS[k] || k)}">${WISHLIST_ICONS[k]}</button>`
    ).join('');
    grid.querySelectorAll('.ik-wicon-opt').forEach((btn) => {
      btn.addEventListener('click', () => setWiconPickerValue(prefix, btn.dataset.icon));
    });
  }

  function setWiconPickerValue(prefix, iconKey) {
    const key = WISHLIST_ICONS[iconKey] ? iconKey : 'bag';
    const hidden = root.querySelector(`#ik-${prefix}-image`);
    const prev = root.querySelector(`#ik-${prefix}-icon-preview`);
    const lbl = root.querySelector(`#ik-${prefix}-icon-label`);
    const body = root.querySelector(`#ik-${prefix}-icon-body`);
    if (hidden) hidden.value = key;
    if (prev) prev.innerHTML = WISHLIST_ICONS[key];
    if (lbl) lbl.textContent = WISHLIST_ICON_LABELS[key] || key;
    root.querySelectorAll(`#ik-${prefix}-icon-grid .ik-wicon-opt`).forEach((b) => {
      b.classList.toggle('active', b.dataset.icon === key);
    });
    body?.classList.remove('open');
  }

  function bindEvents() {
    const q = (sel) => root.querySelector(sel);

    q('#ik-exit-btn').addEventListener('click', close);
    q('#ik-theme-btn').addEventListener('click', () => {
      const s = state();
      s.theme = s.theme === 'dark' ? 'light' : 'dark';
      persist();
      applyTheme();
    });

    q('#ik-menu-btn').addEventListener('click', (e) => {
      e.stopPropagation();
      ui.menuOpen = !ui.menuOpen;
      q('#ik-menu-dropdown').classList.toggle('open', ui.menuOpen);
    });
    root.addEventListener('click', (e) => {
      if (ui.menuOpen && !e.target.closest('#ik-menu-dropdown') && !e.target.closest('#ik-menu-btn')) {
        ui.menuOpen = false;
        q('#ik-menu-dropdown').classList.remove('open');
      }
    });

    q('#ik-add-btn').addEventListener('click', () => openModal('ik-type-modal'));
    q('#ik-pick-idea').addEventListener('click', () => selectAddType('Idea'));
    q('#ik-pick-wishlist').addEventListener('click', () => selectAddType('Wishlist'));

    q('#ik-search-input').addEventListener('input', (e) => {
      ui.searchQuery = e.target.value.trim();
      q('#ik-search-clear').classList.toggle('visible', ui.searchQuery.length > 0);
      renderList();
    });
    q('#ik-search-clear').addEventListener('click', () => {
      ui.searchQuery = '';
      q('#ik-search-input').value = '';
      q('#ik-search-clear').classList.remove('visible');
      renderList();
    });

    q('#ik-viewmode-btn').addEventListener('click', () => {
      const s = state();
      const order = ['group', 'flat', 'grid'];
      s.viewMode = order[(order.indexOf(s.viewMode) + 1) % order.length];
      persist();
      renderViewModeToggle();
      renderList();
    });

    // Auto-hide bar on internal scroll
    const scrollEl = q('#ik-scroll');
    const barEl = q('#ik-autohide-bar');
    scrollEl.addEventListener('scroll', () => {
      const cur = scrollEl.scrollTop;
      const delta = cur - ui.lastScrollTop;
      if (delta > 6 && cur > 70) barEl.classList.add('hidden');
      else if (delta < -6 || cur <= 10) barEl.classList.remove('hidden');
      ui.lastScrollTop = cur;
    }, { passive: true });

    // Add modal events
    q('#ik-f-price').addEventListener('input', (e) => formatRupiahInput(e.target));
    q('#ik-f-icon-toggle').addEventListener('click', () => q('#ik-f-icon-body').classList.toggle('open'));
    q('#ik-add-close').addEventListener('click', () => closeModal('ik-add-modal'));
    q('#ik-add-cancel').addEventListener('click', () => closeModal('ik-add-modal'));
    q('#ik-add-submit').addEventListener('click', submitIdea);

    // Edit Idea events
    q('#ik-edit-close').addEventListener('click', () => closeModal('ik-edit-modal'));
    q('#ik-edit-cancel').addEventListener('click', () => closeModal('ik-edit-modal'));
    q('#ik-edit-save').addEventListener('click', saveEditIdea);

    // Edit Wishlist events
    q('#ik-ew-price').addEventListener('input', (e) => formatRupiahInput(e.target));
    q('#ik-ew-icon-toggle').addEventListener('click', () => q('#ik-ew-icon-body').classList.toggle('open'));
    q('#ik-ew-close').addEventListener('click', () => closeModal('ik-edit-wishlist-modal'));
    q('#ik-ew-cancel').addEventListener('click', () => closeModal('ik-edit-wishlist-modal'));
    q('#ik-ew-save').addEventListener('click', saveEditWishlist);

    // Manage Categories
    q('#ik-m-cats').addEventListener('click', () => { closeMenu(); renderCatManager(); openModal('ik-cat-modal'); });
    q('#ik-cat-close').addEventListener('click', () => { closeModal('ik-cat-modal'); render(); });
    q('#ik-cat-done-btn').addEventListener('click', () => { closeModal('ik-cat-modal'); render(); });
    q('#ik-cat-add-btn').addEventListener('click', addCategory);
    q('#ik-cat-add-input').addEventListener('keydown', (e) => { if (e.key === 'Enter') addCategory(); });

    // Export / Import / Delete All
    q('#ik-m-exp-json').addEventListener('click', exportJson);
    q('#ik-m-exp-doc').addEventListener('click', exportDoc);
    q('#ik-m-imp-json').addEventListener('click', () => {
      closeMenu();
      const inp = q('#ik-import-file');
      inp.value = '';
      inp.click();
    });
    q('#ik-import-file').addEventListener('change', importJson);
    q('#ik-m-del-all').addEventListener('click', () => {
      closeMenu();
      if (state().ideas.length === 0) { showToast('Tidak ada data untuk dihapus.'); return; }
      openModal('ik-del-all-modal');
    });

    // Delete & Delete All
    q('#ik-del-cancel').addEventListener('click', () => { ui.pendingDeleteId = null; closeModal('ik-del-modal'); });
    q('#ik-del-confirm').addEventListener('click', confirmDelete);
    q('#ik-del-all-cancel').addEventListener('click', () => closeModal('ik-del-all-modal'));
    q('#ik-del-all-confirm').addEventListener('click', confirmDeleteAll);

    // Perkaya & Breakdown
    q('#ik-perkaya-close').addEventListener('click', () => closeModal('ik-perkaya-modal'));
    q('#ik-perkaya-cancel').addEventListener('click', () => closeModal('ik-perkaya-modal'));
    q('#ik-perkaya-save').addEventListener('click', applyPerkayaResult);
    q('#ik-breakdown-close').addEventListener('click', () => closeModal('ik-breakdown-modal'));
    q('#ik-breakdown-cancel').addEventListener('click', () => closeModal('ik-breakdown-modal'));
    q('#ik-breakdown-copy').addEventListener('click', copyBreakdownResult);

    // Backdrop tap close
    ['ik-type-modal', 'ik-add-modal', 'ik-edit-modal', 'ik-edit-wishlist-modal', 'ik-detail-modal', 'ik-focus-full-modal', 'ik-cat-modal', 'ik-perkaya-modal', 'ik-breakdown-modal', 'ik-ai-decision-modal', 'ik-del-modal', 'ik-del-all-modal'].forEach((id) => {
      const el = q('#' + id);
      el?.addEventListener('click', (e) => { if (e.target === el) closeModal(id); });
    });
  }

  function closeMenu() {
    ui.menuOpen = false;
    root?.querySelector('#ik-menu-dropdown')?.classList.remove('open');
  }

  function openModal(id) {
    root?.querySelector('#' + id)?.classList.add('show');
  }

  function closeModal(id) {
    root?.querySelector('#' + id)?.classList.remove('show');
  }

  function applyTheme() {
    if (!root) return;
    const isDark = state().theme === 'dark';
    root.classList.toggle('ik-dark', isDark);
    const btn = root.querySelector('#ik-theme-btn');
    if (btn) btn.textContent = isDark ? '☾' : '☀︎';
  }

  function populateCatSelects(selected) {
    const cats = state().categories;
    const html = cats.map((c) => `<option value="${esc(c)}">${esc(c)}</option>`).join('');
    ['#ik-f-cat', '#ik-e-cat', '#ik-ew-cat'].forEach((sel) => {
      const el = root.querySelector(sel);
      if (el) {
        el.innerHTML = html;
        if (selected && cats.includes(selected)) el.value = selected;
      }
    });
  }

  function selectAddType(type) {
    ui.addingType = type;
    closeModal('ik-type-modal');
    const s = state();
    populateCatSelects(type === 'Wishlist' && s.categories.includes('Belanja') ? 'Belanja' : s.categories[0]);
    root.querySelector('#ik-add-title').textContent = type === 'Wishlist' ? 'Wishlist Baru' : 'Idea Baru';
    root.querySelector('#ik-wishlist-fields').style.display = type === 'Wishlist' ? 'block' : 'none';
    root.querySelector('#ik-f-jalur-wrap').style.display = type === 'Wishlist' ? 'none' : 'block';
    root.querySelector('#ik-f-title').value = '';
    root.querySelector('#ik-f-notes').value = '';
    root.querySelector('#ik-f-jalur').value = '';
    root.querySelector('#ik-f-price').value = '';
    root.querySelector('#ik-f-price').dataset.raw = '';
    root.querySelector('#ik-f-pri').value = 'Nanti';
    populateWiconGrid('f', 'bag');
    setWiconPickerValue('f', 'bag');
    openModal('ik-add-modal');
    setTimeout(() => root.querySelector('#ik-f-title')?.focus(), 70);
  }

  function submitIdea() {
    const title = root.querySelector('#ik-f-title').value.trim();
    if (!title) { showToast('Judul tidak boleh kosong'); return; }
    const s = state();
    const newIdea = {
      id: Date.now(),
      title,
      notes: root.querySelector('#ik-f-notes').value.trim(),
      category: root.querySelector('#ik-f-cat').value || 'Personal',
      jalur: ui.addingType === 'Idea' ? root.querySelector('#ik-f-jalur').value.trim() : '',
      next: '',
      priority: root.querySelector('#ik-f-pri').value || 'Nanti',
      status: 'Belum',
      type: ui.addingType,
      price: ui.addingType === 'Wishlist' ? getRupiahRaw('ik-f-price') : 0,
      image: ui.addingType === 'Wishlist' ? (root.querySelector('#ik-f-image').value || 'bag') : '',
    };
    s.ideas.unshift(newIdea);
    if (newIdea.type === 'Wishlist') syncWishlistToCuciMoney(newIdea);
    ui.filterType = newIdea.type;
    persist();
    closeModal('ik-add-modal');
    render();
    showToast(newIdea.type === 'Wishlist' ? 'Wishlist ditambahkan!' : 'Ide ditambahkan!');
  }

  function openEdit(id) {
    closeModal('ik-detail-modal');
    const idea = state().ideas.find((i) => i.id === id);
    if (!idea) return;
    ui.editingId = id;
    populateCatSelects(idea.category);
    if (idea.type === 'Wishlist') {
      root.querySelector('#ik-ew-title').value = idea.title;
      root.querySelector('#ik-ew-notes').value = idea.notes || '';
      root.querySelector('#ik-ew-pri').value = idea.priority;
      const pEl = root.querySelector('#ik-ew-price');
      pEl.value = idea.price ? Number(idea.price).toLocaleString('id-ID') : '';
      pEl.dataset.raw = idea.price || '';
      const curIcon = WISHLIST_ICONS[idea.image] ? idea.image : 'bag';
      populateWiconGrid('ew', curIcon);
      setWiconPickerValue('ew', curIcon);
      openModal('ik-edit-wishlist-modal');
    } else {
      root.querySelector('#ik-e-title').value = idea.title;
      root.querySelector('#ik-e-notes').value = idea.notes || '';
      root.querySelector('#ik-e-pri').value = idea.priority;
      openModal('ik-edit-modal');
    }
  }

  function saveEditIdea() {
    const title = root.querySelector('#ik-e-title').value.trim();
    if (!title) { showToast('Judul tidak boleh kosong'); return; }
    const s = state();
    const idea = s.ideas.find((i) => i.id === ui.editingId);
    if (!idea) return;
    idea.title = title;
    idea.notes = root.querySelector('#ik-e-notes').value.trim();
    idea.category = root.querySelector('#ik-e-cat').value;
    idea.priority = root.querySelector('#ik-e-pri').value;
    ui.editingId = null;
    persist();
    closeModal('ik-edit-modal');
    render();
    showToast('Ide berhasil diperbarui');
  }

  function saveEditWishlist() {
    const title = root.querySelector('#ik-ew-title').value.trim();
    if (!title) { showToast('Judul tidak boleh kosong'); return; }
    const s = state();
    const idea = s.ideas.find((i) => i.id === ui.editingId);
    if (!idea) return;
    idea.title = title;
    idea.notes = root.querySelector('#ik-ew-notes').value.trim();
    idea.category = root.querySelector('#ik-ew-cat').value;
    idea.priority = root.querySelector('#ik-ew-pri').value;
    idea.price = getRupiahRaw('ik-ew-price');
    idea.image = root.querySelector('#ik-ew-image').value || 'bag';
    syncWishlistToCuciMoney(idea);
    ui.editingId = null;
    persist();
    closeModal('ik-edit-wishlist-modal');
    render();
    showToast('Wishlist berhasil diperbarui');
  }

  function askDelete(id) {
    const idea = state().ideas.find((i) => i.id === id);
    if (!idea) return;
    ui.pendingDeleteId = id;
    const pot = potFor(idea);
    root.querySelector('#ik-del-sub').textContent = `"${idea.title}" akan dihapus permanen.` +
      (pot ? ` Dana tabungan Rp ${Number(pot.saved).toLocaleString('id-ID')} di CuciMoney+ tetap aman.` : '');
    openModal('ik-del-modal');
  }

  function confirmDelete() {
    if (!ui.pendingDeleteId) return;
    const s = state();
    s.ideas = s.ideas.filter((i) => i.id !== ui.pendingDeleteId);
    ui.pendingDeleteId = null;
    persist();
    closeModal('ik-del-modal');
    closeModal('ik-detail-modal');
    render();
    showToast('Item dihapus');
  }

  function confirmDeleteAll() {
    const s = state();
    const total = s.ideas.length;
    s.ideas = [];
    ui.filterCat = 'Semua';
    ui.filterJalur = null;
    persist();
    closeModal('ik-del-all-modal');
    render();
    showToast(`${total} item berhasil dihapus`);
  }

  function updateStatus(id, status) {
    const s = state();
    const idea = s.ideas.find((i) => i.id === id);
    if (!idea) return;
    if (status === 'Proses' && idea.type === 'Idea' && idea.status !== 'Proses') {
      const fokus = s.ideas.filter((i) => i.type === 'Idea' && i.status === 'Proses');
      if (fokus.length >= FOCUS_LIMIT) {
        openFocusFull(id, fokus);
        return;
      }
    }
    idea.status = status;
    if (idea.type === 'Wishlist') syncWishlistToCuciMoney(idea);
    persist();
    render();
    if (root.querySelector('#ik-detail-modal').classList.contains('show')) openItemModal(id);
  }

  function updatePriority(id, priority) {
    const s = state();
    const idea = s.ideas.find((i) => i.id === id);
    if (!idea) return;
    idea.priority = priority;
    if (idea.type === 'Wishlist') syncWishlistToCuciMoney(idea);
    persist();
    render();
    if (root.querySelector('#ik-detail-modal').classList.contains('show')) openItemModal(id);
  }

  function openFocusFull(newId, fokus) {
    const modal = root.querySelector('#ik-focus-full-modal');
    const newIdea = state().ideas.find((i) => i.id === newId);
    modal.innerHTML = `
      <div class="ik-sheet ik-sheet-pad">
        <div class="ik-modal-handle"></div>
        <div class="ik-modal-title">Fokus sudah penuh (${FOCUS_LIMIT}/${FOCUS_LIMIT})</div>
        <div class="ik-modal-sub">Untuk mengerjakan “${esc(newIdea?.title || '')}”, parkir salah satu dulu. Ide yang diparkir aman dan tidak hilang.</div>
        ${fokus.map((f) => `
          <button type="button" class="ik-swap-btn" data-park="${f.id}" style="display:block;width:100%;text-align:left;padding:11px 13px;margin-bottom:8px;border:1px solid var(--ik-border);background:var(--ik-input-bg);color:var(--ik-text);border-radius:10px;font:inherit;font-size:13px;cursor:pointer">
            Parkir: ${esc(f.title)}
          </button>
        `).join('')}
        <button type="button" class="ik-btn-cancel" id="ik-ff-cancel" style="width:100%">Batal</button>
      </div>`;
    modal.querySelectorAll('.ik-swap-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        const parkId = Number(btn.dataset.park);
        const s = state();
        const a = s.ideas.find((i) => i.id === newId);
        const b = s.ideas.find((i) => i.id === parkId);
        if (a && b) {
          b.status = 'Parkir';
          a.status = 'Proses';
          persist();
          closeModal('ik-focus-full-modal');
          closeModal('ik-detail-modal');
          render();
          showToast('Fokus diperbarui');
        }
      });
    });
    modal.querySelector('#ik-ff-cancel').addEventListener('click', () => closeModal('ik-focus-full-modal'));
    openModal('ik-focus-full-modal');
  }

  function openItemModal(id) {
    const idea = state().ideas.find((i) => i.id === id);
    if (!idea) return;
    ui.detailId = id;
    const modal = root.querySelector('#ik-detail-modal');
    const color = catColor(idea.category);
    const priceText = idea.price ? `Rp ${Number(idea.price).toLocaleString('id-ID')}` : null;

    modal.innerHTML = `
      <div class="ik-sheet ik-sheet-pad" style="max-height:82vh;overflow-y:auto">
        <div class="ik-modal-handle"></div>
        <div style="display:flex;align-items:flex-start;gap:10px;margin-bottom:14px">
          <div style="flex:1;min-width:0">
            <div class="ik-modal-title" style="margin-bottom:4px;word-break:break-word">${esc(idea.title)}</div>
            <div style="font-size:11.5px;color:var(--ik-sub);display:flex;align-items:center;gap:5px;flex-wrap:wrap">
              <span class="ik-cat-dot" style="background:${color}"></span>${esc(idea.category)}${priceText ? ` · ${priceText}` : ''}
            </div>
          </div>
          <div style="display:flex;gap:4px;flex-shrink:0">
            <button type="button" class="ik-btn-edit" id="ik-dt-edit" title="Edit">
              <svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M9.5 2.5l2 2L4 12H2v-2L9.5 2.5z"/></svg>
            </button>
            <button type="button" class="ik-btn-del" id="ik-dt-del" title="Hapus">
              <svg width="12" height="12" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><line x1="2" y1="2" x2="12" y2="12"/><line x1="12" y1="2" x2="2" y2="12"/></svg>
            </button>
            <button type="button" class="ik-btn-edit" id="ik-dt-close" title="Tutup">✕</button>
          </div>
        </div>

        ${idea.notes ? `<div class="ik-card-notes">${esc(idea.notes)}</div>` : ''}
        ${savingBlockHtml(idea, true)}

        ${idea.type === 'Idea' ? `
          <div style="margin-bottom:14px">
            <label class="ik-form-label">Jalur</label>
            <input class="ik-form-field" id="ik-m-jalur" list="ik-jalur-list" value="${esc(idea.jalur || '')}" placeholder="Kelompokkan ide satu ekosistem" />
            <label class="ik-form-label" style="margin-top:10px">Langkah berikutnya</label>
            <input class="ik-form-field" id="ik-m-next" value="${esc(idea.next || '')}" placeholder="Satu tindakan kecil hari ini" />
          </div>
        ` : ''}

        <div class="ik-act-row">
          <span class="ik-act-label">Status</span>
          <div class="ik-act-btns">
            ${STATUSES.map((s) => `<button type="button" class="ik-act-btn${idea.status === s ? ' on-s' : ''}" data-st="${s}">${s}</button>`).join('')}
          </div>
        </div>
        <div class="ik-act-row">
          <span class="ik-act-label">Prioritas</span>
          <div class="ik-act-btns">
            ${PRIORITIES.map((p) => `<button type="button" class="ik-act-btn${idea.priority === p ? ' on-p' : ''}" style="${idea.priority === p ? `color:${PRI_COLOR[p]};border-color:${PRI_COLOR[p]}55` : ''}" data-pri="${p}">${p}</button>`).join('')}
          </div>
        </div>

        <div class="ik-ai-row">
          <button type="button" class="ik-btn-ai" id="ik-dt-perkaya">Perkaya Ide</button>
          <button type="button" class="ik-btn-ai" id="ik-dt-breakdown">Breakdown</button>
        </div>
      </div>`;

    modal.querySelector('#ik-dt-close').addEventListener('click', () => closeModal('ik-detail-modal'));
    modal.querySelector('#ik-dt-edit').addEventListener('click', () => openEdit(id));
    modal.querySelector('#ik-dt-del').addEventListener('click', () => askDelete(id));
    modal.querySelectorAll('[data-st]').forEach((b) => b.addEventListener('click', () => updateStatus(id, b.dataset.st)));
    modal.querySelectorAll('[data-pri]').forEach((b) => b.addEventListener('click', () => updatePriority(id, b.dataset.pri)));
    modal.querySelector('#ik-dt-perkaya').addEventListener('click', () => runIdekuAi(id, 'perkaya'));
    modal.querySelector('#ik-dt-breakdown').addEventListener('click', () => runIdekuAi(id, 'breakdown'));

    const saveMeta = () => {
      const s = state();
      const target = s.ideas.find((i) => i.id === id);
      if (!target) return;
      target.jalur = (modal.querySelector('#ik-m-jalur')?.value || '').trim();
      target.next = (modal.querySelector('#ik-m-next')?.value || '').trim();
      if (ui.filterJalur && ui.filterJalur !== '__none' && !s.ideas.some((i) => i.type === 'Idea' && i.jalur === ui.filterJalur)) {
        ui.filterJalur = null;
      }
      persist();
      renderFocusPanel();
      renderJalurBar();
      showToast('Tersimpan');
    };
    modal.querySelector('#ik-m-jalur')?.addEventListener('change', saveMeta);
    modal.querySelector('#ik-m-next')?.addEventListener('change', saveMeta);

    openModal('ik-detail-modal');
  }

  async function runIdekuAi(id, mode) {
    const idea = state().ideas.find((i) => i.id === id);
    if (!idea) return;

    const isBreakdown = mode === 'breakdown';
    const modalId = isBreakdown ? 'ik-breakdown-modal' : 'ik-perkaya-modal';
    const body = root.querySelector(isBreakdown ? '#ik-breakdown-body' : '#ik-perkaya-body');
    const titleEl = root.querySelector(isBreakdown ? '#ik-breakdown-idea-title' : '#ik-perkaya-idea-title');
    const actionBtn = root.querySelector(isBreakdown ? '#ik-breakdown-copy' : '#ik-perkaya-save');

    titleEl.textContent = idea.title;
    actionBtn.disabled = true;
    if (isBreakdown) {
      ui.breakdownIdeaId = id;
      ui.breakdownResult = null;
      actionBtn.classList.remove('copied');
      actionBtn.textContent = 'Salin Prompt';
    } else {
      ui.perkayaIdeaId = id;
      ui.perkayaResult = null;
      actionBtn.textContent = 'Terapkan ke Catatan';
    }

    body.innerHTML = `<div class="ik-perkaya-skeleton">
      <div class="ik-skeleton-line" style="width:90%"></div>
      <div class="ik-skeleton-line" style="width:74%"></div>
      <div class="ik-skeleton-line" style="width:86%"></div>
      <div class="ik-skeleton-line" style="width:62%"></div>
    </div>`;
    openModal(modalId);

    try {
      const controller = new AbortController();
      const tId = setTimeout(() => controller.abort(), 20000);
      let res;
      try {
        res = await fetch('/api/ideku', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          signal: controller.signal,
          body: JSON.stringify({
            title: idea.title,
            notes: idea.notes,
            category: idea.category,
            priority: idea.priority,
            mode,
          }),
        });
      } finally {
        clearTimeout(tId);
      }
      const data = await res.json();
      if (!res.ok || data.error || !data.result) {
        throw new Error(data.error === 'quota' ? 'Kuota AI habis, coba beberapa saat lagi.' : (data.error || 'Gagal menghubungi AI.'));
      }
      if (isBreakdown) {
        ui.breakdownResult = data.result;
        body.innerHTML = `<div class="ik-perkaya-result">${esc(data.result)}</div>`;
      } else {
        ui.perkayaResult = data.result;
        const orig = idea.notes ? idea.notes.trim() : '';
        body.innerHTML = `<div class="ik-perkaya-result">${esc(data.result)}</div>` +
          (orig ? `<div class="ik-perkaya-original"><div class="ik-form-label" style="margin-bottom:4px">Catatan sebelumnya</div><div style="font-size:12px;color:var(--ik-muted)">${esc(orig)}</div></div>` : '');
      }
      actionBtn.disabled = false;
    } catch (err) {
      const msg = err.name === 'AbortError' ? 'Timeout terlampaui. Coba lagi.' : (err.message || 'Gagal memproses.');
      body.innerHTML = `<div style="text-align:center;padding:18px 8px">
        <div style="font-size:22px;margin-bottom:6px">⚠️</div>
        <div style="font-size:13.5px;font-weight:600;margin-bottom:4px">Gagal memproses</div>
        <div style="font-size:12px;color:var(--ik-muted);margin-bottom:10px">${esc(msg)}</div>
        <button type="button" class="ik-act-btn" id="ik-ai-retry">Coba lagi</button>
      </div>`;
      body.querySelector('#ik-ai-retry')?.addEventListener('click', () => runIdekuAi(id, mode));
    }
  }

  function applyPerkayaResult() {
    if (!ui.perkayaIdeaId || !ui.perkayaResult) return;
    const s = state();
    const idea = s.ideas.find((i) => i.id === ui.perkayaIdeaId);
    if (!idea) return;
    idea.notes = ui.perkayaResult;
    persist();
    closeModal('ik-perkaya-modal');
    render();
    openItemModal(idea.id);
    showToast('Ide berhasil diperkaya');
  }

  function copyBreakdownResult() {
    if (!ui.breakdownResult) return;
    const btn = root.querySelector('#ik-breakdown-copy');
    const onCopied = () => {
      btn.classList.add('copied');
      btn.textContent = '✓ Tersalin!';
      setTimeout(() => {
        btn.classList.remove('copied');
        btn.textContent = 'Salin Prompt';
      }, 2200);
    };
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(ui.breakdownResult).then(onCopied).catch(() => showToast('Gagal menyalin'));
    } else {
      showToast('Clipboard tidak tersedia');
    }
  }

  function showAIDecisionModal(id) {
    const idea = state().ideas.find((i) => i.id === id);
    if (!idea) return;
    const decision = aiDecisionSystem.analyze(idea);
    const labelColor = { Layak: '#10b981', Pertimbangkan: '#f59e0b', 'Tidak Layak': '#ef4444' };
    const color = labelColor[decision.label] || '#6b7280';
    const modal = root.querySelector('#ik-ai-decision-modal');
    modal.innerHTML = `
      <div class="ik-sheet ik-sheet-pad">
        <div class="ik-modal-handle"></div>
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:14px">
          <div style="width:36px;height:36px;border-radius:9px;background:rgba(124,58,237,0.12);border:1px solid rgba(124,58,237,0.25);display:flex;align-items:center;justify-content:center;flex-shrink:0">✨</div>
          <div>
            <div class="ik-modal-title" style="margin:0">${esc(idea.title)}</div>
            <div style="font-size:11px;color:var(--ik-muted)">Analisis Kelayakan Wishlist</div>
          </div>
        </div>
        <div style="padding:14px;background:${color}18;border:1px solid ${color}40;border-radius:10px;margin-bottom:12px">
          <div style="font-size:10px;color:var(--ik-muted);font-weight:700;text-transform:uppercase;margin-bottom:4px">Keputusan</div>
          <div style="font-size:17px;font-weight:700;color:${color};margin-bottom:6px">${esc(decision.label)}</div>
          <div style="font-size:12.5px;color:var(--ik-sub);line-height:1.5">${esc(decision.reason)}</div>
        </div>
        <div style="padding:11px 12px;background:var(--ik-input-bg);border-radius:8px;border-left:3px solid ${color};margin-bottom:16px">
          <div style="font-size:10px;color:var(--ik-muted);font-weight:600;margin-bottom:2px">Rekomendasi</div>
          <div style="font-size:14px;font-weight:700">${esc(decision.recommendation)}</div>
        </div>
        <div class="ik-modal-actions">
          <button type="button" class="ik-btn-cancel" id="ik-aid-close">Tutup</button>
          <button type="button" class="ik-btn-submit" id="ik-aid-ok" style="background:${color};color:#fff">Paham</button>
        </div>
      </div>`;
    modal.querySelector('#ik-aid-close').addEventListener('click', () => closeModal('ik-ai-decision-modal'));
    modal.querySelector('#ik-aid-ok').addEventListener('click', () => closeModal('ik-ai-decision-modal'));
    openModal('ik-ai-decision-modal');
  }

  function renderCatManager() {
    const s = state();
    const list = root.querySelector('#ik-cat-list');
    if (s.categories.length === 0) {
      list.innerHTML = '<div style="text-align:center;padding:20px;font-size:12.5px;color:var(--ik-muted)">Belum ada kategori.</div>';
      return;
    }
    list.innerHTML = s.categories.map((c) => {
      const count = s.ideas.filter((i) => i.category === c).length;
      return `<div class="ik-cat-item">
        <span class="ik-cat-dot" style="background:${catColor(c)}"></span>
        <span class="ik-cat-item-name">${esc(c)}</span>
        <span class="ik-cat-item-count">${count > 0 ? `${count} item` : ''}</span>
        <button type="button" class="ik-cat-item-del" data-cat="${esc(c)}" ${count > 0 ? 'disabled title="Masih dipakai"' : 'title="Hapus"'}>✕</button>
      </div>`;
    }).join('');
    list.querySelectorAll('.ik-cat-item-del:not(:disabled)').forEach((b) => {
      b.addEventListener('click', () => {
        const name = b.dataset.cat;
        s.categories = s.categories.filter((c) => c !== name);
        persist();
        renderCatManager();
        showToast(`Kategori "${name}" dihapus`);
      });
    });
  }

  function addCategory() {
    const inp = root.querySelector('#ik-cat-add-input');
    const name = inp.value.trim();
    if (!name) return;
    const norm = name.charAt(0).toUpperCase() + name.slice(1);
    const s = state();
    if (s.categories.some((c) => c.toLowerCase() === norm.toLowerCase())) {
      showToast('Kategori sudah ada');
      return;
    }
    s.categories.push(norm);
    inp.value = '';
    persist();
    renderCatManager();
    showToast(`Kategori "${norm}" ditambahkan`);
  }

  function exportJson() {
    closeMenu();
    const s = state();
    if (s.ideas.length === 0) { showToast('Tidak ada ide untuk diekspor.'); return; }
    const blob = new Blob([JSON.stringify(s.ideas, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `ideku-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
    showToast(`${s.ideas.length} item berhasil diekspor`);
  }

  function exportDoc() {
    closeMenu();
    const s = state();
    if (s.ideas.length === 0) { showToast('Tidak ada ide untuk diekspor.'); return; }
    const genDate = new Date().toLocaleDateString('id-ID', { day: 'numeric', month: 'long', year: 'numeric' });
    const itemsHtml = s.ideas.map((i, idx) => `
      <div style="border:1px solid #eee;border-radius:10px;padding:12px 14px;margin-bottom:10px">
        <div style="font-weight:700;font-size:14px">${idx + 1}. [${esc(i.type)}] ${esc(i.title)} — <span style="font-size:11px;color:#666">${esc(i.status)}</span></div>
        <div style="font-size:12px;color:#777;margin-top:4px">Kategori: ${esc(i.category)} · Prioritas: ${esc(i.priority)}${i.price ? ` · Rp ${Number(i.price).toLocaleString('id-ID')}` : ''}</div>
        ${i.notes ? `<div style="font-size:12.5px;color:#333;margin-top:6px;white-space:pre-wrap">${esc(i.notes)}</div>` : ''}
      </div>`).join('');
    const html = `<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8"><title>IdeKu Laporan ${genDate}</title></head><body style="font-family:Inter,sans-serif;max-width:720px;margin:28px auto;padding:0 16px;color:#111"><h1>Laporan Ide &amp; Wishlist IdeKu</h1><p style="color:#666;margin-bottom:20px">${genDate} · ${s.ideas.length} item</p>${itemsHtml}</body></html>`;
    const blob = new Blob([html], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `ideku-laporan-${new Date().toISOString().slice(0, 10)}.html`;
    a.click();
    URL.revokeObjectURL(url);
    showToast('Dokumen HTML berhasil diekspor');
  }

  function importJson(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      try {
        const parsed = JSON.parse(ev.target.result);
        const list = Array.isArray(parsed) ? parsed : (Array.isArray(parsed?.ideas) ? parsed.ideas : null);
        if (!list) throw new Error('Invalid');
        const s = state();
        const existingIds = new Set(s.ideas.map((i) => i.id));
        const normalized = list.map((x, idx) => normalizeItem(x, idx)).filter((x) => x && !existingIds.has(x.id));
        s.ideas = [...normalized, ...s.ideas].sort((a, b) => b.id - a.id);
        normalized.forEach((x) => {
          if (x.category && !s.categories.includes(x.category)) s.categories.push(x.category);
          if (x.type === 'Wishlist') syncWishlistToCuciMoney(x);
        });
        persist();
        render();
        showToast(`${normalized.length} item baru diimpor`);
      } catch (_) {
        showToast('Format JSON tidak valid');
      }
    };
    reader.readAsText(file);
  }

  // ── Renderers ──
  function renderStats() {
    const ideas = state().ideas;
    const belum = ideas.filter((i) => i.status === 'Belum').length;
    const fokus = ideas.filter((i) => i.status === 'Proses' && i.type === 'Idea').length;
    const selesai = ideas.filter((i) => i.status === 'Selesai').length;
    const rows = [
      { label: 'Total', val: ideas.length },
      { label: 'Belum mulai', val: belum },
      { label: `Fokus (maks ${FOCUS_LIMIT})`, val: fokus },
      { label: 'Selesai', val: selesai },
    ];
    root.querySelector('#ik-stats').innerHTML = rows.map((r) => `
      <div class="ik-stat">
        <div class="ik-stat-num">${r.val}</div>
        <div class="ik-stat-label">${r.label}</div>
      </div>`).join('');
  }

  function renderViewModeToggle() {
    const btn = root.querySelector('#ik-viewmode-btn');
    if (!btn) return;
    const mode = state().viewMode;
    const icons = {
      group: '<svg width="15" height="15" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M1 4l2-1.5h2l1 1h6v7.5a1 1 0 01-1 1H2a1 1 0 01-1-1V4z"/></svg>',
      flat: '<svg width="15" height="15" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><line x1="1" y1="3" x2="13" y2="3"/><line x1="1" y1="7" x2="13" y2="7"/><line x1="1" y1="11" x2="13" y2="11"/></svg>',
      grid: '<svg width="15" height="15" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="1" width="5" height="5" rx="1"/><rect x="8" y="1" width="5" height="5" rx="1"/><rect x="1" y="8" width="5" height="5" rx="1"/><rect x="8" y="8" width="5" height="5" rx="1"/></svg>',
    };
    const labels = { group: 'Per kategori', flat: 'List', grid: 'Grid' };
    btn.innerHTML = icons[mode] || icons.group;
    btn.title = `Tampilan: ${labels[mode] || 'Per kategori'}`;
  }

  function renderFilters() {
    const ideas = state().ideas;
    const parkirN = ideas.filter((i) => i.type === ui.filterType && i.status === 'Parkir').length;
    const el = root.querySelector('#ik-filters-status');
    el.innerHTML = `
      <div class="ik-filters-type-inline">
        ${ITEM_TYPES.map((t) => `<button type="button" class="ik-f-btn${ui.filterType === t ? ' on' : ''}" data-type="${t}">${TYPE_ICON[t]}${t}</button>`).join('')}
      </div>
      <span class="ik-filters-label">Status</span>
      ${STATUSES.map((s) => `<button type="button" class="ik-f-btn${ui.filterStatus === s ? ' on' : ''}" data-status="${s}">${s === 'Parkir' ? `Parkir (${parkirN})` : s}</button>`).join('')}
    `;
    el.querySelectorAll('[data-type]').forEach((b) => b.addEventListener('click', () => {
      ui.filterType = b.dataset.type;
      ui.filterCat = 'Semua';
      ui.filterJalur = null;
      render();
    }));
    el.querySelectorAll('[data-status]').forEach((b) => b.addEventListener('click', () => {
      ui.filterStatus = ui.filterStatus === b.dataset.status ? null : b.dataset.status;
      render();
    }));
  }

  function renderFocusPanel() {
    const el = root.querySelector('#ik-focus-panel');
    if (ui.filterType !== 'Idea') { el.style.display = 'none'; return; }
    const fokus = state().ideas.filter((i) => i.type === 'Idea' && i.status === 'Proses');
    el.style.display = 'block';
    const slots = Math.max(0, FOCUS_LIMIT - fokus.length);
    el.innerHTML = `
      <div class="ik-cat-dash-title-row"><span class="ik-cat-dash-title">Fokus sekarang · ${fokus.length}/${FOCUS_LIMIT}</span></div>
      ${fokus.length === 0 ? '<div style="font-size:12px;color:var(--ik-sub);padding:2px 0">Belum ada fokus. Buka sebuah ide lalu ubah statusnya ke “Proses”.</div>' : ''}
      ${fokus.map((f) => `
        <button type="button" class="ik-focus-item" data-id="${f.id}">
          <div class="ik-focus-item-title">${esc(f.title)}${f.jalur ? ` <span class="ik-focus-item-jalur">· ${esc(f.jalur)}</span>` : ''}</div>
          <div class="ik-focus-item-next" style="color:${f.next ? 'var(--ik-text)' : 'var(--ik-sub)'}">${f.next ? '→ ' + esc(f.next) : '+ tulis langkah berikutnya'}</div>
        </button>`).join('')}
      ${slots > 0 && fokus.length > 0 ? `<div style="font-size:11px;color:var(--ik-sub);padding-top:6px">${slots} slot fokus tersisa</div>` : ''}`;
    el.querySelectorAll('.ik-focus-item').forEach((b) => b.addEventListener('click', () => openItemModal(Number(b.dataset.id))));
  }

  function renderJalurBar() {
    const ideas = state().ideas;
    const m = {};
    ideas.filter((i) => i.type === 'Idea' && i.jalur).forEach((i) => { m[i.jalur] = (m[i.jalur] || 0) + 1; });
    const list = Object.entries(m).sort((a, b) => b[1] - a[1]);
    root.querySelector('#ik-jalur-list').innerHTML = list.map(([j]) => `<option value="${esc(j)}">`).join('');
    const el = root.querySelector('#ik-jalur-bar');
    if (ui.filterType !== 'Idea' || list.length === 0) { el.style.display = 'none'; return; }
    const none = ideas.filter((i) => i.type === 'Idea' && !i.jalur).length;
    el.style.display = 'flex';
    const chip = (key, label, n) => `<button type="button" class="ik-f-btn${ui.filterJalur === key ? ' on' : ''}" data-jalur="${esc(key)}">${esc(label)} (${n})</button>`;
    el.innerHTML = '<span class="ik-filters-label">Jalur</span>' +
      list.map(([j, n]) => chip(j, j, n)).join('') +
      (none ? chip('__none', 'Tanpa jalur', none) : '');
    el.querySelectorAll('[data-jalur]').forEach((b) => b.addEventListener('click', () => {
      ui.filterJalur = ui.filterJalur === b.dataset.jalur ? null : b.dataset.jalur;
      render();
    }));
  }

  function renderCatDash() {
    const el = root.querySelector('#ik-cat-dash');
    const scoped = state().ideas.filter((i) => i.type === ui.filterType);
    if (scoped.length === 0) { el.style.display = 'none'; return; }
    const counts = {};
    scoped.forEach((i) => { const c = i.category || 'Lainnya'; counts[c] = (counts[c] || 0) + 1; });
    const entries = Object.entries(counts).sort((a, b) => b[1] - a[1]);
    if (entries.length <= 1 && ui.filterCat === 'Semua') { el.style.display = 'none'; return; }
    el.style.display = 'block';
    const max = entries[0][1];
    el.innerHTML = `
      <div class="ik-cat-dash-title-row">
        <span class="ik-cat-dash-title">Sebaran kategori · ${scoped.length} item</span>
        ${ui.filterCat !== 'Semua' ? `<button type="button" class="ik-cat-dash-clear" id="ik-cat-clear">${esc(ui.filterCat)} ✕</button>` : ''}
      </div>
      <div class="ik-cat-dash-bars">
        ${entries.map(([cat, count]) => {
          const color = catColor(cat);
          const pct = Math.max(6, Math.round((count / max) * 100));
          return `<button type="button" class="ik-cat-dash-row${ui.filterCat === cat ? ' active' : ''}" data-cat="${esc(cat)}">
            <span class="ik-cat-dash-dot" style="background:${color}"></span>
            <span class="ik-cat-dash-name">${esc(cat)}</span>
            <span class="ik-cat-dash-track"><span class="ik-cat-dash-fill" style="width:${pct}%;background:${color}"></span></span>
            <span class="ik-cat-dash-count">${count}</span>
          </button>`;
        }).join('')}
      </div>`;
    el.querySelector('#ik-cat-clear')?.addEventListener('click', () => { ui.filterCat = 'Semua'; render(); });
    el.querySelectorAll('[data-cat]').forEach((b) => b.addEventListener('click', () => {
      ui.filterCat = ui.filterCat === b.dataset.cat ? 'Semua' : b.dataset.cat;
      render();
    }));
  }

  function renderCardHtml(idea) {
    const done = idea.status === 'Selesai';
    const priColor = PRI_COLOR[idea.priority] || '#aaa';
    const catColorVal = catColor(idea.category);
    const statusDot = STATUS_DOT[idea.status] || '#d1d5db';

    if (idea.type === 'Wishlist') {
      const priceText = idea.price ? `Rp ${Number(idea.price).toLocaleString('id-ID')}` : '—';
      const priDotCount = idea.priority === 'Penting' ? 3 : idea.priority === 'Nanti' ? 2 : 1;
      const aiScore = aiDecisionSystem.analyze(idea);
      const scoreClass = aiScore.label === 'Layak' ? 'score-green' : aiScore.label === 'Pertimbangkan' ? 'score-yellow' : 'score-red';
      return `
        <div class="ik-card wishlist-type${done ? ' done' : ''}" data-open-id="${idea.id}" style="border-left:3px solid ${catColorVal}">
          <div class="ik-wishlist-main">
            <div class="ik-wishlist-icon-wrap">${getWishlistIcon(idea.image)}</div>
            <div class="ik-wishlist-content">
              <div class="ik-wishlist-top">
                <div class="ik-wishlist-title${done ? ' done' : ''}">${esc(idea.title)}</div>
                <div class="ik-card-btns">
                  <button type="button" class="ik-btn-edit" data-edit-id="${idea.id}" title="Edit">✎</button>
                  <button type="button" class="ik-btn-del" data-del-id="${idea.id}" title="Hapus">✕</button>
                </div>
              </div>
              <div class="ik-wishlist-price">${priceText}</div>
              <div class="ik-wishlist-meta">${esc(idea.category)} · ${esc(idea.priority)}</div>
            </div>
          </div>
          ${savingBlockHtml(idea, false)}
          <div class="ik-wishlist-footer">
            <div class="ik-wishlist-footer-item">
              <span class="ik-meta-dot" style="background:${statusDot}"></span>
              <span>${esc(idea.status)}</span>
            </div>
            <div class="ik-wishlist-footer-sep"></div>
            <div class="ik-wishlist-footer-item">
              <div class="ik-priority-dots">
                ${[1, 2, 3].map((n) => `<span class="ik-priority-dot" style="background:${n <= priDotCount ? priColor : 'var(--ik-border)'}"></span>`).join('')}
              </div>
              <span>${esc(idea.priority)}</span>
            </div>
            <button type="button" class="ik-score-badge ${scoreClass}" data-ai-id="${idea.id}">${esc(aiScore.label)}</button>
          </div>
        </div>`;
    }

    return `
      <div class="ik-card${done ? ' done' : ''}" data-open-id="${idea.id}" style="border-left:3px solid ${catColorVal}">
        <div class="ik-card-top">
          <div style="flex:1;min-width:0">
            <div class="ik-card-title${done ? ' done' : ''}">
              <span style="display:inline-flex;align-items:center;gap:4px">${TYPE_ICON.Idea}<span>${esc(idea.title)}</span></span>
            </div>
            <div class="ik-card-meta">
              <span class="ik-meta-dot" style="background:${statusDot}"></span>
              <span class="ik-meta-t">${esc(idea.status)}</span>
              <span class="ik-meta-sep">·</span>
              <span class="ik-cat-dot" style="background:${catColorVal}"></span>
              <span class="ik-meta-t">${esc(idea.category)}</span>
              <span class="ik-meta-sep">·</span>
              <span class="ik-meta-pri" style="color:${priColor}">${esc(idea.priority)}</span>
              <span class="ik-meta-date">${fmtDate(idea.id)}</span>
            </div>
          </div>
          <div class="ik-card-btns">
            <button type="button" class="ik-btn-edit" data-edit-id="${idea.id}" title="Edit">✎</button>
            <button type="button" class="ik-btn-del" data-del-id="${idea.id}" title="Hapus">✕</button>
          </div>
        </div>
      </div>`;
  }

  function renderGridCardHtml(idea) {
    const done = idea.status === 'Selesai';
    const priColor = PRI_COLOR[idea.priority] || '#aaa';
    const catColorVal = catColor(idea.category);
    const priceText = idea.price ? `Rp ${Number(idea.price).toLocaleString('id-ID')}` : '';
    return `
      <div class="ik-grid-card${done ? ' done' : ''}" data-open-id="${idea.id}" style="border-top:3px solid ${catColorVal}">
        <div class="ik-grid-head">
          <div class="ik-grid-icon" style="background:${catColorVal}22;color:${catColorVal}">${catIcon(idea.category)}</div>
          <div class="ik-grid-btns">
            <button type="button" class="ik-btn-edit" data-edit-id="${idea.id}">✎</button>
            <button type="button" class="ik-btn-del" data-del-id="${idea.id}">✕</button>
          </div>
        </div>
        <div class="ik-grid-title${done ? ' done' : ''}">${esc(idea.title)}</div>
        <div class="ik-grid-cat" style="color:${catColorVal}">${esc(idea.category)}</div>
        ${priceText ? `<div class="ik-grid-price">${priceText}</div>` : ''}
        <div class="ik-grid-foot">
          <span class="ik-meta-dot" style="background:${STATUS_DOT[idea.status]}"></span>
          <span class="ik-meta-t">${esc(idea.status)}</span>
          <span class="ik-meta-sep">·</span>
          <span class="ik-meta-pri" style="color:${priColor}">${esc(idea.priority)}</span>
        </div>
      </div>`;
  }

  function renderList() {
    const s = state();
    const q = ui.searchQuery.toLowerCase();
    const filtered = s.ideas.filter((i) =>
      i.type === ui.filterType &&
      (ui.filterStatus ? i.status === ui.filterStatus : i.status !== 'Parkir') &&
      (ui.filterCat === 'Semua' || i.category === ui.filterCat) &&
      (!ui.filterJalur || (ui.filterJalur === '__none' ? !i.jalur : i.jalur === ui.filterJalur)) &&
      (!q || i.title.toLowerCase().includes(q) || (i.notes || '').toLowerCase().includes(q))
    );

    const el = root.querySelector('#ik-list');
    if (filtered.length === 0) {
      const hasAny = s.ideas.length > 0;
      const isWish = ui.filterType === 'Wishlist';
      el.innerHTML = `<div class="ik-empty">
        <div class="ik-empty-icon">${isWish ? '🛍️' : '💡'}</div>
        <div class="ik-empty-title">${ui.searchQuery ? 'Tidak ditemukan' : (isWish ? 'Belum ada wishlist' : (hasAny ? 'Tidak ada item di filter ini' : 'Belum ada ide tersimpan'))}</div>
        <div class="ik-empty-txt">${ui.searchQuery ? 'Coba kata kunci lain.' : (isWish ? 'Simpan barang impian & pantau tabungan CuciMoney+.' : 'Ide bagus dimulai dari satu langkah kecil.')}</div>
        ${!ui.searchQuery && !hasAny ? `<button type="button" class="ik-empty-cta" id="ik-empty-add">+ Tambah ${isWish ? 'wishlist' : 'ide'} pertama</button>` : ''}
      </div>`;
      el.querySelector('#ik-empty-add')?.addEventListener('click', () => selectAddType(ui.filterType));
      return;
    }

    if (s.viewMode === 'grid') {
      el.innerHTML = `<div class="ik-grid-wrap">${filtered.map(renderGridCardHtml).join('')}</div>`;
    } else if (s.viewMode === 'flat') {
      el.innerHTML = filtered.map(renderCardHtml).join('');
    } else {
      const groups = {};
      filtered.forEach((i) => {
        const c = i.category || 'Lainnya';
        (groups[c] = groups[c] || []).push(i);
      });
      const collapsedSet = new Set(s.collapsedCats || []);
      const catOrder = Object.keys(groups).sort((a, b) => groups[b].length - groups[a].length);
      el.innerHTML = catOrder.map((cat) => {
        const items = groups[cat];
        const color = catColor(cat);
        const collapsed = collapsedSet.has(cat);
        const doneCount = items.filter((i) => i.status === 'Selesai').length;
        return `<div class="ik-cat-group">
          <button type="button" class="ik-cat-group-header" data-collapse="${esc(cat)}" style="border-left:3px solid ${color}">
            <span class="ik-cat-group-dot" style="background:${color}"></span>
            <span class="ik-cat-group-name">${esc(cat)}</span>
            <span class="ik-cat-group-count">${items.length} item${doneCount ? ` · ${doneCount} selesai` : ''}</span>
            <span class="ik-cat-group-chevron${collapsed ? ' collapsed' : ''}">⌄</span>
          </button>
          <div class="ik-cat-group-body" style="display:${collapsed ? 'none' : 'flex'}">
            ${items.map(renderCardHtml).join('')}
          </div>
        </div>`;
      }).join('');
    }

    el.querySelectorAll('[data-collapse]').forEach((b) => b.addEventListener('click', () => {
      const cat = b.dataset.collapse;
      const set = new Set(s.collapsedCats || []);
      if (set.has(cat)) set.delete(cat); else set.add(cat);
      s.collapsedCats = Array.from(set);
      persist();
      renderList();
    }));
    el.querySelectorAll('[data-open-id]').forEach((c) => c.addEventListener('click', (e) => {
      if (e.target.closest('[data-edit-id]') || e.target.closest('[data-del-id]') || e.target.closest('[data-ai-id]')) return;
      openItemModal(Number(c.dataset.openId));
    }));
    el.querySelectorAll('[data-edit-id]').forEach((b) => b.addEventListener('click', (e) => {
      e.stopPropagation();
      openEdit(Number(b.dataset.editId));
    }));
    el.querySelectorAll('[data-del-id]').forEach((b) => b.addEventListener('click', (e) => {
      e.stopPropagation();
      askDelete(Number(b.dataset.delId));
    }));
    el.querySelectorAll('[data-ai-id]').forEach((b) => b.addEventListener('click', (e) => {
      e.stopPropagation();
      showAIDecisionModal(Number(b.dataset.aiId));
    }));
  }

  function render() {
    if (!root || !root.dataset.ikBuilt) return;
    applyTheme();
    renderStats();
    renderFilters();
    renderFocusPanel();
    renderJalurBar();
    renderCatDash();
    renderViewModeToggle();
    renderList();
  }

  // ── Public Module API ──
  function init(config) {
    hooks = config;
    root = document.getElementById('ideku-overlay');
    if (root) renderShell();
  }

  function open() {
    if (!root) root = document.getElementById('ideku-overlay');
    if (!root) return;
    renderShell();
    syncFromCuciMoney();
    root.classList.add('open');
    render();
  }

  function close() {
    if (root) {
      closeMenu();
      root.classList.remove('open');
    }
  }

  function resetData() {
    const db = getDb();
    if (!db) return;
    db.ideku = defaultState();
    ui.filterType = 'Idea';
    ui.filterStatus = null;
    ui.filterCat = 'Semua';
    ui.filterJalur = null;
    persist();
    render();
  }

  function getSummary() {
    const ideas = state().ideas;
    const activeProcess = ideas.filter((i) => i.type === 'Idea' && i.status === 'Proses');
    const wishCount = ideas.filter((i) => i.type === 'Wishlist' && i.status !== 'Selesai').length;
    const focusTitle = activeProcess[0]?.title || (wishCount > 0 ? `${wishCount} wishlist aktif` : 'Ketuk untuk catat ide');
    const countText = activeProcess.length > 0
      ? `${activeProcess.length} Proses`
      : `${ideas.length} Ide`;
    return {
      countText,
      focusText: activeProcess[0]?.title ? `Fokus: ${activeProcess[0].title}` : focusTitle,
      total: ideas.length,
      prosesCount: activeProcess.length,
    };
  }

  function buildContextForAssistant() {
    const ideas = state().ideas;
    if (!ideas.length) return '';
    const fokus = ideas.filter((i) => i.type === 'Idea' && i.status === 'Proses').slice(0, 3);
    const latestIdeas = ideas.filter((i) => i.type === 'Idea').slice(0, 5);
    const wishlists = ideas.filter((i) => i.type === 'Wishlist').slice(0, 4);
    const lines = ['\n\n## Konteks IdeKu (Ide & Wishlist Tuan)'];
    if (fokus.length) {
      lines.push('Fokus aktif:');
      fokus.forEach((f) => lines.push(`- [id=${f.id}] [Fokus] ${f.title}${f.next ? ` (next: ${f.next})` : ''}`));
    }
    if (latestIdeas.length) {
      lines.push('Ide terbaru:');
      latestIdeas.forEach((i) => lines.push(`- [id=${i.id}] ${i.title} [${i.category} · ${i.priority} · ${i.status}]`));
    }
    if (wishlists.length) {
      lines.push('Wishlist:');
      wishlists.forEach((w) => lines.push(`- [id=${w.id}] ${w.title} (Rp ${Number(w.price || 0).toLocaleString('id-ID')} · ${w.status})`));
    }
    return lines.join('\n');
  }

  function resolveIdekuTarget(ideas, target) {
    const clean = String(target || '').trim();
    if (!clean) return null;
    if (/^\d+$/.test(clean)) {
      return ideas.find((i) => String(i.id) === clean) || null;
    }
    const q = clean.toLowerCase();
    const exact = ideas.filter((i) => String(i.title || '').toLowerCase() === q);
    if (exact.length === 1) return exact[0];
    if (exact.length > 1) return null;
    const partial = ideas.filter((i) => String(i.title || '').toLowerCase().includes(q));
    return partial.length === 1 ? partial[0] : null;
  }

  function normalizeIdekuStatus(raw) {
    const v = String(raw || '').trim().toLowerCase();
    if (['selesai', 'done', 'finish', 'finished', 'complete', 'completed'].includes(v)) return 'Selesai';
    if (['proses', 'process', 'aktif', 'fokus', 'in progress'].includes(v)) return 'Proses';
    if (['parkir', 'park', 'hold', 'tunda'].includes(v)) return 'Parkir';
    if (['belum', 'todo', 'pending', 'open'].includes(v)) return 'Belum';
    return '';
  }

  function normalizeIdekuPriority(raw) {
    const v = String(raw || '').trim().toLowerCase();
    if (['penting', 'tinggi', 'urgent', 'high'].includes(v)) return 'Penting';
    if (['kapan-kapan', 'kapan2', 'rendah', 'low'].includes(v)) return 'Kapan-kapan';
    if (['nanti', 'sedang', 'medium', 'normal'].includes(v)) return 'Nanti';
    return '';
  }

  function applyTagAdd(parts) {
    if (!parts || parts.length < 2) return null;
    const s = state();
    const first = (parts[0] || '').trim();
    const isWishlist = first.toLowerCase() === 'wishlist';
    const title = (isWishlist ? parts[1] : (first.toLowerCase() === 'idea' ? parts[1] : parts[0]))?.trim();
    if (!title) return null;

    if (isWishlist) {
      const price = Math.max(0, Number(String(parts[2] || '0').replace(/\D/g, '')) || 0);
      const category = (parts[3] || 'Belanja').trim() || 'Belanja';
      const priority = PRIORITIES.includes((parts[4] || '').trim()) ? parts[4].trim() : 'Nanti';
      const notes = (parts[5] || '').trim();
      const item = {
        id: Date.now() + Math.floor(Math.random() * 1000),
        title,
        notes,
        category,
        jalur: '',
        next: '',
        priority,
        status: 'Belum',
        type: 'Wishlist',
        price,
        image: 'bag',
      };
      if (!s.categories.includes(category)) s.categories.push(category);
      s.ideas.unshift(item);
      syncWishlistToCuciMoney(item);
      persist();
      if (root?.classList.contains('open')) render();
      return `Wishlist: ${title}`;
    } else {
      const offset = first.toLowerCase() === 'idea' ? 1 : 0;
      const category = (parts[offset + 1] || 'Personal').trim() || 'Personal';
      const priRaw = (parts[offset + 2] || '').trim();
      const priority = PRIORITIES.includes(priRaw) ? priRaw : 'Nanti';
      const notes = (parts[offset + 3] || '').trim();
      const jalur = (parts[offset + 4] || '').trim();
      const item = {
        id: Date.now() + Math.floor(Math.random() * 1000),
        title,
        notes,
        category,
        jalur,
        next: '',
        priority,
        status: 'Belum',
        type: 'Idea',
        price: 0,
        image: '',
      };
      if (!s.categories.includes(category)) s.categories.push(category);
      s.ideas.unshift(item);
      persist();
      if (root?.classList.contains('open')) render();
      return title;
    }
  }

  function applyTagStatus(cmd, parts) {
    const s = state();
    const item = resolveIdekuTarget(s.ideas, parts[0]);
    if (!item) return null;
    const rawSt = String(parts[1] || '').trim();
    const nextStatus = (!rawSt && (cmd === 'DONE' || cmd === 'SELESAI'))
      ? 'Selesai'
      : normalizeIdekuStatus(rawSt);
    if (!nextStatus) return null;
    item.status = nextStatus;
    if (item.type === 'Wishlist') syncWishlistToCuciMoney(item);
    persist();
    if (root?.classList.contains('open')) render();
    return `${item.title} -> ${item.status}`;
  }

  function applyTagEdit(parts) {
    const s = state();
    const item = resolveIdekuTarget(s.ideas, parts[0]);
    if (!item) return null;
    const nextTitle = String(parts[1] || '').trim();
    const nextCat = String(parts[2] || '').trim();
    const nextPri = normalizeIdekuPriority(parts[3]);
    const nextNotes = String(parts[4] || '').trim();
    const nextJalur = String(parts[5] || '').trim();
    const rawPrice = String(parts[6] || '').trim();
    const nextPrice = rawPrice ? Math.max(0, Number(rawPrice.replace(/\D/g, '')) || 0) : null;
    if (!nextTitle && !nextCat && !nextPri && !nextNotes && !nextJalur && nextPrice == null) return null;
    if (nextTitle) item.title = nextTitle;
    if (nextCat) {
      item.category = nextCat;
      if (!s.categories.includes(nextCat)) s.categories.push(nextCat);
    }
    if (nextPri) item.priority = nextPri;
    if (nextNotes) item.notes = nextNotes;
    if (nextJalur && item.type !== 'Wishlist') item.jalur = nextJalur;
    if (nextPrice != null && item.type === 'Wishlist') item.price = nextPrice;
    if (item.type === 'Wishlist') syncWishlistToCuciMoney(item);
    persist();
    if (root?.classList.contains('open')) render();
    return `Diubah "${item.title}"`;
  }

  function applyTagDelete(parts) {
    const s = state();
    const item = resolveIdekuTarget(s.ideas, parts[0]);
    if (!item) return null;
    s.ideas = s.ideas.filter((i) => i.id !== item.id);
    persist();
    if (root?.classList.contains('open')) render();
    return `Dihapus "${item.title}"`;
  }

  window.IdekuModule = {
    defaultState,
    normalizeState,
    init,
    open,
    close,
    render,
    resetData,
    getSummary,
    buildContextForAssistant,
    applyTagAdd,
    applyTagStatus,
    applyTagEdit,
    applyTagDelete,
  };
})();
