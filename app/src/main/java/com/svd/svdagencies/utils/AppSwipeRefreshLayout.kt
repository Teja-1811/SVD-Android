package com.svd.svdagencies.utils

import android.content.Context
import android.util.AttributeSet
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
            post { super.setRefreshing(false) }
        } else if (overlayVisible) {
            overlayVisible = false
            LoadingOverlayManager.hide(this)
        }
    }
}
