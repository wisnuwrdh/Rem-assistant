(function () {
  'use strict';

  // ── Navigation 3 Tab + Double-Tap Tab Asisten Toggle Chat ────────────
  const navBtns = document.querySelectorAll('#bottom-nav .nav-btn');
  const panes = {
    asisten: document.getElementById('tab-asisten'),
    apps: document.getElementById('tab-apps'),
    setelan: document.getElementById('tab-setelan'),
  };

  // ── DOM Refs ─────────────────────────────────────────────────────────
  const stagePanel = document.getElementById('stage-panel');
  const canvas = document.getElementById('live2d-canvas');
  const loadingBadge = document.getElementById('loading-badge');
  const idleBubble = document.getElementById('idle-bubble');
  const modelTag = document.getElementById('model-tag');
  const statusEl = modelTag.querySelector('.status');
  const messagesEl = document.getElementById('messages');
  const msgInput = document.getElementById('msg-input');
  const sendBtn = document.getElementById('send-btn');
  const toastEl = document.getElementById('toast');
  const handleArea = document.getElementById('sheet-handle-area');

  let lastAsistenTap = 0;
  let isChatOpen = true;
  let savedStageHeight = '';
  let activeTab = 'asisten';
  let isShellVisible = !document.hidden;

  function setChatOpen(open) {
    isChatOpen = open;
    if (!open) {
      savedStageHeight = stagePanel.style.height;
      msgInput.blur();
    } else {
      stagePanel.style.height = savedStageHeight;
    }
    panes.asisten.classList.toggle('chat-closed', !open);
    requestAnimationFrame(resizeStage);
  }

  navBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      const target = btn.dataset.tab;
      const wasActive = btn.classList.contains('active');
      if (target === 'asisten' && wasActive) {
        const now = Date.now();
        if (now - lastAsistenTap <= 350) {
          lastAsistenTap = 0;
          setChatOpen(!isChatOpen);
          return;
        }
        lastAsistenTap = now;
        return;
      }
      lastAsistenTap = 0;
      activeTab = target;
      navBtns.forEach((b) => b.classList.toggle('active', b === btn));
      Object.entries(panes).forEach(([k, el]) => el.classList.toggle('active', k === target));
      if (target === 'asisten') {
        requestAnimationFrame(resizeStage);
        resumeStageIfActive();
      } else {
        pauseStageAudioAndIdle();
      }
    });
  });

  // ── Draggable Split Resizer ──────────────────────────────────────────
  let dragging = false;
  let startY = 0;
  let startH = 0;
  const shellEl = document.getElementById('tab-content');
  const appShellEl = document.getElementById('shell');

  handleArea.addEventListener('pointerdown', (e) => {
    if (!isChatOpen) return;
    dragging = true;
    panes.asisten.classList.add('dragging');
    startY = e.clientY;
    startH = stagePanel.getBoundingClientRect().height;
    handleArea.setPointerCapture(e.pointerId);
  });
  handleArea.addEventListener('pointermove', (e) => {
    if (!dragging) return;
    const total = shellEl.getBoundingClientRect().height || 700;
    const nextH = Math.max(total * 0.34, Math.min(total * 0.70, startH + (e.clientY - startY)));
    stagePanel.style.height = nextH + 'px';
    resizeStage();
  });
  const endDrag = () => {
    dragging = false;
    panes.asisten.classList.remove('dragging');
  };
  handleArea.addEventListener('pointerup', endDrag);
  handleArea.addEventListener('pointercancel', endDrag);

  if (typeof ResizeObserver !== 'undefined') {
    new ResizeObserver(() => {
      const r = stagePanel.getBoundingClientRect();
      if (r.width === 0 || r.height === 0) {
        pauseStageAudioAndIdle();
      } else {
        resizeStage();
      }
    }).observe(stagePanel);
  }

  if (typeof IntersectionObserver !== 'undefined' && appShellEl) {
    new IntersectionObserver((entries) => {
      const visible = entries.some((entry) => entry.isIntersecting);
      isShellVisible = visible;
      if (!visible) {
        pauseStageAudioAndIdle();
      } else {
        resumeStageIfActive();
      }
    }, { threshold: 0.05 }).observe(appShellEl);
  }

  // ── Live2D Stage (menggunakan aset yang sama dengan Android) ─────────
  const app = new PIXI.Application({
    view: canvas,
    width: stagePanel.clientWidth || 390,
    height: stagePanel.clientHeight || 420,
    backgroundAlpha: 0,
    antialias: true,
    resolution: window.devicePixelRatio || 1,
    autoDensity: true,
    sharedTicker: false,
  });
  app.renderer.gl.pixelStorei(app.renderer.gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);

  let model = null;
  let currentExpr = 'none';
  let blinkTimer = 3, blinkState = 'open', blinkT = 0;
  let idleBreath = 0, idleHead = 0, idleAngleZ = 0;
  let cursorX = 0.5, cursorY = 0.5, smoothCX = 0.5, smoothCY = 0.5;
  let petActive = false, petIntensity = 0, petHairT = 0, petShown = false;
  let smoothMouth = 0, isSpeaking = false;

  let audioCtx = null, analyser = null, fftBuf = null;
  let ttsSources = [], ttsGen = 0;

  const P = {
    eyeL: 'PARAM_EYE_L_OPEN', eyeR: 'PARAM_EYE_R_OPEN',
    mouthY: 'PARAM_MOUTH_OPEN_Y', mouthF: 'PARAM_MOUTH_FORM',
    angleX: 'PARAM_ANGLE_X', angleY: 'PARAM_ANGLE_Y', angleZ: 'PARAM_ANGLE_Z',
    bodyX: 'PARAM_BODY_ANGLE_X', eyeX: 'PARAM_EYE_BALL_X', eyeY: 'PARAM_EYE_BALL_Y',
    breath: 'PARAM_BREATH', eyeLSmile: 'PARAM_EYE_L_SMILE', eyeRSmile: 'PARAM_EYE_R_SMILE',
  };

  const remFaceParams = {
    PARAM_MOUTH_FORM_01: { current: 0, target: 0, fadeIn: 0.06, fadeOut: 0.04 },
    PARAM_BROW_R_FORM: { current: 0, target: 0, fadeIn: 0.06, fadeOut: 0.04 },
    PARAM_BROW_L_FORM: { current: 0, target: 0, fadeIn: 0.06, fadeOut: 0.04 },
    PARAM_EYE_R_SMILE: { current: 0, target: 0, fadeIn: 0.05, fadeOut: 0.04 },
    PARAM_EYE_L_SMILE: { current: 0, target: 0, fadeIn: 0.05, fadeOut: 0.04 },
    PARAM_EYE_FORM: { current: 0, target: 0, fadeIn: 0.05, fadeOut: 0.04 },
    PARAM_CHEEK_01: { current: 0, target: 0, fadeIn: 0.04, fadeOut: 0.03 },
    PARAM_TEAR_01: { current: 0, target: 0, fadeIn: 0.03, fadeOut: 0.02 },
  };

  const REM_FACE_MAP = {
    none: { PARAM_MOUTH_FORM_01: 0, PARAM_BROW_R_FORM: 0, PARAM_BROW_L_FORM: 0, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 0, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    happy: { PARAM_MOUTH_FORM_01: 1, PARAM_BROW_R_FORM: 0, PARAM_BROW_L_FORM: 0, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 0, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    smile: { PARAM_MOUTH_FORM_01: 1, PARAM_BROW_R_FORM: 1, PARAM_BROW_L_FORM: 1, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 1, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    appeal: { PARAM_MOUTH_FORM_01: 1, PARAM_BROW_R_FORM: 0, PARAM_BROW_L_FORM: 0, PARAM_EYE_R_SMILE: 1, PARAM_EYE_L_SMILE: 1, PARAM_EYE_FORM: 0, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    proud: { PARAM_MOUTH_FORM_01: 0, PARAM_BROW_R_FORM: 0, PARAM_BROW_L_FORM: 0, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 0, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    shame: { PARAM_MOUTH_FORM_01: -0.8, PARAM_BROW_R_FORM: -0.8, PARAM_BROW_L_FORM: -0.8, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 1, PARAM_CHEEK_01: 1, PARAM_TEAR_01: 0 },
    sad: { PARAM_MOUTH_FORM_01: -0.8, PARAM_BROW_R_FORM: -0.5, PARAM_BROW_L_FORM: -0.5, PARAM_EYE_R_SMILE: -1, PARAM_EYE_L_SMILE: -1, PARAM_EYE_FORM: 1, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    cry: { PARAM_MOUTH_FORM_01: -0.8, PARAM_BROW_R_FORM: -0.5, PARAM_BROW_L_FORM: -0.5, PARAM_EYE_R_SMILE: -1, PARAM_EYE_L_SMILE: -1, PARAM_EYE_FORM: 1, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 1 },
    angry: { PARAM_MOUTH_FORM_01: -1, PARAM_BROW_R_FORM: -1, PARAM_BROW_L_FORM: -1, PARAM_EYE_R_SMILE: -0.7, PARAM_EYE_L_SMILE: -0.7, PARAM_EYE_FORM: -0.6, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    upset: { PARAM_MOUTH_FORM_01: -2, PARAM_BROW_R_FORM: -0.8, PARAM_BROW_L_FORM: -0.8, PARAM_EYE_R_SMILE: -0.5, PARAM_EYE_L_SMILE: -0.5, PARAM_EYE_FORM: 1, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    serious: { PARAM_MOUTH_FORM_01: -0.6, PARAM_BROW_R_FORM: -0.5, PARAM_BROW_L_FORM: -0.5, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: -1, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    surprise: { PARAM_MOUTH_FORM_01: -0.6, PARAM_BROW_R_FORM: -0.7, PARAM_BROW_L_FORM: -0.7, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 0, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    doubt: { PARAM_MOUTH_FORM_01: -0.8, PARAM_BROW_R_FORM: -0.5, PARAM_BROW_L_FORM: -0.5, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 0, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    puzzle: { PARAM_MOUTH_FORM_01: -0.5, PARAM_BROW_R_FORM: -0.6, PARAM_BROW_L_FORM: -0.6, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 1, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
    navi: { PARAM_MOUTH_FORM_01: 1, PARAM_BROW_R_FORM: 0.8, PARAM_BROW_L_FORM: 0.8, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 0, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
  };

  const REM_HAND_MAP = {
    happy: [{ PARAM_HAND_R_08: 1, PARAM_HAND_L_01: 1 }, { PARAM_HAND_R_08: 1, PARAM_HAND_L_08: 1 }],
    sad: [{ PARAM_HAND_R_08: 1, PARAM_HAND_L_08: 1 }],
    cry: [{ PARAM_HAND_R_08: 1, PARAM_HAND_L_04: 1 }],
    angry: [{ PARAM_HAND_R_07: 1, PARAM_HAND_L_07: 1 }],
    surprise: [{ PARAM_HAND_R_04: 1, PARAM_HAND_L_04: 1 }],
    shame: [{ PARAM_HAND_R_05: 1, PARAM_HAND_L_05: 1 }],
    upset: [{ PARAM_HAND_R_05: 1, PARAM_HAND_L_07: 1 }],
    serious: [{ PARAM_HAND_R_04: 1, PARAM_HAND_L_04: 1 }],
    proud: [{ PARAM_HAND_R_05: 1, PARAM_HAND_L_01: 1 }],
    doubt: [{ PARAM_HAND_R_07: 1, PARAM_HAND_L_07: 1 }],
    puzzle: [{ PARAM_HAND_R_08: 1, PARAM_HAND_L_01: 1 }],
    appeal: [{ PARAM_HAND_R_04: 1, PARAM_HAND_L_04: 1 }, { PARAM_HAND_R_05: 1, PARAM_HAND_L_05: 1 }],
    smile: [{ PARAM_HAND_R_05: 1, PARAM_HAND_L_07: 1 }],
    idle: [{ PARAM_HAND_R_05: 1, PARAM_HAND_L_07: 1 }],
  };
  const ALL_HAND = Array.from({ length: 11 }, (_, i) => `PARAM_HAND_R_${String(i + 1).padStart(2, '0')}`)
    .concat(Array.from({ length: 11 }, (_, i) => `PARAM_HAND_L_${String(i + 1).padStart(2, '0')}`));
  const REM_MOTION_MAP = { happy: 2, sad: 2, cry: 1, angry: 2, surprise: 1, shame: 2, upset: 1, serious: 2, proud: 1, doubt: 1, puzzle: 3, appeal: 3, smile: 1, idle: 5 };
  let curHand = REM_HAND_MAP.idle[0];

  const IDLE_LINES = [
    { text: '💙 Tuan sedang apa?', audio: 'idle_1.mp3' },
    { text: '💙 Rem di sini, Tuan.', audio: 'idle_2.mp3' },
    { text: '💙 Sudah makan belum, Tuan?', audio: 'idle_3.mp3' },
    { text: '💙 Kalau perlu sesuatu, bilang Rem saja.', audio: 'idle_4.mp3' },
    { text: '💙 Tuan tidak kelelahan?', audio: 'idle_5.mp3' },
    { text: '💙 Rem senang bisa menemani Tuan.', audio: 'idle_6.mp3' },
    { text: '💙 Tuan tampak sibuk… ada yang bisa Rem bantu?', audio: 'idle_7.mp3' },
  ];
  const HEAD_LINES = [
    { text: '💙 T-Tuan…?!', audio: 'head_1.mp3' },
    { text: '💙 Rem jadi salah tingkah…', audio: 'head_2.mp3' },
    { text: '💙 A… apa yang Tuan lakukan…', audio: 'head_3.mp3' },
    { text: '💙 T-Tuan, ini tidak pantas…!', audio: 'head_4.mp3' },
    { text: '💙 Rem… tidak keberatan, tapi…', audio: 'head_5.mp3' },
  ];
  const PET_LINES = [
    { text: '💙 T-Tuan…', audio: 'pet_1.mp3' },
    { text: '💙 Rem… tidak keberatan.', audio: 'pet_2.mp3' },
    { text: '💙 Ini membuat Rem bahagia.', audio: 'pet_3.mp3' },
    { text: '💙 Tuan sangat baik…', audio: 'pet_4.mp3' },
    { text: '💙 Rem tersenyum tanpa sadar.', audio: 'pet_5.mp3' },
  ];
  const BODY_LINES = [
    { text: '💙 Ada yang bisa Rem bantu?', audio: 'body_1.mp3' },
    { text: '💙 Rem siap, Tuan.', audio: 'body_2.mp3' },
    { text: '💙 Tuan mencari Rem?', audio: 'body_3.mp3' },
    { text: '💙 Ada sesuatu yang ingin Tuan sampaikan?', audio: 'body_4.mp3' },
  ];
  const LEG_LINES = [
    { text: '💙 T-Tuan… ke mana Tuan melihat?', audio: 'leg_1.mp3' },
    { text: '💙 Rem harap Tuan menjaga pandangan.', audio: 'leg_2.mp3' },
    { text: '💙 Rem… jadi malu.', audio: 'leg_3.mp3' },
  ];
  const pickRandom = (arr) => arr[Math.floor(Math.random() * arr.length)];

  function setCoreParam(name, val) {
    try {
      const core = model?.internalModel?.coreModel;
      if (!core) return;
      const idx = core.getParameterIndex(name);
      if (idx >= 0) core.setParameterValueByIndex(idx, val, 1.0);
    } catch (_) {}
  }

  function resizeStage() {
    const w = stagePanel.clientWidth || 390;
    const h = stagePanel.clientHeight || 420;
    app.renderer.resize(w, h);
    if (model) {
      const mh = model.internalModel?.height || 800;
      model.scale.set((h * 1.40) / mh);
      model.anchor.set(0.5, 1);
      model.x = w * 0.5;
      model.y = h * 1.44;
    }
  }
  window.addEventListener('resize', resizeStage);

  function setExpression(name) {
    currentExpr = name || 'none';
    const motionName = currentExpr === 'none' ? 'idle' : currentExpr;
    const count = REM_MOTION_MAP[motionName] || 0;
    if (count && model) {
      const idx = Math.floor(Math.random() * count);
      curHand = (REM_HAND_MAP[motionName] || REM_HAND_MAP.idle)[idx % (REM_HAND_MAP[motionName]?.length || 1)] || {};
      try { model.motion(motionName, idx, 2); } catch (_) {}
    }
    const fMap = REM_FACE_MAP[currentExpr] || REM_FACE_MAP.none;
    for (const [k, s] of Object.entries(remFaceParams)) {
      s.target = fMap[k] !== undefined ? fMap[k] : 0;
    }
    statusEl.textContent = currentExpr === 'none' ? '· idle' : `· ${currentExpr}`;
  }

  function onTick(delta) {
    if (!model) return;
    const dt = delta / 60;
    blinkTimer -= dt;
    if (blinkState === 'open' && blinkTimer <= 0) { blinkState = 'closing'; blinkT = 0; }
    let eyeVal = 1;
    if (blinkState === 'closing') { blinkT += dt / 0.08; eyeVal = Math.max(0, 1 - blinkT); if (blinkT >= 1) { blinkState = 'closed'; blinkT = 0; } }
    else if (blinkState === 'closed') { blinkT += dt / 0.05; eyeVal = 0; if (blinkT >= 1) { blinkState = 'opening'; blinkT = 0; } }
    else if (blinkState === 'opening') { blinkT += dt / 0.10; eyeVal = Math.min(1, blinkT); if (blinkT >= 1) { blinkState = 'open'; blinkTimer = 2.5 + Math.random() * 3.5; } }
    setCoreParam(P.eyeL, eyeVal); setCoreParam(P.eyeR, eyeVal);

    idleBreath += dt; idleHead += dt;
    idleAngleZ = Math.sin(idleHead * (Math.PI * 2 / 7)) * 4;
    setCoreParam(P.breath, Math.sin(idleBreath * (Math.PI * 2 / 4)) * 0.4);
    setCoreParam(P.angleZ, idleAngleZ);

    smoothCX += (cursorX - smoothCX) * 3.5 * dt;
    smoothCY += (cursorY - smoothCY) * 3.5 * dt;
    const ax = (smoothCX - 0.5) * 40;
    setCoreParam(P.angleX, ax);
    setCoreParam(P.angleY, (0.5 - smoothCY) * 30);
    setCoreParam(P.eyeX, (smoothCX - 0.5) * 2);
    setCoreParam(P.eyeY, (0.5 - smoothCY) * 1.2);
    setCoreParam(P.bodyX, ax * 0.12);

    let targetMouth = 0;
    if (isSpeaking && analyser && fftBuf) {
      analyser.getByteFrequencyData(fftBuf);
      let sum = 0;
      for (let i = 2; i <= 18; i++) sum += fftBuf[i];
      targetMouth = Math.min(1, (sum / 17 / 255) * 2.2);
    }
    smoothMouth += (targetMouth - smoothMouth) * (targetMouth > smoothMouth ? 0.35 : 0.12);
    setCoreParam(P.mouthY, smoothMouth);
    setCoreParam(P.mouthF, isSpeaking ? 0.3 : 0);

    for (const [k, s] of Object.entries(remFaceParams)) {
      if (s.current !== s.target) {
        const sp = Math.abs(s.target) > Math.abs(s.current) ? s.fadeIn : s.fadeOut;
        s.current += (s.target - s.current) * sp;
        if (Math.abs(s.current - s.target) < 0.001) s.current = s.target;
      }
      setCoreParam(k, s.current);
    }

    petIntensity += ((petActive ? 1 : 0) - petIntensity) * (petActive ? 4 * dt : 2 * dt);
    if (petIntensity > 0.001) {
      if (petActive) petHairT += dt;
      if (petActive && !petShown && petHairT > 1.5) {
        petShown = true;
        didPet = true;
        resetIdleTimer();
        triggerBubbleLine(pickRandom(PET_LINES), 'happy', 3000);
      }
      setCoreParam(P.eyeLSmile, petIntensity * 0.9);
      setCoreParam(P.eyeRSmile, petIntensity * 0.9);
    } else {
      petShown = false; petHairT = 0;
    }
  }

  function tickRemHand() {
    if (!model) return;
    ALL_HAND.forEach((p) => setCoreParam(p, 0));
    Object.entries(curHand).forEach(([p, v]) => setCoreParam(p, v));
    setCoreParam('PARAM_ARM_R_ORDER', 1);
    setCoreParam('PARAM_ARM_L_ORDER', 1);
  }

  let bubbleTimer = null;
  let idleTimer = null;
  let lastBubbleAt = 0;
  let exprRevertTimer = null;
  const clipCache = new Map();

  function isStageActiveAndVisible() {
    if (document.hidden || document.visibilityState === 'hidden') return false;
    if (!isShellVisible) return false;
    if (activeTab !== 'asisten') return false;
    if (appShellEl && getComputedStyle(appShellEl).visibility === 'hidden') return false;
    const r = stagePanel.getBoundingClientRect();
    return r.width > 0 && r.height > 0;
  }

  function stopAllAudio() {
    ttsGen++;
    ttsSources.forEach((s) => { try { s.stop(); } catch (_) {} });
    ttsSources = [];
    isSpeaking = false;
    modelTag.classList.remove('speaking');
    statusEl.textContent = currentExpr === 'none' ? '· idle' : `· ${currentExpr}`;
  }

  function pauseStageAudioAndIdle() {
    clearTimeout(idleTimer);
    idleTimer = null;
    clearTimeout(bubbleTimer);
    idleBubble.classList.remove('show');
    stopAllAudio();
    if (audioCtx && audioCtx.state === 'running') {
      audioCtx.suspend().catch(() => {});
    }
  }

  function resumeStageIfActive() {
    if (!isStageActiveAndVisible()) return;
    resetIdleTimer();
  }

  document.addEventListener('visibilitychange', () => {
    if (document.hidden || document.visibilityState === 'hidden') {
      pauseStageAudioAndIdle();
    } else {
      resumeStageIfActive();
    }
  });
  window.addEventListener('pagehide', pauseStageAudioAndIdle);
  window.addEventListener('beforeunload', pauseStageAudioAndIdle);
  document.addEventListener('freeze', pauseStageAudioAndIdle);

  async function playClip(filename) {
    if (!filename || isSpeaking || isThinking || !isStageActiveAndVisible()) return;
    ensureAudio();
    if (audioCtx.state === 'suspended') {
      try { await audioCtx.resume(); } catch (_) { return; }
    }
    if (audioCtx.state !== 'running' || !isStageActiveAndVisible()) return;

    const gen = ++ttsGen;
    try {
      let audioBuf = clipCache.get(filename);
      if (!audioBuf) {
        const resp = await fetch(`/live2d/characters/rem/audio/${encodeURIComponent(filename)}`);
        if (!resp.ok || gen !== ttsGen || !isStageActiveAndVisible()) return;
        const arr = await resp.arrayBuffer();
        if (gen !== ttsGen || !isStageActiveAndVisible()) return;
        audioBuf = await audioCtx.decodeAudioData(arr);
        clipCache.set(filename, audioBuf);
      }
      if (gen !== ttsGen || !isStageActiveAndVisible()) return;
      const src = audioCtx.createBufferSource();
      src.buffer = audioBuf;
      src.connect(analyser);
      isSpeaking = true;
      modelTag.classList.add('speaking');
      statusEl.textContent = '· berbicara…';
      src.onended = () => {
        if (gen !== ttsGen) return;
        isSpeaking = false;
        modelTag.classList.remove('speaking');
        statusEl.textContent = currentExpr === 'none' ? '· idle' : `· ${currentExpr}`;
      };
      src.start();
      ttsSources.push(src);
    } catch (_) {}
  }

  function showBubble(txt, durationMs = 3500) {
    idleBubble.textContent = txt;
    idleBubble.classList.add('show');
    clearTimeout(bubbleTimer);
    bubbleTimer = setTimeout(() => idleBubble.classList.remove('show'), durationMs);
  }

  function triggerBubbleLine(line, exprName = null, durationMs = 3500) {
    if (!line || isThinking || isSpeaking || !isStageActiveAndVisible()) return;
    showBubble(line.text, durationMs);
    if (line.audio) playClip(line.audio);
    if (exprName) {
      setExpression(exprName);
      clearTimeout(exprRevertTimer);
      exprRevertTimer = setTimeout(() => {
        if (currentExpr === exprName && !isThinking) setExpression('none');
      }, 6000);
    }
  }

  function resetIdleTimer() {
    clearTimeout(idleTimer);
    idleTimer = null;
    if (!isStageActiveAndVisible()) return;
    const scheduleNext = (delayMs) => {
      idleTimer = setTimeout(() => {
        if (!isStageActiveAndVisible()) return;
        if (!isThinking && !isSpeaking) {
          setExpression('none');
          triggerBubbleLine(pickRandom(IDLE_LINES), null, 5000);
        }
        scheduleNext(25000 + Math.random() * 15000);
      }, delayMs);
    };
    scheduleNext(30000);
  }
  resetIdleTimer();

  let tapStartX = 0, tapStartY = 0, tapMoved = false, didPet = false;
  const isHead = (y, x) => y < 0.48 && x > 0.12 && x < 0.88;
  const isBody = (y, x) => y >= 0.48 && y < 0.75 && x > 0.15 && x < 0.85;
  const isLeg = (y, x) => y >= 0.75 && y < 0.95 && x > 0.20 && x < 0.80;

  canvas.addEventListener('pointerdown', (e) => {
    ensureAudio();
    if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume().catch(() => {});
    const r = canvas.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width;
    const y = (e.clientY - r.top) / r.height;
    cursorX = Math.max(0, Math.min(1, x));
    cursorY = Math.max(0, Math.min(1, y));
    if (isHead(y, x)) petActive = true;
    tapStartX = e.clientX;
    tapStartY = e.clientY;
    tapMoved = false;
    didPet = false;
  });
  canvas.addEventListener('pointermove', (e) => {
    const r = canvas.getBoundingClientRect();
    cursorX = Math.max(0, Math.min(1, (e.clientX - r.left) / r.width));
    cursorY = Math.max(0, Math.min(1, (e.clientY - r.top) / r.height));
    if (Math.abs(e.clientX - tapStartX) > 10 || Math.abs(e.clientY - tapStartY) > 10) {
      tapMoved = true;
    }
  });
  canvas.addEventListener('pointerup', (e) => {
    petActive = false;
    if (blinkState === 'open') blinkTimer = 0;
    if (tapMoved || didPet) return;
    const now = Date.now();
    if (now - lastBubbleAt < 4000) return;
    lastBubbleAt = now;
    resetIdleTimer();

    const r = canvas.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width;
    const y = (e.clientY - r.top) / r.height;
    if (isHead(y, x)) triggerBubbleLine(pickRandom(HEAD_LINES), 'shame', 3500);
    else if (isBody(y, x)) triggerBubbleLine(pickRandom(BODY_LINES), 'surprise', 3000);
    else if (isLeg(y, x)) triggerBubbleLine(pickRandom(LEG_LINES), 'appeal', 3000);
  });
  canvas.addEventListener('pointercancel', () => { petActive = false; });

  // ── Persistent Local Chat History ────────────────────────────────────
  const STORAGE_KEY = 'rem_assistant_history_v1';
  let chatHistory = [];
  try { chatHistory = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]'); } catch (_) {}
  if (!chatHistory.length) {
    chatHistory = [{ role: 'assistant', content: 'Selamat datang. Rem adalah pelayan Tuan. Ada yang bisa Rem bantu? 💙' }];
    localStorage.setItem(STORAGE_KEY, JSON.stringify(chatHistory));
  }

  function renderBubble(role, text) {
    const row = document.createElement('div');
    row.className = `msg-row ${role}`;
    if (role === 'assistant') {
      const av = document.createElement('div');
      av.className = 'rem-avatar';
      av.textContent = 'R';
      row.appendChild(av);
    }
    const b = document.createElement('div');
    b.className = 'bubble';
    b.textContent = text;
    row.appendChild(b);
    messagesEl.appendChild(row);
    messagesEl.scrollTop = messagesEl.scrollHeight;
    return b;
  }
  chatHistory.forEach((m) => renderBubble(m.role, m.content));

  // ── TTS Player (/api/tts) ────────────────────────────────────────────
  function ensureAudio() {
    if (audioCtx) return;
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    analyser = audioCtx.createAnalyser();
    analyser.fftSize = 256;
    fftBuf = new Uint8Array(analyser.frequencyBinCount);
    analyser.connect(audioCtx.destination);
  }

  async function playTTS(text, isJapanese) {
    if (!text || !isStageActiveAndVisible()) return;
    ensureAudio();
    if (audioCtx.state === 'suspended') await audioCtx.resume().catch(() => {});
    if (audioCtx.state !== 'running' || !isStageActiveAndVisible()) return;
    ttsGen++;
    const gen = ttsGen;
    ttsSources.forEach((s) => { try { s.stop(); } catch (_) {} });
    ttsSources = [];

    try {
      statusEl.textContent = '· berbicara…';
      const r = await fetch('/api/tts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: text.slice(0, 500), translated: isJapanese }),
      });
      if (!r.ok || gen !== ttsGen || !isStageActiveAndVisible()) throw new Error('TTS unavailable');
      const buf = await audioCtx.decodeAudioData(await r.arrayBuffer());
      if (gen !== ttsGen || !isStageActiveAndVisible()) return;
      const src = audioCtx.createBufferSource();
      src.buffer = buf;
      src.connect(analyser);
      isSpeaking = true;
      modelTag.classList.add('speaking');
      src.onended = () => {
        if (gen !== ttsGen) return;
        isSpeaking = false;
        modelTag.classList.remove('speaking');
        statusEl.textContent = '· idle';
      };
      src.start();
      ttsSources.push(src);
    } catch (_) {
      isSpeaking = false;
      modelTag.classList.remove('speaking');
      statusEl.textContent = '· idle';
    }
  }

  // ── Phase 1: Personal Memory Store (ceminan SQLite Room di Preview) ─
  const MEM_KEY = 'rem_user_memories_v1';
  const SUM_KEY = 'rem_chat_summaries_v1';
  const AUTO_LEARN_KEY = 'rem_auto_learn_v1';
  let userMemories = [];
  let chatSummaries = [];
  let autoLearnMemory = localStorage.getItem(AUTO_LEARN_KEY) !== 'false';
  let selectedMemCat = 'preferensi';

  try { userMemories = JSON.parse(localStorage.getItem(MEM_KEY) || '[]'); } catch (_) {}
  try { chatSummaries = JSON.parse(localStorage.getItem(SUM_KEY) || '[]'); } catch (_) {}

  const memCountSub = document.getElementById('mem-count-sub');
  const memAutoToggle = document.getElementById('mem-auto-toggle');
  const memCatRow = document.getElementById('mem-cat-row');
  const memKeyInput = document.getElementById('mem-key-input');
  const memValInput = document.getElementById('mem-val-input');
  const memAddBtn = document.getElementById('mem-add-btn');
  const memListEl = document.getElementById('mem-list');
  const memClearBtn = document.getElementById('mem-clear-btn');
  const resetAsistenBtn = document.getElementById('reset-asisten-btn');

  function saveMemories() {
    localStorage.setItem(MEM_KEY, JSON.stringify(userMemories));
    localStorage.setItem(SUM_KEY, JSON.stringify(chatSummaries));
    renderMemoriesUI();
  }

  function upsertMemory(category, factKey, factValue, confidence = 0.85) {
    const key = String(factKey || '').trim().toLowerCase().replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 48);
    const val = String(factValue || '').trim().slice(0, 180);
    if (!key || !val) return;
    const now = Date.now();
    const idx = userMemories.findIndex((m) => m.factKey === key);
    if (idx >= 0) {
      userMemories[idx] = { ...userMemories[idx], category, factValue: val, confidence, updatedAt: now };
    } else {
      userMemories.unshift({ id: now + Math.random(), category, factKey: key, factValue: val, confidence, updatedAt: now });
    }
    saveMemories();
  }

  function renderMemoriesUI(learningNow = false) {
    if (memCountSub) {
      memCountSub.textContent = learningNow ? 'Menganalisis…' : `${userMemories.length} fakta • Lokal`;
    }
    if (memAutoToggle) memAutoToggle.classList.toggle('on', autoLearnMemory);
    if (!memListEl) return;
    memListEl.innerHTML = '';
    if (!userMemories.length) {
      const empty = document.createElement('p');
      empty.style.cssText = 'font-size:11.5px; color:var(--subtle); line-height:1.5;';
      empty.textContent = 'Belum ada fakta personal tersimpan. Ngobrol dengan Rem atau tambahkan manual di atas.';
      memListEl.appendChild(empty);
      if (memClearBtn) memClearBtn.style.display = 'none';
      return;
    }
    if (memClearBtn) memClearBtn.style.display = 'block';
    userMemories.forEach((m) => {
      const row = document.createElement('div');
      row.className = 'mem-item';
      const info = document.createElement('div');
      info.style.minWidth = '0';
      const meta = document.createElement('div');
      meta.style.cssText = 'font-size:10px; color:var(--subtle); margin-bottom:2px;';
      const catSpan = document.createElement('span');
      catSpan.style.cssText = 'color:var(--accent); font-weight:600; text-transform:uppercase; margin-right:6px;';
      catSpan.textContent = m.category;
      const keySpan = document.createElement('span');
      keySpan.textContent = `• ${m.factKey}`;
      meta.append(catSpan, keySpan);
      const valEl = document.createElement('div');
      valEl.style.cssText = 'font-size:12.5px; color:var(--text); word-break:break-word;';
      valEl.textContent = m.factValue;
      info.append(meta, valEl);

      const del = document.createElement('button');
      del.type = 'button';
      del.className = 'mem-del-btn';
      del.textContent = '×';
      del.title = 'Hapus memori';
      del.addEventListener('click', () => {
        userMemories = userMemories.filter((item) => item.factKey !== m.factKey);
        saveMemories();
      });

      row.append(info, del);
      memListEl.appendChild(row);
    });
  }
  renderMemoriesUI();

  if (memAutoToggle) {
    memAutoToggle.addEventListener('click', () => {
      autoLearnMemory = !autoLearnMemory;
      localStorage.setItem(AUTO_LEARN_KEY, String(autoLearnMemory));
      renderMemoriesUI();
    });
  }
  if (memCatRow) {
    memCatRow.querySelectorAll('.mem-cat-chip').forEach((chip) => {
      chip.addEventListener('click', () => {
        selectedMemCat = chip.dataset.cat;
        memCatRow.querySelectorAll('.mem-cat-chip').forEach((c) => c.classList.toggle('active', c === chip));
      });
    });
  }
  if (memAddBtn) {
    memAddBtn.addEventListener('click', () => {
      const val = (memValInput?.value || '').trim();
      if (!val) return;
      const rawKey = (memKeyInput?.value || '').trim();
      upsertMemory(selectedMemCat, rawKey || `catatan_${Date.now()}`, val, 1.0);
      if (memKeyInput) memKeyInput.value = '';
      if (memValInput) memValInput.value = '';
    });
  }
  if (memClearBtn) {
    memClearBtn.addEventListener('click', () => {
      userMemories = [];
      chatSummaries = [];
      saveMemories();
    });
  }
  if (resetAsistenBtn) {
    resetAsistenBtn.addEventListener('click', () => {
      stopAllAudio();
      userMemories = [];
      chatSummaries = [];
      saveMemories();
      chatHistory = [{ role: 'assistant', content: 'Selamat datang. Rem adalah pelayan Tuan. Ada yang bisa Rem bantu? 💙' }];
      localStorage.setItem(STORAGE_KEY, JSON.stringify(chatHistory));
      messagesEl.innerHTML = '';
      chatHistory.forEach((m) => renderBubble(m.role, m.content));
    });
  }

  function buildPersonalizedSystemPrompt() {
    const kronikRules =
      '\n\n## Integrasi Jurnal Kronik\n' +
      'Kamu terhubung dengan buku jurnal pribadi Tuan yang bernama **Kronik**.\n' +
      '- Jika Tuan bercerita panjang tentang hari/perasaannya atau meminta dicatat ke jurnal/Kronik, sisipkan tag tersembunyi di akhir jawaban (sebelum [EXPR:...]):\n' +
      '  [KRONIK:ADD|judul_singkat|isi_jurnal_natural|mood_opsional]\n' +
      '  Pilihan mood valid: Fokus, Lelah, Terinspirasi, Berat, Stabil, Semangat, Kabur.\n' +
      '- Jika Tuan meminta membuka buku Kronik / membaca catatannya, sisipkan: [OPEN:kronik]';

    const linkkuRules =
      '\n\n## Integrasi Bookmark LinkKu\n' +
      'Kamu juga terhubung dengan pengelola bookmark **LinkKu** milik Tuan (hanya bookmark publik).\n' +
      '- Jika Tuan meminta menyimpan link/URL atau artikel/video penting ke LinkKu, sisipkan tag tersembunyi di akhir jawaban (sebelum [EXPR:...]):\n' +
      '  [LINKKU:ADD|url_lengkap|judul_opsional|catatan_opsional|kategori_opsional|tag1,tag2]\n' +
      '- Jika Tuan meminta membuka LinkKu atau melihat daftar linknya, sisipkan: [OPEN:linkku]';

    const cuciRules =
      '\n\n## Integrasi Keuangan CuciMoney+\n' +
      'Kamu juga terhubung dengan pencatat keuangan **CuciMoney+** milik Tuan.\n' +
      '- Jika Tuan menyebut baru mengeluarkan uang, jajan, beli sesuatu, bayar, atau menerima pemasukan/gaji/transfer masuk, sisipkan tag tersembunyi di akhir jawaban (sebelum [EXPR:...]):\n' +
      '  [CUCI:ADD|out_atau_in|nominal_angka|judul_singkat|kategori_opsional|akun_opsional]\n' +
      '  Contoh keluar: [CUCI:ADD|out|25000|Kopi Susu|Makan & Minum|Tunai]\n' +
      '  Contoh masuk: [CUCI:ADD|in|500000|Bonus Proyek|Bonus|]\n' +
      '- Jika Tuan meminta membuka CuciMoney+ / dompet / catatan keuangannya, sisipkan: [OPEN:cucimoney]';

    const idekuRules =
      '\n\n## Integrasi Ide & Wishlist IdeKu\n' +
      'Kamu juga terhubung dengan pengelola ide & wishlist **IdeKu** milik Tuan.\n' +
      '- Jika Tuan menyampaikan ide proyek/aplikasi/konten atau meminta mencatat ide baru, sisipkan tag tersembunyi di akhir jawaban (sebelum [EXPR:...]):\n' +
      '  [IDEKU:ADD|Idea|judul_ide|kategori|Penting_atau_Nanti_atau_Kapan-kapan|catatan_singkat|jalur_opsional]\n' +
      '- Jika Tuan menyebut barang impian/ingin beli suatu barang nanti (wishlist), sisipkan:\n' +
      '  [IDEKU:ADD|Wishlist|nama_barang|harga_angka|Belanja|Penting_atau_Nanti_atau_Kapan-kapan|catatan_singkat]\n' +
      '- Jika Tuan meminta membuka IdeKu atau melihat daftar ide/wishlistnya, sisipkan: [OPEN:ideku]';

    const dailyosRules =
      '\n\n## Integrasi Habit & To-Do Daily OS\n' +
      'Kamu juga terhubung dengan **Daily OS** (pelacak habit & to-do harian) milik Tuan.\n' +
      '- Jika Tuan ingin menambah habit baru, sisipkan tag tersembunyi di akhir jawaban (sebelum [EXPR:...]):\n' +
      '  [DAILYOS:HABIT|nama_habit|kategori_opsional|ikon_opsional]\n' +
      '- Jika Tuan meminta menambah to-do / tugas baru, sisipkan:\n' +
      '  [DAILYOS:TODO|judul_tugas|urgent_atau_soon_atau_normal|YYYY-MM-DD_opsional]\n' +
      '- Jika Tuan melapor sudah menyelesaikan habit atau to-do tertentu, sisipkan:\n' +
      '  [DAILYOS:CHECK|kata_kunci_nama_habit_atau_tugas]\n' +
      '- Jika Tuan meminta membuka Daily OS / melihat habit atau to-do nya, sisipkan: [OPEN:dailyos]';

    let block = remSystemPrompt + kronikRules + linkkuRules + cuciRules + idekuRules + dailyosRules;
    if (userMemories.length) {
      block += '\n\n## Memori Personal Tentang Tuan\n' +
        userMemories.slice(0, 20).map((m) => `- [${m.category}] ${m.factKey}: ${m.factValue}`).join('\n');
    }
    if (chatSummaries.length) {
      block += '\n\n## Ringkasan Sesi Sebelumnya\n' +
        chatSummaries.slice(0, 3).map((s) => `- ${s}`).join('\n');
    }
    block += buildKronikContextForAssistant();
    block += buildLinkkuContextForAssistant();
    block += window.CuciMoneyModule?.buildContextForAssistant?.() || '';
    block += window.IdekuModule?.buildContextForAssistant?.() || '';
    block += window.DailyOsModule?.buildContextForAssistant?.() || '';
    block += '\nGunakan memori & konteks Kronik / LinkKu / CuciMoney+ / IdeKu / Daily OS di atas secara natural tanpa menyebut kata database atau log.';
    return block;
  }

  async function extractMemoriesInBackground() {
    if (!autoLearnMemory) return;
    renderMemoriesUI(true);
    try {
      const res = await fetch('/api/memory/extract', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages: chatHistory.slice(-6),
          existingFacts: userMemories.slice(0, 25).map((m) => ({ factKey: m.factKey, factValue: m.factValue })),
        }),
      });
      if (!res.ok) return;
      const data = await res.json();
      if (Array.isArray(data?.facts)) {
        data.facts.forEach((f) => upsertMemory(f.category || 'catatan', f.factKey, f.factValue, f.confidence || 0.8));
      }
      if (typeof data?.summary === 'string' && data.summary.trim().length > 8) {
        const s = data.summary.trim();
        if (chatSummaries[0] !== s) {
          chatSummaries.unshift(s);
          chatSummaries = chatSummaries.slice(0, 5);
          saveMemories();
        }
      }
    } catch (_) {
    } finally {
      renderMemoriesUI(false);
    }
  }

  // ── Unified EcosystemKu Store & Kronik (Jurnal & Legenda) Module ───────
  const ECO_DB_KEY = 'ecosystemku_db_v1';
  const $ = (id) => document.getElementById(id);

  const MOOD_LIST = [
    {
      status: 'Fokus',
      svgKey: 'sword',
      svg: `<svg viewBox="0 0 24 24" fill="none" stroke="#c9a84c" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20L20 4M4 20l4-1 1-4M20 4l-4 1-1 4"/><path d="M9 15l6-6"/></svg>`,
    },
    {
      status: 'Lelah',
      svgKey: 'shield-crack',
      svg: `<svg viewBox="0 0 24 24" fill="none" stroke="#c9a84c" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3L4 7v6c0 4 3.5 7.5 8 9 4.5-1.5 8-5 8-9V7l-8-4z"/><path d="M10 10l2 3 2-2"/></svg>`,
    },
    {
      status: 'Terinspirasi',
      svgKey: 'star-glow',
      svg: `<svg viewBox="0 0 24 24" fill="none" stroke="#c9a84c" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><polygon points="12,2 15.1,8.3 22,9.3 17,14.1 18.2,21 12,17.8 5.8,21 7,14.1 2,9.3 8.9,8.3"/><line x1="12" y1="2" x2="12" y2="0.5"/><line x1="20" y1="4" x2="21.5" y2="2.5"/><line x1="4" y1="4" x2="2.5" y2="2.5"/></svg>`,
    },
    {
      status: 'Berat',
      svgKey: 'cloud-dark',
      svg: `<svg viewBox="0 0 24 24" fill="none" stroke="#c9a84c" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M18 10c0-3.3-2.7-6-6-6a6 6 0 00-5.9 5A4 4 0 006 17h12a4 4 0 000-8z"/><line x1="9" y1="20" x2="9" y2="22"/><line x1="12" y1="20" x2="12" y2="22"/><line x1="15" y1="20" x2="15" y2="22"/></svg>`,
    },
    {
      status: 'Stabil',
      svgKey: 'shield',
      svg: `<svg viewBox="0 0 24 24" fill="none" stroke="#c9a84c" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3L4 7v6c0 4 3.5 7.5 8 9 4.5-1.5 8-5 8-9V7l-8-4z"/><polyline points="9,12 11,14 15,10"/></svg>`,
    },
    {
      status: 'Semangat',
      svgKey: 'bolt',
      svg: `<svg viewBox="0 0 24 24" fill="none" stroke="#c9a84c" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="13,2 3,14 12,14 11,22 21,10 12,10"/></svg>`,
    },
    {
      status: 'Kabur',
      svgKey: 'spiral',
      svg: `<svg viewBox="0 0 24 24" fill="none" stroke="#c9a84c" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M12 12c0 0 2-2 2-4a2 2 0 00-4 0c0 3 3 5 3 8a3 3 0 01-6 0c0-4 4-7 4-11a4 4 0 018 0c0 5-5 8-5 12"/></svg>`,
    },
  ];

  function pad2(n) { return String(n).padStart(2, '0'); }
  function todayStr() {
    const d = new Date();
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  }
  function fmtShort(d) {
    try { return new Date(d + 'T00:00:00').toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' }); }
    catch (_) { return d; }
  }
  function genId() {
    return Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
  }
  function escapeHtml(str) {
    return String(str || '').replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
  }
  function toIsoString(value, fallback = undefined) {
    if (typeof value === 'string' && value.trim()) return value;
    if (value instanceof Date && !Number.isNaN(value.getTime())) return value.toISOString();
    if (fallback !== undefined) return fallback;
    return new Date().toISOString();
  }
  function isArchivedRecord(record) {
    return record?.isArchived === true
      || record?.archived === true
      || record?.status === 'archived'
      || Boolean(record?.archivedAt);
  }
  function normalizeHabitLogItem(item) {
    return {
      habitName: item?.habitName || item?.name || 'Habit',
      category: item?.category || '',
      completedAt: toIsoString(item?.completedAt || item?.doneAt || item?.timestamp),
    };
  }
  function normalizeMoodObj(rawMood) {
    if (!rawMood) return null;
    const statusStr = typeof rawMood === 'string' ? rawMood.trim() : String(rawMood.status || '').trim();
    if (!statusStr) return null;
    const match = MOOD_LIST.find((m) => m.status.toLowerCase() === statusStr.toLowerCase());
    if (match) return { status: match.status, svgKey: match.svgKey };
    return { status: statusStr, svgKey: rawMood.svgKey || 'sword' };
  }
  function normalizeJournalEntry(entry, fallbackOrder = 0) {
    const date = entry?.date || todayStr();
    const createdAt = toIsoString(entry?.createdAt || entry?.generatedAt, `${date}T00:00:00.000Z`);
    const updatedAt = toIsoString(entry?.updatedAt || entry?.modifiedAt || createdAt, createdAt);
    const completedAt = entry?.completedAt
      ? toIsoString(entry.completedAt, null)
      : (entry?.generatedAt ? toIsoString(entry.generatedAt, null) : null);
    return {
      id: entry?.id || genId(),
      date,
      title: entry?.title || '',
      content: entry?.content || '',
      order: Number.isFinite(entry?.order) ? entry.order : fallbackOrder,
      mood: normalizeMoodObj(entry?.mood),
      habits_log: Array.isArray(entry?.habits_log) ? entry.habits_log.map(normalizeHabitLogItem) : [],
      source: entry?.source || 'manual',
      generatedAt: entry?.generatedAt || null,
      createdAt,
      updatedAt,
      completedAt,
      archivedAt: entry?.archivedAt || null,
      isArchived: isArchivedRecord(entry),
      pinned: entry?.pinned === true,
    };
  }
  function normalizeJournalEntries(list) {
    return (Array.isArray(list) ? list : [])
      .map((entry, idx) => normalizeJournalEntry(entry, idx))
      .sort((a, b) => {
        if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
        return (b.date || '').localeCompare(a.date || '');
      });
  }
  function getVisibleEntries(list) {
    return normalizeJournalEntries(list).filter((e) => !isArchivedRecord(e));
  }

  function loadEcoDb() {
    let db = null;
    try { db = JSON.parse(localStorage.getItem(ECO_DB_KEY) || 'null'); } catch (_) {}
    if (!db || typeof db !== 'object') db = {};
    db.cucimoney = window.CuciMoneyModule ? window.CuciMoneyModule.normalizeState(db.cucimoney) : (db.cucimoney || {});
    if (!db.kronik || !Array.isArray(db.kronik.entries)) db.kronik = { entries: [] };

    if (db.kronik.entries.length === 0) {
      const lsV3 = localStorage.getItem('buku_jurnal_v3');
      if (lsV3) {
        try {
          const parsed = JSON.parse(lsV3);
          if (Array.isArray(parsed) && parsed.length > 0) {
            db.kronik.entries = normalizeJournalEntries(parsed).map((e, i) => ({ ...e, order: i }));
          }
        } catch (_) {}
      }
      if (db.kronik.entries.length === 0) {
        const lsV2 = localStorage.getItem('buku_jurnal_v2');
        if (lsV2) {
          try {
            const oldDb = JSON.parse(lsV2);
            if (oldDb && typeof oldDb === 'object') {
              db.kronik.entries = Object.keys(oldDb).sort().map((d, i) => normalizeJournalEntry({
                id: genId(),
                date: d,
                title: '',
                content: oldDb[d] || '',
                order: i,
                source: 'manual',
                createdAt: `${d}T00:00:00.000Z`,
                updatedAt: `${d}T00:00:00.000Z`,
              }, i));
            }
          } catch (_) {}
        }
      }
    } else {
      db.kronik.entries = normalizeJournalEntries(db.kronik.entries);
    }

    db.dailyos = window.DailyOsModule ? window.DailyOsModule.normalizeState(db.dailyos) : (db.dailyos || { habits: [], todos: [] });
    db.ideku = window.IdekuModule ? window.IdekuModule.normalizeState(db.ideku) : (db.ideku || { ideas: [], categories: ['Dev', 'Bisnis', 'Konten', 'Belajar', 'Kreatif', 'Personal', 'Belanja'], viewMode: 'group', theme: 'light', collapsedCats: [] });
    if (!db.linkku || typeof db.linkku !== 'object') {
      db.linkku = {
        links: [],
        categories: [
          { id: 'yt', n: 'YouTube' },
          { id: 'tools', n: 'Tools' },
          { id: 'reading', n: 'Reading' },
          { id: 'project', n: 'Project' },
        ],
        privateCategories: [],
        pinHash: '',
        extraTagsNormal: [],
        extraTagsPriv: [],
        seededDefaultCats: true,
      };
    } else {
      if (!Array.isArray(db.linkku.links)) db.linkku.links = [];
      if (!Array.isArray(db.linkku.categories)) db.linkku.categories = [];
      if (!Array.isArray(db.linkku.privateCategories)) db.linkku.privateCategories = [];
      if (typeof db.linkku.pinHash !== 'string') db.linkku.pinHash = '';
      if (!Array.isArray(db.linkku.extraTagsNormal)) db.linkku.extraTagsNormal = [];
      if (!Array.isArray(db.linkku.extraTagsPriv)) db.linkku.extraTagsPriv = [];
      if (!db.linkku.seededDefaultCats && db.linkku.categories.length === 0) {
        db.linkku.categories = [
          { id: 'yt', n: 'YouTube' },
          { id: 'tools', n: 'Tools' },
          { id: 'reading', n: 'Reading' },
          { id: 'project', n: 'Project' },
        ];
        db.linkku.seededDefaultCats = true;
      }
    }
    return db;
  }

  let ecoDb = loadEcoDb();
  let entries = ecoDb.kronik.entries;
  let lkLinks = ecoDb.linkku.links;
  let lkCats = ecoDb.linkku.categories;
  let lkPrivCats = ecoDb.linkku.privateCategories;
  let lkPinHash = ecoDb.linkku.pinHash || '';
  let lkExtraTagsNormal = ecoDb.linkku.extraTagsNormal || [];
  let lkExtraTagsPriv = ecoDb.linkku.extraTagsPriv || [];
  let lkSeededCats = Boolean(ecoDb.linkku.seededDefaultCats);

  function saveEcoDb() {
    ecoDb.kronik = { entries: normalizeJournalEntries(entries) };
    ecoDb.linkku = {
      links: lkLinks,
      categories: lkCats,
      privateCategories: lkPrivCats,
      pinHash: lkPinHash,
      extraTagsNormal: lkExtraTagsNormal,
      extraTagsPriv: lkExtraTagsPriv,
      seededDefaultCats: lkSeededCats,
    };
    ecoDb.assistant = {
      historyCount: chatHistory.length,
      memoryCount: userMemories.length,
      updatedAt: Date.now(),
    };
    if (window.CuciMoneyModule) {
      ecoDb.cucimoney = window.CuciMoneyModule.normalizeState(ecoDb.cucimoney);
    }
    if (window.DailyOsModule) {
      ecoDb.dailyos = window.DailyOsModule.normalizeState(ecoDb.dailyos);
    }
    localStorage.setItem(ECO_DB_KEY, JSON.stringify(ecoDb));
  }
  saveEcoDb();

  function showToastMessage(msg, isSuccess = false) {
    toastEl.textContent = msg;
    toastEl.classList.toggle('success', isSuccess);
    toastEl.classList.add('show');
    clearTimeout(showToastMessage._t);
    showToastMessage._t = setTimeout(() => toastEl.classList.remove('show'), 4200);
  }

  function buildKronikContextForAssistant() {
    const visible = getVisibleEntries(entries);
    if (!visible.length) {
      return '\n\n## Jurnal Kronik Saat Ini\nBelum ada lembaran jurnal yang ditulis Tuan.';
    }
    let block = `\n\n## Jurnal Kronik Terbaru (${visible.length} lembaran aktif)\n`;
    visible.slice(0, 3).forEach((e) => {
      const moodTxt = e.mood?.status ? ` [Mood: ${e.mood.status}]` : '';
      const titleTxt = e.title ? `"${e.title}"` : '(Tanpa Judul)';
      const snippet = String(e.content || '').replace(/\s+/g, ' ').trim().slice(0, 120);
      block += `- ${e.date}${moodTxt} ${titleTxt}: ${snippet || '(kosong)'}\n`;
    });
    return block;
  }

  const appsTodayDateEl = $('apps-today-date');
  if (appsTodayDateEl) {
    try {
      const dStr = new Intl.DateTimeFormat('id-ID', { weekday: 'long', day: 'numeric', month: 'short', year: 'numeric' }).format(new Date());
      appsTodayDateEl.textContent = dStr.charAt(0).toUpperCase() + dStr.slice(1);
    } catch (_) {}
  }

  // ── Kronik 3-Phase UI Controller ───────────────────────────────────────
  const kronikHeroCard = $('kronik-hero-card');
  const kronikHeroBadge = $('kronik-hero-badge');
  const kronikHeroQuote = $('kronik-hero-quote');
  const kronikHeroFooter = $('kronik-hero-footer');
  const kronikTileBtn = $('kronik-tile-btn');
  const resetKronikBtn = $('reset-kronik-btn');

  const kronikOverlay = $('kronik-overlay');
  const kronikExitBtn = $('kronik-exit-btn');
  const kronikCoverScreen = $('kronik-cover-screen');
  const kronikJournalScreen = $('kronik-journal-screen');
  const kronikCoverPanel = $('kronik-book-cover-panel');
  const kronikCoverHint = $('kronik-cover-hint');
  const kronikCoverTagline = $('kronik-cover-tagline');
  const kronikSpreadBackBtn = $('kronik-spread-back-btn');
  const kronikBookPages = $('kronik-book-pages');
  const kronikSpine = kronikOverlay?.querySelector('.book-spine-fixed');
  const kronikTocList = $('kronik-toc-list');
  const kronikSettingsPanel = $('kronik-settings-panel');
  const kronikBtnSettings = $('kronik-btn-settings');
  const kronikBtnSelect = $('kronik-btn-select');
  const kronikBtnDeleteSelected = $('kronik-btn-delete-selected');
  const kronikBtnExport = $('kronik-btn-export');
  const kronikBtnImport = $('kronik-btn-import');
  const kronikImportInput = $('kronik-import-input');
  const kronikNewFromSpread = $('kronik-new-from-spread');
  const kronikPageViewport = $('kronik-page-viewport');
  const kronikSaveDot = $('kronik-save-dot');
  const kronikSwipeHint = $('kronik-swipe-hint');

  const kronikMoodOverlay = $('kronik-mood-overlay');
  const kronikMoodGrid = $('kronik-mood-grid');
  const kronikBtnAiDetect = $('kronik-btn-ai-detect');
  const kronikBtnMoodClear = $('kronik-btn-mood-clear');
  const kronikMoodAiResult = $('kronik-mood-ai-result');

  let jCurrentId = null;
  let jIsFlipping = false;
  let jSaveTimer = null;
  let jPageEl = null;
  let jBookIsOpen = false;
  let selectMode = false;
  let selectedIds = new Set();

  function renderKronikAppsSummary() {
    const visible = getVisibleEntries(entries);
    const todayEntry = visible.find((e) => e.date === todayStr()) || visible[0] || null;
    if (!kronikHeroBadge || !kronikHeroQuote || !kronikHeroFooter) return;
    if (!todayEntry) {
      kronikHeroBadge.textContent = 'Buka Buku';
      kronikHeroQuote.textContent = '“Belum ada lembaran — ketuk untuk mulai menulis...”';
      kronikHeroFooter.textContent = '0 lembaran · Ketuk Buka';
      return;
    }
    kronikHeroBadge.textContent = todayEntry.mood?.status || (todayEntry.pinned ? '📌 Pin' : fmtShort(todayEntry.date));
    const snippetSource = todayEntry.content?.trim() || todayEntry.title?.trim() || 'Lembaran baru...';
    kronikHeroQuote.textContent = `“${snippetSource.slice(0, 72)}${snippetSource.length > 72 ? '...' : ''}”`;
    kronikHeroFooter.textContent = `${visible.length} lembaran · ${todayEntry.date === todayStr() ? 'Jurnal Hari Ini' : fmtShort(todayEntry.date)}`;
  }

  function jSaveEntry(id, fields) {
    const idx = entries.findIndex((e) => e.id === id);
    if (idx >= 0) {
      entries[idx] = { ...entries[idx], ...fields, updatedAt: new Date().toISOString() };
      entries = normalizeJournalEntries(entries);
      saveEcoDb();
      renderKronikAppsSummary();
    }
  }

  function jAddEntry(customFields = {}) {
    const now = new Date().toISOString();
    const e = normalizeJournalEntry({
      id: genId(),
      date: customFields.date || todayStr(),
      title: customFields.title || '',
      content: customFields.content || '',
      order: 0,
      mood: customFields.mood ? normalizeMoodObj(customFields.mood) : null,
      habits_log: Array.isArray(customFields.habits_log) ? customFields.habits_log : [],
      source: customFields.source || 'manual',
      generatedAt: customFields.source === 'rem_chat' ? now : null,
      createdAt: now,
      updatedAt: now,
      completedAt: null,
      archivedAt: null,
      isArchived: false,
      pinned: Boolean(customFields.pinned),
    });
    entries.unshift(e);
    entries = normalizeJournalEntries(entries).map((x, i) => ({ ...x, order: i }));
    saveEcoDb();
    renderKronikAppsSummary();
    return e;
  }

  function openKronikOverlay() {
    if (!kronikOverlay) return;
    window.CuciMoneyModule?.close?.();
    window.IdekuModule?.close?.();
    window.DailyOsModule?.close?.();
    closeLinkkuOverlay?.();
    kronikOverlay.classList.add('open');
    jRenderSpread();
    renderKronikAppsSummary();
  }

  function closeKronikOverlay() {
    clearTimeout(jSaveTimer);
    jFlushSave();
    closeMoodPicker();
    if (kronikOverlay) kronikOverlay.classList.remove('open');
    renderKronikAppsSummary();
  }

  function openBook() {
    if (jBookIsOpen || !kronikCoverPanel) return;
    jBookIsOpen = true;
    const coverFace = kronikCoverPanel.querySelector('.book-cover-face');
    const coverInner = kronikCoverPanel.querySelector('.book-cover-inner');
    if (kronikCoverHint) kronikCoverHint.style.display = 'none';
    kronikSpine?.classList.add('anim-hidden');
    if (kronikBookPages) {
      kronikBookPages.style.transition = 'opacity 0.15s ease';
      kronikBookPages.style.opacity = '0';
    }
    coverInner?.classList.remove('show');
    if (coverFace) coverFace.style.display = '';
    kronikCoverPanel.classList.add('opening');
    setTimeout(() => { if (coverFace) coverFace.style.display = 'none'; }, 440);
    setTimeout(() => {
      kronikSpine?.classList.remove('anim-hidden');
      jRenderSpread();
      if (kronikCoverTagline) kronikCoverTagline.style.display = 'none';
      if (kronikSpreadBackBtn) kronikSpreadBackBtn.style.display = '';
    }, 900);
  }

  function goToCover() {
    if (!jBookIsOpen || !kronikCoverPanel) return;
    jBookIsOpen = false;
    const coverFace = kronikCoverPanel.querySelector('.book-cover-face');
    const coverInner = kronikCoverPanel.querySelector('.book-cover-inner');
    kronikSpine?.classList.add('anim-hidden');
    if (kronikCoverTagline) kronikCoverTagline.style.display = '';
    if (kronikSpreadBackBtn) kronikSpreadBackBtn.style.display = 'none';
    if (kronikCoverHint) kronikCoverHint.style.display = '';
    if (coverFace) coverFace.style.display = 'none';
    coverInner?.classList.add('show');
    kronikCoverPanel.classList.remove('opening');
    setTimeout(() => {
      coverInner?.classList.remove('show');
      if (coverFace) coverFace.style.display = '';
    }, 440);
    setTimeout(() => {
      kronikSpine?.classList.remove('anim-hidden');
      if (kronikBookPages) {
        kronikBookPages.style.transition = 'opacity 0.25s ease';
        kronikBookPages.style.opacity = '';
        setTimeout(() => { kronikBookPages.style.transition = ''; }, 260);
      }
    }, 900);
  }

  function jRenderSpread() {
    if (!kronikTocList) return;
    kronikTocList.innerHTML = '';
    const all = getVisibleEntries(entries);
    if (all.length === 0) {
      kronikTocList.innerHTML = '<li class="toc-empty">Belum ada lembaran.<br>Tap ✦ + Lembaran Baru untuk mulai ✦</li>';
      return;
    }
    all.forEach((entry, i) => {
      const label = entry.title || '(tanpa judul)';
      const dateStr = entry.date ? fmtShort(entry.date) : '—';
      const moodStatus = entry.mood?.status || null;
      const moodM = moodStatus ? MOOD_LIST.find((m) => m.status === moodStatus) : null;
      const li = document.createElement('li');
      li.className = 'toc-item' + (selectedIds.has(entry.id) ? ' selected' : '') + (entry.pinned ? ' pinned' : '');
      if (moodStatus) li.dataset.mood = moodStatus;
      li.dataset.id = entry.id;
      const moodBadge = moodM ? `<span class="toc-mood" title="${escapeHtml(moodStatus)}">${moodM.svg}</span>` : '';
      const pinBadge = entry.pinned ? '<span class="toc-pin" title="Disematkan">📌</span>' : '';
      li.innerHTML = `
        <div class="toc-checkbox"></div>
        <span class="toc-num">${i + 1}</span>
        <span class="toc-preview">${escapeHtml(label)}</span>
        <span class="toc-dots"></span>
        <span class="toc-date">${escapeHtml(dateStr)}</span>${pinBadge}${moodBadge}`;
      li.addEventListener('click', () => {
        if (selectMode) toggleSelectItem(entry.id);
        else jGoFullscreen(entry.id);
      });

      let lpTimer = null;
      let lpTriggered = false;
      const lpStart = () => {
        if (selectMode) return;
        lpTriggered = false;
        lpTimer = setTimeout(() => {
          lpTriggered = true;
          if (navigator.vibrate) navigator.vibrate(15);
          togglePinEntry(entry.id);
        }, 500);
      };
      const lpCancel = () => { if (lpTimer) { clearTimeout(lpTimer); lpTimer = null; } };
      li.addEventListener('pointerdown', lpStart);
      li.addEventListener('pointerup', lpCancel);
      li.addEventListener('pointermove', lpCancel);
      li.addEventListener('pointerleave', lpCancel);
      li.addEventListener('pointercancel', lpCancel);
      li.addEventListener('click', (e) => {
        if (lpTriggered) {
          e.stopPropagation();
          e.preventDefault();
          lpTriggered = false;
        }
      }, true);

      kronikTocList.appendChild(li);
    });
    kronikTocList.classList.toggle('select-mode', selectMode);
  }

  function togglePinEntry(id) {
    const idx = entries.findIndex((e) => e.id === id);
    if (idx < 0) return;
    entries[idx].pinned = !entries[idx].pinned;
    entries[idx].updatedAt = new Date().toISOString();
    entries = normalizeJournalEntries(entries);
    saveEcoDb();
    jRenderSpread();
    renderKronikAppsSummary();
  }

  function renderQuestLog(habitsLog) {
    if (!Array.isArray(habitsLog) || habitsLog.length === 0) {
      return `<div class="quest-log">
        <div class="quest-log-title">Quest Log</div>
        <div class="quest-log-empty">Belum ada aktivitas tercatat hari ini.</div>
      </div>`;
    }
    const items = habitsLog.map((item) => {
      const name = item.habitName || item.name || 'Habit';
      const cat = item.category || '';
      let time = '';
      try {
        if (item.completedAt) time = new Date(item.completedAt).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' });
      } catch (_) {}
      return `<div class="quest-item">
        <span class="quest-check">✦</span>
        <div class="quest-info">
          <div class="quest-name">${escapeHtml(name)}</div>
          ${cat ? `<div class="quest-cat">${escapeHtml(cat)}</div>` : ''}
        </div>
        ${time ? `<div class="quest-time">${escapeHtml(time)}</div>` : ''}
      </div>`;
    }).join('');
    return `<div class="quest-log"><div class="quest-log-title">Quest Log</div>${items}</div>`;
  }

  function jBuildPage(oldEl, inAnim, outAnim) {
    if (!kronikPageViewport) return;
    const visible = getVisibleEntries(entries);
    const entry = visible.find((e) => e.id === jCurrentId) || normalizeJournalEntry({ id: jCurrentId, date: todayStr() });
    const entryIdx = Math.max(0, visible.findIndex((e) => e.id === jCurrentId));
    const entryNum = entryIdx + 1;
    const totalEntries = Math.max(1, visible.length);

    kronikPageViewport.querySelectorAll('.journal-page:not([data-active])').forEach((el) => el.remove());

    if (oldEl) {
      oldEl.removeAttribute('data-active');
      if (outAnim) {
        oldEl.classList.add(outAnim);
        const tOut = setTimeout(() => { if (oldEl.parentNode) oldEl.remove(); }, 420);
        oldEl.addEventListener('animationend', () => { clearTimeout(tOut); if (oldEl.parentNode) oldEl.remove(); }, { once: true });
      } else {
        oldEl.remove();
      }
    }

    const moodM = entry.mood ? MOOD_LIST.find((m) => m.status === entry.mood.status) : null;
    const moodBtnInner = moodM
      ? `${moodM.svg}<span>${escapeHtml(moodM.status)}</span>`
      : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="14" height="14"><circle cx="12" cy="12" r="4"/><circle cx="12" cy="12" r="9" opacity="0.3"/></svg><span>mood</span>';

    const div = document.createElement('div');
    div.className = 'journal-page';
    div.dataset.active = '1';
    div.innerHTML = `
      <div class="page-inner">
        <div class="page-topbar">
          <div class="topbar-left">
            <button type="button" class="btn-back-spread">← Tutup</button>
            <button type="button" class="btn-delete-inline">hapus</button>
          </div>
          <div class="topbar-right">
            <button type="button" class="btn-mood${moodM ? ' has-mood' : ''}" title="Pilih mood">${moodBtnInner}</button>
            <span class="page-indicator">${entryNum} / ${totalEntries}</span>
            <button type="button" class="btn-new-inline" title="Lembaran baru">+</button>
          </div>
        </div>
        <div class="page-header-fields">
          <input class="entry-title-input" id="kronik-entry-title" type="text" placeholder="Judul lembaran..." value="${escapeHtml(entry.title || '')}" />
          <input class="entry-date-input" id="kronik-entry-date" type="date" value="${escapeHtml(entry.date || todayStr())}" />
        </div>
        <textarea id="kronik-entry-ta" placeholder="Tulis sesuatu...">${escapeHtml(entry.content || '')}</textarea>
        ${renderQuestLog(entry.habits_log || [])}
      </div>`;

    kronikPageViewport.appendChild(div);
    jPageEl = div;

    if (inAnim) {
      div.classList.add(inAnim);
      const tIn = setTimeout(() => { div.classList.remove(inAnim); jIsFlipping = false; }, 420);
      div.addEventListener('animationend', () => { clearTimeout(tIn); div.classList.remove(inAnim); jIsFlipping = false; }, { once: true });
    } else {
      jIsFlipping = false;
    }

    div.querySelector('.btn-back-spread')?.addEventListener('click', jGoToSpread);
    div.querySelector('.btn-delete-inline')?.addEventListener('click', jDeleteEntry);
    div.querySelector('.btn-mood')?.addEventListener('click', openMoodPicker);
    div.querySelector('.btn-new-inline')?.addEventListener('click', jNewEntry);

    const ta = div.querySelector('#kronik-entry-ta');
    const dateI = div.querySelector('#kronik-entry-date');
    const titleI = div.querySelector('#kronik-entry-title');
    if (ta) {
      ta.addEventListener('input', jOnType);
    }
    if (dateI) {
      dateI.addEventListener('input', () => {
        jSaveEntry(jCurrentId, { date: dateI.value || todayStr() });
        jRenderSpread();
      });
    }
    if (titleI) {
      titleI.addEventListener('input', () => {
        kronikSaveDot?.classList.add('show');
        clearTimeout(jSaveTimer);
        jSaveTimer = setTimeout(() => {
          jFlushSave();
        }, 500);
      });
    }
  }

  function jGoFullscreen(id) {
    if (!id) {
      const e = jAddEntry();
      id = e.id;
    }
    jCurrentId = id;
    kronikCoverScreen?.classList.add('hidden');
    kronikJournalScreen?.classList.remove('hidden');
    if (kronikExitBtn) kronikExitBtn.style.display = 'none';
    jBuildPage(null, 'flip-in-forward');
    if (kronikSwipeHint) {
      kronikSwipeHint.style.opacity = '1';
      setTimeout(() => { if (kronikSwipeHint) kronikSwipeHint.style.opacity = '0'; }, 3000);
    }
  }

  function jGoToSpread() {
    clearTimeout(jSaveTimer);
    jFlushSave();
    if (kronikPageViewport) kronikPageViewport.innerHTML = '';
    jPageEl = null;
    kronikJournalScreen?.classList.add('hidden');
    kronikCoverScreen?.classList.remove('hidden');
    if (kronikExitBtn) kronikExitBtn.style.display = '';
    jRenderSpread();
    renderKronikAppsSummary();
  }

  function jNewEntry() {
    clearTimeout(jSaveTimer);
    jFlushSave();
    const e = jAddEntry();
    jCurrentId = e.id;
    jBuildPage(jPageEl, 'flip-in-forward', 'flip-out-forward');
  }

  function jDeleteEntry() {
    if (!jCurrentId || !jPageEl) return;
    const btn = jPageEl.querySelector('.btn-delete-inline');
    if (!btn) return;
    if (btn.dataset.confirm !== '1') {
      btn.dataset.confirm = '1';
      btn.textContent = 'arsip?';
      btn.style.color = 'rgba(180,45,30,0.9)';
      setTimeout(() => {
        if (btn) {
          btn.dataset.confirm = '';
          btn.textContent = 'hapus';
          btn.style.color = '';
        }
      }, 2500);
      return;
    }
    clearTimeout(jSaveTimer);
    const idx = entries.findIndex((e) => e.id === jCurrentId);
    if (idx < 0) return;
    const now = new Date().toISOString();
    entries[idx] = { ...entries[idx], isArchived: true, archivedAt: now, updatedAt: now };
    saveEcoDb();
    renderKronikAppsSummary();
    const visible = getVisibleEntries(entries);
    if (visible.length === 0) {
      jGoToSpread();
    } else {
      const targetIdx = Math.min(idx, visible.length - 1);
      jCurrentId = visible[targetIdx].id;
      jBuildPage(jPageEl, 'flip-in-backward', 'flip-out-backward');
    }
  }

  function jOnType() {
    kronikSaveDot?.classList.add('show');
    clearTimeout(jSaveTimer);
    jSaveTimer = setTimeout(jFlushSave, 600);
  }

  function jFlushSave() {
    if (!jPageEl || !jCurrentId) return;
    const ta = jPageEl.querySelector('#kronik-entry-ta');
    const titleI = jPageEl.querySelector('#kronik-entry-title');
    const idx = entries.findIndex((e) => e.id === jCurrentId);
    if (idx >= 0) {
      if (ta) entries[idx].content = ta.value;
      if (titleI) entries[idx].title = titleI.value;
      entries[idx].updatedAt = new Date().toISOString();
      saveEcoDb();
      renderKronikAppsSummary();
    }
    kronikSaveDot?.classList.remove('show');
  }

  function jFlipForward() {
    if (jIsFlipping) return;
    jFlushSave();
    const visible = getVisibleEntries(entries);
    const idx = visible.findIndex((e) => e.id === jCurrentId);
    if (idx < 0 || idx >= visible.length - 1) return;
    jIsFlipping = true;
    jCurrentId = visible[idx + 1].id;
    jBuildPage(jPageEl, 'flip-in-forward', 'flip-out-forward');
  }

  function jFlipBackward() {
    if (jIsFlipping) return;
    jFlushSave();
    const visible = getVisibleEntries(entries);
    const idx = visible.findIndex((e) => e.id === jCurrentId);
    if (idx <= 0) {
      jGoToSpread();
      return;
    }
    jIsFlipping = true;
    jCurrentId = visible[idx - 1].id;
    jBuildPage(jPageEl, 'flip-in-backward', 'flip-out-backward');
  }

  if (kronikCoverPanel) {
    let cDragStartX = 0;
    let cDragged = false;
    kronikCoverPanel.addEventListener('pointerdown', (e) => { cDragStartX = e.clientX; cDragged = false; });
    kronikCoverPanel.addEventListener('pointermove', (e) => { if (Math.abs(e.clientX - cDragStartX) > 12) cDragged = true; });
    kronikCoverPanel.addEventListener('click', () => {
      if (!jBookIsOpen && !cDragged) openBook();
    });
  }
  if (kronikCoverScreen) {
    let csX = 0;
    let csY = 0;
    kronikCoverScreen.addEventListener('touchstart', (e) => { csX = e.touches[0].clientX; csY = e.touches[0].clientY; }, { passive: true });
    kronikCoverScreen.addEventListener('touchend', (e) => {
      const dx = e.changedTouches[0].clientX - csX;
      const dy = e.changedTouches[0].clientY - csY;
      if (Math.abs(dx) < 50 || Math.abs(dy) > Math.abs(dx) * 0.8) return;
      if (!jBookIsOpen && dx < 0) openBook();
      else if (jBookIsOpen && dx > 0) goToCover();
    }, { passive: true });
  }
  if (kronikJournalScreen) {
    let jsX = 0;
    let jsY = 0;
    kronikJournalScreen.addEventListener('touchstart', (e) => { jsX = e.touches[0].clientX; jsY = e.touches[0].clientY; }, { passive: true });
    kronikJournalScreen.addEventListener('touchend', (e) => {
      const dx = e.changedTouches[0].clientX - jsX;
      const dy = e.changedTouches[0].clientY - jsY;
      if (Math.abs(dx) < 60 || Math.abs(dy) > Math.abs(dx) * 0.65) return;
      if (dx < 0) jFlipForward();
      else jFlipBackward();
    }, { passive: true });
  }

  function toggleSelectMode() {
    selectMode = !selectMode;
    selectedIds.clear();
    if (kronikBtnSelect) {
      kronikBtnSelect.innerHTML = selectMode
        ? '<span class="settings-icon">☑</span> Mode Pilih: Aktif'
        : '<span class="settings-icon">☑</span> Pilih Lembaran';
    }
    if (kronikBtnDeleteSelected) kronikBtnDeleteSelected.style.display = selectMode ? '' : 'none';
    if (kronikBtnExport) kronikBtnExport.style.display = selectMode ? 'none' : '';
    if (kronikBtnImport) kronikBtnImport.style.display = selectMode ? 'none' : '';
    updateSelectedCount();
    jRenderSpread();
  }
  function updateSelectedCount() {
    const el = $('kronik-selected-count');
    if (el) el.textContent = String(selectedIds.size);
  }
  function toggleSelectItem(id) {
    if (selectedIds.has(id)) selectedIds.delete(id);
    else selectedIds.add(id);
    updateSelectedCount();
    kronikTocList?.querySelectorAll('.toc-item').forEach((li) => {
      if (li.dataset.id === id) li.classList.toggle('selected', selectedIds.has(id));
    });
  }

  if (kronikBtnSettings) {
    kronikBtnSettings.addEventListener('click', () => {
      const open = kronikSettingsPanel?.classList.toggle('open');
      kronikBtnSettings.classList.toggle('active', open);
      if (!open && selectMode) toggleSelectMode();
    });
  }
  if (kronikBtnSelect) kronikBtnSelect.addEventListener('click', toggleSelectMode);
  if (kronikBtnDeleteSelected) {
    kronikBtnDeleteSelected.addEventListener('click', () => {
      if (selectedIds.size === 0) return;
      if (kronikBtnDeleteSelected.dataset.confirm !== '1') {
        kronikBtnDeleteSelected.dataset.confirm = '1';
        kronikBtnDeleteSelected.innerHTML = `<span class="settings-icon">✕</span> Yakin hapus ${selectedIds.size} lembaran?`;
        setTimeout(() => {
          kronikBtnDeleteSelected.dataset.confirm = '';
          kronikBtnDeleteSelected.innerHTML = `<span class="settings-icon">✕</span> Hapus Dipilih (<span id="kronik-selected-count">${selectedIds.size}</span>)`;
        }, 2500);
        return;
      }
      const now = new Date().toISOString();
      entries = entries.map((e) => (selectedIds.has(e.id) ? { ...e, isArchived: true, archivedAt: now, updatedAt: now } : e));
      saveEcoDb();
      selectedIds.clear();
      toggleSelectMode();
      kronikSettingsPanel?.classList.remove('open');
      kronikBtnSettings?.classList.remove('active');
      jRenderSpread();
      renderKronikAppsSummary();
    });
  }
  if (kronikBtnExport) {
    kronikBtnExport.addEventListener('click', () => {
      const data = {
        version: 2,
        entries: normalizeJournalEntries(entries),
        exportedAt: new Date().toISOString(),
      };
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `kronik-backup-${todayStr()}.json`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      kronikSettingsPanel?.classList.remove('open');
      kronikBtnSettings?.classList.remove('active');
      showToastMessage('Backup Kronik JSON diunduh', true);
    });
  }
  if (kronikImportInput) {
    kronikImportInput.addEventListener('change', (ev) => {
      const file = ev.target.files?.[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const parsed = JSON.parse(String(reader.result || 'null'));
          const rawEntries = Array.isArray(parsed)
            ? parsed
            : (parsed && Array.isArray(parsed.entries) ? parsed.entries : null);
          if (!rawEntries) throw new Error('Format JSON tidak valid');
          const normalizedImported = normalizeJournalEntries(rawEntries);
          const existingIds = new Set(entries.map((x) => x.id));
          const newOnes = normalizedImported.filter((x) => x.id && !existingIds.has(x.id));
          entries = normalizeJournalEntries([...newOnes, ...entries]).map((e, i) => ({ ...e, order: i }));
          saveEcoDb();
          jRenderSpread();
          renderKronikAppsSummary();
          kronikSettingsPanel?.classList.remove('open');
          kronikBtnSettings?.classList.remove('active');
          showToastMessage(`${newOnes.length} lembaran berhasil diimport`, true);
        } catch (_) {
          showToastMessage('Gagal import: format file tidak valid');
        }
      };
      reader.readAsText(file);
      ev.target.value = '';
    });
  }

  // ── Mood Picker & AI Mood Detection (/api/kronik/mood) ─────────────────
  if (kronikMoodGrid) {
    MOOD_LIST.forEach((m) => {
      const opt = document.createElement('div');
      opt.className = 'mood-option';
      opt.dataset.mood = m.status;
      opt.innerHTML = `${m.svg}<span class="mood-label">${escapeHtml(m.status)}</span>`;
      opt.addEventListener('click', () => selectMood(m.status, m.svgKey));
      kronikMoodGrid.appendChild(opt);
    });
  }

  function openMoodPicker() {
    if (!kronikMoodOverlay) return;
    jFlushSave();
    kronikMoodOverlay.classList.add('open');
    if (kronikMoodAiResult) kronikMoodAiResult.textContent = '';
    const entry = entries.find((e) => e.id === jCurrentId);
    const activeMood = entry?.mood?.status || null;
    kronikMoodGrid?.querySelectorAll('.mood-option').forEach((opt) => {
      opt.classList.toggle('active', opt.dataset.mood === activeMood);
    });
  }
  function closeMoodPicker() {
    kronikMoodOverlay?.classList.remove('open');
  }
  function updateMoodBtn(status) {
    const btn = jPageEl?.querySelector('.btn-mood');
    if (!btn) return;
    if (status) {
      const m = MOOD_LIST.find((x) => x.status === status);
      btn.innerHTML = `${m ? m.svg : ''}<span>${escapeHtml(status)}</span>`;
      btn.classList.add('has-mood');
    } else {
      btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="14" height="14"><circle cx="12" cy="12" r="4"/><circle cx="12" cy="12" r="9" opacity="0.3"/></svg><span>mood</span>';
      btn.classList.remove('has-mood');
    }
  }
  function selectMood(status, svgKey) {
    if (!jCurrentId) return;
    jSaveEntry(jCurrentId, { mood: { status, svgKey } });
    updateMoodBtn(status);
    kronikMoodGrid?.querySelectorAll('.mood-option').forEach((opt) => {
      opt.classList.toggle('active', opt.dataset.mood === status);
    });
    jRenderSpread();
    setTimeout(closeMoodPicker, 320);
  }
  if (kronikMoodOverlay) {
    kronikMoodOverlay.addEventListener('click', (e) => {
      if (e.target === kronikMoodOverlay) closeMoodPicker();
    });
  }
  if (kronikBtnMoodClear) {
    kronikBtnMoodClear.addEventListener('click', () => {
      if (!jCurrentId) return;
      jSaveEntry(jCurrentId, { mood: null });
      updateMoodBtn(null);
      kronikMoodGrid?.querySelectorAll('.mood-option').forEach((opt) => opt.classList.remove('active'));
      jRenderSpread();
      closeMoodPicker();
    });
  }
  if (kronikBtnAiDetect) {
    kronikBtnAiDetect.addEventListener('click', async () => {
      if (!jCurrentId) return;
      jFlushSave();
      const entry = entries.find((e) => e.id === jCurrentId);
      const rawText = [entry?.title, entry?.content].filter(Boolean).join('\n').trim();
      if (rawText.length < 10) {
        if (kronikMoodAiResult) kronikMoodAiResult.textContent = 'Tulis minimal 10 karakter dulu ya...';
        return;
      }
      kronikBtnAiDetect.classList.add('loading');
      kronikBtnAiDetect.disabled = true;
      if (kronikMoodAiResult) kronikMoodAiResult.textContent = 'Menganalisis suasana hati...';
      try {
        const res = await fetch('/api/kronik/mood', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ text: rawText }),
        });
        const data = await res.json();
        const found = data?.mood ? MOOD_LIST.find((m) => m.status.toLowerCase() === String(data.mood).toLowerCase()) : null;
        if (found) {
          if (kronikMoodAiResult) kronikMoodAiResult.textContent = `AI menyarankan: ${found.status} — tap kartu untuk terapkan`;
          kronikMoodGrid?.querySelectorAll('.mood-option').forEach((opt) => {
            opt.classList.toggle('active', opt.dataset.mood === found.status);
          });
        } else {
          if (kronikMoodAiResult) kronikMoodAiResult.textContent = data?.error || 'Mood belum terdeteksi, pilih manual ya';
        }
      } catch (_) {
        if (kronikMoodAiResult) kronikMoodAiResult.textContent = 'Gagal terhubung ke server AI.';
      } finally {
        kronikBtnAiDetect.classList.remove('loading');
        kronikBtnAiDetect.disabled = false;
      }
    });
  }

  if (kronikHeroCard) kronikHeroCard.addEventListener('click', openKronikOverlay);
  if (kronikTileBtn) kronikTileBtn.addEventListener('click', openKronikOverlay);
  if (kronikExitBtn) kronikExitBtn.addEventListener('click', closeKronikOverlay);
  if (kronikSpreadBackBtn) kronikSpreadBackBtn.addEventListener('click', goToCover);
  if (kronikNewFromSpread) kronikNewFromSpread.addEventListener('click', () => jGoFullscreen(null));
  if (resetKronikBtn) {
    resetKronikBtn.addEventListener('click', () => {
      entries = [];
      jCurrentId = null;
      saveEcoDb();
      jRenderSpread();
      renderKronikAppsSummary();
      showToastMessage('Data Kronik berhasil direset', true);
    });
  }

  renderKronikAppsSummary();

  function applyKronikTagsFromReply(rawText) {
    let changed = false;
    const shouldOpen = /\[OPEN:kronik\]/i.test(rawText);
    const addRegex = /\[KRONIK:ADD\|([^\]\r\n]+)\]/gi;
    let m;
    while ((m = addRegex.exec(rawText)) !== null) {
      const parts = String(m[1] || '').split('|').map((s) => s.trim());
      const title = parts[0] || '';
      const content = parts[1] || '';
      const moodStr = parts[2] || '';
      if (title || content) {
        const created = jAddEntry({
          title,
          content,
          mood: moodStr || null,
          source: 'rem_chat',
        });
        changed = true;
        showToastMessage(`Kronik: Tersimpan "${created.title || 'Catatan baru'}"`, true);
      }
    }
    if (changed) {
      jRenderSpread();
      renderKronikAppsSummary();
    }
    if (shouldOpen) {
      openKronikOverlay();
      if (!jBookIsOpen) setTimeout(openBook, 120);
    }
  }

  // ── LinkKu Module (Bookmark + Mode Privat PIN + AES-GCM WebCrypto) ─────
  function buildLinkkuContextForAssistant() {
    const pub = lkLinks.filter((l) => !l.p);
    if (!pub.length) {
      return '\n\n## Bookmark LinkKu Saat Ini\nBelum ada link publik tersimpan di LinkKu.';
    }
    let block = `\n\n## Bookmark LinkKu Terbaru (${pub.length} link publik)\n`;
    pub.slice(0, 4).forEach((l) => {
      const catName = lkCats.find((c) => c.id === l.cat)?.n || '';
      const meta = [lkGetDom(l.url), catName].filter(Boolean).join(' · ');
      block += `- "${l.ti || lkGetDom(l.url)}" (${meta})${l.note ? `: ${String(l.note).slice(0, 80)}` : ''}\n`;
    });
    return block;
  }

  const linkkuOverlay = $('linkku-overlay');
  const linkkuHeroCard = $('linkku-hero-card');
  const linkkuHeroBadge = $('linkku-hero-badge');
  const linkkuHeroTitle = $('linkku-hero-title');
  const linkkuHeroSub = $('linkku-hero-sub');
  const linkkuTileBtn = $('linkku-tile-btn');
  const resetLinkkuBtn = $('reset-linkku-btn');

  let lkCur = 'all';
  let lkFlt = 'all';
  let lkQuery = '';
  let lkEditId = null;
  let lkPrivMode = false;
  let lkPinBuf = '';
  let lkPinCb = null;
  let lkPinRawMode = false;
  let lkSpStep = 1;
  let lkSpFirst = '';
  let lkSpBuf = '';
  let lkBsId = null;
  let lkUrlFetchTimer = null;
  let lkSearchTimer = null;

  function lkGenId() {
    return Math.random().toString(36).slice(2, 8) + Date.now().toString(36);
  }
  function lkGetDom(url) {
    try { return new URL(url).hostname.replace(/^www\./, ''); }
    catch (_) { return String(url || ''); }
  }
  function lkThumbForUrl(url) {
    const yt = String(url || '').match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})/);
    return yt ? `https://img.youtube.com/vi/${yt[1]}/mqdefault.jpg` : '';
  }
  function lkFmtDate(ts) {
    const d = Date.now() - Number(ts || 0);
    if (d < 60000) return 'Baru';
    if (d < 3600000) return `${Math.floor(d / 60000)} mnt`;
    if (d < 86400000) return `${Math.floor(d / 3600000)} jam`;
    if (d < 604800000) return `${Math.floor(d / 86400000)} hari`;
    return new Date(ts).toLocaleDateString('id-ID', { day: 'numeric', month: 'short' });
  }
  function lkOpenExternal(url) {
    if (!url) return;
    const normalized = /^https?:\/\//i.test(url) ? url : `https://${url}`;
    const a = document.createElement('a');
    a.href = normalized;
    a.target = '_blank';
    a.rel = 'noopener noreferrer';
    document.body.appendChild(a);
    a.click();
    a.remove();
  }
  function lkToast(msg, type = '') {
    const container = $('lk-tc');
    if (!container) return;
    const el = document.createElement('div');
    el.className = `lk-toast${type ? ` ${type}` : ''}`;
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(() => el.remove(), 2400);
  }
  function lkOpenOv(id) {
    const el = $(id);
    if (el) el.classList.add('open');
  }
  function lkCloseOv(id) {
    const el = $(id);
    if (el) el.classList.remove('open');
  }
  function lkConfirm(title, message, cb, btnLbl = 'Hapus', btnCls = 'lk-bdanger') {
    const btn = $('lk-cfm-btn');
    if (!btn) return;
    $('lk-cfm-tit').textContent = title;
    $('lk-cfm-msg').textContent = message;
    btn.textContent = btnLbl;
    btn.className = btnCls;
    btn.onclick = () => {
      cb();
      lkCloseOv('lk-ov-cfm');
    };
    lkOpenOv('lk-ov-cfm');
  }

  async function lkHashPin(pin) {
    const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(pin));
    return Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, '0')).join('');
  }
  async function lkSetPin(pin) {
    lkPinHash = await lkHashPin(pin);
    saveEcoDb();
    lkUpdPinSt();
  }
  async function lkVerifyPin(pin) {
    if (!lkPinHash) return false;
    return (await lkHashPin(pin)) === lkPinHash;
  }
  async function lkDeriveKey(pinHexOrStr) {
    const base = await crypto.subtle.importKey('raw', new TextEncoder().encode(pinHexOrStr), { name: 'PBKDF2' }, false, ['deriveKey']);
    return crypto.subtle.deriveKey(
      { name: 'PBKDF2', salt: new TextEncoder().encode('linkku-salt-v1'), iterations: 100000, hash: 'SHA-256' },
      base,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    );
  }
  async function lkEncryptObj(obj, pinHexOrStr) {
    const key = await lkDeriveKey(pinHexOrStr);
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const enc = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, new TextEncoder().encode(JSON.stringify(obj)));
    const combined = new Uint8Array(iv.byteLength + enc.byteLength);
    combined.set(iv, 0);
    combined.set(new Uint8Array(enc), 12);
    let bin = '';
    for (let i = 0; i < combined.length; i += 1) bin += String.fromCharCode(combined[i]);
    return btoa(bin);
  }
  async function lkDecryptObj(b64, pinHexOrStr) {
    const key = await lkDeriveKey(pinHexOrStr);
    const bin = atob(b64);
    const combined = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i += 1) combined[i] = bin.charCodeAt(i);
    const iv = combined.slice(0, 12);
    const data = combined.slice(12);
    const dec = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, data);
    return JSON.parse(new TextDecoder().decode(dec));
  }

  function renderLinkkuAppsSummary() {
    const pub = lkLinks.filter((l) => !l.p);
    if (linkkuHeroBadge) linkkuHeroBadge.textContent = `${pub.length} Link`;
    if (pub.length === 0) {
      if (linkkuHeroTitle) linkkuHeroTitle.textContent = 'Belum ada link tersimpan';
      if (linkkuHeroSub) linkkuHeroSub.textContent = 'Ketuk untuk simpan link favoritmu';
      return;
    }
    const latest = pub.slice().sort((a, b) => (b.ts || 0) - (a.ts || 0))[0];
    const dom = lkGetDom(latest.url);
    const catName = lkCats.find((c) => c.id === latest.cat)?.n || '';
    if (linkkuHeroTitle) linkkuHeroTitle.textContent = latest.ti || dom;
    if (linkkuHeroSub) linkkuHeroSub.textContent = [dom, catName].filter(Boolean).join(' · ') || 'LinkKu tersinkronisasi';
  }

  function lkOpenSidebar() {
    $('lk-sidebar')?.classList.add('open');
    const bd = $('lk-sb-backdrop');
    if (bd) {
      bd.style.display = 'block';
      requestAnimationFrame(() => bd.classList.add('show'));
    }
  }
  function lkCloseSidebar() {
    $('lk-sidebar')?.classList.remove('open');
    const bd = $('lk-sb-backdrop');
    if (bd) {
      bd.classList.remove('show');
      setTimeout(() => {
        if (!bd.classList.contains('show')) bd.style.display = 'none';
      }, 220);
    }
  }

  function lkEnterPrivate() {
    lkPrivMode = true;
    lkCur = 'private-all';
    linkkuOverlay?.classList.add('lk-pm');
    if ($('lk-nav-n')) $('lk-nav-n').style.display = 'none';
    if ($('lk-nav-p')) $('lk-nav-p').style.display = 'block';
    if ($('lk-btn-pt')) $('lk-btn-pt').style.color = '#c084fc';
    lkRender();
  }
  function lkExitPrivate() {
    lkPrivMode = false;
    lkCur = 'all';
    linkkuOverlay?.classList.remove('lk-pm');
    if ($('lk-nav-n')) $('lk-nav-n').style.display = 'block';
    if ($('lk-nav-p')) $('lk-nav-p').style.display = 'none';
    if ($('lk-btn-pt')) $('lk-btn-pt').style.color = '';
    lkRender();
  }
  function lkTogglePrivate() {
    if (lkPrivMode) {
      lkExitPrivate();
      return;
    }
    if (!lkPinHash) {
      lkToast('Atur PIN dulu di Pengaturan', 'err');
      return;
    }
    lkOpenPin('Masukkan PIN', () => lkEnterPrivate());
  }

  function lkUpdDots(id, buf) {
    document.querySelectorAll(`#${id} .lk-pdot`).forEach((d, idx) => d.classList.toggle('on', idx < buf.length));
  }
  function lkUpdPinSt() {
    const el = $('lk-pin-st');
    if (el) el.textContent = lkPinHash ? 'Sudah diatur' : 'Belum diatur';
  }
  function lkOpenPin(title, cb, rawMode = false) {
    lkPinBuf = '';
    lkPinCb = cb;
    lkPinRawMode = rawMode;
    if ($('lk-pin-tit')) $('lk-pin-tit').textContent = title;
    if ($('lk-p-err')) $('lk-p-err').textContent = '';
    lkUpdDots('lk-p-dots', lkPinBuf);
    lkOpenOv('lk-ov-pin');
  }
  async function lkCheckPin() {
    if (lkPinBuf.length < 4) return;
    if (lkPinRawMode) {
      const cb = lkPinCb;
      const pin = lkPinBuf;
      lkPinCb = null;
      lkPinRawMode = false;
      lkCloseOv('lk-ov-pin');
      if (cb) cb(pin);
      return;
    }
    const ok = await lkVerifyPin(lkPinBuf);
    if (ok) {
      const cb = lkPinCb;
      const pin = lkPinBuf;
      lkPinCb = null;
      lkCloseOv('lk-ov-pin');
      if (cb) cb(pin);
    } else {
      if ($('lk-p-err')) $('lk-p-err').textContent = 'PIN salah';
      lkPinBuf = '';
      lkUpdDots('lk-p-dots', lkPinBuf);
      const modal = document.querySelector('#lk-ov-pin .lk-modal');
      modal?.animate?.(
        [{ transform: 'translateX(0)' }, { transform: 'translateX(-6px)' }, { transform: 'translateX(6px)' }, { transform: 'translateX(-4px)' }, { transform: 'translateX(0)' }],
        { duration: 280 }
      );
    }
  }
  function lkOpenSetPin() {
    lkCloseOv('lk-ov-set');
    const startNewPinFlow = () => {
      setTimeout(() => {
        lkSpStep = 1;
        lkSpFirst = '';
        lkSpBuf = '';
        if ($('lk-sp-hint')) $('lk-sp-hint').textContent = 'Masukkan PIN 4 digit baru';
        if ($('lk-sp-err')) $('lk-sp-err').textContent = '';
        lkUpdDots('lk-sp-dots', lkSpBuf);
        lkOpenOv('lk-ov-sp');
      }, 180);
    };
    if (lkPinHash) {
      setTimeout(() => {
        lkOpenPin('Verifikasi PIN Lama', startNewPinFlow);
      }, 180);
    } else {
      startNewPinFlow();
    }
  }
  async function lkCheckSetPin() {
    if (lkSpBuf.length < 4) return;
    if (lkSpStep === 1) {
      lkSpFirst = lkSpBuf;
      lkSpBuf = '';
      lkSpStep = 2;
      if ($('lk-sp-hint')) $('lk-sp-hint').textContent = 'Konfirmasi PIN';
      lkUpdDots('lk-sp-dots', lkSpBuf);
    } else if (lkSpBuf === lkSpFirst) {
      await lkSetPin(lkSpFirst);
      lkCloseOv('lk-ov-sp');
      lkToast('PIN diatur', 'ok');
    } else {
      if ($('lk-sp-err')) $('lk-sp-err').textContent = 'PIN tidak cocok';
      lkSpStep = 1;
      lkSpFirst = '';
      lkSpBuf = '';
      if ($('lk-sp-hint')) $('lk-sp-hint').textContent = 'Masukkan PIN 4 digit baru';
      lkUpdDots('lk-sp-dots', lkSpBuf);
    }
  }

  function lkRenderSB() {
    const pub = lkLinks.filter((l) => !l.p);
    const prv = lkLinks.filter((l) => l.p);
    if ($('lk-cnt-all')) $('lk-cnt-all').textContent = pub.length;
    if ($('lk-cnt-priv')) $('lk-cnt-priv').textContent = prv.length;

    const mkCats = (list, cats, priv) => cats.map((c) => {
      const cnt = list.filter((x) => (priv ? x.pcat === c.id : x.cat === c.id)).length;
      if (priv && !cnt) return '';
      const act = lkCur === c.id ? 'active' : '';
      return `<div class="lk-ci ${act}" data-cat="${escapeHtml(c.id)}"><div class="lk-ci-l"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg><span class="lk-ci-lbl">${escapeHtml(c.n)}</span></div><span class="lk-ccnt">${cnt}</span></div>`;
    }).join('');

    if ($('lk-cat-list')) $('lk-cat-list').innerHTML = mkCats(pub, lkCats, false);
    if ($('lk-cat-list-p')) {
      $('lk-cat-list-p').innerHTML = mkCats(prv, lkPrivCats, true)
        || '<div style="padding:5px 14px; font-family:var(--lk-fm); font-size:10px; color:var(--lk-text3)">Belum ada</div>';
    }

    document.querySelectorAll(`${lkPrivMode ? '#lk-nav-p' : '#lk-nav-n'} .lk-ci[data-cat]`).forEach((el) => {
      el.classList.toggle('active', el.dataset.cat === lkCur);
    });
  }

  function lkFiltered() {
    let rows = lkLinks.filter((l) => (lkPrivMode ? Boolean(l.p) : !l.p));
    if (lkCur !== 'all' && lkCur !== 'private-all') {
      rows = lkPrivMode ? rows.filter((l) => l.pcat === lkCur) : rows.filter((l) => l.cat === lkCur);
    }
    if (lkQuery) {
      const lq = lkQuery.toLowerCase();
      rows = rows.filter((l) =>
        (l.ti || '').toLowerCase().includes(lq)
        || (l.url || '').toLowerCase().includes(lq)
        || (l.note || '').toLowerCase().includes(lq)
        || (l.tags || []).some((t) => String(t).toLowerCase().includes(lq))
      );
    }
    if (lkFlt === 'recent') rows = rows.slice().sort((a, b) => (b.ts || 0) - (a.ts || 0)).slice(0, 20);
    return rows;
  }

  function lkCardHtml(l) {
    const dom = lkGetDom(l.url);
    const fav = `https://www.google.com/s2/favicons?domain=${encodeURIComponent(dom)}&sz=32`;
    const cat = lkPrivMode
      ? lkPrivCats.find((c) => c.id === l.pcat)
      : lkCats.find((c) => c.id === l.cat);
    const tags = (l.tags || []).map((t) => `<span class="lk-ctag">${escapeHtml(t)}</span>`).join('');
    let hue = 0;
    for (let i = 0; i < dom.length; i += 1) hue = (hue * 31 + dom.charCodeAt(i)) % 360;
    const fbStyle = `style="--cfav-bg:hsl(${hue},16%,13%)"`;
    const initLetter = escapeHtml((l.ti || dom || '?').charAt(0).toUpperCase());
    const thumb = l.thumb
      ? `<img src="${escapeHtml(l.thumb)}" alt="" loading="lazy" data-lk-thumb="1">`
      : `<div class="lk-thumb-fb" ${fbStyle}><img class="lk-fav-big" src="${fav}" alt="" loading="lazy"><div class="lk-cletter" style="display:none">${initLetter}</div></div>`;
    const titleHtml = l.ti
      ? `<div class="lk-ctitle2">${escapeHtml(l.ti)}</div>`
      : `<div class="lk-ctitle2" style="color:var(--lk-text3);font-style:italic">${escapeHtml(dom)}</div>`;
    return `<div class="lk-card" data-id="${escapeHtml(l.id)}">
      <div class="lk-thumb" data-letter="${initLetter}">${thumb}</div>
      <div class="lk-cbody">
        <div class="lk-csrc"><img src="${fav}" alt=""><span>${escapeHtml(dom)}</span>${cat ? `<span style="color:var(--lk-text3)"> · ${escapeHtml(cat.n)}</span>` : ''}</div>
        ${titleHtml}
        ${l.note ? `<div class="lk-cnote" data-lk-note="${escapeHtml(l.id)}">${escapeHtml(l.note)}</div><div class="lk-cnote-arr" data-lk-toggle-note="${escapeHtml(l.id)}"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg></div>` : ''}
        ${tags ? `<div class="lk-ctags">${tags}</div>` : ''}
      </div>
      <div class="lk-cfoot">
        <span class="lk-cdate">${lkFmtDate(l.ts)}</span>
        <div class="lk-cacts">
          <button type="button" class="lk-cbtn" data-lk-act="copy" data-id="${escapeHtml(l.id)}" title="Salin URL"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg></button>
          <button type="button" class="lk-cbtn" data-lk-act="edit" data-id="${escapeHtml(l.id)}" title="Edit"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
          <button type="button" class="lk-cbtn del" data-lk-act="del" data-id="${escapeHtml(l.id)}" title="Hapus"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></button>
        </div>
      </div>
    </div>`;
  }

  function lkRenderCards() {
    const grid = $('lk-grid');
    if (!grid) return;
    const rows = lkFiltered();
    let title = 'Semua Link';
    if (lkPrivMode) {
      title = lkCur === 'private-all' ? 'Semua Privat' : (lkPrivCats.find((c) => c.id === lkCur)?.n || 'Kategori Privat');
    } else if (lkCur !== 'all') {
      title = lkCats.find((c) => c.id === lkCur)?.n || 'Kategori';
    }
    if ($('lk-pg-title')) $('lk-pg-title').textContent = title;
    if ($('lk-pg-count')) $('lk-pg-count').textContent = `${rows.length} link`;

    if (!rows.length) {
      const isSearch = Boolean(lkQuery);
      const isCat = lkCur !== 'all' && lkCur !== 'private-all';
      let icon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>';
      let emptyTitle = 'Belum ada link';
      let emptySub = 'Simpan link favoritmu di sini, akses kapan saja.';
      if (isSearch) {
        icon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>';
        emptyTitle = 'Tidak ditemukan';
        emptySub = 'Coba kata kunci lain';
      } else if (isCat) {
        icon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>';
        emptyTitle = 'Kategori kosong';
        emptySub = 'Belum ada link di kategori ini. Tap tombol + di bawah.';
      }
      grid.innerHTML = `<div class="lk-empty"><div class="lk-empty-icon">${icon}</div><div class="lk-empty-t">${emptyTitle}</div><div class="lk-empty-s">${emptySub}</div></div>`;
      return;
    }

    grid.innerHTML = rows.map(lkCardHtml).join('');
    grid.querySelectorAll('img[data-lk-thumb]').forEach((img) => {
      img.addEventListener('error', () => {
        const wrap = img.parentElement;
        if (!wrap) return;
        const letter = escapeHtml(wrap.dataset.letter || '?');
        wrap.innerHTML = `<div class="lk-thumb-fb"><div class="lk-cletter">${letter}</div></div>`;
      });
    });
    grid.querySelectorAll('.lk-fav-big').forEach((img) => {
      img.addEventListener('error', () => {
        img.style.display = 'none';
        if (img.nextElementSibling) img.nextElementSibling.style.display = 'flex';
      });
    });
    grid.querySelectorAll('.lk-csrc img').forEach((img) => {
      img.addEventListener('error', () => { img.style.display = 'none'; });
    });
  }

  function lkRender() {
    lkRenderSB();
    lkRenderCards();
    renderLinkkuAppsSummary();
  }

  function lkOpenBS(id) {
    const l = lkLinks.find((x) => x.id === id);
    if (!l) return;
    lkBsId = id;
    const dom = lkGetDom(l.url);
    if ($('lk-bs-title')) $('lk-bs-title').textContent = l.ti || dom;
    if ($('lk-bs-url')) $('lk-bs-url').textContent = dom;
    const img = $('lk-bs-thumb');
    if (img) {
      if (l.thumb) {
        img.src = l.thumb;
        img.style.display = 'block';
      } else {
        img.style.display = 'none';
      }
    }
    const openA = $('lk-bs-open');
    if (openA) {
      const normalized = /^https?:\/\//i.test(l.url) ? l.url : `https://${l.url}`;
      openA.href = normalized;
    }
    $('lk-bs-ov')?.classList.add('open');
  }
  function lkCloseBS() {
    $('lk-bs-ov')?.classList.remove('open');
    lkBsId = null;
  }

  function lkCopyUrl(id) {
    const l = lkLinks.find((x) => x.id === id);
    if (!l) return;
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(l.url).then(() => lkToast('URL disalin', 'ok')).catch(() => {
        lkToast('Gagal menyalin URL', 'err');
      });
      return;
    }
    const ta = document.createElement('textarea');
    ta.value = l.url;
    ta.style.cssText = 'position:fixed;opacity:0;top:0;left:0';
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    try {
      document.execCommand('copy');
      lkToast('URL disalin', 'ok');
    } catch (_) {
      lkToast('Gagal menyalin URL', 'err');
    }
    document.body.removeChild(ta);
  }

  function lkIsPrivForm() {
    return Boolean($('lk-tog-p')?.classList.contains('on'));
  }
  function lkPopCatSel(selectedId) {
    const sel = $('lk-lc');
    if (!sel) return;
    const cats = lkIsPrivForm() ? lkPrivCats : lkCats;
    sel.innerHTML = '<option value="">— Tanpa Kategori —</option>'
      + cats.map((c) => `<option value="${escapeHtml(c.id)}"${selectedId === c.id ? ' selected' : ''}>${escapeHtml(c.n)}</option>`).join('');
  }
  function lkGetAllTags() {
    const prv = lkIsPrivForm();
    const set = new Set();
    lkLinks.filter((l) => (prv ? Boolean(l.p) : !l.p)).forEach((l) => (l.tags || []).forEach((t) => set.add(t)));
    (prv ? lkExtraTagsPriv : lkExtraTagsNormal).forEach((t) => set.add(t));
    return [...set].sort();
  }
  function lkGetSelectedTags() {
    return ($('lk-ltg')?.value || '').split(',').map((t) => t.trim()).filter(Boolean);
  }
  function lkSetSelectedTags(tags) {
    if ($('lk-ltg')) $('lk-ltg').value = tags.join(', ');
  }
  function lkRenderTagPicker(selected = []) {
    const picker = $('lk-tag-picker');
    if (!picker) return;
    const allTags = lkGetAllTags();
    const sel = new Set(selected);
    if (!allTags.length) {
      picker.innerHTML = '<span style="font-family:var(--lk-fm);font-size:10px;color:var(--lk-text3)">Belum ada tag — tambahkan di bawah</span>';
      return;
    }
    picker.innerHTML = allTags.map((t) =>
      `<span class="lk-tpick${sel.has(t) ? ' on' : ''}" data-tag="${escapeHtml(t)}">${escapeHtml(t)}<span class="lk-tpick-del" data-del-tag="${escapeHtml(t)}">×</span></span>`
    ).join('');
  }
  function lkResetTagPicker(selected = []) {
    if ($('lk-tag-new-i')) $('lk-tag-new-i').value = '';
    lkSetSelectedTags(selected);
    lkRenderTagPicker(selected);
  }

  function lkShowUP(url) {
    const w = $('lk-upw');
    if (!w) return;
    const d = lkGetDom(url);
    const fav = `https://www.google.com/s2/favicons?domain=${encodeURIComponent(d)}&sz=32`;
    w.style.display = 'block';
    w.innerHTML = `<div class="lk-uprev"><img src="${fav}" alt=""><span class="lk-uprev-t">${escapeHtml(url)}</span><span id="lk-ftag" style="font-size:9px;color:var(--lk-text3)">Fetch…</span></div>`;
    const im = w.querySelector('img');
    im?.addEventListener('error', () => { im.style.display = 'none'; });
  }
  async function lkAutoFetchPreview(url) {
    const ft = $('lk-ftag');
    try {
      const normalized = /^https?:\/\//i.test(url) ? url : `https://${url}`;
      const res = await fetch('/api/linkku/meta', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: normalized }),
      });
      if (!res.ok) { if (ft) ft.textContent = '–'; return; }
      const d = await res.json();
      const tiEl = $('lk-lti');
      if (tiEl && !tiEl.value && d?.title) tiEl.value = d.title;
      if (ft) ft.textContent = d?.title || d?.thumb ? '✓' : '–';
    } catch (_) {
      if (ft) ft.textContent = '–';
    }
  }
  async function lkFetchMetaAndPatch(url, lid) {
    try {
      const normalized = /^https?:\/\//i.test(url) ? url : `https://${url}`;
      const res = await fetch('/api/linkku/meta', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: normalized }),
      });
      if (!res.ok) return;
      const d = await res.json();
      const idx = lkLinks.findIndex((l) => l.id === lid);
      if (idx === -1) return;
      let updated = false;
      if (d?.title && (!lkLinks[idx].ti || lkLinks[idx].ti === lkGetDom(lkLinks[idx].url))) {
        lkLinks[idx].ti = d.title;
        updated = true;
      }
      if (d?.thumb && !lkLinks[idx].thumb) {
        lkLinks[idx].thumb = d.thumb;
        updated = true;
      }
      if (updated) {
        saveEcoDb();
        lkRender();
      }
    } catch (_) {}
  }

  function lkOpenAddModal() {
    lkEditId = null;
    if ($('lk-m-lt')) $('lk-m-lt').textContent = 'Tambah Link';
    ['lk-lu', 'lk-lti', 'lk-ln', 'lk-ltg'].forEach((id) => {
      if ($(id)) $(id).value = '';
    });
    $('lk-tog-p')?.classList.toggle('on', lkPrivMode);
    if ($('lk-upw')) $('lk-upw').style.display = 'none';
    lkResetTagPicker([]);
    lkPopCatSel(lkCur !== 'all' && lkCur !== 'private-all' ? lkCur : '');
    lkOpenOv('lk-ov-l');
    setTimeout(() => $('lk-lu')?.focus(), 160);
  }
  function lkEditLink(id) {
    const l = lkLinks.find((x) => x.id === id);
    if (!l) return;
    lkEditId = id;
    if ($('lk-m-lt')) $('lk-m-lt').textContent = 'Edit Link';
    if ($('lk-lu')) $('lk-lu').value = l.url || '';
    if ($('lk-lti')) $('lk-lti').value = l.ti || '';
    if ($('lk-ln')) $('lk-ln').value = l.note || '';
    $('lk-tog-p')?.classList.toggle('on', Boolean(l.p));
    lkResetTagPicker(l.tags || []);
    lkPopCatSel(l.p ? (l.pcat || '') : (l.cat || ''));
    lkShowUP(l.url);
    lkOpenOv('lk-ov-l');
  }
  function lkDeleteLink(id) {
    lkConfirm('Hapus Link?', 'Link akan dihapus permanen.', () => {
      lkLinks = lkLinks.filter((l) => l.id !== id);
      saveEcoDb();
      lkRender();
      lkToast('Dihapus', 'err');
    });
  }
  function lkSaveLinkForm() {
    const rawUrl = ($('lk-lu')?.value || '').trim();
    if (!rawUrl) {
      lkToast('URL tidak boleh kosong', 'err');
      return;
    }
    const url = /^https?:\/\//i.test(rawUrl) ? rawUrl : `https://${rawUrl}`;
    const ti = ($('lk-lti')?.value || '').trim() || lkGetDom(url);
    const note = ($('lk-ln')?.value || '').trim();
    const catVal = $('lk-lc')?.value || '';
    const tags = lkGetSelectedTags();
    const prv = lkIsPrivForm();
    if (lkEditId) {
      const idx = lkLinks.findIndex((l) => l.id === lkEditId);
      if (idx !== -1) {
        lkLinks[idx] = prv
          ? { ...lkLinks[idx], url, ti, note, pcat: catVal, cat: '', tags, p: true }
          : { ...lkLinks[idx], url, ti, note, cat: catVal, pcat: '', tags, p: false };
      }
      lkToast('Diperbarui', 'ok');
      lkFetchMetaAndPatch(url, lkEditId);
    } else {
      const nl = prv
        ? { id: lkGenId(), url, ti, note, pcat: catVal, cat: '', tags, p: true, thumb: lkThumbForUrl(url), ts: Date.now() }
        : { id: lkGenId(), url, ti, note, cat: catVal, pcat: '', tags, p: false, thumb: lkThumbForUrl(url), ts: Date.now() };
      lkLinks.unshift(nl);
      lkToast('Disimpan', 'ok');
      lkFetchMetaAndPatch(url, nl.id);
    }
    saveEcoDb();
    lkRender();
    lkCloseOv('lk-ov-l');
    lkEditId = null;
  }

  function lkRenderCatMgr() {
    const list = $('lk-cml');
    if (!list) return;
    if (!lkCats.length) {
      list.innerHTML = '<div style="font-family:var(--lk-fm);font-size:11px;color:var(--lk-text3);text-align:center;padding:14px">Belum ada kategori</div>';
      return;
    }
    list.innerHTML = lkCats.map((c) =>
      `<div class="lk-cmi"><div style="display:flex;align-items:center;gap:7px;font-size:12px;font-weight:600"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>${escapeHtml(c.n)}</div><button type="button" class="lk-cbtn del" data-lk-del-cat="${escapeHtml(c.id)}"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></button></div>`
    ).join('');
  }
  function lkRenderPrivCatMgr() {
    const list = $('lk-pcml');
    if (!list) return;
    if (!lkPrivCats.length) {
      list.innerHTML = '<div style="font-family:var(--lk-fm);font-size:11px;color:var(--lk-text3);text-align:center;padding:14px">Belum ada kategori privat</div>';
      return;
    }
    list.innerHTML = lkPrivCats.map((c) =>
      `<div class="lk-cmi"><div style="display:flex;align-items:center;gap:7px;font-size:12px;font-weight:600"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>${escapeHtml(c.n)}</div><button type="button" class="lk-cbtn del" data-lk-del-pcat="${escapeHtml(c.id)}"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></button></div>`
    ).join('');
  }

  async function lkExportData() {
    const privLinks = lkLinks.filter((l) => l.p);
    const pubLinks = lkLinks.filter((l) => !l.p);
    let payload;
    if (privLinks.length > 0 && lkPinHash) {
      try {
        const encryptedPriv = await lkEncryptObj({ links: privLinks, cats: lkPrivCats }, lkPinHash);
        payload = { L: pubLinks, C: lkCats, ep: encryptedPriv, ts: Date.now(), v: 5 };
      } catch (_) {
        lkToast('Gagal enkripsi', 'err');
        return;
      }
    } else {
      payload = { L: pubLinks, C: lkCats, PC: lkPrivCats, ts: Date.now(), v: 5 };
    }
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `linkku-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    lkToast('Export berhasil', 'ok');
  }
  function lkDoImportConfirm(allLinks, parsed, privCats = []) {
    lkConfirm('Import Data?', `Import ${allLinks.length} link.`, () => {
      const existingIds = new Set(lkLinks.map((l) => l.id));
      const freshLinks = allLinks.filter((l) => l && l.id && !existingIds.has(l.id));
      lkLinks = [...freshLinks, ...lkLinks];
      const existingCats = new Set(lkCats.map((c) => c.id));
      const inCats = parsed.C || parsed.cats || [];
      lkCats = [...lkCats, ...inCats.filter((c) => c && c.id && !existingCats.has(c.id))];
      const existingPC = new Set(lkPrivCats.map((c) => c.id));
      const inPC = [...(parsed.PC || []), ...privCats];
      lkPrivCats = [...lkPrivCats, ...inPC.filter((c) => c && c.id && !existingPC.has(c.id))];
      saveEcoDb();
      lkRender();
      lkToast(`${freshLinks.length} link diimport`, 'ok');
    }, 'Import', 'lk-bpri');
  }

  function lkResetAllData() {
    lkLinks = [];
    lkCats = [];
    lkPrivCats = [];
    lkPinHash = '';
    lkExtraTagsNormal = [];
    lkExtraTagsPriv = [];
    lkSeededCats = true;
    saveEcoDb();
    lkExitPrivate();
    lkUpdPinSt();
    lkRender();
  }

  function openLinkkuOverlay() {
    window.CuciMoneyModule?.close?.();
    window.IdekuModule?.close?.();
    window.DailyOsModule?.close?.();
    closeKronikOverlay();
    linkkuOverlay?.classList.add('open');
    lkUpdPinSt();
    lkRender();
  }
  function closeLinkkuOverlay() {
    lkCloseSidebar();
    lkCloseBS();
    linkkuOverlay?.classList.remove('open');
  }

  if (linkkuHeroCard) linkkuHeroCard.addEventListener('click', openLinkkuOverlay);
  if (linkkuTileBtn) linkkuTileBtn.addEventListener('click', openLinkkuOverlay);
  $('lk-exit-btn')?.addEventListener('click', closeLinkkuOverlay);
  $('lk-btn-mob')?.addEventListener('click', () => {
    if ($('lk-sidebar')?.classList.contains('open')) lkCloseSidebar();
    else lkOpenSidebar();
  });
  $('lk-sb-backdrop')?.addEventListener('click', lkCloseSidebar);
  $('lk-btn-pt')?.addEventListener('click', lkTogglePrivate);
  $('lk-exit-pm')?.addEventListener('click', () => { lkExitPrivate(); lkCloseSidebar(); });
  $('lk-btn-set')?.addEventListener('click', () => { lkUpdPinSt(); lkOpenOv('lk-ov-set'); });
  $('lk-btn-add')?.addEventListener('click', lkOpenAddModal);

  $('lk-si')?.addEventListener('input', (e) => {
    clearTimeout(lkSearchTimer);
    const val = e.target.value;
    lkSearchTimer = setTimeout(() => {
      lkQuery = val.trim();
      lkRenderCards();
    }, 180);
  });

  document.querySelectorAll('.lk-fchip').forEach((chip) => {
    chip.addEventListener('click', () => {
      lkFlt = chip.dataset.f || 'all';
      document.querySelectorAll('.lk-fchip').forEach((c) => c.classList.toggle('active', c.dataset.f === lkFlt));
      lkRenderCards();
    });
  });

  $('lk-sidebar')?.addEventListener('click', (e) => {
    const item = e.target.closest('.lk-ci[data-cat]');
    if (item) {
      lkCur = item.dataset.cat;
      lkCloseSidebar();
      lkRender();
    }
  });
  $('lk-open-cat-mgr')?.addEventListener('click', () => { lkRenderCatMgr(); lkOpenOv('lk-ov-cat'); });
  $('lk-open-priv-cat-mgr')?.addEventListener('click', () => { lkRenderPrivCatMgr(); lkOpenOv('lk-ov-priv-cat'); });

  // Card Grid Click + Long-Press Delegation
  let lkLpTimer = null;
  let lkLpMoved = false;
  let lkLpTriggered = false;
  const lkGridEl = $('lk-grid');
  if (lkGridEl) {
    lkGridEl.addEventListener('touchstart', (e) => {
      const card = e.target.closest('.lk-card[data-id]');
      if (!card || e.target.closest('.lk-cacts') || e.target.closest('.lk-cnote') || e.target.closest('.lk-cnote-arr')) return;
      lkLpMoved = false;
      lkLpTriggered = false;
      const cid = card.dataset.id;
      lkLpTimer = setTimeout(() => {
        if (!lkLpMoved) {
          lkLpTriggered = true;
          if (navigator.vibrate) navigator.vibrate(40);
          lkOpenBS(cid);
        }
      }, 420);
    }, { passive: true });
    lkGridEl.addEventListener('touchmove', () => {
      lkLpMoved = true;
      clearTimeout(lkLpTimer);
    }, { passive: true });
    lkGridEl.addEventListener('touchend', () => { clearTimeout(lkLpTimer); }, { passive: true });
    lkGridEl.addEventListener('touchcancel', () => { clearTimeout(lkLpTimer); lkLpTriggered = false; }, { passive: true });

    lkGridEl.addEventListener('click', (e) => {
      if (lkLpTriggered) {
        lkLpTriggered = false;
        e.preventDefault();
        return;
      }
      const actBtn = e.target.closest('[data-lk-act]');
      if (actBtn) {
        e.stopPropagation();
        const id = actBtn.dataset.id;
        const act = actBtn.dataset.lkAct;
        if (act === 'copy') lkCopyUrl(id);
        else if (act === 'edit') lkEditLink(id);
        else if (act === 'del') lkDeleteLink(id);
        return;
      }
      const noteToggle = e.target.closest('[data-lk-note], [data-lk-toggle-note]');
      if (noteToggle) {
        e.stopPropagation();
        const nid = noteToggle.dataset.lkNote || noteToggle.dataset.lkToggleNote;
        const noteEl = lkGridEl.querySelector(`[data-lk-note="${nid}"]`);
        const arrEl = lkGridEl.querySelector(`[data-lk-toggle-note="${nid}"]`);
        noteEl?.classList.toggle('exp');
        arrEl?.classList.toggle('exp');
        return;
      }
      const card = e.target.closest('.lk-card[data-id]');
      if (card) {
        const l = lkLinks.find((x) => x.id === card.dataset.id);
        if (l?.url) lkOpenExternal(l.url);
      }
    });
  }

  // Bottom Sheet Events
  $('lk-bs-ov')?.addEventListener('click', (e) => {
    if (e.target === $('lk-bs-ov')) lkCloseBS();
  });
  $('lk-bs-open')?.addEventListener('click', () => { setTimeout(lkCloseBS, 60); });
  $('lk-bs-copy')?.addEventListener('click', () => { if (lkBsId) lkCopyUrl(lkBsId); lkCloseBS(); });
  $('lk-bs-edit')?.addEventListener('click', () => { const id = lkBsId; lkCloseBS(); if (id) lkEditLink(id); });
  $('lk-bs-del')?.addEventListener('click', () => { const id = lkBsId; lkCloseBS(); if (id) lkDeleteLink(id); });

  // Add/Edit Link Modal Events
  $('lk-lu')?.addEventListener('input', (e) => {
    clearTimeout(lkUrlFetchTimer);
    const val = e.target.value.trim();
    if (!val) {
      if ($('lk-upw')) $('lk-upw').style.display = 'none';
      return;
    }
    lkShowUP(val);
    lkUrlFetchTimer = setTimeout(() => lkAutoFetchPreview(val), 850);
  });
  $('lk-tog-p')?.addEventListener('click', () => {
    const tog = $('lk-tog-p');
    if (!tog) return;
    if (!tog.classList.contains('on') && !lkPinHash) {
      lkToast('Atur PIN dulu di Pengaturan', 'err');
      return;
    }
    tog.classList.toggle('on');
    lkPopCatSel('');
    lkRenderTagPicker(lkGetSelectedTags());
  });
  $('lk-save-btn')?.addEventListener('click', lkSaveLinkForm);
  $('lk-tag-add-btn')?.addEventListener('click', () => {
    const inp = $('lk-tag-new-i');
    const t = (inp?.value || '').trim();
    if (!t) return;
    if (inp) inp.value = '';
    const prv = lkIsPrivForm();
    const bucket = prv ? lkExtraTagsPriv : lkExtraTagsNormal;
    if (!bucket.includes(t)) {
      bucket.push(t);
      saveEcoDb();
    }
    const sel = new Set(lkGetSelectedTags());
    sel.add(t);
    lkSetSelectedTags([...sel]);
    lkRenderTagPicker([...sel]);
  });
  $('lk-tag-new-i')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      $('lk-tag-add-btn')?.click();
    }
  });
  $('lk-tag-picker')?.addEventListener('click', (e) => {
    const delBtn = e.target.closest('[data-del-tag]');
    if (delBtn) {
      e.stopPropagation();
      const t = delBtn.dataset.delTag;
      lkConfirm(`Hapus tag "${t}"?`, 'Tag akan dihapus dari semua link di mode ini.', () => {
        const sel = new Set(lkGetSelectedTags());
        sel.delete(t);
        lkSetSelectedTags([...sel]);
        lkExtraTagsNormal = lkExtraTagsNormal.filter((x) => x !== t);
        lkExtraTagsPriv = lkExtraTagsPriv.filter((x) => x !== t);
        const prv = lkIsPrivForm();
        lkLinks = lkLinks.map((l) => ((prv ? Boolean(l.p) : !l.p) ? { ...l, tags: (l.tags || []).filter((x) => x !== t) } : l));
        saveEcoDb();
        lkRender();
        lkRenderTagPicker([...sel]);
        lkToast(`Tag "${t}" dihapus`, 'err');
      });
      return;
    }
    const chip = e.target.closest('.lk-tpick[data-tag]');
    if (chip) {
      const t = chip.dataset.tag;
      const sel = new Set(lkGetSelectedTags());
      if (sel.has(t)) sel.delete(t);
      else sel.add(t);
      lkSetSelectedTags([...sel]);
      lkRenderTagPicker([...sel]);
    }
  });

  // PIN & Set PIN Pad Delegation
  $('lk-p-pad')?.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-pi]');
    if (!btn) return;
    const val = btn.dataset.pi;
    if (val === 'DEL') {
      lkPinBuf = lkPinBuf.slice(0, -1);
      lkUpdDots('lk-p-dots', lkPinBuf);
    } else if (val === 'OK') {
      lkCheckPin();
    } else if (lkPinBuf.length < 4) {
      lkPinBuf += val;
      lkUpdDots('lk-p-dots', lkPinBuf);
      if (lkPinBuf.length === 4) setTimeout(lkCheckPin, 110);
    }
  });
  $('lk-sp-pad')?.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-spi]');
    if (!btn) return;
    const val = btn.dataset.spi;
    if (val === 'DEL') {
      lkSpBuf = lkSpBuf.slice(0, -1);
      lkUpdDots('lk-sp-dots', lkSpBuf);
    } else if (val === 'OK') {
      lkCheckSetPin();
    } else if (lkSpBuf.length < 4) {
      lkSpBuf += val;
      lkUpdDots('lk-sp-dots', lkSpBuf);
      if (lkSpBuf.length === 4) setTimeout(lkCheckSetPin, 110);
    }
  });

  // Settings & Category Manager Events
  $('lk-set-pin-row')?.addEventListener('click', lkOpenSetPin);
  $('lk-export-row')?.addEventListener('click', lkExportData);
  $('lk-import-row')?.addEventListener('click', () => $('lk-imp-i')?.click());
  $('lk-imp-i')?.addEventListener('change', (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (ev) => {
      try {
        const parsed = JSON.parse(String(ev.target?.result || '{}'));
        if (!Array.isArray(parsed.L) && !Array.isArray(parsed.links)) {
          lkToast('Format file tidak valid', 'err');
          return;
        }
        const pubLinks = parsed.L || parsed.links;
        if (parsed.ep) {
          lkCloseOv('lk-ov-set');
          setTimeout(() => {
            lkOpenPin('Masukkan PIN untuk import', async (rawPin) => {
              try {
                const priv = await lkDecryptObj(parsed.ep, await lkHashPin(rawPin));
                await lkSetPin(rawPin);
                lkDoImportConfirm([...pubLinks, ...(priv.links || [])], parsed, priv.cats || []);
              } catch (_) {
                lkToast('Gagal decrypt — PIN salah atau data rusak', 'err');
              }
            }, true);
          }, 180);
        } else {
          lkDoImportConfirm(pubLinks, parsed, parsed.PC || []);
        }
      } catch (_) {
        lkToast('Gagal membaca file JSON', 'err');
      }
      e.target.value = '';
    };
    reader.readAsText(file);
  });
  $('lk-reset-row')?.addEventListener('click', () => {
    lkConfirm('Reset Semua Data?', 'Semua link dan kategori dihapus permanen.', () => {
      lkResetAllData();
      lkCloseOv('lk-ov-set');
      lkToast('Data direset', 'err');
    });
  });

  $('lk-add-cat-btn')?.addEventListener('click', () => {
    const inp = $('lk-nc-n');
    const n = (inp?.value || '').trim();
    if (!n) { lkToast('Nama wajib diisi', 'err'); return; }
    lkCats.push({ id: `${n.toLowerCase().replace(/\s+/g, '-')}-${lkGenId().slice(0, 4)}`, n });
    if (inp) inp.value = '';
    saveEcoDb();
    lkRenderCatMgr();
    lkRenderSB();
    lkToast('Kategori ditambahkan', 'ok');
  });
  $('lk-cml')?.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-lk-del-cat]');
    if (!btn) return;
    const id = btn.dataset.lkDelCat;
    lkConfirm('Hapus Kategori?', 'Link di kategori ini tidak terhapus.', () => {
      lkCats = lkCats.filter((c) => c.id !== id);
      lkLinks = lkLinks.map((l) => (l.cat === id ? { ...l, cat: '' } : l));
      saveEcoDb();
      lkRenderCatMgr();
      lkRender();
      lkToast('Kategori dihapus', 'err');
    });
  });

  $('lk-add-priv-cat-btn')?.addEventListener('click', () => {
    const inp = $('lk-npc-n');
    const n = (inp?.value || '').trim();
    if (!n) { lkToast('Nama wajib diisi', 'err'); return; }
    lkPrivCats.push({ id: `pc-${n.toLowerCase().replace(/\s+/g, '-')}-${lkGenId().slice(0, 4)}`, n });
    if (inp) inp.value = '';
    saveEcoDb();
    lkRenderPrivCatMgr();
    lkRenderSB();
    lkToast('Kategori privat ditambahkan', 'ok');
  });
  $('lk-pcml')?.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-lk-del-pcat]');
    if (!btn) return;
    const id = btn.dataset.lkDelPcat;
    lkConfirm('Hapus Kategori Privat?', 'Link di kategori ini tidak terhapus.', () => {
      lkPrivCats = lkPrivCats.filter((c) => c.id !== id);
      lkLinks = lkLinks.map((l) => (l.pcat === id ? { ...l, pcat: '' } : l));
      saveEcoDb();
      lkRenderPrivCatMgr();
      lkRender();
      lkToast('Kategori privat dihapus', 'err');
    });
  });

  document.querySelectorAll('#linkku-overlay .lk-ov').forEach((ov) => {
    ov.addEventListener('click', (e) => {
      if (e.target === ov) lkCloseOv(ov.id);
    });
  });
  document.querySelectorAll('[data-lk-close]').forEach((btn) => {
    btn.addEventListener('click', () => lkCloseOv(btn.dataset.lkClose));
  });

  if (resetLinkkuBtn) {
    resetLinkkuBtn.addEventListener('click', () => {
      lkResetAllData();
      showToastMessage('Data LinkKu berhasil direset', true);
    });
  }

  renderLinkkuAppsSummary();

  function applyLinkkuTagsFromReply(rawText) {
    let changed = false;
    const shouldOpen = /\[OPEN:linkku\]/i.test(rawText);
    const addRegex = /\[LINKKU:ADD\|([^\]\r\n]+)\]/gi;
    let m;
    while ((m = addRegex.exec(rawText)) !== null) {
      const parts = String(m[1] || '').split('|').map((s) => s.trim());
      const rawUrl = parts[0] || '';
      if (!rawUrl) continue;
      const url = /^https?:\/\//i.test(rawUrl) ? rawUrl : `https://${rawUrl}`;
      const title = parts[1] || lkGetDom(url);
      const note = parts[2] || '';
      const catName = parts[3] || '';
      const tagsRaw = parts[4] || '';
      const tags = tagsRaw ? tagsRaw.split(',').map((t) => t.trim()).filter(Boolean) : [];
      let catId = '';
      if (catName) {
        const found = lkCats.find((c) => c.n.toLowerCase() === catName.toLowerCase() || c.id.toLowerCase() === catName.toLowerCase());
        if (found) catId = found.id;
        else {
          catId = `${catName.toLowerCase().replace(/\s+/g, '-')}-${lkGenId().slice(0, 4)}`;
          lkCats.push({ id: catId, n: catName });
        }
      }
      const nl = {
        id: lkGenId(),
        url,
        ti: title,
        note,
        cat: catId,
        pcat: '',
        tags,
        p: false,
        thumb: lkThumbForUrl(url),
        ts: Date.now(),
      };
      lkLinks.unshift(nl);
      changed = true;
      lkFetchMetaAndPatch(url, nl.id);
      showToastMessage(`LinkKu: Tersimpan "${title}"`, true);
    }
    if (changed) {
      saveEcoDb();
      lkRender();
    }
    if (shouldOpen) openLinkkuOverlay();
  }

  // ── CuciMoney+ Integration & Bridge ──────────────────────────────────
  const cucimoneyHeroCard = $('cucimoney-hero-card');
  const cucimoneyEyeBtn = $('cucimoney-eye-btn');
  const cucimoneyTileBtn = $('cucimoney-tile-btn');
  const resetCucimoneyBtn = $('reset-cucimoney-btn');

  function renderCuciMoneyAppsSummary() {
    if (!window.CuciMoneyModule) return;
    const sum = window.CuciMoneyModule.getSummary();
    const bookEl = $('cucimoney-hero-book');
    const balEl = $('cucimoney-hero-balance');
    const expEl = $('cucimoney-hero-today-exp');
    if (bookEl) bookEl.textContent = sum.bookName;
    if (balEl) balEl.textContent = sum.formattedBalance;
    if (expEl) expEl.textContent = sum.formattedTodayExpense;
  }

  function openCuciMoneyOverlay() {
    closeKronikOverlay();
    closeLinkkuOverlay();
    window.IdekuModule?.close?.();
    window.DailyOsModule?.close?.();
    window.CuciMoneyModule?.open?.();
  }

  if (window.CuciMoneyModule) {
    window.CuciMoneyModule.init({
      getDb: () => ecoDb,
      saveDb: saveEcoDb,
      onUpdate: () => {
        renderCuciMoneyAppsSummary();
        renderIdekuAppsSummary();
      },
      showMainToast: showToastMessage,
    });
    renderCuciMoneyAppsSummary();
  }

  if (cucimoneyHeroCard) cucimoneyHeroCard.addEventListener('click', openCuciMoneyOverlay);
  if (cucimoneyTileBtn) cucimoneyTileBtn.addEventListener('click', openCuciMoneyOverlay);
  if (cucimoneyEyeBtn) {
    cucimoneyEyeBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      window.CuciMoneyModule?.toggleVisibility?.();
      renderCuciMoneyAppsSummary();
    });
  }
  if (resetCucimoneyBtn) {
    resetCucimoneyBtn.addEventListener('click', () => {
      window.CuciMoneyModule?.resetData?.();
      renderCuciMoneyAppsSummary();
      showToastMessage('Data CuciMoney+ berhasil direset', true);
    });
  }

  function applyCuciTagsFromReply(rawText) {
    if (!window.CuciMoneyModule) return;
    const shouldOpen = /\[OPEN:cucimoney\]/i.test(rawText);
    const addRegex = /\[CUCI:ADD\|([^\]\r\n]+)\]/gi;
    let m;
    let changed = false;
    while ((m = addRegex.exec(rawText)) !== null) {
      const parts = String(m[1] || '').split('|').map((s) => s.trim());
      const resLabel = window.CuciMoneyModule.applyTagAdd(parts);
      if (resLabel) {
        changed = true;
        showToastMessage(`CuciMoney+: Tercatat "${resLabel}"`, true);
      }
    }
    if (changed) {
      renderCuciMoneyAppsSummary();
      renderIdekuAppsSummary();
    }
    if (shouldOpen) openCuciMoneyOverlay();
  }

  // ── IdeKu Integration & Bridge ───────────────────────────────────────
  const idekuHeroCard = $('ideku-hero-card');
  const idekuTileBtn = $('ideku-tile-btn');
  const resetIdekuBtn = $('reset-ideku-btn');

  function renderIdekuAppsSummary() {
    if (!window.IdekuModule) return;
    const sum = window.IdekuModule.getSummary();
    const countEl = $('ideku-hero-count');
    const focusEl = $('ideku-hero-focus');
    if (countEl) countEl.textContent = sum.countText;
    if (focusEl) focusEl.textContent = sum.focusText;
  }

  function openIdekuOverlay() {
    closeKronikOverlay();
    closeLinkkuOverlay();
    window.CuciMoneyModule?.close?.();
    window.DailyOsModule?.close?.();
    window.IdekuModule?.open?.();
  }

  if (window.IdekuModule) {
    window.IdekuModule.init({
      getDb: () => ecoDb,
      saveDb: saveEcoDb,
      onUpdate: () => {
        renderIdekuAppsSummary();
        renderCuciMoneyAppsSummary();
      },
      showMainToast: showToastMessage,
    });
    renderIdekuAppsSummary();
  }

  if (idekuHeroCard) idekuHeroCard.addEventListener('click', openIdekuOverlay);
  if (idekuTileBtn) idekuTileBtn.addEventListener('click', openIdekuOverlay);
  if (resetIdekuBtn) {
    resetIdekuBtn.addEventListener('click', () => {
      window.IdekuModule?.resetData?.();
      renderIdekuAppsSummary();
      showToastMessage('Data IdeKu berhasil direset', true);
    });
  }

  function applyIdekuTagsFromReply(rawText) {
    if (!window.IdekuModule) return;
    const shouldOpen = /\[OPEN:ideku\]/i.test(rawText);
    const addRegex = /\[IDEKU:ADD\|([^\]\r\n]+)\]/gi;
    let m;
    let changed = false;
    while ((m = addRegex.exec(rawText)) !== null) {
      const parts = String(m[1] || '').split('|').map((s) => s.trim());
      const resLabel = window.IdekuModule.applyTagAdd(parts);
      if (resLabel) {
        changed = true;
        showToastMessage(`IdeKu: Ditambahkan "${resLabel}"`, true);
      }
    }
    if (changed) {
      renderIdekuAppsSummary();
      renderCuciMoneyAppsSummary();
    }
    if (shouldOpen) openIdekuOverlay();
  }

  // ── Daily OS Integration & Bridge ────────────────────────────────────
  const dailyosHeroCard = $('dailyos-hero-card');
  const dailyosTileBtn = $('dailyos-tile-btn');
  const resetDailyosBtn = $('reset-dailyos-btn');

  function renderDailyOsAppsSummary() {
    if (!window.DailyOsModule) return;
    const sum = window.DailyOsModule.getSummary();
    const streakEl = $('dailyos-hero-streak');
    const progEl = $('dailyos-hero-progress-lbl');
    const pctEl = $('dailyos-hero-pct');
    const barEl = $('dailyos-hero-bar');
    const pendEl = $('dailyos-hero-pending');
    if (streakEl) streakEl.textContent = sum.streakText;
    if (progEl) progEl.textContent = sum.progressLabel;
    if (pctEl) pctEl.textContent = `${sum.pct}%`;
    if (barEl) barEl.style.width = `${sum.pct}%`;
    if (pendEl) pendEl.textContent = sum.pendingTodosText;
  }

  function openDailyOsOverlay() {
    closeKronikOverlay();
    closeLinkkuOverlay();
    window.CuciMoneyModule?.close?.();
    window.IdekuModule?.close?.();
    window.DailyOsModule?.open?.();
  }

  if (window.DailyOsModule) {
    window.DailyOsModule.init({
      getDb: () => ecoDb,
      saveDb: saveEcoDb,
      onUpdate: () => {
        renderDailyOsAppsSummary();
        renderIdekuAppsSummary();
      },
      showMainToast: showToastMessage,
    });
    renderDailyOsAppsSummary();
  }

  if (dailyosHeroCard) dailyosHeroCard.addEventListener('click', openDailyOsOverlay);
  if (dailyosTileBtn) dailyosTileBtn.addEventListener('click', openDailyOsOverlay);
  if (resetDailyosBtn) {
    resetDailyosBtn.addEventListener('click', () => {
      window.DailyOsModule?.resetData?.();
      renderDailyOsAppsSummary();
      showToastMessage('Data Daily OS berhasil direset', true);
    });
  }

  function applyDailyOsTagsFromReply(rawText) {
    if (!window.DailyOsModule) return;
    const shouldOpen = /\[OPEN:dailyos\]/i.test(rawText);
    const tagRegex = /\[DAILYOS:([^\]\r\n]+)\]/gi;
    let m;
    let changed = false;
    while ((m = tagRegex.exec(rawText)) !== null) {
      const parts = String(m[1] || '').split('|').map((s) => s.trim());
      const resLabel = window.DailyOsModule.applyTagAction(parts);
      if (resLabel) {
        changed = true;
        showToastMessage(`Daily OS: ${resLabel}`, true);
      }
    }
    if (changed) {
      renderDailyOsAppsSummary();
    }
    if (shouldOpen) openDailyOsOverlay();
  }

  // ── Send Message (/api/chat SSE) ─────────────────────────────────────
  let isThinking = false;
  let remSystemPrompt = 'Kamu adalah Rem — asisten virtual sopan dan perhatian. Sisipkan [EXPR:happy/smile/shame/sad/surprise/none] di akhir jawaban.';

  async function handleSend() {
    const text = msgInput.value.trim();
    if (!text || isThinking) return;
    resetIdleTimer();
    idleBubble.classList.remove('show');
    stopAllAudio();
    ensureAudio();
    if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume().catch(() => {});
    isThinking = true;
    sendBtn.disabled = true;
    msgInput.value = '';

    renderBubble('user', text);
    chatHistory.push({ role: 'user', content: text });
    localStorage.setItem(STORAGE_KEY, JSON.stringify(chatHistory));
    setExpression('puzzle');
    statusEl.textContent = '· mengetik…';
    const replyBubble = renderBubble('assistant', '…');

    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages: chatHistory.slice(-10),
          context: { systemPrompt: buildPersonalizedSystemPrompt(), expression: currentExpr },
        }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);

      const reader = res.body.getReader();
      const dec = new TextDecoder();
      let buf = '', raw = '', finalDone = null;

      const visibleOf = (s) => {
        let v = s
          .replace(/\[JA\][\s\S]*?(\[\/JA\]|$)/gi, '')
          .replace(/\[EXPR:[a-z]+\]/gi, '')
          .replace(/\[CUCI:[^\]\r\n]+\]/gi, '')
          .replace(/\[IDEKU:[^\]\r\n]+\]/gi, '')
          .replace(/\[DAILYOS:[^\]\r\n]+\]/gi, '')
          .replace(/\[KRONIK:[^\]\r\n]+\]/gi, '')
          .replace(/\[LINKKU:[^\]\r\n]+\]/gi, '')
          .replace(/\[OPEN:[a-z0-9-]+\]/gi, '');
        const b = v.lastIndexOf('[');
        return b !== -1 && !v.slice(b).includes(']') ? v.slice(0, b) : v;
      };

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buf += dec.decode(value, { stream: true });
        const events = buf.split('\n\n');
        buf = events.pop() ?? '';
        for (const evt of events) {
          let ev = 'message', data = '';
          for (const line of evt.split('\n')) {
            if (line.startsWith('event:')) ev = line.slice(6).trim();
            else if (line.startsWith('data:')) data += line.slice(5).trim();
          }
          if (!data) continue;
          const p = JSON.parse(data);
          if (ev === 'delta') {
            raw += p.delta || '';
            const vis = visibleOf(raw);
            if (vis) { replyBubble.textContent = vis; messagesEl.scrollTop = messagesEl.scrollHeight; }
          } else if (ev === 'done') {
            finalDone = p;
          } else if (ev === 'error') {
            throw new Error(p.error || 'Stream error');
          }
        }
      }

      const fullRaw = finalDone?.text || visibleOf(raw);
      const exprMatch = fullRaw.match(/\[EXPR:([a-z]+)\]/i);
      const cleanReply = fullRaw
        .replace(/\[CUCI:[^\]\r\n]+\]/gi, '')
        .replace(/\[IDEKU:[^\]\r\n]+\]/gi, '')
        .replace(/\[DAILYOS:[^\]\r\n]+\]/gi, '')
        .replace(/\[KRONIK:[^\]\r\n]+\]/gi, '')
        .replace(/\[LINKKU:[^\]\r\n]+\]/gi, '')
        .replace(/\[OPEN:[a-z0-9-]+\]/gi, '')
        .replace(/\[EXPR:[a-z]+\]/gi, '')
        .replace(/\[JA\][\s\S]*?(\[\/JA\]|$)/gi, '')
        .trim();
      replyBubble.textContent = cleanReply;
      chatHistory.push({ role: 'assistant', content: cleanReply });
      localStorage.setItem(STORAGE_KEY, JSON.stringify(chatHistory));
      saveEcoDb();

      applyKronikTagsFromReply(fullRaw);
      applyLinkkuTagsFromReply(fullRaw);
      applyCuciTagsFromReply(fullRaw);
      applyIdekuTagsFromReply(fullRaw);
      applyDailyOsTagsFromReply(fullRaw);

      if (exprMatch) setExpression(exprMatch[1].toLowerCase());
      else setExpression('none');

      const ttsText = (finalDone?.tts || '').trim() || cleanReply;
      playTTS(ttsText, Boolean(finalDone?.tts));
      if (cleanReply) extractMemoriesInBackground();
    } catch (err) {
      replyBubble.parentElement.remove();
      setExpression('none');
      showToastMessage(`Gagal menghubungi server: ${err.message}`, false);
    } finally {
      isThinking = false;
      sendBtn.disabled = false;
      statusEl.textContent = '· idle';
    }
  }

  sendBtn.addEventListener('click', handleSend);
  msgInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { e.preventDefault(); handleSend(); }
  });

  // ── Bootstrap Live2D Model from Android Assets ───────────────────────
  (async () => {
    try {
      const cfg = await fetch('/live2d/characters/rem/character.json').then((r) => r.json());
      if (cfg?.systemPrompt) remSystemPrompt = cfg.systemPrompt;
      model = await PIXI.live2d.Live2DModel.from('/live2d/characters/rem/Character.model3.json', { autoInteract: false });
      app.stage.addChild(model);
      resizeStage();
      app.ticker.add(onTick, null, -50);
      app.ticker.add(tickRemHand, null, -100);
      loadingBadge.classList.add('hidden');
      setExpression('appeal');
      setTimeout(() => setExpression('none'), 3000);
    } catch (err) {
      loadingBadge.textContent = 'Model Live2D gagal dimuat: ' + (err?.message || err);
    }
  })();
})();
