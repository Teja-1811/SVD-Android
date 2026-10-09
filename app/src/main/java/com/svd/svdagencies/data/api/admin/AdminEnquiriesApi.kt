package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.AdminEnquiriesResponse
import com.svd.svdagencies.data.model.admin.AdminEnquiryStatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AdminEnquiriesApi {

    @GET("api/admin/enquiries/active/")
    suspend fun getActiveEnquiries(): AdminEnquiriesResponse

    @GET("api/admin/enquiries/resolved/")
    suspend fun getResolvedEnquiries(): AdminEnquiriesResponse

    @POST("api/admin/enquiries/{enquiry_id}/status/")
    suspend fun updateEnquiryStatus(
        @Path("enquiry_id") enquiryId: Int,
        @Body body: Map<String, String>
    ): AdminEnquiryStatusResponse
}
