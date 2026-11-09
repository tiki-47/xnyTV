package com.xny.tvclient.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.RequiresApi

/**
 * WebView ???????? * ?????URL???????????????? */
class WebViewManager(
    private val context: Context,
    private val container: FrameLayout,
    private val progressBar: ProgressBar,
    private val onWebViewCreated: ((WebView) -> Unit)? = null
) {
    companion object {
        private const val TAG = "WebViewManager"
    }

    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentUrl: String = "about:blank"
    private var currentTextZoom: Int = 100  // WebView 文字+布局缩放，100 = 100%

    private var refreshRunnable: Runnable? = null
    private var rebuildRunnable: Runnable? = null
    private var whiteScreenRunnable: Runnable? = null

    // ===================== ????=====================

    fun init(url: String) {
        currentUrl = url
        createWebView()
        loadUrl(url)
    }

    private fun createWebView() {
        destroyWebView()
        val wv = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setupSettings(settings)
            webViewClient = TvWebViewClient()
            webChromeClient = TvWebChromeClient()
            // ??????? View ????????WebSettings ??            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        webView = wv
        container.addView(wv, 0)
        onWebViewCreated?.invoke(wv)
        Log.d(TAG, "WebView created")
    }

    @Suppress("SetJavaScriptEnabled")
    private fun setupSettings(settings: WebSettings) {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false  // ?? +/- ?????????
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = false
            mediaPlaybackRequiresUserGesture = false
        }
    }

    // ===================== ?? / ?? / ?? =====================

    fun loadUrl(url: String) {
        currentUrl = url
        ConfigManager.setDisplayUrl(context, url)
        handler.post {
            webView?.loadUrl(url)
        }
        Log.d(TAG, "Loading URL: $url")
    }

    fun reload() {
        handler.post {
            webView?.reload()
        }
        Log.d(TAG, "Reloading page")
    }

    fun rebuildAndLoad(url: String? = null) {
        val targetUrl = url ?: currentUrl
        handler.post {
            Log.i(TAG, "Rebuilding WebView for url: $targetUrl")
            createWebView()
            loadUrl(targetUrl)
        }
    }

    private fun destroyWebView() {
        val wv = webView ?: return
        webView = null  // ??????????????
        handler.post {
            try {
                container.removeView(wv)
                wv.stopLoading()
                wv.clearHistory()
                wv.clearCache(true)
                wv.loadUrl("about:blank")
                wv.onPause()
                wv.removeAllViews()
                wv.destroy()
            } catch (_: Exception) {}
        }
    }

    fun getCurrentUrl(): String = currentUrl

    fun getWebView(): WebView? = webView

    // ===================== ???? =====================

    /**
     * ??/??????
     * @param intervalSeconds ??0????
     */
    fun scheduleRefresh(intervalSeconds: Int) {
        refreshRunnable?.let { handler.removeCallbacks(it) }
        if (intervalSeconds <= 0) return
        val intervalMs = intervalSeconds * 1000L
        refreshRunnable = object : Runnable {
            override fun run() {
                Log.d(TAG, "Auto refresh triggered")
                reload()
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.postDelayed(refreshRunnable!!, intervalMs)
        Log.d(TAG, "Scheduled refresh every ${intervalSeconds}s")
    }

    /**
     * ??/?????? WebView
     * @param intervalSeconds ??0????
     */
    fun scheduleRebuild(intervalSeconds: Int) {
        rebuildRunnable?.let { handler.removeCallbacks(it) }
        if (intervalSeconds <= 0) return
        val intervalMs = intervalSeconds * 1000L
        rebuildRunnable = object : Runnable {
            override fun run() {
                Log.i(TAG, "Scheduled WebView rebuild triggered")
                rebuildAndLoad()
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.postDelayed(rebuildRunnable!!, intervalMs)
        Log.d(TAG, "Scheduled rebuild every ${intervalSeconds}s")
    }

    /**
     * ?????????0??????
     */
    fun startWhiteScreenCheck(enabled: Boolean) {
        whiteScreenRunnable?.let { handler.removeCallbacks(it) }
        if (!enabled) return
        val checkInterval = 60_000L
        whiteScreenRunnable = object : Runnable {
            override fun run() {
                if (isWhiteScreen()) {
                    Log.w(TAG, "White screen detected! Rebuilding WebView...")
                    rebuildAndLoad()
                }
                handler.postDelayed(this, checkInterval)
            }
        }
        handler.postDelayed(whiteScreenRunnable!!, checkInterval)
    }

    /**
     * ?????????WebView ????????
     */
    private fun isWhiteScreen(): Boolean {
        val wv = webView ?: return false
        return try {
            if (wv.width <= 0 || wv.height <= 0) return false
            val bitmap = Bitmap.createBitmap(
                minOf(wv.width, 200), minOf(wv.height, 200), Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            wv.draw(canvas)

            // Sample check: if >95% of pixels are white or near-white, it's a white screen
            var whitePixels = 0
            val total = bitmap.width * bitmap.height
            for (x in 0 until bitmap.width step 5) {
                for (y in 0 until bitmap.height step 5) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    if (r > 240 && g > 240 && b > 240) whitePixels++
                }
            }
            bitmap.recycle()
            val ratio = whitePixels.toFloat() / (total / 25)
            Log.d(TAG, "White screen ratio: $ratio")
            ratio > 0.95f
        } catch (e: Exception) {
            false
        }
    }

    fun stopAll() {
        refreshRunnable?.let { handler.removeCallbacks(it) }
        rebuildRunnable?.let { handler.removeCallbacks(it) }
        whiteScreenRunnable?.let { handler.removeCallbacks(it) }
        destroyWebView()
    }

    /**
     * 遥控器缩放：全页面缩放（包含图片、布局），使用 CSS zoom 属性
     * deltaPct: +10 放大 10%，-10 缩小 10%
     */
    fun adjustZoom(deltaPct: Int) {
        val wv = webView ?: return
        currentTextZoom = (currentTextZoom + deltaPct).coerceIn(30, 400)
        Log.d(TAG, "Zoom adjusted to: $currentTextZoom%")
        // CSS zoom 缩放整页（含图片/布局），textZoom 仅缩放文字，这里两者都设置
        wv.settings.textZoom = currentTextZoom.coerceIn(50, 300)
        wv.evaluateJavascript("""
            (function(){
                // 设置 CSS zoom 实现全页缩放
                document.documentElement.style.zoom = '${currentTextZoom}%';
                // 显示提示层
                var o=document.getElementById('_zt');if(o)o.remove();
                o=document.createElement('div');o.id='_zt';
                o.style='position:fixed;top:20px;right:20px;background:rgba(0,0,0,0.75);color:#fff;padding:14px 26px;border-radius:10px;font-size:24px;font-weight:bold;z-index:2147483647;pointer-events:none;font-family:sans-serif';
                o.textContent='🔍 缩放: ${currentTextZoom}%';
                document.body.appendChild(o);
                setTimeout(function(){if(o.parentNode)o.parentNode.removeChild(o);},2500);
            })();
        """.trimIndent(), null)
    }

    // ===================== 截图 =====================

    /**
     * 截图主入口：API 26+ 用 PixelCopy（支持硬件加速），否则切软件渲染
     */
    fun captureScreenshot(callback: (String?) -> Unit) {
        val wv = webView
        if (wv == null || wv.width <= 0 || wv.height <= 0) {
            Log.w(TAG, "Screenshot skipped: WebView not ready w=${wv?.width} h=${wv?.height}")
            callback(null)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            captureWithPixelCopy(wv, callback)
        } else {
            captureWithSoftwareLayer(wv, callback)
        }
    }

    /**
     * API 26+ PixelCopy 截图，正确支持硬件加速的 WebView
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun captureWithPixelCopy(wv: WebView, callback: (String?) -> Unit) {
        val activity = context as? android.app.Activity
        val window = activity?.window
        if (window == null) {
            Log.w(TAG, "No Window reference, falling back to software layer")
            captureWithSoftwareLayer(wv, callback)
            return
        }
        // 获取 WebView 在 Window 中的位置，裁剪出 WebView 区域
        val loc = IntArray(2)
        wv.getLocationInWindow(loc)
        val srcRect = android.graphics.Rect(loc[0], loc[1], loc[0] + wv.width, loc[1] + wv.height)
        val full = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
        android.view.PixelCopy.request(window, srcRect, full, { result ->
            if (result == android.view.PixelCopy.SUCCESS) {
                compressAndReturn(full, callback)
            } else {
                Log.w(TAG, "PixelCopy failed ($result), fallback to software layer")
                full.recycle()
                captureWithSoftwareLayer(wv, callback)
            }
        }, handler)
    }

    /**
     * 软件渲染截图（API < 26 备用）：临时切软件层再 draw()
     */
    private fun captureWithSoftwareLayer(wv: WebView, callback: (String?) -> Unit) {
        handler.post {
            try {
                val origLayer = if (wv.layerType == View.LAYER_TYPE_HARDWARE)
                    View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_NONE
                // 临时切换到软件渲染，使 draw() 能捕获到内容
                wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                val full = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(full)
                wv.draw(canvas)
                wv.setLayerType(origLayer, null)
                compressAndReturn(full, callback)
            } catch (e: Exception) {
                Log.e(TAG, "Software layer screenshot failed: ${e.message}")
                callback(null)
            }
        }
    }

    /**
     * 将全尺寸 Bitmap 缩小到 480×270，压缩为 JPEG，base64 编码后回调
     */
    private fun compressAndReturn(full: Bitmap, callback: (String?) -> Unit) {
        try {
            val maxW = 480; val maxH = 270
            val scaleW = maxW.toFloat() / full.width
            val scaleH = maxH.toFloat() / full.height
            val scale = minOf(scaleW, scaleH, 1f)
            val bmpW = (full.width * scale).toInt().coerceAtLeast(1)
            val bmpH = (full.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(full, bmpW, bmpH, true)
            full.recycle()
            val baos = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 45, baos)
            scaled.recycle()
            val bytes = baos.toByteArray()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            Log.d(TAG, "Screenshot OK: ${bmpW}x${bmpH}, ${bytes.size / 1024}KB, b64=${base64.length}")
            callback(base64)
        } catch (e: Exception) {
            Log.e(TAG, "Compress failed: ${e.message}")
            callback(null)
        }
    }

    // ===================== WebViewClient / WebChromeClient =====================

    inner class TvWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 0
        }

        override fun onPageFinished(view: WebView, url: String?) {
            progressBar.visibility = View.GONE
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView, request: WebResourceRequest, error: WebResourceError
        ) {
            Log.e(TAG, "WebView error: ${error.description} for ${request.url}")
            if (request.isForMainFrame) {
                handler.postDelayed({ reload() }, 5000)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            Log.e(TAG, "WebView error(legacy): $description")
            handler.postDelayed({ reload() }, 5000)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            // ???? WebView ??????????????
            view.loadUrl(request.url.toString())
            return true
        }
    }

    inner class TvWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progressBar.progress = newProgress
            if (newProgress == 100) {
                progressBar.visibility = View.GONE
            }
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            Log.d(TAG, "Console[${consoleMessage.messageLevel()}]: ${consoleMessage.message()}")
            return true
        }
    }
}
