package com.myselfhridoy.streambdplayer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
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

    private val INTERCEPT_SCRIPT = """
(function() {
    if (window.__snifferInjected) return;
    window.__snifferInjected = true;

    var EXTS = ['.m3u8', '.mp4', '.mpd', '.mkv', '.flv', '.webm', '.ts'];
    var reported = new Set();

    function isMedia(url) {
        if (!url || typeof url !== 'string') return false;
        if (url.startsWith('blob:') || url.startsWith('data:')) return false;
        var lower = url.toLowerCase();
        return EXTS.some(function(e) { return lower.includes(e); });
    }

    function report(url, headersJson) {
        if (!url || reported.has(url)) return;
        reported.add(url);
        try { window.Sniffer.onStreamFound(url, headersJson || '{}'); } catch(e) {}
    }

    function scanText(text) {
        if (!text || typeof text !== 'string') return;
        var matches = text.match(/https?:\/\/[^\s"'\\,\[\]{}]+/g) || [];
        matches.forEach(function(m) { if (isMedia(m)) report(m, '{}'); });
    }

    // 1. XHR
    var oOpen = XMLHttpRequest.prototype.open;
    var oSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function(method, url) {
        this._sUrl = url;
        if (isMedia(url)) report(url, '{}');
        return oOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function() {
        this.addEventListener('load', function() {
            try { 
                var text = this.responseText;
                if (text) {
                    var match = text.match(/https?:\/\/[^\s"'\\]+\.m3u8[^\s"\\]*/i) ||
                                text.match(/https?:\/\/[^\s"'\\]+\.mpd[^\s"\\]*/i);
                    if (match) {
                        var setCookie = this.getResponseHeader('set-cookie') || '';
                        report(match[0], JSON.stringify({"Cookie": setCookie}));
                    }
                    scanText(text); 
                }
            } catch(e) {}
        });
        return oSend.apply(this, arguments);
    };

    // 2. Fetch
    var oFetch = window.fetch;
    window.fetch = function(input, init) {
        var url = typeof input === 'string' ? input : (input && input.url);
        if (isMedia(url)) report(url, '{}');
        return oFetch.apply(this, arguments).then(function(res) {
            res.clone().text().then(scanText).catch(function(){});
            return res;
        });
    };

    // 3. <video> / <source> src property
    var srcDesc = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
    if (srcDesc && srcDesc.set) {
        Object.defineProperty(HTMLMediaElement.prototype, 'src', {
            set: function(v) { if (isMedia(v)) report(v, '{}'); return srcDesc.set.call(this, v); },
            get: function() { return srcDesc.get.call(this); },
            configurable: true
        });
    }

    // 4. setAttribute (src, data-src)
    var oSetAttr = Element.prototype.setAttribute;
    Element.prototype.setAttribute = function(name, value) {
        if ((name === 'src' || name === 'data-src') && isMedia(value)) report(value, '{}');
        return oSetAttr.apply(this, arguments);
    };

    // 5. MutationObserver — dynamically added video/source
    new MutationObserver(function(mutations) {
        mutations.forEach(function(m) {
            m.addedNodes.forEach(function(node) {
                if (!node.querySelectorAll) return;
                node.querySelectorAll('video, source').forEach(function(el) {
                    var s = el.src || el.getAttribute('src');
                    if (isMedia(s)) report(s, '{}');
                });
                if (node.nodeName === 'VIDEO' || node.nodeName === 'SOURCE') {
                    var s = node.src || node.getAttribute('src');
                    if (isMedia(s)) report(s, '{}');
                }
            });
        });
    }).observe(document.documentElement, { childList: true, subtree: true });

    // 6. HLS.js hook
    Object.defineProperty(window, 'Hls', {
        configurable: true,
        set: function(Hls) {
            if (Hls && Hls.prototype && Hls.prototype.loadSource) {
                var oLoad = Hls.prototype.loadSource;
                Hls.prototype.loadSource = function(src) {
                    report(src, '{}');
                    return oLoad.apply(this, arguments);
                };
            }
            Object.defineProperty(window, 'Hls', { value: Hls, writable: true, configurable: true });
        }
    });

    // 7. Shaka Player hook
    Object.defineProperty(window, 'shaka', {
        configurable: true,
        set: function(shaka) {
            try {
                if (shaka && shaka.Player && shaka.Player.prototype.load) {
                    var oLoad = shaka.Player.prototype.load;
                    shaka.Player.prototype.load = function(url) {
                        report(url, '{}');
                        return oLoad.apply(this, arguments);
                    };
                }
            } catch(e) {}
            Object.defineProperty(window, 'shaka', { value: shaka, writable: true, configurable: true });
        }
    });

    // 8. Video.js hook
    Object.defineProperty(window, 'videojs', {
        configurable: true,
        set: function(vjs) {
            if (vjs && vjs.prototype) {
                var oSrc = vjs.prototype.src;
                if (oSrc) {
                    vjs.prototype.src = function(src) {
                        if (typeof src === 'string' && isMedia(src)) report(src, '{}');
                        if (src && src.src && isMedia(src.src)) report(src.src, '{}');
                        return oSrc.apply(this, arguments);
                    };
                }
            }
            Object.defineProperty(window, 'videojs', { value: vjs, writable: true, configurable: true });
        }
    });
})();
""".trimIndent()

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
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
                        webView.post { webView.destroy() }
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

                webView.addJavascriptInterface(object : Any() {
                    @android.webkit.JavascriptInterface
                    fun onStreamFound(streamUrl: String, headersJson: String) {
                        // Stream URL এর domain এর cookie নয়,
                        // পুরো toffeelive.com এর সব cookie নাও
                        val cdnCookie = CookieManager.getInstance().getCookie(streamUrl)
                        val parentCookie = CookieManager.getInstance().getCookie("https://toffeelive.com")
                        
                        val capturedHeaders = mutableMapOf<String, String>()
                        
                        // Edge-Cache-Cookie টা .toffeelive.com এ set হয়,
                        // bldcmprod-cdn.toffeelive.com subdomain তাই পাবে
                        if (!cdnCookie.isNullOrEmpty()) capturedHeaders["Cookie"] = cdnCookie
                        else if (!parentCookie.isNullOrEmpty()) capturedHeaders["Cookie"] = parentCookie
                        
                        // Try parsing headersJson if any other headers were passed
                        try {
                            if (headersJson.isNotBlank() && headersJson != "{}") {
                                val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                                val additionalHeaders: Map<String, String> = com.google.gson.Gson().fromJson(headersJson, type)
                                for ((k, v) in additionalHeaders) {
                                    if (v.isNotBlank()) capturedHeaders[k] = v
                                }
                            }
                        } catch (e: Exception) {}
                        
                        // Referer ও দরকার হতে পারে
                        capturedHeaders["Referer"] = "https://toffeelive.com/"
                        
                        finish(ResolvedToken(url = streamUrl, headers = capturedHeaders))
                    }
                }, "Sniffer")

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                        // Check if it's a media stream
                        if (isMediaStream(reqUrl)) {
                            val capturedHeaders = request.requestHeaders?.toMutableMap() ?: mutableMapOf()
                            
                            val cookieString = CookieManager.getInstance().getCookie(reqUrl)
                            val parentCookie = CookieManager.getInstance().getCookie("https://toffeelive.com")
                            if (!cookieString.isNullOrEmpty()) capturedHeaders["Cookie"] = cookieString
                            else if (!parentCookie.isNullOrEmpty()) capturedHeaders["Cookie"] = parentCookie
                            
                            capturedHeaders["Referer"] = "https://toffeelive.com/"

                            finish(ResolvedToken(url = reqUrl, headers = capturedHeaders))
                            return WebResourceResponse("text/plain", "UTF-8", null) // Block further loading
                        }

                        // Block known ad/tracker domains to speed up sniffing
                        if (isAdOrTracker(reqUrl)) {
                            return WebResourceResponse("text/plain", "UTF-8", null)
                        }

                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.evaluateJavascript(INTERCEPT_SCRIPT, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(INTERCEPT_SCRIPT, null)
                        // SPA route change এর জন্য retry
                        view?.postDelayed({ view.evaluateJavascript(INTERCEPT_SCRIPT, null) }, 1500)
                        view?.postDelayed({ view.evaluateJavascript(INTERCEPT_SCRIPT, null) }, 4000)
                        
                        // Inject a script to simulate a click if the player requires interaction
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
                        webView.post { webView.destroy() }
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
               lowerUrl.contains(".flv") ||
               lowerUrl.contains(".mpd")
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
