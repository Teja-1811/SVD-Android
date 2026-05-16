package com.svd.svdagencies.data.api.auth

import com.svd.svdagencies.App
import com.svd.svdagencies.BuildConfig
import com.svd.svdagencies.data.api.admin.*
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.data.api.customer.ProductApi
import com.svd.svdagencies.data.api.delivery.DeliveryApi
import com.svd.svdagencies.data.api.user.UserApi
import com.svd.svdagencies.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL = "https://www.svdagencies.shop/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val cacheSize = (5 * 1024 * 1024).toLong() // 5 MB
    private val cache = okhttp3.Cache(App.context.cacheDir, cacheSize)

    // Client for general API calls (includes AuthInterceptor)
    private val client = OkHttpClient.Builder()
        .cache(cache)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(AuthInterceptor(SessionManager(App.context)))
        .addInterceptor(logging)
        .build()

    // Client specifically for Auth (NO AuthInterceptor to avoid interference)
    private val authClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
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

    val adminEnquiriesApi: AdminEnquiriesApi by lazy {
        retrofit.create(AdminEnquiriesApi::class.java)
    }

    val adminOrdersApi: AdminOrdersApi by lazy {
        retrofit.create(AdminOrdersApi::class.java)
    }

    val adminDeliveryDashboardApi: AdminDeliveryDashboardApi by lazy {
        retrofit.create(AdminDeliveryDashboardApi::class.java)
    }

    val adminStockApi: AdminStockApi by lazy {
        retrofit.create(AdminStockApi::class.java)
    }

    val subscriptionApi: SubscriptionApi by lazy {
        retrofit.create(SubscriptionApi::class.java)
    }

    val customerApi: CustomerApi by lazy {
        retrofit.create(CustomerApi::class.java)
    }

    val productApi: ProductApi by lazy {
        retrofit.create(ProductApi::class.java)
    }

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    val deliveryApi: DeliveryApi by lazy {
        retrofit.create(DeliveryApi::class.java)
    }

    val pushApi: PushApi by lazy {
        retrofit.create(PushApi::class.java)
    }

    /**
     * Helper to construct full image URLs for items.
     * The final path is expected to be BASE_URL + images/items/ + fileName
     */
    fun getImageUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("http", ignoreCase = true)) return path
        
        val cleanPath = path.removePrefix("/")
        val finalPath = if (cleanPath.startsWith("images/items/")) {
            cleanPath
        } else {
            "images/items/$cleanPath"
        }
        
        return BASE_URL.removeSuffix("/") + "/" + finalPath
    }

    /**
     * Helper to construct full logo URLs for companies.
     */
    fun getLogoUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("http", ignoreCase = true)) return path
        val cleanPath = path.removePrefix("/")
        return BASE_URL.removeSuffix("/") + "/" + cleanPath
    }
}
