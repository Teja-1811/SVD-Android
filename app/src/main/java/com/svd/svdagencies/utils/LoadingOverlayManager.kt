package com.svd.svdagencies.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.svd.svdagencies.R
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

object LoadingOverlayManager {
    private val overlays = WeakHashMap<Activity, WeakReference<FrameLayout>>()
    private val owners = WeakHashMap<Activity, MutableSet<Any>>()
    fun show(anchor: View) { resolveActivity(anchor.context)?.let { show(it, anchor) } }
    fun hide(anchor: View) { resolveActivity(anchor.context)?.let { hide(it, anchor) } }
    fun show(activity: Activity) = show(activity, activity)
    fun hide(activity: Activity) = hide(activity, activity)
    private fun show(activity: Activity, owner: Any) {
        if (activity.isFinishing || activity.isDestroyed) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        owners.getOrPut(activity) { Collections.newSetFromMap(WeakHashMap<Any, Boolean>()) }.add(owner)
        val overlay = overlays[activity]?.get() ?: createOverlay(activity).also {
            overlays[activity] = WeakReference(it)
            content.addView(it)
        }
        if (overlay.parent == null) content.addView(overlay)
        overlay.visibility = View.VISIBLE
        overlay.bringToFront()
        (overlay.getChildAt(0) as HeartbeatLogoView).startPulse()
    }
    private fun hide(activity: Activity, owner: Any) {
        owners[activity]?.remove(owner)
        if (owners[activity]?.isNotEmpty() == true) return
        owners.remove(activity)
        overlays[activity]?.get()?.let {
            (it.getChildAt(0) as HeartbeatLogoView).stopPulse()
            it.visibility = View.GONE
        }
    }
    private fun createOverlay(activity: Activity): FrameLayout = FrameLayout(activity).apply {
        layoutParams = ViewGroup.LayoutParams(-1, -1)
        setBackgroundColor(ContextCompat.getColor(activity, R.color.loading_overlay_scrim))
        isClickable = true
        isFocusable = true
        visibility = View.GONE
        val density = resources.displayMetrics.density
        addView(HeartbeatLogoView(activity), FrameLayout.LayoutParams((144*density).toInt(), (144*density).toInt(), Gravity.CENTER))
    }
    private fun resolveActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
