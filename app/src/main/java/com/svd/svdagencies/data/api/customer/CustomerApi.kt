package com.svd.svdagencies.data.api.customer

import com.svd.svdagencies.data.model.admin.CompaniesListResponse
import com.svd.svdagencies.data.model.customer.CustomerContactResponse
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.data.model.customer.CustomerOfferResponse
import com.svd.svdagencies.data.model.customer.CustomerStatementResponse
import com.svd.svdagencies.data.model.customer.GenericResponse
import com.svd.svdagencies.data.model.customer.PaymentGatewayInitResponse
import com.svd.svdagencies.data.model.customer.RaisedQueriesResponse
import com.svd.svdagencies.data.model.customer.SupportTicketSummaryResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CustomerApi {
    @GET("api/order-timings/")
    fun getOrderTimings(): Call<Map<String, com.svd.svdagencies.utils.CustomerOrderWindow.Window>>

    @GET("api/customer/profile/")
    fun getProfile(): Call<Map<String, Any>>

    @POST("api/customer/profile/")
    fun updateProfile(@Body body: Map<String, String>): Call<Map<String, Any>>

    @GET("api/customer-dashboard/")
    fun getDashboard(): Call<CustomerDashboardResponse>

    @GET("api/customer/statement/")
    fun getStatement(
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Call<CustomerStatementResponse>

    @POST("api/customer/payment/record/")
    fun recordCustomerPayment(
        @Body body: Map<String, Any?>
    ): Call<GenericResponse>

    @GET("api/customer/payment/attempt-status/")
    fun getPaymentAttemptStatus(@Query("payment_order_id") paymentOrderId: String? = null, @Query("subscription_id") subscriptionId: Int? = null): Call<PaymentGatewayInitResponse>

    @POST("api/customer/payment/initiate/")
    fun initiateGatewayPayment(
        @Body body: Map<String, String>
    ): Call<PaymentGatewayInitResponse>

    @GET("api/companies/")
    suspend fun getCompanies(): CompaniesListResponse

    @POST("api/contact/")
    fun submitContact(
        @Body body: Map<String, String>
    ): Call<CustomerContactResponse>

    @GET("api/enquiries/summary/")
    fun getSupportTicketSummary(): Call<SupportTicketSummaryResponse>

    @GET("api/enquiries/my/")
    fun getRaisedQueries(): Call<RaisedQueriesResponse>

    @GET("api/customer/offers/")
    fun getOffers(): Call<CustomerOfferResponse>
}
