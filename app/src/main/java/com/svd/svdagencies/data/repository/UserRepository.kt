package com.svd.svdagencies.data.repository

import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.user.UserDashboardResponse
import com.svd.svdagencies.data.model.user.UserPlan
import com.svd.svdagencies.data.model.user.UserPlansResponse
import com.svd.svdagencies.data.model.user.UserProfileUpdateResponse
import com.svd.svdagencies.data.model.user.UserCustomer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

object UserRepository {

    private val api = ApiClient.userApi
    private val dashboardObservers = mutableListOf<WeakReference<UserDashboardObserver>>()
    private var lastDashboardCall: Call<UserDashboardResponse>? = null
    private var lastPlansCall: Call<UserPlansResponse>? = null
    private var cachedDashboard: UserDashboardResponse? = null
    private var cachedPlans: List<UserPlan>? = null
    private var cachedProfileCall: Call<UserProfileUpdateResponse>? = null

    @Synchronized
    fun registerObserver(observer: UserDashboardObserver) {
        cleanObservers()
        if (dashboardObservers.any { it.get() == observer }) {
            cachedDashboard?.let { observer.onDashboardUpdated(it) }
            return
        }

        dashboardObservers.add(WeakReference(observer))
        cachedDashboard?.let { observer.onDashboardUpdated(it) }
    }

    @Synchronized
    fun unregisterObserver(observer: UserDashboardObserver) {
        dashboardObservers.removeAll { it.get() == null || it.get() == observer }
    }

    fun getCachedDashboard(): UserDashboardResponse? = cachedDashboard
    fun getCachedPlans(): List<UserPlan>? = cachedPlans

    fun fetchDashboard(
        userId: Int,
        onSuccess: ((UserDashboardResponse) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        lastDashboardCall?.cancel()

        lastDashboardCall = api.getDashboard(userId).apply {
            enqueue(object : Callback<UserDashboardResponse> {
                override fun onResponse(
                    call: Call<UserDashboardResponse>,
                    response: Response<UserDashboardResponse>
                ) {
                    lastDashboardCall = null

                    if (!response.isSuccessful) {
                        onError?.invoke("Server error: ${response.code()}")
                        return
                    }

                    val body = response.body()
                    if (body == null) {
                        onError?.invoke("Empty dashboard response")
                        return
                    }

                    cachedDashboard = body
                    notifyObservers(body)
                    onSuccess?.invoke(body)
                }

                override fun onFailure(call: Call<UserDashboardResponse>, t: Throwable) {
                    lastDashboardCall = null
                    if (call.isCanceled) return
                    onError?.invoke(t.localizedMessage ?: "Unable to reach server")
                }
            })
        }
    }

    fun fetchPlans(
        onSuccess: (List<UserPlan>) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        lastPlansCall?.cancel()

        lastPlansCall = api.getAvailablePlans().apply {
            enqueue(object : Callback<UserPlansResponse> {
                override fun onResponse(
                    call: Call<UserPlansResponse>,
                    response: Response<UserPlansResponse>
                ) {
                    lastPlansCall = null

                    if (!response.isSuccessful) {
                        onError?.invoke("Server error: ${response.code()}")
                        return
                    }

                    val body = response.body()
                    val plans = body?.plans ?: emptyList()
                    cachedPlans = plans
                    onSuccess(plans)
                }

                override fun onFailure(call: Call<UserPlansResponse>, t: Throwable) {
                    lastPlansCall = null
                    if (call.isCanceled) return
                    onError?.invoke(t.localizedMessage ?: "Unable to reach server")
                }
            })
        }
    }

    fun updateProfile(
        userId: Int,
        updates: Map<String, Any>,
        onSuccess: ((UserCustomer) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (updates.isEmpty()) {
            onError?.invoke("No profile updates provided")
            return
        }

        val payload = updates.toMutableMap().apply {
            put("user_id", userId)
        }

        cachedProfileCall?.cancel()

        cachedProfileCall = api.updateProfile(payload).apply {
            enqueue(object : Callback<UserProfileUpdateResponse> {
                override fun onResponse(
                    call: Call<UserProfileUpdateResponse>,
                    response: Response<UserProfileUpdateResponse>
                ) {
                    cachedProfileCall = null

                    if (!response.isSuccessful) {
                        onError?.invoke("Server error: ${response.code()}")
                        return
                    }

                    val body = response.body()
                    if (body == null) {
                        onError?.invoke("Empty profile response")
                        return
                    }

                    if (!body.status.equals("success", ignoreCase = true)) {
                        onError?.invoke("Profile update failed")
                        return
                    }

                    val updatedCustomer = body.customer
                    val snapshot = cachedDashboard
                    if (snapshot != null) {
                        val updatedDashboard = snapshot.copy(customer = updatedCustomer)
                        cachedDashboard = updatedDashboard
                        notifyObservers(updatedDashboard)
                    }

                    onSuccess?.invoke(updatedCustomer)
                }

                override fun onFailure(call: Call<UserProfileUpdateResponse>, t: Throwable) {
                    cachedProfileCall = null
                    if (call.isCanceled) return
                    onError?.invoke(t.localizedMessage ?: "Unable to reach server")
                }
            })
        }
    }

    private fun notifyObservers(data: UserDashboardResponse) {
        cleanObservers()
        dashboardObservers.forEach { reference ->
            reference.get()?.onDashboardUpdated(data)
        }
    }

    @Synchronized
    private fun cleanObservers() {
        dashboardObservers.removeAll { it.get() == null }
    }
}
