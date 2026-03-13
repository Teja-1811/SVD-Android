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

        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        RefreshManager.setupRefresh(swipeRefresh) {
            refreshData()
        }

        // Load data when screen opens
        RefreshManager.startRefresh(swipeRefresh)
        loadData()
    }

    protected fun stopRefresh() {
        RefreshManager.stopRefresh(swipeRefresh)
    }

    abstract fun loadData()
    abstract fun refreshData()
}
