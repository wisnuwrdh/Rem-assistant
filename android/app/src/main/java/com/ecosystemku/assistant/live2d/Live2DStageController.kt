package com.ecosystemku.assistant.live2d

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.JavascriptInterface
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

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(context: Context): WebView {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        return WebView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
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

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }
            }

            loadUrl("https://appassets.androidplatform.net/assets/live2d/stage.html")
            webView = this
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
