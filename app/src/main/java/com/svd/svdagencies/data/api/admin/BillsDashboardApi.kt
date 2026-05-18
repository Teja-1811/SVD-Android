package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import com.svd.svdagencies.data.model.admin.Bills.BillDetailResponse
import com.svd.svdagencies.data.model.admin.Bills.BillItemDetail
import com.svd.svdagencies.data.model.admin.Bills.BillListResponse
import com.svd.svdagencies.data.model.admin.Bills.CreateBillRequest
import com.svd.svdagencies.data.model.admin.Bills.CreateBillResponse
import com.svd.svdagencies.data.model.admin.Bills.EditBillRequest
import com.svd.svdagencies.data.model.admin.customerData.CustomerDashboardResponse
import com.svd.svdagencies.data.model.admin.customerData.GenericSuccessResponse
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDuesResponse
import com.svd.svdagencies.data.model.delivery.DeliveryAgentSubmissionRequest
import com.svd.svdagencies.data.model.delivery.DeliveryAgentSubmissionResponse
import okhttp3.ResponseBody
import retrofit2.http.*

interface BillsDashboardApi {

    @GET("api/bills/list/")
    suspend fun getBills(
        @Query("customer") customerId: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int? = 1
    ): BillListResponse

    @GET("api/customer-list/")
    suspend fun getCustomersForBill(): CustomerDashboardResponse

    @GET("api/bills/{bill_id}/")
    suspend fun getBillDetail(@Path("bill_id") billId: Int): BillDetailResponse

    @GET("api/bills/{bill_id}/items/")
    suspend fun getBillItems(@Path("bill_id") billId: Int): List<BillItemDetail>

    @POST("api/bills/create/")
    suspend fun createBill(@Body request: CreateBillRequest): CreateBillResponse

    @POST("api/bills/{bill_id}/edit/")
    suspend fun editBill(
        @Path("bill_id") billId: Int,
        @Body request: EditBillRequest
    ): GenericSuccessResponse

    @DELETE("api/bills/{bill_id}/delete/")
    suspend fun deleteBill(@Path("bill_id") billId: Int): Map<String, Any>

    @GET("api/bills/{bill_id}/download/")
    suspend fun downloadBill(@Path("bill_id") billId: Int): ResponseBody

    @GET("api/bills/delivery-boys/summary/")
    suspend fun getDeliveryAgentDues(
        @Query("delivery_boy_id") deliveryBoyId: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): DeliveryAgentDuesResponse

    @POST("api/bills/delivery-boys/submissions/")
    suspend fun saveDeliveryAgentSubmission(
        @Body request: DeliveryAgentSubmissionRequest
    ): DeliveryAgentSubmissionResponse
}
