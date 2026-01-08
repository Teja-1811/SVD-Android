package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.MonthlyPaymentSummaryResponse
import com.svd.svdagencies.data.model.admin.PaymentsDashboardResponse
import com.svd.svdagencies.data.model.admin.SaveDailyPaymentsRequest
import retrofit2.http.*

interface AdminPaymentsApi {

    @GET("payments/dashboard/")
    suspend fun getPaymentsDashboard(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): PaymentsDashboardResponse

    @POST("payments/save-daily/")
    suspend fun saveDailyPayments(
        @Body request: SaveDailyPaymentsRequest
    ): Map<String, Boolean>

    @GET("payments/monthly-summary/")
    suspend fun getMonthlyPaymentSummary(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): MonthlyPaymentSummaryResponse
}
