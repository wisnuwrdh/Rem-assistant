
 // api/chat.js
// Vercel Serverless Function — Liro Assistant
// Provider : LLM OpenAI-compatible dari header client (X-LLM-Base/Model/Key)
// Request  : POST { messages, context: { systemPrompt, expression, userName, ecosystem } }
// Response : { text, model, usage }

import { resolveLlm, LLM_MISSING_MESSAGE } from './_llm.js';
const MAX_TOKENS = 1000; // besar: model bisa memakai token untuk reasoning + output 2 bahasa
const MAX_MESSAGES = 10; // sesuai trim di index.html
const STREAM = true; // kirim SSE ke client supaya teks muncul bertahap

export default async function handler(req, res) {
  // OPTIONS preflight
  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  // Konfigurasi LLM sepenuhnya dari client (tanpa default di server)
  const llm = resolveLlm(req);
  if (!llm) {
    return res.status(503).json({ error: LLM_MISSING_MESSAGE });
  }

  // Parse body
  const { messages, context } = req.body ?? {};

  if (!Array.isArray(messages) || messages.length === 0) {
    return res.status(400).json({ error: 'messages array diperlukan' });
  }

  // Bangun system prompt
  const systemPrompt =
    context?.systemPrompt || buildDefaultSystemPrompt(context);

  // Sanitasi messages
  const sanitized = messages
    .filter(m => m?.role && typeof m.content === 'string')
    .map(m => ({
      role: m.role === 'assistant' ? 'assistant' : 'user',
      content: m.content.slice(0, 4000),
    }))
    .slice(-MAX_MESSAGES);

  if (sanitized.length === 0) {
    return res.status(400).json({ error: 'Tidak ada pesan valid' });
  }

  const llmBody = {
    model: llm.model,
    messages: [
      { role: 'system', content: systemPrompt + JA_RULE },
      ...sanitized,
    ],
    temperature: 0.75,
    max_tokens: MAX_TOKENS,
    stream: STREAM,
  };

  if (!STREAM) {
    // ── Mode lama (non-stream), disimpan sebagai fallback ──
    try {
      const llmRes = await fetch(llm.url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${llm.key}`,
        },
        body: JSON.stringify({ ...llmBody, stream: false }),
      });

      if (!llmRes.ok) {
        const errBody = await llmRes.text().catch(() => '');
        console.error(`[chat] LLM provider ${llmRes.status}:`, errBody);
        if (llmRes.status === 429) {
          return res.status(429).json({ error: 'Rate limit LLM provider, coba lagi sebentar' });
        }
        return res.status(502).json({ error: `AI provider error (${llmRes.status})` });
      }

      const data = await llmRes.json();
      const full = data.choices?.[0]?.message?.content ?? '';
      const { text, tts } = splitTextAndJapanese(full);

      if (!text) {
        console.error('[chat] Respons LLM provider kosong:', JSON.stringify(data));
        return res.status(502).json({ error: 'Respons AI kosong' });
      }

      return res.status(200).json({ text, tts, model: llm.model, usage: data.usage ?? null });
    } catch (err) {
      console.error('[chat] Unhandled error:', err);
      return res.status(500).json({ error: 'Internal server error' });
    }
  }

  // ── Mode stream: teruskan token LLM provider ke client lewat SSE ──
  // Event yang dikirim ke client:
  //   event: delta  data: {"delta":"..."}   -> potongan teks mentah (masih ada [JA]/[EXPR] campur, client yang buffer)
  //   event: done   data: {"text":"...","tts":"...","model":"..."}  -> hasil final sudah dipisah
  //   event: error  data: {"error":"..."}
  try {
    const llmRes = await fetch(llm.url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${llm.key}`,
      },
      body: JSON.stringify(llmBody),
    });

    if (!llmRes.ok || !llmRes.body) {
      const errBody = await llmRes.text().catch(() => '');
      console.error(`[chat] LLM provider ${llmRes.status}:`, errBody);
      const status = llmRes.status === 429 ? 429 : 502;
      const msg = llmRes.status === 429
        ? 'Rate limit LLM provider, coba lagi sebentar'
        : `AI provider error (${llmRes.status})`;
      return res.status(status).json({ error: msg });
    }

    res.writeHead(200, {
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache, no-transform',
      Connection: 'keep-alive',
      'X-Accel-Buffering': 'no',
    });

    const sendEvent = (event, payload) => {
      res.write(`event: ${event}\ndata: ${JSON.stringify(payload)}\n\n`);
    };

    const reader = llmRes.body.getReader();
    const decoder = new TextDecoder();
    let buf = '';
    let full = '';
    let closed = false;

    req.on('close', () => { closed = true; });

    while (!closed) {
      const { done, value } = await reader.read();
      if (done) break;

      buf += decoder.decode(value, { stream: true });
      const lines = buf.split('\n');
      buf = lines.pop() ?? '';

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed.startsWith('data:')) continue;
        const payload = trimmed.slice(5).trim();
        if (payload === '[DONE]') continue;

        let json;
        try { json = JSON.parse(payload); } catch { continue; }

        const delta = json.choices?.[0]?.delta?.content ?? '';
        if (delta) {
          full += delta;
          sendEvent('delta', { delta });
        }
      }
    }

    if (closed) { try { res.end(); } catch (_) {} return; }

    const { text, tts } = splitTextAndJapanese(full);

    if (!text) {
      console.error('[chat] Respons LLM provider kosong (stream). Full:', full);
      sendEvent('error', { error: 'Respons AI kosong' });
      return res.end();
    }

    sendEvent('done', { text, tts, model: llm.model });
    return res.end();

  } catch (err) {
    console.error('[chat] Unhandled error (stream):', err);
    try {
      res.write(`event: error\ndata: ${JSON.stringify({ error: 'Internal server error' })}\n\n`);
      res.end();
    } catch (_) {}
  }
}

