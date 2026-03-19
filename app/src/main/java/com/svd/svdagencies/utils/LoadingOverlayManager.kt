package com.svd.svdagencies.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.svd.svdagencies.R
import java.util.WeakHashMap

object LoadingOverlayManager {

    private val overlays = WeakHashMap<Activity, FrameLayout>()
    private val visibleCounts = WeakHashMap<Activity, Int>()
    private const val BLUR_RADIUS = 24f
    private const val DIMMED_ALPHA = 0.55f

    fun show(anchor: View) {
        resolveActivity(anchor.context)?.let(::show)
    }

    fun hide(anchor: View) {
        resolveActivity(anchor.context)?.let(::hide)
    }

    fun show(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val nextCount = (visibleCounts[activity] ?: 0) + 1
        visibleCounts[activity] = nextCount

        val overlay = overlays[activity] ?: createOverlay(activity).also {
            overlays[activity] = it
            content.addView(it)
        }

        if (overlay.parent == null) {
            content.addView(overlay)
        }

        overlay.visibility = View.VISIBLE
        overlay.bringToFront()
        applyBlur(content, overlay, true)
    }

    fun hide(activity: Activity) {
        val currentCount = visibleCounts[activity] ?: 0
        if (currentCount <= 1) {
            visibleCounts.remove(activity)
            val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
            overlays[activity]?.visibility = View.GONE
            applyBlur(content, overlays[activity], false)
        } else {
            visibleCounts[activity] = currentCount - 1
        }
    }

    private fun createOverlay(activity: Activity): FrameLayout {
        val overlay = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(activity, R.color.loading_overlay_scrim))
            isClickable = true
            isFocusable = true
            visibility = View.GONE
        }

        val progress = CircularProgressIndicator(activity).apply {
            isIndeterminate = true
            indicatorSize = 72
            trackThickness = 6
            setIndicatorColor(ContextCompat.getColor(activity, R.color.brand_red))
        }

        overlay.addView(
            progress,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        return overlay
    }

    private fun applyBlur(content: ViewGroup, overlay: View?, enabled: Boolean) {
        for (index in 0 until content.childCount) {
            val child = content.getChildAt(index)
            if (child === overlay) continue

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                child.setRenderEffect(
                    if (enabled) {
                        RenderEffect.createBlurEffect(BLUR_RADIUS, BLUR_RADIUS, Shader.TileMode.CLAMP)
                    } else {
                        null
                    }
                )
            }

            child.alpha = if (enabled) DIMMED_ALPHA else 1f
            child.isEnabled = !enabled
        }
    }

    private fun resolveActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) {
                return current
            }
            current = current.baseContext
        }
        return null
    }
}
