// api/kronikMood.js
// Deteksi mood jurnal Kronik menggunakan Sumopod AI
// Request  : POST { text: string }
// Response : { mood: "Fokus" | "Lelah" | "Terinspirasi" | "Berat" | "Stabil" | "Semangat" | "Kabur" | null }

const SUMOPOD_API_URL = 'https://ai.sumopod.com/v1/chat/completions';
const SUMOPOD_MODEL = 'glm-5.3-flash';
const VALID_MOODS = [
  'Fokus',
  'Lelah',
  'Terinspirasi',
  'Berat',
  'Stabil',
  'Semangat',
  'Kabur',
];

export default async function handler(req, res) {
  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const sumopodKey = req.headers['x-sumopod-key']?.trim() || process.env.SUMOPOD_API_KEY;
  if (!sumopodKey) {
    return res.status(503).json({ mood: null, error: 'SUMOPOD_API_KEY belum dikonfigurasi' });
  }

  const text = typeof req.body?.text === 'string' ? req.body.text.trim() : '';
  if (text.length < 10) {
    return res.status(400).json({ mood: null, error: 'Teks jurnal terlalu singkat' });
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
          {
            role: 'user',
            content: `Analisis teks jurnal berikut dan tentukan satu mood yang paling sesuai dari pilihan ini: ${VALID_MOODS.join(', ')}. Balas hanya dengan satu kata dari pilihan tersebut tanpa tanda baca atau penjelasan.\n\nTeks:\n${text.slice(0, 900)}`,
          },
        ],
        temperature: 0.1,
        max_tokens: 40,
        stream: false,
      }),
    });

    if (!sumopodRes.ok) {
      return res.status(502).json({ mood: null, error: `AI error (${sumopodRes.status})` });
    }

    const data = await sumopodRes.json();
    const raw = (data?.choices?.[0]?.message?.content ?? '').trim().toLowerCase();
    const found =
      VALID_MOODS.find((m) => raw === m.toLowerCase()) ||
      VALID_MOODS.find((m) => raw.includes(m.toLowerCase())) ||
      null;

    return res.status(200).json({ mood: found });
  } catch (err) {
    console.error('[kronikMood] error:', err);
    return res.status(500).json({ mood: null, error: 'Gagal mendeteksi mood' });
  }
}
