package com.ecosystemku.assistant.data.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.ecosystemku.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class LinkkuMetaResult(
    val title: String = "",
    val thumb: String = ""
)

class LinkkuMetaClient(
    private val httpClient: OkHttpClient,
    private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val bitmapCache = object : LruCache<String, Bitmap>(48) {}

    suspend fun fetchMetadata(rawUrl: String): LinkkuMetaResult = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.trim()
        if (cleanUrl.isEmpty()) return@withContext LinkkuMetaResult()

        val ytId = extractYoutubeId(cleanUrl)
        if (ytId != null) {
            val ytThumb = "https://img.youtube.com/vi/$ytId/mqdefault.jpg"
            val ytTitle = runCatching {
                val encoded = URLEncoder.encode(cleanUrl, "UTF-8")
                val req = Request.Builder()
                    .url("https://www.youtube.com/oembed?url=$encoded&format=json")
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) ""
                    else {
                        val body = resp.body?.string().orEmpty()
                        json.parseToJsonElement(body).jsonObject["title"]?.jsonPrimitive?.content.orEmpty()
                    }
                }
            }.getOrDefault("")
            return@withContext LinkkuMetaResult(title = ytTitle, thumb = ytThumb)
        }

        // Try backend proxy first if configured, fallback to direct fetch
        val backendResult = runCatching {
            val payload = JSONObject().put("url", cleanUrl).toString()
            val req = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/linkku/meta")
                .post(payload.toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null
                else {
                    val obj = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
                    val t = obj["title"]?.jsonPrimitive?.content.orEmpty()
                    val th = obj["thumb"]?.jsonPrimitive?.content.orEmpty()
                    if (t.isNotBlank() || th.isNotBlank()) LinkkuMetaResult(t, th) else null
                }
            }
        }.getOrNull()

        if (backendResult != null) return@withContext backendResult

        runCatching {
            val req = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android) EcosystemKuLinkku/1.0")
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use LinkkuMetaResult()
                val ctype = resp.header("Content-Type").orEmpty().lowercase()
                if (!ctype.contains("html") && ctype.isNotBlank()) return@use LinkkuMetaResult()
                val html = resp.body?.string().orEmpty().take(180_000)
                val ogTitle = extractMeta(html, "property", "og:title")
                    .ifBlank { extractMeta(html, "name", "twitter:title") }
                    .ifBlank { extractTitleTag(html) }
                val rawThumb = extractMeta(html, "property", "og:image")
                    .ifBlank { extractMeta(html, "name", "twitter:image") }
                val resolvedThumb = if (rawThumb.isNotBlank()) {
                    runCatching { URL(URL(cleanUrl), rawThumb).toString() }.getOrDefault("")
                } else ""
                LinkkuMetaResult(title = ogTitle, thumb = resolvedThumb)
            }
        }.getOrDefault(LinkkuMetaResult())
    }

    suspend fun loadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        bitmapCache.get(url)?.let { return@withContext it }
        runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android) EcosystemKuLinkku/1.0")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bytes = resp.body?.bytes() ?: return@use null
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@use null
                bitmapCache.put(url, bmp)
                bmp
            }
        }.getOrNull()
    }

    private fun extractYoutubeId(url: String): String? {
        val m = Regex("""(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/shorts/)([a-zA-Z0-9_-]{11})""").find(url)
        return m?.groupValues?.getOrNull(1)
    }

    private fun extractMeta(html: String, attr: String, name: String): String {
        val esc = Regex.escape(name)
        val r1 = Regex("""<meta[^>]+$attr=["']$esc["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val r2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+$attr=["']$esc["']""", RegexOption.IGNORE_CASE)
        val raw = r1.find(html)?.groupValues?.getOrNull(1) ?: r2.find(html)?.groupValues?.getOrNull(1) ?: ""
        return decodeHtml(raw)
    }

    private fun extractTitleTag(html: String): String {
        val m = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)
        return decodeHtml(m?.groupValues?.getOrNull(1).orEmpty())
    }

    private fun decodeHtml(s: String): String {
        return s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .trim()
    }
}
