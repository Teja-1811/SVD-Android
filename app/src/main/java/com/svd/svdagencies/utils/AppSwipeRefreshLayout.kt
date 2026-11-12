package com.svd.svdagencies.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R

class AppSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private var logoRefreshing = false
    init {
        setColorSchemeColors(android.graphics.Color.TRANSPARENT)
        setProgressBackgroundColorSchemeColor(android.graphics.Color.TRANSPARENT)
        hideNativeIndicator()
    }
    override fun setRefreshing(refreshing: Boolean) {
        logoRefreshing = refreshing
        super.setRefreshing(false)
        if (refreshing) LoadingOverlayManager.show(this) else LoadingOverlayManager.hide(this)
    }
    override fun isRefreshing(): Boolean = logoRefreshing
    override fun onDetachedFromWindow() { LoadingOverlayManager.hide(this); super.onDetachedFromWindow() }

    // Gesture refreshes use a private SwipeRefreshLayout path, bypassing setRefreshing().
    override fun setOnRefreshListener(listener: OnRefreshListener?) {
        super.setOnRefreshListener(if (listener == null) null else OnRefreshListener {
            setRefreshing(true)
            listener.onRefresh()
        })
    }

    private fun hideNativeIndicator() {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == "androidx.swiperefreshlayout.widget.CircleImageView") {
                // Keep drawing enabled: its animation completion dispatches the refresh callback.
                child.alpha = 1f
                child.elevation = 0f
                child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        }
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        hideNativeIndicator()
    }

    override fun canChildScrollUp(): Boolean {
        return findScrollableChild(this)?.canScrollVertically(-1) == true || super.canChildScrollUp()
    }

    private fun findScrollableChild(view: View): View? {
        if (view !== this && view.canScrollVertically(-1)) return view
        if (view !is ViewGroup) return null

        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            findScrollableChild(child)?.let { return it }
        }
        return null
    }
}
