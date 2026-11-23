// api/dailyos.js
// AI Coach, 30-day Coach Memory, Smart To-Do Parser, dan Rekomendasi Task untuk Daily OS menggunakan Sumopod AI
// Request  : POST { mode: "coach" | "memory" | "parse_todo" | "recommend_todo", ... }
// Response : mode-specific JSON payload

const SUMOPOD_API_URL = 'https://ai.sumopod.com/v1/chat/completions';
const SUMOPOD_MODEL = 'glm-5.3-flash';

function stripJsonFences(raw) {
  let clean = String(raw || '').trim();
  clean = clean.replace(/```json\s*/gi, '').replace(/```\s*/g, '').trim();
  const firstBracket = clean.indexOf('[');
  const firstBrace = clean.indexOf('{');
  if (firstBracket !== -1 && (firstBrace === -1 || firstBracket < firstBrace)) {
    const lastBracket = clean.lastIndexOf(']');
    if (lastBracket > firstBracket) return clean.slice(firstBracket, lastBracket + 1);
  }
  if (firstBrace !== -1) {
    const lastBrace = clean.lastIndexOf('}');
    if (lastBrace > firstBrace) return clean.slice(firstBrace, lastBrace + 1);
  }
  return clean;
}

function normalizePriority(p) {
  const v = String(p || '').trim().toLowerCase();
  if (v === 'urgent' || v === 'hari ini') return 'urgent';
  if (v === 'soon' || v === 'minggu ini') return 'soon';
  return 'normal';
}

function normalizeDeadline(dl) {
  if (!dl || dl === 'null') return '';
  const s = String(dl).trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;
  return '';
}

