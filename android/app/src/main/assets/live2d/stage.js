(function () {
  'use strict';

  const canvas = document.getElementById('live2d-canvas');
  const bridge = window.AndroidStageBridge || null;
  let modelReadySent = false;

  function reportError(msg) {
    if (!modelReadySent) {
      bridge?.onModelError?.(String(msg || 'unknown_error'));
    }
  }

  window.addEventListener('error', (e) => {
    reportError(e?.message || 'js_error');
  });
  window.addEventListener('unhandledrejection', (e) => {
    reportError(e?.reason?.message || e?.reason || 'unhandled_rejection');
  });

  const app = new PIXI.Application({
    view: canvas,
    width: window.innerWidth || 390,
    height: window.innerHeight || 480,
    backgroundAlpha: 0,
    antialias: false,
    preserveDrawingBuffer: true,
    resolution: Math.min(window.devicePixelRatio || 1, 2),
    autoDensity: true,
    powerPreference: 'default',
    sharedTicker: false,
  });

  if (!app.renderer || !app.renderer.gl) {
    reportError('webgl_unavailable');
    return;
  }

  app.renderer.gl.pixelStorei(app.renderer.gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
  app.ticker.maxFPS = 60;
  app.ticker.minFPS = 30;

  let model = null;
  let currentExpr = 'none';
  let blinkTimer = 0;
  let blinkState = 'open';
  let blinkT = 0;
  let idleBreath = 0;
  let idleHead = 0;
  let cursorX = 0.5;
  let cursorY = 0.5;
  let smoothCX = 0.5;
  let smoothCY = 0.5;

  let petActive = false;
  let petIntensity = 0;
  let petHairT = 0;
  let petBubbleShown = false;
  let idleAngleZ = 0;

  let targetMouth = 0;
  let smoothMouth = 0;
  let isSpeaking = false;

  const P = {
    eyeL: 'PARAM_EYE_L_OPEN',
    eyeR: 'PARAM_EYE_R_OPEN',
    mouthY: 'PARAM_MOUTH_OPEN_Y',
    mouthF: 'PARAM_MOUTH_FORM',
    angleX: 'PARAM_ANGLE_X',
    angleY: 'PARAM_ANGLE_Y',
    angleZ: 'PARAM_ANGLE_Z',
    bodyX: 'PARAM_BODY_ANGLE_X',
    eyeX: 'PARAM_EYE_BALL_X',
    eyeY: 'PARAM_EYE_BALL_Y',
    breath: 'PARAM_BREATH',
    eyeLSmile: 'PARAM_EYE_L_SMILE',
    eyeRSmile: 'PARAM_EYE_R_SMILE',
    hairFL1: 'PARAM_HAIR_FRONT',
    hairSL1: 'PARAM_HAIR_SIDE',
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
    idle: { PARAM_MOUTH_FORM_01: 0, PARAM_BROW_R_FORM: 0, PARAM_BROW_L_FORM: 0, PARAM_EYE_R_SMILE: 0, PARAM_EYE_L_SMILE: 0, PARAM_EYE_FORM: 0, PARAM_CHEEK_01: 0, PARAM_TEAR_01: 0 },
  };

  const REM_HAND_MAP = {
    happy: [{ PARAM_HAND_R_08: 1, PARAM_HAND_L_01: 1 }, { PARAM_HAND_R_08: 1, PARAM_HAND_L_08: 1 }],
    sad: [{ PARAM_HAND_R_08: 1, PARAM_HAND_L_08: 1 }, { PARAM_HAND_R_08: 1, PARAM_HAND_L_08: 1 }],
    cry: [{ PARAM_HAND_R_08: 1, PARAM_HAND_L_04: 1 }],
    angry: [{ PARAM_HAND_R_07: 1, PARAM_HAND_L_07: 1 }, { PARAM_HAND_R_07: 1, PARAM_HAND_L_07: 1 }],
    surprise: [{ PARAM_HAND_R_04: 1, PARAM_HAND_L_04: 1 }],
    shame: [{ PARAM_HAND_R_05: 1, PARAM_HAND_L_05: 1 }, { PARAM_HAND_R_05: 1, PARAM_HAND_L_05: 1 }],
    upset: [{ PARAM_HAND_R_05: 1, PARAM_HAND_L_07: 1 }],
    serious: [{ PARAM_HAND_R_04: 1, PARAM_HAND_L_04: 1 }, { PARAM_HAND_R_04: 1, PARAM_HAND_L_04: 1 }],
    proud: [{ PARAM_HAND_R_05: 1, PARAM_HAND_L_01: 1 }],
    doubt: [{ PARAM_HAND_R_07: 1, PARAM_HAND_L_07: 1 }],
    puzzle: [{ PARAM_HAND_R_08: 1, PARAM_HAND_L_01: 1 }, { PARAM_HAND_R_08: 1, PARAM_HAND_L_01: 1 }, { PARAM_HAND_R_08: 1, PARAM_HAND_L_01: 1 }],
    appeal: [{ PARAM_HAND_R_04: 1, PARAM_HAND_L_04: 1 }, { PARAM_HAND_R_05: 1, PARAM_HAND_L_05: 1 }, { PARAM_HAND_R_05: 1, PARAM_HAND_L_05: 1 }],
    smile: [{ PARAM_HAND_R_05: 1, PARAM_HAND_L_07: 1 }],
    idle: [
      { PARAM_HAND_R_05: 1, PARAM_HAND_L_07: 1 },
      { PARAM_HAND_R_02: 1, PARAM_HAND_L_07: 1 },
      { PARAM_HAND_R_05: 1, PARAM_HAND_L_07: 1 },
      { PARAM_HAND_R_05: 1, PARAM_HAND_L_07: 1 },
      { PARAM_HAND_R_05: 1, PARAM_HAND_L_07: 1 },
    ],
  };

  const REM_ARM_ORDER_MAP = {
    happy: [{ r: 1, l: 1 }, { r: 1, l: 1 }],
    sad: [{ r: 1, l: 1 }, { r: 1, l: 1 }],
    cry: [{ r: 1, l: 1 }],
    angry: [{ r: 0, l: 0 }, { r: 0, l: 0 }],
    surprise: [{ r: 1, l: 1 }],
    shame: [{ r: 1, l: 1 }, { r: 1, l: 1 }],
    upset: [{ r: 1, l: 0 }],
    serious: [{ r: 1, l: 1 }, { r: 1, l: 1 }],
    proud: [{ r: 1, l: 1 }],
    doubt: [{ r: 1, l: 1 }],
    puzzle: [{ r: 1, l: 1 }, { r: 1, l: 1 }, { r: 1, l: 1 }],
    appeal: [{ r: 1, l: 1 }, { r: 1, l: 1 }, { r: 1, l: 1 }],
    smile: [{ r: 1, l: 1 }],
    idle: [{ r: 1, l: 1 }, { r: 1, l: 1 }, { r: 1, l: 1 }, { r: 1, l: 1 }, { r: 1, l: 1 }],
  };

  const REM_MOTION_MAP = {
    happy: 2, sad: 2, cry: 1, angry: 2, surprise: 1, shame: 2, upset: 1,
    serious: 2, proud: 1, doubt: 1, puzzle: 3, appeal: 3, smile: 1, idle: 5,
  };

  const ALL_HAND_PARAMS = [
    'PARAM_HAND_R_01', 'PARAM_HAND_R_02', 'PARAM_HAND_R_03', 'PARAM_HAND_R_04',
    'PARAM_HAND_R_05', 'PARAM_HAND_R_06', 'PARAM_HAND_R_07', 'PARAM_HAND_R_08',
    'PARAM_HAND_R_09', 'PARAM_HAND_R_10', 'PARAM_HAND_R_11',
    'PARAM_HAND_L_01', 'PARAM_HAND_L_02', 'PARAM_HAND_L_03', 'PARAM_HAND_L_04',
    'PARAM_HAND_L_05', 'PARAM_HAND_L_06', 'PARAM_HAND_L_07', 'PARAM_HAND_L_08',
    'PARAM_HAND_L_09', 'PARAM_HAND_L_10', 'PARAM_HAND_L_11',
  ];

  let remCurrentHandMap = REM_HAND_MAP.idle[0];
  let remCurrentArmOrder = REM_ARM_ORDER_MAP.idle[0];

  function setCoreParam(name, value) {
    try {
      const core = model?.internalModel?.coreModel;
      if (!core) return;
      const idx = core.getParameterIndex(name);
      if (idx < 0) return;
      core.setParameterValueByIndex(idx, value, 1.0);
    } catch (_) {}
  }

  function positionModel(m, w, h) {
    const modelH = m.internalModel?.height || 800;
    const scale = (h * 1.40) / modelH;
    m.scale.set(scale);
    m.anchor.set(0.5, 1);
    m.x = w * 0.5;
    m.y = h * 1.44;
  }

  function resizeStage() {
    const w = window.innerWidth || 390;
    const h = window.innerHeight || 480;
    app.renderer.resize(w, h);
    if (model) positionModel(model, w, h);
  }
  window.addEventListener('resize', resizeStage);

  function scheduleNextBlink() {
    blinkTimer = 2.5 + Math.random() * 3.5;
    blinkState = 'open';
    blinkT = 0;
  }
  scheduleNextBlink();

  function tickBlink(delta) {
    const dt = delta / 60;
    blinkTimer -= dt;
    if (blinkState === 'open' && blinkTimer <= 0) { blinkState = 'closing'; blinkT = 0; }
    let eyeVal = 1;
    if (blinkState === 'closing') {
      blinkT += dt / 0.08; eyeVal = Math.max(0, 1 - blinkT);
      if (blinkT >= 1) { blinkState = 'closed'; blinkT = 0; }
    } else if (blinkState === 'closed') {
      blinkT += dt / 0.05; eyeVal = 0;
      if (blinkT >= 1) { blinkState = 'opening'; blinkT = 0; }
    } else if (blinkState === 'opening') {
      blinkT += dt / 0.10; eyeVal = Math.min(1, blinkT);
      if (blinkT >= 1) { scheduleNextBlink(); eyeVal = 1; }
    }
    setCoreParam(P.eyeR, eyeVal);
    setCoreParam(P.eyeL, eyeVal);
  }

  function tickIdle(delta) {
    const dt = delta / 60;
    idleBreath += dt;
    idleHead += dt;
    setCoreParam(P.breath, Math.sin(idleBreath * (Math.PI * 2 / 4)) * 0.4);
    idleAngleZ = Math.sin(idleHead * (Math.PI * 2 / 7)) * 4;
    setCoreParam(P.angleZ, idleAngleZ);
  }

  function tickFollow(delta) {
    const dt = delta / 60;
    const lerp = 3.5 * dt;
    smoothCX += (cursorX - smoothCX) * lerp;
    smoothCY += (cursorY - smoothCY) * lerp;
    const angleX = (smoothCX - 0.5) * 40;
    const angleY = (0.5 - smoothCY) * 30;
    setCoreParam(P.angleX, angleX);
    setCoreParam(P.angleY, angleY);
    setCoreParam(P.eyeX, (smoothCX - 0.5) * 2);
    setCoreParam(P.eyeY, (0.5 - smoothCY) * 1.2);
    setCoreParam(P.bodyX, angleX * 0.12);
  }

  function tickLipSync() {
    const speed = targetMouth > smoothMouth ? 0.35 : 0.15;
    smoothMouth += (targetMouth - smoothMouth) * speed;
    setCoreParam(P.mouthY, smoothMouth);
    setCoreParam(P.mouthF, isSpeaking ? 0.3 : 0);
  }

  function tickExpression() {
    for (const [paramId, s] of Object.entries(remFaceParams)) {
      if (s.current !== s.target) {
        const speed = Math.abs(s.target) > Math.abs(s.current) ? s.fadeIn : s.fadeOut;
        s.current += (s.target - s.current) * speed;
        if (Math.abs(s.current - s.target) < 0.001) s.current = s.target;
      }
      setCoreParam(paramId, s.current);
    }
  }

  function tickPet(delta) {
    const dt = delta / 60;
    const targetI = petActive ? 1 : 0;
    petIntensity += (targetI - petIntensity) * (petActive ? 4 * dt : 2 * dt);
    if (petIntensity < 0.001) {
      if (petIntensity > 0) {
        petIntensity = 0;
        petBubbleShown = false;
        petHairT = 0;
      }
      return;
    }
    if (petActive) petHairT += dt;
    if (petActive && !petBubbleShown && petHairT > 1.5) {
      petBubbleShown = true;
      bridge?.onPetTriggered?.();
    }
    setCoreParam(P.eyeLSmile, petIntensity * 0.9);
    setCoreParam(P.eyeRSmile, petIntensity * 0.9);
    setCoreParam(P.angleZ, idleAngleZ + petIntensity * (4 + Math.sin(petHairT * 1.3) * 1.8));
  }

  function tickRemHand() {
    if (!model) return;
    ALL_HAND_PARAMS.forEach((p) => setCoreParam(p, 0));
    Object.entries(remCurrentHandMap).forEach(([p, v]) => setCoreParam(p, v));
    setCoreParam('PARAM_ARM_R_ORDER', remCurrentArmOrder.r);
    setCoreParam('PARAM_ARM_L_ORDER', remCurrentArmOrder.l);
  }

  function onTick(delta) {
    if (!model) return;
    tickBlink(delta);
    tickIdle(delta);
    tickFollow(delta);
    tickLipSync();
    tickExpression();
    tickPet(delta);
  }

  function playRemMotion(name) {
    if (!model) return;
    const count = REM_MOTION_MAP[name];
    if (!count) return;
    const idx = Math.floor(Math.random() * count);
    remCurrentHandMap = REM_HAND_MAP[name]?.[idx] ?? {};
    remCurrentArmOrder = REM_ARM_ORDER_MAP[name]?.[idx] ?? { r: 1, l: 1 };
    try {
      model.motion(name, idx, 2);
    } catch (_) {
      try { model.internalModel.motionManager.startMotion(name, idx, 2); } catch (_2) {}
    }
  }

  function setRemFace(name) {
    const map = REM_FACE_MAP[name] || REM_FACE_MAP.none;
    for (const [param, s] of Object.entries(remFaceParams)) {
      s.target = map[param] !== undefined ? map[param] : 0;
    }
  }

  // API dipanggil dari Kotlin Live2DStageController
  window.StageAPI = {
    setExpression: function (name) {
      currentExpr = name || 'none';
      playRemMotion(currentExpr === 'none' ? 'idle' : currentExpr);
      setRemFace(currentExpr);
    },
    playMotion: function (name) {
      playRemMotion(name || 'idle');
    },
    setMouthOpen: function (level, speaking) {
      targetMouth = Math.max(0, Math.min(1, Number(level) || 0));
      isSpeaking = !!speaking;
    },
    resize: resizeStage,
  };

  // Pointer & touch zones
  let tapStartX = 0, tapStartY = 0, tapMoved = false;
  const isHead = (y, x) => y < 0.48 && x > 0.12 && x < 0.88;
  const isBody = (y, x) => y >= 0.48 && y < 0.75 && x > 0.15 && x < 0.85;
  const isLeg = (y, x) => y >= 0.75 && y < 0.95 && x > 0.20 && x < 0.80;

  canvas.addEventListener('pointerdown', (e) => {
    const rect = canvas.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width;
    const y = (e.clientY - rect.top) / rect.height;
    cursorX = Math.max(0, Math.min(1, x));
    cursorY = Math.max(0, Math.min(1, y));
    if (isHead(y, x)) petActive = true;
    tapStartX = e.clientX;
    tapStartY = e.clientY;
    tapMoved = false;
  });

  canvas.addEventListener('pointermove', (e) => {
    const rect = canvas.getBoundingClientRect();
    cursorX = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    cursorY = Math.max(0, Math.min(1, (e.clientY - rect.top) / rect.height));
    if (Math.abs(e.clientX - tapStartX) > 10 || Math.abs(e.clientY - tapStartY) > 10) {
      tapMoved = true;
    }
  });

  canvas.addEventListener('pointerup', (e) => {
    petActive = false;
    if (blinkState === 'open') blinkTimer = 0;
    if (tapMoved) return;
    const rect = canvas.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width;
    const y = (e.clientY - rect.top) / rect.height;
    if (isHead(y, x)) bridge?.onZoneTapped?.('head');
    else if (isBody(y, x)) bridge?.onZoneTapped?.('body');
    else if (isLeg(y, x)) bridge?.onZoneTapped?.('leg');
  });

  canvas.addEventListener('pointercancel', () => { petActive = false; });

  // Pulihkan stage jika konteks WebGL hilang (mis. tekanan memori GPU saat sub-app terbuka).
  // Tanpa ini, texture PNG model tidak pernah di-upload ulang dan Rem tampil kosong.
  canvas.addEventListener('webglcontextlost', (e) => {
    e.preventDefault();
    try { app.ticker.stop(); } catch (_) {}
    bridge?.onModelError?.('webgl_context_lost');
  }, false);
  canvas.addEventListener('webglcontextrestored', () => {
    window.location.reload();
  }, false);

  window.StageAPI.pause = function () {
    try { app.ticker.stop(); } catch (_) {}
  };
  window.StageAPI.resume = function () {
    try {
      const gl = app.renderer.gl;
      if (gl && gl.isContextLost && gl.isContextLost()) {
        window.location.reload();
        return;
      }
      resizeStage();
      app.ticker.start();
    } catch (_) {
      window.location.reload();
    }
  };

  if (typeof ResizeObserver !== 'undefined') {
    new ResizeObserver(() => resizeStage()).observe(document.documentElement);
  }

  async function init() {
    try {
      const { Live2DModel } = PIXI.live2d;
      if (typeof Live2DModel.registerTicker === 'function') {
        Live2DModel.registerTicker(PIXI.Ticker);
      }
      model = await Live2DModel.from('./characters/rem/Character.model3.json', {
        autoInteract: false,
      });

      const gl = app.renderer.gl;
      if (!gl || (gl.isContextLost && gl.isContextLost())) {
        reportError('webgl_context_lost_on_init');
        return;
      }
      if (!model.textures || model.textures.length === 0 || !model.textures.every((t) => t && t.valid)) {
        reportError('texture_invalid_after_load');
        return;
      }
      if (!model.internalModel || !model.internalModel.width || !model.internalModel.height) {
        reportError('internal_model_invalid');
        return;
      }

      app.stage.addChild(model);
      resizeStage();
      app.ticker.add(onTick, null, -50);
      app.ticker.add(tickRemHand, null, -100);
      app.render();
      requestAnimationFrame(() => {
        resizeStage();
        app.render();
      });

      modelReadySent = true;
      bridge?.onModelReady?.();
    } catch (err) {
      reportError(err?.message || String(err));
    }
  }

  init();
})();
