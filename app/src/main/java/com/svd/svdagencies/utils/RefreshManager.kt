package com.svd.svdagencies.utils

import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R

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
        // Standardize colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.status_bar,
            R.color.icon_green,
            R.color.icon_blue
        )

        // Set listener
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