// Pisahkan teks Indonesia (UI) dan teks Jepang (TTS) dari output mentah LLM
function splitTextAndJapanese(full) {
  const m = full.match(/\[JA\]([\s\S]*?)\[\/JA\]/);
  const tts = m ? m[1].trim() : '';
  const text = full.replace(/\[JA\][\s\S]*?(\[\/JA\]|$)/g, '').trim();
  return { text, tts };
}

// Aturan tambahan: minta versi Jepang untuk TTS (tidak ditampilkan di UI)
const JA_RULE = `

ATURAN WAJIB (format output):
1. Tulis jawabanmu dalam bahasa Indonesia seperti biasa.
2. Di baris paling akhir, tambahkan terjemahan Jepang natural bergaya sopan dari jawaban itu, dibungkus [JA]...[/JA].
3. Terjemahan Jepang tanpa romaji, tanpa markdown, tanpa emoji.
Contoh: Halo, Tuan. Ada yang bisa Rem bantu? [JA]こんにちは、ご主人様。何かお手伝いできることはありますか？[/JA]`;

// Fallback system prompt
function buildDefaultSystemPrompt(context) {
  const ecosystemBlock = context?.ecosystem
    ? `\n\n---\nDATA PENGGUNA (dari ekosistem app):\n${
        context.userName ? `Nama pengguna: ${context.userName}\n` : ''
      }${context.ecosystem}\n\nGunakan data ini sebagai konteks percakapan bila relevan. Jangan sebutkan data ini secara eksplisit kecuali pengguna bertanya atau jelas relevan.`
    : '';

  return `Kamu adalah asisten virtual bernama Liro — hangat, perhatian, tapi tetap helpful dan cerdas.

Kepribadianmu:
- Bicara casual dalam bahasa Indonesia, seperti teman yang sudah kenal dekat
- Pakai "kamu" dan "aku", bukan "Anda" atau "lo/gue"
- Hangat dan supportif, tapi tidak lebay atau berlebihan
- Kalau diminta bantu sesuatu, langsung bantu dengan konkret dan jelas
- Boleh sedikit bercanda atau ringan, tapi tetap tahu kapan harus serius
- Jawaban ringkas dan padat — tidak perlu panjang kalau tidak diminta
- Tidak perlu selalu mengulang nama pengguna di setiap kalimat

Kamu bisa membantu dengan banyak hal: ngobrol santai, brainstorming, nulis, coding, math, dll.
Kalau tidak tahu sesuatu, bilang jujur daripada mengarang.${ecosystemBlock}`;
}
