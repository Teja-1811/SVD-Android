package com.svd.svdagencies.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.utils.RefreshManager

abstract class BaseRefreshFragment(
    layoutId: Int
) : Fragment(layoutId) {

    protected lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Try to find swipeRefresh in the current view
        val sr = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        if (sr != null) {
            swipeRefresh = sr
            RefreshManager.setupRefresh(swipeRefresh) {
                refreshData()
            }
            // Load data when screen opens
            RefreshManager.startRefresh(swipeRefresh)
        }
        
        loadData()
    }

    protected fun stopRefresh() {
        if (::swipeRefresh.isInitialized) {
            RefreshManager.stopRefresh(swipeRefresh)
        }
    }

    abstract fun loadData()
    abstract fun refreshData()
}
