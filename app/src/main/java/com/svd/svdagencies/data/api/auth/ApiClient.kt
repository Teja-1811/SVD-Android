package com.svd.svdagencies.data.api.auth

import com.svd.svdagencies.App
import com.svd.svdagencies.data.api.admin.AdminItemsApi
import com.svd.svdagencies.data.api.admin.AdminOrdersApi
import com.svd.svdagencies.data.api.admin.AdminPaymentsApi
import com.svd.svdagencies.data.api.admin.BillsDashboardApi
import com.svd.svdagencies.data.api.admin.CashbookApi
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.auth.AuthInterceptor
import com.svd.svdagencies.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL =
        "http://ec2-18-235-222-205.compute-1.amazonaws.com/"

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(SessionManager(App.Companion.context)))
        .addInterceptor(logging)
        .build()


    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val adminCustomerDashboard: CustomerDashboardApi by lazy {
        retrofit.create(CustomerDashboardApi::class.java)
    }

    val adminItemsApi: AdminItemsApi by lazy {
        retrofit.create(AdminItemsApi::class.java)
    }

    val billsDashboardApi: BillsDashboardApi by lazy {
        retrofit.create(BillsDashboardApi::class.java)
    }

    val cashbookApi: CashbookApi by lazy {
        retrofit.create(CashbookApi::class.java)
    }

    val adminPaymentsApi: AdminPaymentsApi by lazy {
        retrofit.create(AdminPaymentsApi::class.java)
    }

    val adminOrdersApi: AdminOrdersApi by lazy {
        retrofit.create(AdminOrdersApi::class.java)
    }
}
