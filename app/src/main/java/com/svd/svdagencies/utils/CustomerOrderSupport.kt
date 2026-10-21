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
            .enqueue(object : retrofit2.Callback<JsonObject> {
                override fun onResponse(call: retrofit2.Call<JsonObject>, response: retrofit2.Response<JsonObject>) {
                    windows = if (response.isSuccessful) parseWindows(response.body()) else emptyMap()
                    done()
                }
                override fun onFailure(call: retrofit2.Call<JsonObject>, t: Throwable) {
                    windows = emptyMap()
                    done()
                }
            })
    }

    // Parse explicitly: the release shrinker can erase the generic Map value type,
    // leaving Gson map values that cannot be cast to Window.
    internal fun parseWindows(body: JsonObject?): Map<String, Window> =
        listOf("regular", "pre_order").mapNotNull { kind ->
            val row = body?.get(kind)?.takeIf { it.isJsonObject }?.asJsonObject
                ?: return@mapNotNull null
            val open = row.get("is_open")?.takeIf { it.isJsonPrimitive }
                ?.asJsonPrimitive?.takeIf { it.isBoolean }?.asBoolean ?: false
            val message = row.get("message")?.takeIf { it.isJsonPrimitive }
                ?.asJsonPrimitive?.takeIf { it.isString }?.asString
                ?: "Order timings unavailable. Please refresh."
            kind to Window(is_open = open, message = message)
        }.toMap()

    fun isOpen(orderType: String = "regular"): Boolean =
        orderType == "pre_order" || windows[orderType]?.is_open == true

    fun statusMessage(orderType: String = "regular"): String =
        if (orderType == "pre_order")
            "Pre-orders are open 24 hours. Choose delivery at least 2 days from today."
        else if (windows.isEmpty())
            "Refresh to check the admin's current order timings."
        else windows[orderType]?.message ?: "Order timings unavailable. Please refresh."
}
