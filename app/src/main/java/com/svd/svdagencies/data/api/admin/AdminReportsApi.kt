package com.svd.svdagencies.data.api.admin

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.*

interface AdminReportsApi {
    @GET("api/gst/") suspend fun gst(@QueryMap filters: Map<String, String>): JsonObject
    @GET("api/gst/purchases/") suspend fun purchaseReviews(@QueryMap filters: Map<String, String>): JsonObject
    @POST("api/gst/purchases/") suspend fun reviewPurchase(@Body values: JsonObject): JsonObject
    @POST("api/gst/settings/") suspend fun settings(@Body values: JsonObject): JsonObject
    @POST("api/gst/generate/") suspend fun generate(@Body values: JsonObject): JsonObject
    @Streaming @GET("api/gst/report/") suspend fun gstPdf(@QueryMap filters: Map<String, String>): ResponseBody
    @Streaming @GET("api/purchase-report/") suspend fun purchasePdf(@QueryMap filters: Map<String, String>): ResponseBody
    @GET("api/purchase-report/summary/") suspend fun purchases(@QueryMap filters: Map<String, String>): JsonObject
}
