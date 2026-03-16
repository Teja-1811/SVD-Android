package com.svd.svdagencies.data.api.user

import com.svd.svdagencies.data.model.user.UserDashboardResponse
import com.svd.svdagencies.data.model.user.UserOffersResponse
import com.svd.svdagencies.data.model.user.UserPlansResponse
import com.svd.svdagencies.data.model.user.UserProfileUpdateResponse
import kotlin.jvm.JvmSuppressWildcards
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserApi {
    @GET("api/user/dashboard/full/")
    fun getDashboard(
        @Query("user_id") userId: Int
    ): Call<UserDashboardResponse>

    @GET("api/user/offers/active/")
    fun getActiveOffers(): Call<UserOffersResponse>

    @GET("api/user/plans/available/")
    fun getAvailablePlans(): Call<UserPlansResponse>

    @POST("api/user/profile/update/")
    fun updateProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<UserProfileUpdateResponse>

    @POST("api/user/payment/subscription/pause/")
    fun pauseSubscription(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<Map<String, Any>>
}
