package com.svd.svdagencies.data.repository

import com.svd.svdagencies.data.model.user.UserDashboardResponse

interface UserDashboardObserver {
    fun onDashboardUpdated(data: UserDashboardResponse)
}
