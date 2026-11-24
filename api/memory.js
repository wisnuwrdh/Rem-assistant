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

  const { mode, messages, existingFacts, facts: inputFacts, summaries: inputSummaries, previousSummary } = req.body ?? {};

  if (mode === 'compact_session') {
    const turns = Array.isArray(messages)
      ? messages
          .filter((m) => m?.role && typeof m.content === 'string' && m.content.trim())
          .slice(0, 24)
          .map((m) => `${m.role === 'assistant' ? 'Rem' : 'Tuan'}: ${m.content.trim().slice(0, 500)}`)
      : [];

    if (!turns.length) {
      return res.status(200).json({ rollingSummary: sanitizePlainText(previousSummary || '') });
    }

    const prevBlock = typeof previousSummary === 'string' && previousSummary.trim()
      ? `Ringkasan konteks sebelumnya:\n${previousSummary.trim()}\n\n`
      : '';

    const compactSystem = `Kamu adalah modul peringkas konteks obrolan (rolling context compaction) untuk asisten virtual bernama Rem.
Tugasmu: gabungkan ringkasan sebelumnya (jika ada) dengan potongan percakapan lama berikut menjadi satu ringkasan padat agar konteks obrolan panjang tetap nyambung.

Aturan ketat:
1. Jawab HANYA teks ringkasan biasa bahasa Indonesia (tanpa JSON, tanpa markdown **tebal**/*miring*/bullet, tanpa tanda em dash —).
2. Pertahankan topik utama, keputusan penting, angka/nama spesifik, pertanyaan terbuka, dan janji/rencana yang disebut.
3. Panjang 3 sampai 6 kalimat ringkas (maksimal 650 karakter).
4. Jangan mengarang fakta di luar isi percakapan.`;

    const compactUser = `${prevBlock}Potongan percakapan yang perlu dipadatkan:\n${turns.join('\n')}`;

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
            { role: 'system', content: compactSystem },
            { role: 'user', content: compactUser },
          ],
          temperature: 0.2,
          max_tokens: 450,
          stream: false,
        }),
      });
      if (!llmRes.ok) {
        return res.status(200).json({ rollingSummary: sanitizePlainText(previousSummary || ''), error: `upstream_${llmRes.status}` });
      }
      const data = await llmRes.json();
      const rawContent = data?.choices?.[0]?.message?.content ?? '';
      const rollingSummary = sanitizePlainText(rawContent).slice(0, 700);
      return res.status(200).json({ rollingSummary });
    } catch (err) {
      console.error('[memory] compact_session error:', err);
      return res.status(200).json({ rollingSummary: sanitizePlainText(previousSummary || '') });
    }
  }

  if (mode === 'profile') {
    const factsList = Array.isArray(inputFacts)
      ? inputFacts.filter((f) => f && f.factKey && f.factValue).slice(0, 60)
      : [];
    const summaryList = Array.isArray(inputSummaries)
      ? inputSummaries.filter((s) => typeof s === 'string' && s.trim()).slice(0, 10)
      : [];

    if (factsList.length === 0 && summaryList.length === 0) {
      return res.status(200).json({ sections: [], updatedAt: Date.now() });
    }

    const factsText = factsList.length
      ? factsList.map((f) => `- [${f.category || 'catatan'}] ${f.factKey}: ${f.factValue}`).join('\n')
      : '(belum ada fakta)';
    const sumText = summaryList.length
      ? summaryList.map((s) => `- ${s}`).join('\n')
      : '(tidak ada)';

    const profileSystem = `Kamu adalah modul penyusun Ringkasan Memori personal untuk asisten virtual bernama Rem.
Tugasmu: rangkum seluruh fakta personal pengguna menjadi dokumen profil naratif yang rapi, hangat, dan mudah dibaca (mirip halaman Ringkasan Memori).

Aturan ketat:
1. Jawab HANYA dengan objek JSON valid tanpa teks tambahan atau markdown:
   {"sections":[{"title":"Judul Bagian","body":"Paragraf naratif bahasa Indonesia"}]}
2. Gunakan sudut pandang orang kedua ("Kamu...", "Fokusmu...", "Saat ini kamu...").
3. Buat 1 sampai 4 bagian yang paling relevan dengan data nyata (misalnya "Gambaran Umum", "Karier & Tujuan", "Kebiasaan & Preferensi", "Catatan Penting"). Jangan mengarang bagian kalau datanya tidak ada.
4. Setiap body berupa 1 paragraf padat dan natural (2-5 kalimat), teks biasa tanpa markdown (**tebal**, *miring*, bullet) dan tanpa tanda em dash (—).
5. Jangan pernah mengarang fakta di luar data yang diberikan.`;

    const profileUser = `Fakta personal pengguna:\n${factsText}\n\nRangkuman sesi percakapan:\n${sumText}`;

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
            { role: 'system', content: profileSystem },
            { role: 'user', content: profileUser },
          ],
          temperature: 0.25,
          max_tokens: 900,
          stream: false,
        }),
      });

      if (!llmRes.ok) {
        return res.status(200).json({ sections: [], error: `upstream_${llmRes.status}` });
      }

      const data = await llmRes.json();
      const rawContent = data?.choices?.[0]?.message?.content ?? '';
      const parsed = parseJsonObject(rawContent);
      const sections = Array.isArray(parsed?.sections)
        ? parsed.sections
            .filter((s) => s && typeof s.title === 'string' && typeof s.body === 'string')
            .map((s) => ({
              title: sanitizePlainText(s.title).slice(0, 60),
              body: sanitizePlainText(s.body).slice(0, 1200),
            }))
            .filter((s) => s.title && s.body)
            .slice(0, 5)
        : [];

      return res.status(200).json({ sections, updatedAt: Date.now() });
    } catch (err) {
      console.error('[memory] profile synthesis error:', err);
      return res.status(200).json({ sections: [], updatedAt: Date.now() });
    }
  }

  if (!Array.isArray(messages) || messages.length === 0) {
    return res.status(400).json({ error: 'messages array diperlukan' });
  }

  const sanitizedTurns = messages
    .filter((m) => m?.role && typeof m.content === 'string' && m.content.trim())
    .slice(-8)
    .map((m) => `${m.role === 'assistant' ? 'Rem' : 'User'}: ${m.content.trim().slice(0, 600)}`);

  if (sanitizedTurns.length === 0) {
    return res.status(200).json({ facts: [], deleteKeys: [], summary: null });
  }

  const knownFactsText = Array.isArray(existingFacts) && existingFacts.length > 0
    ? existingFacts
        .slice(0, 35)
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
2. Format JSON: {"facts":[{"category":"identitas|preferensi|kebiasaan|tujuan|catatan","factKey":"snake_case_singkat","factValue":"nilai ringkas bahasa Indonesia","confidence":0.85}],"deleteKeys":["fact_key_yang_diminta_dihapus"],"summary":"ringkasan 1 kalimat singkat topik obrolan atau null"}
3. Jika User mengubah atau mengoreksi fakta yang sudah ada, gunakan factKey yang sama dengan factValue baru.
4. Jika User secara eksplisit meminta melupakan/menghapus info tertentu atau menyatakan suatu fakta lama tidak lagi berlaku dan harus dihapus, masukkan factKey yang cocok dari daftar Memori saat ini ke "deleteKeys". Jika tidak ada yang dihapus, isi "deleteKeys": [].
5. Jika tidak ada fakta personal baru yang layak disimpan dari percakapan ini, kembalikan {"facts":[],"deleteKeys":[],"summary":null}.
6. Jangan ulangi fakta yang nilainya persis sama dengan memori yang sudah ada.`;

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
        max_tokens: 500,
        stream: false,
      }),
    });

    if (!llmRes.ok) {
      return res.status(200).json({ facts: [], deleteKeys: [], summary: null, skipped: `upstream_${llmRes.status}` });
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

    const deleteKeys = Array.isArray(parsed?.deleteKeys)
      ? parsed.deleteKeys
          .filter((k) => typeof k === 'string')
          .map((k) => k.trim().toLowerCase().replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 48))
          .filter((k) => k.length >= 2)
          .slice(0, 8)
      : [];

    const summary = typeof parsed?.summary === 'string' && parsed.summary.trim().length > 4
      ? parsed.summary.trim().slice(0, 220)
      : null;

    return res.status(200).json({ facts, deleteKeys, summary });
  } catch (err) {
    console.error('[memory] extraction error:', err);
    return res.status(200).json({ facts: [], deleteKeys: [], summary: null });
  }
}

function sanitizePlainText(txt) {
  return String(txt || '')
    .replace(/\*\*/g, '')
    .replace(/__/g, '')
    .replace(/\*([^*\r\n]+)\*/g, '$1')
    .replace(/`([^`\r\n]+)`/g, '$1')
    .replace(/(^|\n)\s*\u2014\s*/g, '$1')
    .replace(/([.!?,:;(\[])\s*\u2014\s*/g, '$1 ')
    .replace(/\s*\u2014\s*/g, ', ')
    .trim();
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
