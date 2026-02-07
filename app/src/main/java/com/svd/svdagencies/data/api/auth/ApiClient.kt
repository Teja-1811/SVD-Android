package com.svd.svdagencies.data.api.auth

import com.svd.svdagencies.App
import com.svd.svdagencies.data.api.admin.AdminApi
import com.svd.svdagencies.data.api.admin.AdminCompaniesApi
import com.svd.svdagencies.data.api.admin.AdminItemsApi
import com.svd.svdagencies.data.api.admin.AdminOrdersApi
import com.svd.svdagencies.data.api.admin.AdminPaymentsApi
import com.svd.svdagencies.data.api.admin.AdminStockApi
import com.svd.svdagencies.data.api.admin.BillsDashboardApi
import com.svd.svdagencies.data.api.admin.CashbookApi
import com.svd.svdagencies.data.api.admin.CustomerDashboardApi
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL = "https://svdagencies.shop/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Client for general API calls (includes AuthInterceptor)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(SessionManager(App.context)))
        .addInterceptor(logging)
        .build()

    // Client specifically for Auth (NO AuthInterceptor to avoid interference)
    private val authClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(authClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi by lazy {
        authRetrofit.create(AuthApi::class.java)
    }

    val adminApi: AdminApi by lazy {
        retrofit.create(AdminApi::class.java)
    }

    val adminCustomerDashboard: CustomerDashboardApi by lazy {
        retrofit.create(CustomerDashboardApi::class.java)
    }

    val adminItemsApi: AdminItemsApi by lazy {
        retrofit.create(AdminItemsApi::class.java)
    }

    val adminCompaniesApi: AdminCompaniesApi by lazy {
        retrofit.create(AdminCompaniesApi::class.java)
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

    val adminStockApi: AdminStockApi by lazy {
        retrofit.create(AdminStockApi::class.java)
    }

    val customerApi: CustomerApi by lazy {
        retrofit.create(CustomerApi::class.java)
    }
}
