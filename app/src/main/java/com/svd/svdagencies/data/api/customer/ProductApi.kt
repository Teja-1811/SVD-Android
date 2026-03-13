package com.svd.svdagencies.data.api.customer

import com.svd.svdagencies.data.model.customer.CatalogResponse
import com.svd.svdagencies.data.model.customer.CategoryResponse
import com.svd.svdagencies.data.model.customer.ProductResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductApi {

    @GET("api/categories/")
    fun getCategories(): Call<List<CategoryResponse>>

    @GET("api/products/")
    fun getProducts(
        @Query("category_id") categoryId: Int
    ): Call<List<ProductResponse>>

    @GET("api/cataloge/")
    suspend fun getCustomerCatalog(
        @Query("company_id") companyId: Int?,
        @Query("include_empty") includeEmpty: Boolean = false
    ): CatalogResponse
}