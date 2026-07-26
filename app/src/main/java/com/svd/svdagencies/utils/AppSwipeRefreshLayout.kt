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

    private var overlayVisible = false

    init {
        setColorSchemeResources(R.color.transparent)
        setProgressBackgroundColorSchemeResource(android.R.color.transparent)
    }

    override fun setRefreshing(refreshing: Boolean) {
        super.setRefreshing(refreshing)
        if (refreshing) {
            if (!overlayVisible) {
                overlayVisible = true
                LoadingOverlayManager.show(this)
            }
        } else if (overlayVisible) {
            overlayVisible = false
            LoadingOverlayManager.hide(this)
        }
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
