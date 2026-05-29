import os

file_path = "d:/Projects/StreamPlay/app/src/main/java/com/myselfhridoy/streambdplayer/utils/AddonManager.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add missing imports for Coroutines
imports_to_add = """import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull"""

if "import kotlinx.coroutines.CoroutineScope" not in content:
    content = content.replace("import kotlinx.coroutines.Dispatchers", imports_to_add + "\nimport kotlinx.coroutines.Dispatchers")


# The JavascriptInterface block
js_interface_old = """            webView.addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun onResult(result: String) {
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                @JavascriptInterface
                fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resume("[]")
                    }
                }
            }, interfaceName)"""

js_interface_new = """            webView.addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun onResult(result: String) {
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                @JavascriptInterface
                fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resume("[]")
                    }
                }

                @JavascriptInterface
                fun doFetch(reqId: String, url: String, optionsJson: String?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val requestBuilder = okhttp3.Request.Builder().url(url)
                            if (optionsJson != null && optionsJson != "null" && optionsJson != "undefined") {
                                val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                                val options: Map<String, Any>? = gson.fromJson(optionsJson, type)
                                if (options != null) {
                                    val method = (options["method"] as? String)?.uppercase() ?: "GET"
                                    val headersMap = options["headers"] as? Map<String, String>
                                    headersMap?.forEach { (k, v) ->
                                        requestBuilder.addHeader(k, v)
                                    }
                                    
                                    val bodyStr = options["body"] as? String
                                    if (method == "POST" || method == "PUT" || method == "PATCH") {
                                        val mediaType = "application/json".toMediaTypeOrNull()
                                        val reqBody = (bodyStr ?: "").toRequestBody(mediaType)
                                        requestBuilder.method(method, reqBody)
                                    } else {
                                        requestBuilder.method(method, null)
                                    }
                                }
                            }
                            
                            val response = client.newCall(requestBuilder.build()).execute()
                            val body = response.body?.string() ?: ""
                            val status = response.code
                            
                            withContext(Dispatchers.Main) {
                                val base64Body = android.util.Base64.encodeToString(body.toByteArray(), android.util.Base64.NO_WRAP)
                                webView.evaluateJavascript("javascript:window.onAndroidFetchResponse('$reqId', $status, '$base64Body');", null)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                webView.evaluateJavascript("javascript:window.onAndroidFetchError('$reqId', '${e.message?.replace("'", "\\\\'") ?: "Error"}');", null)
                            }
                        }
                    }
                }
            }, interfaceName)"""

content = content.replace(js_interface_old, js_interface_new)

html_old = """            val html = ${'\"\"\"'}
                <html>
                <head>
                    <script>
                        $cryptoJsCode
                    </script>
                    <script>
                        try {
                            var decodedCode = decodeURIComponent(escape(window.atob("$addonCodeBase64")));
                            var parserFunc = new Function("CryptoJS", decodedCode)(CryptoJS);
                            
                            if (typeof parserFunc === 'function') {
                                parserFunc("$type", "$tmdbId", $seasonArg, $episodeArg)
                                    .then(function(sources) {
                                        AndroidBridge.onResult(JSON.stringify(sources));
                                    })
                                    .catch(function(err) {
                                        AndroidBridge.onError(err.toString());
                                    });
                            } else {
                                AndroidBridge.onError("Parser is not a function");
                            }
                        } catch(e) {
                            AndroidBridge.onError(e.toString());
                        }
                    </script>
                </head>
                <body></body>
                </html>
            ${'\"\"\"'}.trimIndent()"""

html_new = """            val html = ${'\"\"\"'}
                <html>
                <head>
                    <script>
                        $cryptoJsCode
                    </script>
                    <script>
                        window.fetchCallbacks = {};
                        window.fetch = function(url, options) {
                            return new Promise(function(resolve, reject) {
                                var reqId = Math.random().toString(36).substring(7);
                                window.fetchCallbacks[reqId] = { resolve: resolve, reject: reject };
                                var optionsStr = options ? JSON.stringify(options) : null;
                                AndroidBridge.doFetch(reqId, url, optionsStr);
                            });
                        };
                        window.onAndroidFetchResponse = function(reqId, status, base64Body) {
                            var cb = window.fetchCallbacks[reqId];
                            if (cb) {
                                try {
                                    var bodyStr = decodeURIComponent(escape(window.atob(base64Body)));
                                    var response = {
                                        ok: status >= 200 && status < 300,
                                        status: status,
                                        text: function() { return Promise.resolve(bodyStr); },
                                        json: function() { return Promise.resolve(JSON.parse(bodyStr)); }
                                    };
                                    cb.resolve(response);
                                } catch(e) {
                                    cb.reject(e);
                                }
                                delete window.fetchCallbacks[reqId];
                            }
                        };
                        window.onAndroidFetchError = function(reqId, errorMsg) {
                            var cb = window.fetchCallbacks[reqId];
                            if (cb) {
                                cb.reject(new Error(errorMsg));
                                delete window.fetchCallbacks[reqId];
                            }
                        };
                    </script>
                    <script>
                        try {
                            var decodedCode = decodeURIComponent(escape(window.atob("$addonCodeBase64")));
                            var parserFunc = new Function("CryptoJS", decodedCode)(CryptoJS);
                            
                            if (typeof parserFunc === 'function') {
                                parserFunc("$type", "$tmdbId", $seasonArg, $episodeArg)
                                    .then(function(sources) {
                                        AndroidBridge.onResult(JSON.stringify(sources));
                                    })
                                    .catch(function(err) {
                                        AndroidBridge.onError(err.toString());
                                    });
                            } else {
                                AndroidBridge.onError("Parser is not a function");
                            }
                        } catch(e) {
                            AndroidBridge.onError(e.toString());
                        }
                    </script>
                </head>
                <body></body>
                </html>
            ${'\"\"\"'}.trimIndent()"""

content = content.replace(html_old, html_new)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Updated AddonManager.kt")
