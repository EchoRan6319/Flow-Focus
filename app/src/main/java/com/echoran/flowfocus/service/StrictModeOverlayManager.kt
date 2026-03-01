package com.echoran.flowfocus.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.echoran.flowfocus.R

class StrictModeOverlayManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun showOverlay(blockedAppName: String) {
        if (overlayView != null) return // Already showing

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

        // In a real app we would use a proper layout XML file
        // Here we simulate it by inflating a simple layout or creating views programmatically
        // For brevity and to avoid creating XML resource files, we'll build it in memory
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#E6000000")) // Semi-transparent black

            val textView = TextView(context).apply {
                text = "心流番茄：严格模式\n\n已拦截应用: $blockedAppName\n\n请回到专注中！"
                textSize = 24f
                setTextColor(android.graphics.Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 64)
            }

            val backButton = Button(context).apply {
                text = "回到心流番茄"
                setOnClickListener {
                    // Redirect back to our app
                    val intent = android.content.Intent(context, com.echoran.flowfocus.MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                    context.startActivity(intent)
                    removeOverlay()
                }
            }

            addView(textView)
            addView(backButton)
        }

        overlayView = layout

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateMessage(message: String) {
        (overlayView as? android.widget.LinearLayout)?.let { layout ->
            (layout.getChildAt(0) as? TextView)?.text = message
        }
    }

    fun removeOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
