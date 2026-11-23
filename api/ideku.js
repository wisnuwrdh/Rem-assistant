// api/ideku.js
// Perkaya Ide & Prompt Breakdown untuk IdeKu menggunakan LLM yang diatur di Setelan
// Request  : POST { title, notes, category, priority, mode: "perkaya" | "breakdown" }
// Response : { result: string }

import { resolveLlm, LLM_MISSING_MESSAGE } from './_llm.js';

export default async function handler(req, res) {
  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const llm = resolveLlm(req);
  if (!llm) {
    return res.status(503).json({ error: LLM_MISSING_MESSAGE });
  }

  const title = typeof req.body?.title === 'string' ? req.body.title.trim() : '';
  const notes = typeof req.body?.notes === 'string' ? req.body.notes.trim() : '';
  const category = typeof req.body?.category === 'string' ? req.body.category.trim() : 'Personal';
  const priority = typeof req.body?.priority === 'string' ? req.body.priority.trim() : 'Nanti';
  const mode = req.body?.mode === 'breakdown' ? 'breakdown' : 'perkaya';

  if (!title) {
    return res.status(400).json({ error: 'Judul ide diperlukan' });
  }

  const priorityCtx = {
    Penting: 'tinggi — eksekusi dalam 1–2 minggu',
    Nanti: 'sedang — bisa dimulai bulan ini',
    'Kapan-kapan': 'rendah — tanpa deadline ketat',
  };

  const systemPrompt =
    mode === 'breakdown'
      ? 'Kamu adalah asisten yang membantu merapikan dan menyusun ide menjadi prompt breakdown yang siap dipakai di AI lain (ChatGPT, Gemini, dll). Tugasmu bukan mengeksekusi breakdown, tapi menghasilkan prompt yang terstruktur, jelas, dan kaya konteks — supaya AI yang menerima prompt ini bisa langsung menghasilkan rencana eksekusi yang konkret dan actionable. Gunakan bahasa Indonesia yang santai.'
      : 'Kamu adalah asisten kreatif dan strategis yang membantu memperkaya ide singkat menjadi catatan yang tajam, konkret, dan siap dieksekusi dalam bahasa Indonesia yang ringkas dan natural.';

  const userPrompt =
    mode === 'breakdown'
      ? `Rapikan ide berikut menjadi prompt breakdown yang siap di-paste ke AI lain:

Judul: ${title}
Kategori: ${category}
Prioritas: ${priority} (${priorityCtx[priority] || 'fleksibel'})${notes ? `\nCatatan: ${notes}` : ''}

Hasilkan prompt yang:
1. Punya konteks ide yang jelas dan lengkap
2. Meminta rencana eksekusi 5–8 langkah konkret dengan estimasi waktu
3. Menyebutkan kategori "${category}" supaya AI bisa menyesuaikan saran tools/platform yang relevan
4. Mencerminkan urgensi berdasarkan prioritas "${priority}"
5. Meminta langkah pertama yang bisa dilakukan hari ini atau minggu ini

Keluarkan hanya teks prompt-nya saja, tanpa pembuka atau penutup tambahan.`
      : `Perkaya ide berikut menjadi catatan ringkas namun kaya wawasan:

Judul: ${title}
Kategori: ${category}
Prioritas: ${priority}${notes ? `\nCatatan awal: ${notes}` : ''}

Berikan:
- Inti konsep & nilai utamanya (2 kalimat)
- 3 langkah awal paling konkret untuk memulai
- Potensi tantangan atau tools/referensi yang relevan

Tulis rapi, padat, langsung ke poin dalam bahasa Indonesia.`;

  try {
    const llmRes = await fetch(llm.url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${llm.key}`,
      },
      body: JSON.stringify({
        model: llm.model,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt },
        ],
        temperature: 0.65,
        max_tokens: 700,
        stream: false,
      }),
    });

    if (!llmRes.ok) {
      if (llmRes.status === 429) {
        return res.status(429).json({ error: 'quota' });
      }
      return res.status(502).json({ error: `AI provider error (${llmRes.status})` });
    }

    const data = await llmRes.json();
    const result = (data?.choices?.[0]?.message?.content ?? '').trim();
    if (!result) {
      return res.status(502).json({ error: 'Respons AI kosong' });
    }

    return res.status(200).json({ result });
  } catch (err) {
    console.error('[ideku] error:', err);
    return res.status(500).json({ error: 'Gagal menghubungi AI IdeKu' });
  }
}
