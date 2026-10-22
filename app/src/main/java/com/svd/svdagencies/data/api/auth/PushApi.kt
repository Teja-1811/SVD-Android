package com.svd.svdagencies.data.api.auth

import com.svd.svdagencies.data.model.PushDeviceRegisterRequest
import com.svd.svdagencies.data.model.PushDeviceResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

interface PushApi {
    @POST("api/mobile/push/register/")
    fun registerDevice(
        @Body request: PushDeviceRegisterRequest,
        @Header("Authorization") authorization: String? = null
    ): Call<PushDeviceResponse>

    @POST("api/mobile/push/unregister/")
    fun unregisterDevice(
        @Body request: PushDeviceRegisterRequest,
        @Header("Authorization") authorization: String? = null
    ): Call<PushDeviceResponse>
}
