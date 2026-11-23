package com.ecosystemku.assistant.live2d

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject

interface Live2DStageController {
    fun setExpression(name: String)
    fun playMotion(name: String)
    fun setMouthOpen(level: Float, isSpeaking: Boolean)
}

interface Live2DStageCallbacks {
    fun onModelReady()
    fun onZoneTapped(zone: String)
    fun onPetTriggered()
    fun onModelError(message: String)
}

class WebViewLive2DStageController(
    private val callbacks: Live2DStageCallbacks
) : Live2DStageController {

    private var webView: WebView? = null
    private var isActive = true

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(context: Context): WebView {
        // Bersihkan instance lama (mis. saat stage dibuat ulang lewat tombol Muat Ulang)
        webView?.let { old ->
            runCatching {
                old.stopLoading()
                old.removeJavascriptInterface("AndroidStageBridge")
                old.destroy()
            }
        }
        webView = null

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Gunakan warna solid DominantBg (#0F0E0C) agar WebGL canvas tidak hilang
            // akibat bug compositing WebView transparan di dalam Compose AndroidView.
            setBackgroundColor(Color.parseColor("#0F0E0C"))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mediaPlaybackRequiresUserGesture = false
            }

            addJavascriptInterface(StageJsBridge(callbacks), "AndroidStageBridge")

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    val msg = "${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
                    if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                        Log.e("Live2DStage", msg)
                    } else {
                        Log.d("Live2DStage", msg)
                    }
                    return super.onConsoleMessage(consoleMessage)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    if (!isActive) evalJs("window.StageAPI?.pause?.();")
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        callbacks.onModelError("webview_error: ${error.description}")
                    }
                }

                // Renderer WebView bisa dimatikan sistem saat memori menipis (sering terjadi
                // ketika sub-app berat terbuka). Tanpa override ini, seluruh aplikasi ikut crash.
                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail
                ): Boolean {
                    if (webView === view) webView = null
                    runCatching { view.destroy() }
                    callbacks.onModelError("renderer_gone")
                    return true
                }
            }

            loadUrl("https://appassets.androidplatform.net/assets/live2d/stage.html")
            webView = this
        }
    }

    /** Hentikan render saat stage tertutup sub-app/tab lain, lanjutkan saat terlihat lagi. */
    fun setActive(active: Boolean) {
        if (isActive == active) return
        isActive = active
        val wv = webView ?: return
        wv.post {
            if (active) {
                wv.evaluateJavascript("window.StageAPI?.resume?.();", null)
            } else {
                wv.evaluateJavascript("window.StageAPI?.pause?.();", null)
            }
        }
    }

    override fun setExpression(name: String) {
        evalJs("window.StageAPI?.setExpression(${JSONObject.quote(name)});")
    }

    override fun playMotion(name: String) {
        evalJs("window.StageAPI?.playMotion(${JSONObject.quote(name)});")
    }

    override fun setMouthOpen(level: Float, isSpeaking: Boolean) {
        val clamped = level.coerceIn(0f, 1f)
        evalJs("window.StageAPI?.setMouthOpen($clamped, $isSpeaking);")
    }

    private fun evalJs(script: String) {
        val wv = webView ?: return
        wv.post { wv.evaluateJavascript(script, null) }
    }

    private class StageJsBridge(
        private val callbacks: Live2DStageCallbacks
    ) {
        @JavascriptInterface
        fun onModelReady() = callbacks.onModelReady()

        @JavascriptInterface
        fun onZoneTapped(zone: String) = callbacks.onZoneTapped(zone)

        @JavascriptInterface
        fun onPetTriggered() = callbacks.onPetTriggered()

        @JavascriptInterface
        fun onModelError(message: String) = callbacks.onModelError(message)
    }
}
