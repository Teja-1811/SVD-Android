package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.CustomerPaymentResponse
import com.svd.svdagencies.data.model.admin.MonthlyPaymentSummaryResponse
import com.svd.svdagencies.data.model.admin.PaymentsDashboardResponse
import com.svd.svdagencies.data.model.admin.SaveDailyPaymentsRequest
import retrofit2.http.*

interface AdminPaymentsApi {

    @GET("api/payments/dashboard/")
    suspend fun getPaymentsDashboard(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): PaymentsDashboardResponse

    @POST("api/payments/save-daily/")
    suspend fun saveDailyPayments(
        @Body request: SaveDailyPaymentsRequest
    ): Map<String, Any>

    @GET("api/payments/monthly-summary/")
    suspend fun getMonthlyPaymentSummary(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): MonthlyPaymentSummaryResponse

    @GET("api/customer-payments/")
    suspend fun getCustomerPayments(
        @Query("customer") customer: String? = null,
        @Query("transaction_id") transactionId: String? = null
    ): CustomerPaymentResponse

    @POST("api/customer-payments/update-status/{payment_id}/")
    suspend fun updatePaymentStatus(
        @Path("payment_id") paymentId: Int,
        @Body body: Map<String, String>
    ): Map<String, Any>

    @DELETE("api/customer-payments/delete/{payment_id}/")
    suspend fun deletePayment(
        @Path("payment_id") paymentId: Int
    ): Map<String, Any>
}
