// api/memory.js
// Ekstraksi fakta personal pengguna secara terstruktur dari riwayat obrolan
// Provider : LLM OpenAI-compatible dari header client (X-LLM-Base/Model/Key)
// Request  : POST { messages: [{ role, content }], existingFacts?: [{ factKey, factValue }] }
// Response : { facts: [{ category, factKey, factValue, confidence }], summary: string | null }

import { resolveLlm, LLM_MISSING_MESSAGE } from './_llm.js';
const VALID_CATEGORIES = new Set(['identitas', 'preferensi', 'kebiasaan', 'tujuan', 'catatan']);

export default async function handler(req, res) {
  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const llm = resolveLlm(req);
  if (!llm) {
    return res.status(200).json({ facts: [], summary: null, skipped: 'llm_not_configured' });
  }

  const { messages, existingFacts } = req.body ?? {};
  if (!Array.isArray(messages) || messages.length === 0) {
    return res.status(400).json({ error: 'messages array diperlukan' });
  }

  const sanitizedTurns = messages
    .filter((m) => m?.role && typeof m.content === 'string' && m.content.trim())
    .slice(-8)
    .map((m) => `${m.role === 'assistant' ? 'Rem' : 'User'}: ${m.content.trim().slice(0, 600)}`);

  if (sanitizedTurns.length === 0) {
    return res.status(200).json({ facts: [], summary: null });
  }

  const knownFactsText = Array.isArray(existingFacts) && existingFacts.length > 0
    ? existingFacts
        .slice(0, 25)
        .map((f) => `- ${f.factKey}: ${f.factValue}`)
        .join('\n')
    : '(belum ada memori tersimpan)';

  const systemPrompt = `Kamu adalah modul ekstraksi memori personal untuk asisten virtual bernama Rem.
Tugasmu: baca potongan percakapan terbaru dan ekstrak HANYA fakta personal yang EKSPLISIT diungkapkan oleh User (bukan ucapan fiksi/roleplay Rem, bukan asumsi atau tebakan).

Kategori valid:
- identitas (nama, panggilan, pekerjaan, domisili, umur, perangkat)
- preferensi (kesukaan, ketidaksukaan, makanan/minuman, gaya bicara, musik, hobi)
- kebiasaan (rutinitas harian, jam tidur/bangun, pola kerja)
- tujuan (rencana, target belajar, proyek, cita-cita)
- catatan (fakta penting lain yang layak diingat jangka panjang)

Aturan ketat:
1. Jawab HANYA dengan objek JSON valid tanpa teks tambahan atau markdown.
2. Format JSON: {"facts":[{"category":"identitas|preferensi|kebiasaan|tujuan|catatan","factKey":"snake_case_singkat","factValue":"nilai ringkas bahasa Indonesia","confidence":0.85}],"summary":"ringkasan 1 kalimat singkat topik obrolan atau null"}
3. Jika User mengubah atau mengoreksi fakta yang sudah ada, gunakan factKey yang sama dengan factValue baru.
4. Jika tidak ada fakta personal baru yang layak disimpan dari percakapan ini, kembalikan {"facts":[],"summary":null}.
5. Jangan ulangi fakta yang nilainya persis sama dengan memori yang sudah ada.`;

  const userPayload = `Memori saat ini:\n${knownFactsText}\n\nPercakapan terbaru:\n${sanitizedTurns.join('\n')}`;

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
          { role: 'user', content: userPayload },
        ],
        temperature: 0.15,
        max_tokens: 450,
        stream: false,
      }),
    });

    if (!llmRes.ok) {
      return res.status(200).json({ facts: [], summary: null, skipped: `upstream_${llmRes.status}` });
    }

    const data = await llmRes.json();
    const rawContent = data?.choices?.[0]?.message?.content ?? '';
    const parsed = parseJsonObject(rawContent);

    const facts = Array.isArray(parsed?.facts)
      ? parsed.facts
          .filter((f) => f && typeof f.factKey === 'string' && typeof f.factValue === 'string')
          .map((f) => {
            const normalizedKey = f.factKey
              .trim()
              .toLowerCase()
              .replace(/[^a-z0-9_]+/g, '_')
              .replace(/^_+|_+$/g, '')
              .slice(0, 48);
            const category = VALID_CATEGORIES.has(String(f.category).toLowerCase())
              ? String(f.category).toLowerCase()
              : 'catatan';
            const value = f.factValue.trim().slice(0, 180);
            const confidence = Math.max(0.3, Math.min(1.0, Number(f.confidence) || 0.8));
            return { category, factKey: normalizedKey, factValue: value, confidence };
          })
          .filter((f) => f.factKey.length >= 2 && f.factValue.length >= 1)
          .slice(0, 6)
      : [];

    const summary = typeof parsed?.summary === 'string' && parsed.summary.trim().length > 4
      ? parsed.summary.trim().slice(0, 220)
      : null;

    return res.status(200).json({ facts, summary });
  } catch (err) {
    console.error('[memory] extraction error:', err);
    return res.status(200).json({ facts: [], summary: null });
  }
}

function parseJsonObject(raw) {
  if (!raw || typeof raw !== 'string') return null;
  const cleaned = raw.replace(/```json\s*/gi, '').replace(/```/g, '').trim();
  try {
    return JSON.parse(cleaned);
  } catch {
    const start = cleaned.indexOf('{');
    const end = cleaned.lastIndexOf('}');
    if (start !== -1 && end > start) {
      try {
        return JSON.parse(cleaned.slice(start, end + 1));
      } catch {
        return null;
      }
    }
    return null;
  }
}
