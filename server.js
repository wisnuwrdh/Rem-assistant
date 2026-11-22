import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import chatHandler from './api/chat.js';
import dailyosHandler from './api/dailyos.js';
import idekuHandler from './api/ideku.js';
import kronikMoodHandler from './api/kronikMood.js';
import linkkuMetaHandler from './api/linkkuMeta.js';
import memoryHandler from './api/memory.js';
import ttsHandler from './api/tts.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = Number(process.env.PORT) || 3000;
const HOST = '0.0.0.0';

app.use('/api', (req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  next();
});

app.use('/api', express.json({ limit: '2mb' }));

app.all('/api/chat', async (req, res) => {
  try {
    await chatHandler(req, res);
  } catch (err) {
    console.error('[server] /api/chat error:', err);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
});

app.all('/api/memory/extract', async (req, res) => {
  try {
    await memoryHandler(req, res);
  } catch (err) {
    console.error('[server] /api/memory/extract error:', err);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
});

app.all('/api/kronik/mood', async (req, res) => {
  try {
    await kronikMoodHandler(req, res);
  } catch (err) {
    console.error('[server] /api/kronik/mood error:', err);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
});

app.all('/api/linkku/meta', async (req, res) => {
  try {
    await linkkuMetaHandler(req, res);
  } catch (err) {
    console.error('[server] /api/linkku/meta error:', err);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
});

app.all('/api/ideku', async (req, res) => {
  try {
    await idekuHandler(req, res);
  } catch (err) {
    console.error('[server] /api/ideku error:', err);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
});

app.all('/api/dailyos', async (req, res) => {
  try {
    await dailyosHandler(req, res);
  } catch (err) {
    console.error('[server] /api/dailyos error:', err);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
});

app.all('/api/tts', async (req, res) => {
  try {
    await ttsHandler(req, res);
  } catch (err) {
    console.error('[server] /api/tts error:', err);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
});

// Serve aset Live2D milik project Android untuk preview web
app.use(
  '/live2d',
  express.static(path.join(__dirname, 'android/app/src/main/assets/live2d'))
);

// Serve web preview non-PWA yang mencerminkan UI 3-tab Android Kotlin (tanpa cache stale di iframe)
app.use('/preview', (_req, res, next) => {
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');
  next();
}, express.static(path.join(__dirname, 'preview'), {
  index: 'index.html',
  etag: false,
  lastModified: false,
}));

app.get('/', (_req, res) => {
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');
  res.sendFile(path.join(__dirname, 'preview/index.html'));
});

app.listen(PORT, HOST, () => {
  console.log(`EcosystemKu backend + preview running at http://${HOST}:${PORT}`);
});
