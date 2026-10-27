package com.svd.svdagencies.utils

import android.content.Context
import android.widget.Toast
import com.google.gson.JsonObject
import com.svd.svdagencies.data.api.auth.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET

interface AgencyDetailsApi {
    @GET("api/business-details/") fun read(): Call<JsonObject>
}

object PaymentConfig {
    const val PAYEE_NAME = "Sri Vijay Durga Milk Agency"
    fun load(context: Context, ready: (JsonObject) -> Unit) {
        ApiClient.retrofit.create(AgencyDetailsApi::class.java).read().enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) return
                val body = response.body()
                if (response.isSuccessful && body != null) ready(body)
                else Toast.makeText(context, "Unable to load agency details. Please try again.", Toast.LENGTH_LONG).show()
            }
            override fun onFailure(call: Call<JsonObject>, error: Throwable) {
                if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) return
                Toast.makeText(context, "Unable to load agency details. Please try again.", Toast.LENGTH_LONG).show()
            }
        })
    }
    fun withUpiId(context: Context, ready: (String) -> Unit) {
        load(context) { data ->
            val value = data.getAsJsonObject("business")?.get("upi_id")?.asString.orEmpty()
            if (value.isNotBlank()) ready(value)
            else Toast.makeText(context, "Ask the administrator to configure the payment UPI ID.", Toast.LENGTH_LONG).show()
        }
    }
}
