package com.myselfhridoy.streambdplayer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object WebViewSniffer {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun sniff(
        context: Context,
        url: String,
        headers: Map<String, String>? = null,
        timeoutMs: Long = 15000L
    ): ResolvedToken? = withContext(Dispatchers.Main) {
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                var isResolved = false
                val webView = WebView(context)

                fun finish(token: ResolvedToken?) {
                    if (!isResolved) {
                        isResolved = true
                        webView.destroy()
                        if (continuation.isActive) {
                            continuation.resume(token)
                        }
                    }
                }

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false // Allow autoplay
                    loadsImagesAutomatically = false // Optimize
                    blockNetworkImage = true // Optimize
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                        // Check if it's a media stream
                        if (isMediaStream(reqUrl)) {
                            val capturedHeaders = request.requestHeaders ?: mapOf()
                            // Pass back the real stream URL and any headers (like Referer) captured by WebView
                            finish(ResolvedToken(url = reqUrl, headers = capturedHeaders))
                            return WebResourceResponse("text/plain", "UTF-8", null) // Block further loading
                        }

                        // Block known ad/tracker domains to speed up sniffing
                        if (isAdOrTracker(reqUrl)) {
                            return WebResourceResponse("text/plain", "UTF-8", null)
                        }

                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject a script to simulate a click if the player requires interaction
                        // This helps bypass "click to play" embeds
                        view?.evaluateJavascript(
                            """
                            (function() {
                                var btns = document.querySelectorAll('button, .play-btn, .plyr__control--overlaid, .vjs-big-play-button');
                                for(var i=0; i<btns.length; i++){
                                    btns[i].click();
                                }
                            })();
                            """.trimIndent(), null
                        )
                    }
                }

                // Load the URL with initial headers if provided
                if (headers != null && headers.isNotEmpty()) {
                    webView.loadUrl(url, headers)
                } else {
                    webView.loadUrl(url)
                }

                continuation.invokeOnCancellation {
                    if (!isResolved) {
                        isResolved = true
                        webView.destroy()
                    }
                }
            }
        }
    }

    private fun isMediaStream(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains(".m3u8") || 
               lowerUrl.contains(".mp4") || 
               lowerUrl.contains(".mkv") || 
               lowerUrl.contains(".flv")
    }

    private fun isAdOrTracker(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("googleads") ||
               lowerUrl.contains("doubleclick.net") ||
               lowerUrl.contains("analytics") ||
               lowerUrl.contains("popads") ||
               lowerUrl.contains("popcash")
    }
}
