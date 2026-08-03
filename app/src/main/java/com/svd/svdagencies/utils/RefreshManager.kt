package com.svd.svdagencies.utils

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * A central manager to handle SwipeRefreshLayout configuration and behavior across the app.
 */
object RefreshManager {

    /**
     * Configures a SwipeRefreshLayout with standard colors and a refresh listener.
     */
    fun setupRefresh(
        swipeRefreshLayout: SwipeRefreshLayout,
        onRefresh: () -> Unit
    ) {
        swipeRefreshLayout.setOnRefreshListener {
            onRefresh()
        }
    }

    /**
     * Helper to show the refreshing state (usually for initial load).
     */
    fun startRefresh(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.isRefreshing = true
    }

    /**
     * Helper to hide the refreshing state.
     */
    fun stopRefresh(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.isRefreshing = false
    }
}
