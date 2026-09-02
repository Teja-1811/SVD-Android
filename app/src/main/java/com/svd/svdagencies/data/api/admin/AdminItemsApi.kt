package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.Items.AdminCategoriesResponse
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.svd.svdagencies.data.model.admin.Items.AdminItemsByCategoryResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface AdminItemsApi {

    @GET("api/items/")
    fun getAllItems(): Call<List<AdminItem>>

    @GET("api/items/categories/")
    suspend fun getCategories(): AdminCategoriesResponse

    @GET("api/items/by-category/")
    suspend fun getItemsByCategory(
        @Query("category") category: String
    ): AdminItemsByCategoryResponse

    @Multipart
    @POST("api/items/add/")
    suspend fun addItem(
        @Part("code") code: RequestBody?,
        @Part("name") name: RequestBody,
        @Part("company_id") companyId: RequestBody?,
        @Part("category") category: RequestBody?,
        @Part("selling_price") sellingPrice: RequestBody?,
        @Part("buying_price") buyingPrice: RequestBody?,
        @Part("mrp") mrp: RequestBody?,
        @Part("base_price") basePrice: RequestBody?,
        @Part("gst_percentage") gstPercentage: RequestBody?,
        @Part("ltr") ltr: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part("stock_quantity") stockQuantity: RequestBody?,
        @Part("pcs_count") pcsCount: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Map<String, Any>

    @Multipart
    @POST("api/items/edit/{id}/")
    suspend fun editItem(
        @Path("id") id: Int,
        @Part("code") code: RequestBody?,
        @Part("name") name: RequestBody?,
        @Part("company_id") companyId: RequestBody?,
        @Part("category") category: RequestBody?,
        @Part("selling_price") sellingPrice: RequestBody?,
        @Part("buying_price") buyingPrice: RequestBody?,
        @Part("mrp") mrp: RequestBody?,
        @Part("base_price") basePrice: RequestBody?,
        @Part("gst_percentage") gstPercentage: RequestBody?,
        @Part("ltr") ltr: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part("stock_quantity") stockQuantity: RequestBody?,
        @Part("pcs_count") pcsCount: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Map<String, Any>

    @POST("api/items/freeze-toggle/{id}/")
    suspend fun toggleFreezeItem(
        @Path("id") id: Int
    ): Map<String, Any>
}
