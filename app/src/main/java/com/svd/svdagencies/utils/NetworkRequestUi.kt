package com.svd.svdagencies.utils

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import okhttp3.Call
import okhttp3.EventListener
import java.io.IOException
import java.lang.ref.WeakReference

/** One foreground loader for all API calls, including calls made from dialogs. */
object NetworkRequestUi : Application.ActivityLifecycleCallbacks {
    private val main = Handler(Looper.getMainLooper())
    private var foreground = WeakReference<Activity>(null)
    private var pending = 0
    private var dialog: Dialog? = null
    private var installed = false
    private val show = Runnable {
        val activity = foreground.get()
        if (pending > 0 && activity != null && !activity.isFinishing && !activity.isDestroyed && dialog == null) {
            val density = activity.resources.displayMetrics.density
            val panel = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 32)
                setBackgroundColor(Color.WHITE)
                contentDescription = "Loading. Please wait"
            }
            panel.addView(ProgressBar(activity), LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt()))
            panel.addView(TextView(activity).apply {
                text = "Please wait…"
                setTextColor(Color.rgb(24, 59, 71))
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, (16 * density).toInt(), 0, 0)
            })
            dialog = Dialog(activity).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(panel)
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                show()
                window?.setLayout((200 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
    }

    fun install(application: Application) {
        if (!installed) {
            installed = true
            application.registerActivityLifecycleCallbacks(this)
        }
    }

    private fun dismiss() {
        main.removeCallbacks(show)
        dialog?.dismiss()
        dialog = null
    }

    private fun render() {
        if (pending == 0 || foreground.get() == null) dismiss()
        else if (dialog == null) {
            main.removeCallbacks(show)
            main.postDelayed(show, 150)
        }
    }

    fun listenerFactory() = EventListener.Factory {
        object : EventListener() {
            private var started = false
            private var finished = false
            override fun callStart(call: Call) {
                main.post { if (!started && !finished) { started = true; pending++; render() } }
            }
            private fun finish() {
                main.post {
                    if (!finished) {
                        finished = true
                        if (started) pending = (pending - 1).coerceAtLeast(0)
                        render()
                    }
                }
            }
            override fun callEnd(call: Call) = finish()
            override fun callFailed(call: Call, ioe: IOException) = finish()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (foreground.get() !== activity) dismiss()
        foreground = WeakReference(activity)
        render()
    }
    override fun onActivityPaused(activity: Activity) {
        if (foreground.get() === activity) { foreground.clear(); dismiss() }
    }
    override fun onActivityDestroyed(activity: Activity) {
        if (foreground.get() === activity) { foreground.clear(); dismiss() }
    }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
}
