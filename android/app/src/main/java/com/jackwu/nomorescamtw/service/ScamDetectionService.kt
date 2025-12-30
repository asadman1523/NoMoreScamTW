package com.jackwu.nomorescamtw.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import com.jackwu.nomorescamtw.MyApplication
import com.jackwu.nomorescamtw.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScamDetectionService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var lastCheckedUrl: String = ""
    private var countdownTimer: CountDownTimer? = null

    // Supported browser package names and their URL bar IDs
    private val browserConfig = mapOf(
        "com.android.chrome" to "com.android.chrome:id/url_bar",
        "com.google.android.apps.chrome" to "com.google.android.apps.chrome:id/url_bar",
        "org.mozilla.firefox" to "org.mozilla.firefox:id/url_bar_title",
        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (!browserConfig.containsKey(packageName)) return

        val rootNode = rootInActiveWindow ?: return
        
        // Find URL node
        val urlNode = findUrlNode(rootNode, browserConfig[packageName])
        
        if (urlNode != null && urlNode.text != null) {
            val url = urlNode.text.toString()
            if (url.isNotEmpty() && url != lastCheckedUrl) {
                lastCheckedUrl = url
                checkUrl(url)
            }
        }
    }

    private fun findUrlNode(node: AccessibilityNodeInfo, viewId: String?): AccessibilityNodeInfo? {
        if (viewId == null) return null
        
        val nodes = node.findAccessibilityNodeInfosByViewId(viewId)
        if (!nodes.isNullOrEmpty()) {
            return nodes[0]
        }
        return null
    }

    private fun checkUrl(url: String) {
        val app = applicationContext as MyApplication
        val repository = app.repository

        serviceScope.launch(Dispatchers.IO) {
            val result = repository.checkUrl(url)
            if (result != null) {
                withContext(Dispatchers.Main) {
                    showOverlay(result.name, result.count, result.url)
                }
            } else {
                withContext(Dispatchers.Main) {
                    hideOverlay()
                }
            }
        }
    }

    private fun showOverlay(name: String, count: Int, url: String) {
        if (overlayView != null) return // Already showing

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER

            val inflater = LayoutInflater.from(this)
            overlayView = inflater.inflate(R.layout.overlay_warning, null)

            val tvDetail = overlayView!!.findViewById<TextView>(R.id.tvOverlayDetail)
            tvDetail.text = "${getString(R.string.label_name)}: $name | ${getString(R.string.label_count)}: $count"

            val tvUrl = overlayView!!.findViewById<TextView>(R.id.tvOverlayUrl)
            tvUrl.text = url

            val btnLeave = overlayView!!.findViewById<Button>(R.id.btnLeave)
            btnLeave.setOnClickListener {
                hideOverlay()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            val btnClose = overlayView!!.findViewById<Button>(R.id.btnOverlayClose)
            btnClose.setOnClickListener {
                hideOverlay()
            }

            val tvTitle = overlayView!!.findViewById<TextView>(R.id.tvOverlayTitle)
            val originalTitle = getString(R.string.warning_default_detail)

            // Start 60 second countdown, show close button after 10 seconds, auto leave when finished
            countdownTimer = object : CountDownTimer(60000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                    tvTitle.text = "$originalTitle ($secondsRemaining)"

                    // Show close button after 10 seconds (50 seconds remaining)
                    if (secondsRemaining <= 50) {
                        btnClose.visibility = View.VISIBLE
                    }
                }

                override fun onFinish() {
                    // Auto leave when countdown finishes
                    hideOverlay()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }.start()

            windowManager?.addView(overlayView, params)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideOverlay() {
        countdownTimer?.cancel()
        countdownTimer = null

        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}