package com.svd.svdagencies.data.api.delivery

import com.svd.svdagencies.data.model.delivery.*
import com.svd.svdagencies.data.model.admin.Bills.BillDetailResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET

interface DeliveryApi {

    @GET("api/delivery/routes/")
    fun getRoutes(): Call<List<DeliveryRoute>>

    @GET("api/delivery/bill-customers/")
    fun getBillCustomers(
        @retrofit2.http.Query("q") query: String? = null,
        @retrofit2.http.Query("route_id") routeId: Int? = null
    ): Call<DeliveryBillCustomersResponse>

    @GET("api/delivery/bill-items/")
    fun getBillItems(@retrofit2.http.Query("customer_id") customerId: Int): Call<DeliveryBillItemsResponse>

    @retrofit2.http.POST("api/delivery/generate-bill/")
    fun generateBill(@Body request: DeliveryGenerateBillRequest): Call<DeliveryGenerateBillResponse>

    @GET("api/delivery/agent-summary/")
    fun getAgentSummary(
        @retrofit2.http.Query("date") date: String? = null,
        @retrofit2.http.Query("start_date") startDate: String? = null,
        @retrofit2.http.Query("end_date") endDate: String? = null
    ): Call<DeliveryAgentDuesResponse>

    @GET("api/delivery/today-bills/{customer_id}/")
    fun getTodayBills(@retrofit2.http.Path("customer_id") customerId: Int): Call<DeliveryTodayBillsResponse>

    @GET("api/delivery/customer-op/{customer_id}/")
    fun getCustomerOpeningBalance(@retrofit2.http.Path("customer_id") customerId: Int): Call<CustomerOpeningBalanceResponse>

    @GET("api/bills/{bill_id}/")
    fun getBillDetails(@retrofit2.http.Path("bill_id") billId: Int): Call<BillDetailResponse>

    @GET("api/bills/{bill_id}/items/")
    fun getBillItemsDetail(@retrofit2.http.Path("bill_id") billId: Int): Call<List<com.svd.svdagencies.data.model.user.UserBillItem>>

    @GET("api/delivery/customer-payments/{customer_id}/")
    fun getCustomerPaymentRecords(
        @retrofit2.http.Path("customer_id") customerId: Int,
        @retrofit2.http.Query("month") month: Int? = null,
        @retrofit2.http.Query("year") year: Int? = null
    ): Call<CustomerPaymentHistoryResponse>
}
