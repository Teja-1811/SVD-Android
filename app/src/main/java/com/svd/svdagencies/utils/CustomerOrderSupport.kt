package com.svd.svdagencies.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.Calendar

data class LatestCustomerOrder(
    val orderNumber: String?,
    val placedAtMillis: Long,
    val items: List<LatestCustomerOrderItem>
)

data class LatestCustomerOrderItem(
    val productId: Int,
    val name: String,
    val company: String,
    val quantity: Double,
    val unitPrice: Double
)

object LatestCustomerOrderStore {
    private const val PREF_NAME = "svd_customer_orders"
    private const val KEY_LATEST_ORDER = "latest_order"
    private val gson = Gson()

    private fun latestOrderKey(context: Context): String {
        val session = SessionManager(context)
        return "${KEY_LATEST_ORDER}_${session.getRole()}_${session.getUserId()}"
    }

    fun save(context: Context, latestOrder: LatestCustomerOrder) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(latestOrderKey(context), gson.toJson(latestOrder)).apply()
    }

    fun get(context: Context): LatestCustomerOrder? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(latestOrderKey(context), null) ?: return null
        return runCatching {
            val root = gson.fromJson(json, JsonObject::class.java)
            val items = root.getAsJsonArray("items")?.mapNotNull { element ->
                val item = element.asJsonObject
                LatestCustomerOrderItem(
                    productId = item.get("productId")?.asInt ?: return@mapNotNull null,
                    name = item.get("name")?.asString.orEmpty(),
                    company = item.get("company")?.asString.orEmpty(),
                    quantity = item.get("quantity")?.asDouble ?: 0.0,
                    unitPrice = item.get("unitPrice")?.asDouble ?: 0.0
                )
            }.orEmpty()

            LatestCustomerOrder(
                orderNumber = root.get("orderNumber")?.takeUnless { it.isJsonNull }?.asString,
                placedAtMillis = root.get("placedAtMillis")?.asLong ?: 0L,
                items = items
            )
        }.getOrElse {
            clear(context)
            null
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(latestOrderKey(context)).apply()
    }
}

object CustomerOrderWindow {
    data class Window(val is_open: Boolean = false, val message: String = "Checking order timings...")
    private var windows: Map<String, Window> = emptyMap()

    fun refresh(done: () -> Unit = {}) {
        com.svd.svdagencies.data.api.auth.ApiClient.customerApi.getOrderTimings()
            .enqueue(object : retrofit2.Callback<Map<String, Window>> {
                override fun onResponse(call: retrofit2.Call<Map<String, Window>>, response: retrofit2.Response<Map<String, Window>>) {
                    windows = if (response.isSuccessful) response.body().orEmpty() else emptyMap()
                    done()
                }
                override fun onFailure(call: retrofit2.Call<Map<String, Window>>, t: Throwable) {
                    windows = emptyMap()
                    done()
                }
            })
    }

    fun isOpen(orderType: String = "regular"): Boolean =
        orderType == "pre_order" || windows[orderType]?.is_open == true

    fun statusMessage(orderType: String = "regular"): String =
        if (orderType == "pre_order")
            "Pre-orders are open 24 hours. Choose delivery at least 2 days from today."
        else if (windows.isEmpty())
            "Refresh to check the admin's current order timings."
        else windows[orderType]?.message ?: "Order timings unavailable. Please refresh."
}
