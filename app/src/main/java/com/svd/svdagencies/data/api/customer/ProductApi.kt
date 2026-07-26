package com.svd.svdagencies.data.api.customer

import com.google.gson.JsonElement
import com.svd.svdagencies.data.model.customer.CatalogResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductApi {

    @GET("api/categories/")
    fun getCategories(): Call<JsonElement>

    @GET("api/products/")
    fun getProducts(
        @Query("category_id") categoryId: Int
    ): Call<JsonElement>

    @GET("api/cataloge/")
    suspend fun getCustomerCatalog(
        @Query("company_id") companyId: Int?,
        @Query("include_empty") includeEmpty: Boolean = false
    ): CatalogResponse
}
