// api/linkkuMeta.js
// Fetch metadata (title & og:image) dari URL untuk modul Linkku
// Request  : POST { url: string }
// Response : { title: string, thumb: string }

function isBlockedHost(hostname) {
  const h = String(hostname || '').toLowerCase();
  if (!h) return true;
  if (h === 'localhost' || h.endsWith('.local') || h.endsWith('.internal')) return true;
  if (h === '127.0.0.1' || h === '0.0.0.0' || h === '::1') return true;
  if (/^10\./.test(h) || /^192\.168\./.test(h) || /^169\.254\./.test(h)) return true;
  if (/^172\.(1[6-9]|2\d|3[01])\./.test(h)) return true;
  return false;
}

function decodeHtml(str) {
  return String(str || '')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&#39;/gi, "'")
    .replace(/&quot;/gi, '"')
    .replace(/&#x27;/gi, "'")
    .replace(/&#x2F;/gi, '/')
    .trim();
}

function extractMeta(html, attrName, attrVal) {
  const esc = attrVal.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const r1 = new RegExp(`<meta[^>]+${attrName}=["']${esc}["'][^>]+content=["']([^"']+)["']`, 'i');
  const r2 = new RegExp(`<meta[^>]+content=["']([^"']+)["'][^>]+${attrName}=["']${esc}["']`, 'i');
  const m = html.match(r1) || html.match(r2);
  return m?.[1] ? decodeHtml(m[1]) : '';
}

function resolveUrl(candidate, baseUrl) {
  if (!candidate) return '';
  try {
    return new URL(candidate, baseUrl).toString();
  } catch {
    return '';
  }
}

export default async function handler(req, res) {
  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const rawUrl = typeof req.body?.url === 'string' ? req.body.url.trim() : '';
  if (!rawUrl) {
    return res.status(400).json({ title: '', thumb: '', error: 'URL wajib diisi' });
  }

  let parsed;
  try {
    parsed = new URL(rawUrl);
  } catch {
    return res.status(400).json({ title: '', thumb: '', error: 'Format URL tidak valid' });
  }

  if (!['http:', 'https:'].includes(parsed.protocol) || isBlockedHost(parsed.hostname)) {
    return res.status(400).json({ title: '', thumb: '', error: 'URL tidak diizinkan' });
  }

  const ytMatch = rawUrl.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})/);
  if (ytMatch) {
    const thumb = `https://img.youtube.com/vi/${ytMatch[1]}/mqdefault.jpg`;
    let title = '';
    try {
      const oRes = await fetch(`https://www.youtube.com/oembed?url=${encodeURIComponent(rawUrl)}&format=json`, {
        signal: globalThis.AbortSignal?.timeout?.(5000),
      });
      if (oRes.ok) {
        const data = await oRes.json();
        title = typeof data?.title === 'string' ? data.title.trim() : '';
      }
    } catch {
      // ignore oembed failure; thumbnail stays valid
    }
    return res.status(200).json({ title, thumb });
  }

  try {
    const resp = await fetch(parsed.toString(), {
      method: 'GET',
      redirect: 'follow',
      headers: {
        'User-Agent': 'Mozilla/5.0 (compatible; EcosystemKuLinkku/1.0)',
        Accept: 'text/html,application/xhtml+xml;q=0.9,*/*;q=0.8',
      },
      signal: globalThis.AbortSignal?.timeout?.(6000),
    });

    if (!resp.ok) {
      return res.status(200).json({ title: '', thumb: '' });
    }

    const ctype = (resp.headers.get('content-type') || '').toLowerCase();
    if (!ctype.includes('text/html') && !ctype.includes('application/xhtml')) {
      return res.status(200).json({ title: '', thumb: '' });
    }

    const html = (await resp.text()).slice(0, 180000);
    const titleTagMatch = html.match(/<title[^>]*>([^<]+)<\/title>/i);
    const title =
      extractMeta(html, 'property', 'og:title') ||
      extractMeta(html, 'name', 'twitter:title') ||
      (titleTagMatch?.[1] ? decodeHtml(titleTagMatch[1]) : '');

    const rawThumb =
      extractMeta(html, 'property', 'og:image') ||
      extractMeta(html, 'name', 'twitter:image') ||
      extractMeta(html, 'name', 'twitter:image:src');
    const thumb = resolveUrl(rawThumb, parsed.toString());

    return res.status(200).json({ title, thumb });
  } catch (err) {
    console.error('[linkkuMeta] fetch error:', err?.message || err);
    return res.status(200).json({ title: '', thumb: '' });
  }
}
