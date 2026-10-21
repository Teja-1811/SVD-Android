package com.svd.svdagencies.data.api.customer

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CustomerHistoryApi {
    @GET("api/customer/orders/history/")
    fun getOrderHistory(@Query("limit") limit: Int = 50): Call<CustomerOrderHistoryResponse>

    @DELETE("api/customer/orders/history/{orderId}/")
    fun deleteOrder(@Path("orderId") orderId: Int): Call<Map<String, Any>>

    @GET("api/customer/reports/monthly/")
    fun getMonthlyReport(@Query("date") date: String): Call<CustomerMonthlyReport>

    @GET("api/customer/brochure/")
    fun downloadBrochure(): Call<ResponseBody>
}

data class CustomerOrderHistoryResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("orders") val orders: List<CustomerOrderHistoryItem> = emptyList(),
)

data class CustomerOrderHistoryItem(
    @SerializedName("order_id") val orderId: Int,
    @SerializedName("order_number") val orderNumber: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("order_date") val orderDate: String = "",
    @SerializedName("delivery_date") val deliveryDate: String = "",
    @SerializedName("delivery_charge") val deliveryCharge: Double = 0.0,
    @SerializedName("grand_total") val grandTotal: Double = 0.0,
    @SerializedName("can_delete") val canDelete: Boolean = false,
    @SerializedName("items") val items: List<CustomerOrderHistoryLine> = emptyList(),
)

data class CustomerOrderHistoryLine(
    @SerializedName("item_name") val itemName: String = "",
    @SerializedName("requested_quantity") val quantity: Int = 0,
    @SerializedName("discount") val discount: Double = 0.0,
    @SerializedName("requested_price") val unitPrice: Double = 0.0,
    @SerializedName("requested_total") val total: Double = 0.0,
)

data class CustomerMonthlyReport(
    @SerializedName("date") val date: String = "",
    @SerializedName("total_bills") val totalBills: Int = 0,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName("average_amount") val averageAmount: Double = 0.0,
)