export default async function handler(req, res) {
  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const sumopodKey = req.headers['x-sumopod-key']?.trim() || process.env.SUMOPOD_API_KEY;
  if (!sumopodKey) {
    return res.status(503).json({ error: 'SUMOPOD_API_KEY belum dikonfigurasi' });
  }

  const body = req.body || {};
  const mode = typeof body.mode === 'string' ? body.mode.trim() : 'coach';
  const todayLabel = typeof body.todayLabel === 'string' && body.todayLabel.trim()
    ? body.todayLabel.trim()
    : new Date().toLocaleDateString('id-ID', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

  let systemPrompt = '';
  let userPrompt = '';
  let maxTokens = 700;
  let temperature = 0.6;

  if (mode === 'coach') {
    const habitSummary = String(body.habitSummary || '').trim() || '(tidak ada habit dijadwalkan hari ini)';
    const todoSummary = String(body.todoSummary || '').trim() || '(tidak ada task aktif)';
    const memory = body.memory && typeof body.memory === 'object' ? body.memory : null;
    const idekuIdeas = Array.isArray(body.idekuIdeas) ? body.idekuIdeas : [];

    const memorySection = memory && memory.patterns
      ? `\n--- CATATAN MENTOR (pola 30 hari terakhir) ---\nPola umum: ${memory.patterns}\nTitik lemah: ${memory.weakSpots || '-'}\nYang sudah kuat: ${memory.strengths || '-'}\nInsight: ${memory.coachInsight || '-'}\n----------------------------------------------`
      : '';

    const idekuSection = idekuIdeas.length > 0
      ? `\n\nIDE YANG SEDANG DIKERJAKAN USER (dari IdeKu):\n${idekuIdeas.map((i) => `- ${i.title || ''}${i.category ? ` [${i.category}]` : ''}${i.notes ? `: ${i.notes}` : ''}`).join('\n')}\n(Gunakan ini sebagai konteks untuk rekomendasi yang lebih relevan dengan proyek/ide user.)`
      : '';

    systemPrompt =
      'Kamu adalah mentor produktivitas yang sudah kenal user ini. Bicara seperti teman yang jujur — singkat, langsung, kadang nyelekit kalau perlu. Bahasa Indonesia kasual, boleh campur English kalau natural. Hindari kalimat motivasi klise seperti "aku percaya kamu bisa", "mari kita", "komitmen yang kuat". Jangan terdengar seperti bot.';

    userPrompt = `Hari ini ${todayLabel}.

HABIT TERJADWAL HARI INI:
${habitSummary}

TO-DO (dikelompokkan berdasarkan urgensi waktu):
${todoSummary}${memorySection}${idekuSection}

Tulis briefing harian — maksimal 2-3 paragraf pendek, padat, dan langsung ke intinya. Gaya: pesan WhatsApp dari mentor yang kenal kamu baik, bukan pidato motivasi. Kalau ada yang sudah selesai, sebut spesifik dan lanjut. Kalau ada yang belum dan kamu punya data polanya, tantang langsung — jangan basa-basi atau filosofis. Pilih 1-2 hal paling penting untuk disorot, jangan bahas semuanya. Jangan pakai bullet point. Jangan mulai dengan sapaan klise seperti "Hei!", "Wah!", "Oke,", atau "Jadi,". PENTING: todo yang berlabel [BESOK], [MINGGU INI], atau [NANTI] jangan disuruh dikerjakan hari ini — cukup sebut sebagai pengingat kalau perlu, bukan tuntutan.`;
    maxTokens = 600;
    temperature = 0.7;
  } else if (mode === 'memory') {
    const statsText = String(body.statsText || '').trim();
    if (!statsText) {
      return res.status(400).json({ error: 'statsText diperlukan' });
    }
    systemPrompt = 'Kamu adalah analis data perilaku. Return HANYA JSON valid tanpa teks lain, tanpa backtick, tanpa markdown.';
    userPrompt = `Analisis data habit 30 hari terakhir user ini dan buat catatan mentor yang berguna.

Tanggal analisis: ${todayLabel}

DATA HABIT:
${statsText}

Return HANYA JSON valid tanpa penjelasan, tanpa backtick, tanpa markdown. Format:
{
  "patterns": "deskripsi pola konsistensi secara keseluruhan, habit mana yang kuat dan lemah",
  "weakSpots": "habit atau hari spesifik yang sering di-skip, pola yang perlu diperhatikan",
  "strengths": "apa yang sudah berjalan baik, streak atau konsistensi yang patut diapresiasi",
  "coachInsight": "insight untuk coach: cara terbaik mendekati user ini, apakah perlu di-push keras atau didukung, pola avoidance yang terdeteksi",
  "confidence": "high | medium | low (low kalau data kurang dari 7 hari)"
}`;
    maxTokens = 550;
    temperature = 0.3;
  } else if (mode === 'parse_todo') {
    const input = String(body.input || '').trim();
    if (!input) {
      return res.status(400).json({ error: 'Teks input diperlukan' });
    }
    systemPrompt = 'Kamu adalah parser JSON. Return HANYA JSON array valid tanpa teks lain, tanpa backtick, tanpa markdown.';
    userPrompt = `Ekstrak SEMUA task/todo dari teks berikut. Bisa 1 task, bisa banyak — tergantung isi teks.
Return HANYA JSON array tanpa penjelasan, tanpa backtick, tanpa markdown.

Hari ini: ${todayLabel}
Teks: "${input.slice(0, 1500)}"

Format — array of objects:
[
  {
    "judul": "string max 60 karakter",
    "deadline": "YYYY-MM-DD atau null",
    "priority": "urgent | soon | normal",
    "subtasks": ["string"]
  }
]

Aturan judul:
- Judul HARUS mencakup detail spesifik dari teks: durasi (15 menit, 1 jam), target (5 lembar, 10 kali), atau konteks penting lainnya
- Contoh BENAR: "Latihan Reading English 15 Menit", "Baca Buku Psychology 5 Lembar", "Push Up 20 Kali"
- Contoh SALAH: "Latihan Reading", "Baca Buku", "Push Up" — terlalu umum, detail hilang
- Jangan potong informasi penting hanya untuk mempersingkat judul

Aturan priority:
- urgent = hari ini / besok / sekarang
- soon = 2–7 hari ke depan / minggu ini
- normal = lebih dari seminggu / tidak ada urgensi

Subtasks: max 3, isi hanya jika ada langkah persiapan jelas di teks.
Deadline: format YYYY-MM-DD, atau null jika tidak disebutkan.
Pisahkan setiap task/keinginan yang berbeda menjadi objek terpisah.`;
    maxTokens = 700;
    temperature = 0.25;
  } else if (mode === 'recommend_todo') {
    const existingHabitsText = String(body.existingHabitsText || '').trim() || '(tidak ada)';
    const habitGapSummary = String(body.habitGapSummary || '').trim() || '(tidak ada habit hari ini)';
    const doneTodosText = String(body.doneTodosText || '').trim() || '(belum ada)';
    const activeTodosText = String(body.activeTodosText || '').trim() || '(tidak ada)';
    const memory = body.memory && typeof body.memory === 'object' ? body.memory : null;
    const idekuIdeas = Array.isArray(body.idekuIdeas) ? body.idekuIdeas : [];

    const memorySection = memory && memory.patterns
      ? `\nPOLA KEBIASAAN USER (dari analisis 30 hari):\n${memory.patterns}\nTitik lemah: ${memory.weakSpots || '-'}\nInsight: ${memory.coachInsight || '-'}`
      : '';

    const idekuSection = idekuIdeas.length > 0
      ? `\n\nIDE YANG SEDANG DIKERJAKAN USER (dari IdeKu — gunakan sebagai konteks rekomendasi yang lebih relevan):\n${idekuIdeas.map((i) => `- ${i.title || ''}${i.category ? ` [${i.category}]` : ''}${i.notes ? `: ${i.notes}` : ''}`).join('\n')}`
      : '';

    systemPrompt = 'Kamu adalah AI task recommender. Return HANYA JSON array valid tanpa teks lain, tanpa backtick, tanpa markdown.';
    userPrompt = `Kamu adalah AI yang membantu user merekomendasikan task to-do yang relevan dan actionable.

Tanggal hari ini: ${todayLabel}

PRIORITAS KONTEKS:
1) Habit gaps dan progres habit hari ini
2) IdeKu
3) To-do aktif / selesai sebagai konteks, bukan sumber utama

HABIT YANG SUDAH ADA (jangan direkomendasikan sebagai to-do rutin):
${existingHabitsText}

HABIT GAP HARI INI:
${habitGapSummary}

TO-DO YANG SUDAH SELESAI (konteks pekerjaan terakhir):
${doneTodosText}

TO-DO YANG MASIH AKTIF (jangan direkomendasikan lagi):
${activeTodosText}${memorySection}${idekuSection}

Tugas kamu: rekomendasikan task to-do yang BARU, SPESIFIK, dan ONE-OFF — bukan kegiatan rutin yang sudah ada di habit.

Fokus pada:
- Follow-up logis dari to-do yang baru selesai
- Task konkret yang mendukung goal jangka panjang dari to-do aktif
- Task yang muncul dari habit gap atau dari ide di IdeKu

JANGAN rekomendasikan:
- Apapun yang namanya mirip atau sama dengan habit yang sudah ada
- Task yang sudah ada di to-do aktif
- Task generik yang tidak spesifik ke konteks user ini

Buat maksimal 5 rekomendasi yang paling relevan. Kalau yang benar-benar cocok hanya 1-2, cukup segitu. Kalau tidak ada yang cocok, return array kosong [].

Return HANYA JSON array tanpa penjelasan, tanpa backtick, tanpa markdown:
[
  {
    "judul": "string max 60 karakter, spesifik dan actionable",
    "deadline": "YYYY-MM-DD atau null",
    "priority": "urgent | soon | normal",
    "alasan": "1 kalimat singkat kenapa task ini relevan sekarang"
  }
]`;
    maxTokens = 850;
    temperature = 0.5;
  } else {
    return res.status(400).json({ error: `Mode tidak dikenali: ${mode}` });
  }

  try {
    const sumopodRes = await fetch(SUMOPOD_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${sumopodKey}`,
      },
      body: JSON.stringify({
        model: SUMOPOD_MODEL,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt },
        ],
        temperature,
        max_tokens: maxTokens,
        stream: false,
      }),
    });

    if (!sumopodRes.ok) {
      if (sumopodRes.status === 429) {
        return res.status(429).json({ error: 'quota' });
      }
      return res.status(502).json({ error: `AI provider error (${sumopodRes.status})` });
    }

    const data = await sumopodRes.json();
    const raw = (data?.choices?.[0]?.message?.content ?? '').trim();
    if (!raw) {
      return res.status(502).json({ error: 'Respons AI kosong' });
    }

    if (mode === 'coach') {
      return res.status(200).json({ text: raw });
    }

    const cleaned = stripJsonFences(raw);
    let parsed;
    try {
      parsed = JSON.parse(cleaned);
    } catch {
      return res.status(502).json({ error: 'Format respons AI tidak valid' });
    }

    if (mode === 'memory') {
      if (!parsed || typeof parsed !== 'object' || !parsed.patterns || !parsed.coachInsight) {
        return res.status(502).json({ error: 'Data memory AI tidak lengkap' });
      }
      return res.status(200).json({
        memory: {
          patterns: String(parsed.patterns || ''),
          weakSpots: String(parsed.weakSpots || '-'),
          strengths: String(parsed.strengths || '-'),
          coachInsight: String(parsed.coachInsight || ''),
          confidence: String(parsed.confidence || 'medium'),
        },
      });
    }

    const list = Array.isArray(parsed) ? parsed : [parsed];

    if (mode === 'parse_todo') {
      const tasks = list
        .filter((t) => t && typeof t === 'object' && String(t.judul || '').trim())
        .map((t) => ({
          judul: String(t.judul).trim().slice(0, 80),
          deadline: normalizeDeadline(t.deadline),
          priority: normalizePriority(t.priority),
          subtasks: Array.isArray(t.subtasks)
            ? t.subtasks.map((s) => String(s || '').trim()).filter(Boolean).slice(0, 3)
            : [],
        }));
      return res.status(200).json({ tasks });
    }

    if (mode === 'recommend_todo') {
      const recommendations = list
        .filter((t) => t && typeof t === 'object' && String(t.judul || '').trim())
        .slice(0, 5)
        .map((t) => ({
          judul: String(t.judul).trim().slice(0, 80),
          deadline: normalizeDeadline(t.deadline),
          priority: normalizePriority(t.priority),
          alasan: String(t.alasan || '').trim(),
        }));
      return res.status(200).json({ recommendations });
    }

    return res.status(200).json({ text: raw });
  } catch (err) {
    console.error('[dailyos] error:', err);
    return res.status(500).json({ error: 'Gagal menghubungi AI Daily OS' });
  }
}
