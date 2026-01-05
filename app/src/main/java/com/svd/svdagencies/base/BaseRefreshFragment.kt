package com.svd.svdagencies.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R

abstract class BaseRefreshFragment(
    layoutId: Int
) : Fragment(layoutId) {

    protected lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        swipeRefresh.setColorSchemeResources(
            R.color.status_bar,
            R.color.icon_green,
            R.color.icon_blue
        )

        swipeRefresh.setOnRefreshListener {
            refreshData()
        }

        // Load data when screen opens
        swipeRefresh.isRefreshing = true
        loadData()
    }

    protected fun stopRefresh() {
        swipeRefresh.isRefreshing = false
    }

    abstract fun loadData()
    abstract fun refreshData()
}
