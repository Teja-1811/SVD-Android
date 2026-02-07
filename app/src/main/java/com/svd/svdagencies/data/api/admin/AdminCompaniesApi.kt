package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.CatalogItem
import com.svd.svdagencies.data.model.admin.CompaniesListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface AdminCompaniesApi {

    @GET("api/companies/")
    suspend fun getCompanies(): CompaniesListResponse

    @Multipart
    @POST("api/companies/add/")
    suspend fun addCompany(
        @Part("name") name: RequestBody,
        @Part("website_link") websiteLink: RequestBody?,
        @Part logo: MultipartBody.Part?
    ): Map<String, Any>

    @Multipart
    @POST("api/companies/edit/{id}/")
    suspend fun editCompany(
        @Path("id") id: Int,
        @Part("name") name: RequestBody?,
        @Part("website_link") websiteLink: RequestBody?,
        @Part logo: MultipartBody.Part?
    ): Map<String, Any>

    @GET("api/companies/items/{company_id}/")
    suspend fun getCompanyItems(@Path("company_id") companyId: Int): List<CatalogItem>
}
