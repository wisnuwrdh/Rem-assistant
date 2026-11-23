// api/_llm.js
// Helper bersama: konfigurasi LLM 100% dari client (tanpa default di server).
// Header yang dikirim app Android dari menu Setelan:
//   X-LLM-Base  : Base URL OpenAI-compatible, mis. https://openrouter.ai/api/v1
//   X-LLM-Model : nama model, mis. openai/gpt-4o-mini
//   X-LLM-Key   : API key
// Endpoint akhir: <Base>/chat/completions (otomatis ditambahkan bila belum ada).

export const LLM_MISSING_MESSAGE =
  'LLM belum dikonfigurasi. Isi Base URL, API Key, dan Model di Setelan.';

function header(req, name) {
  return String(req.headers?.[name] ?? '').trim();
}

export function resolveLlm(req) {
  const base = header(req, 'x-llm-base');
  const model = header(req, 'x-llm-model');
  const key = header(req, 'x-llm-key');
  if (!base || !model || !key) return null;
  if (!/^https?:\/\//i.test(base)) return null;

  let url = base.replace(/\/+$/, '');
  if (!/\/chat\/completions$/i.test(url)) url += '/chat/completions';
  return { url, model, key };
}
