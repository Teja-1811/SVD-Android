package com.svd.svdagencies.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    fun save(context: Context, latestOrder: LatestCustomerOrder) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LATEST_ORDER, gson.toJson(latestOrder)).apply()
    }

    fun get(context: Context): LatestCustomerOrder? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LATEST_ORDER, null) ?: return null
        val type = object : TypeToken<LatestCustomerOrder>() {}.type
        return runCatching { gson.fromJson<LatestCustomerOrder>(json, type) }.getOrNull()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LATEST_ORDER).apply()
    }
}

object CustomerOrderWindow {
    private const val START_MINUTES = 9 * 60
    private const val END_MINUTES = 20 * 60

    fun isOpen(now: Calendar = Calendar.getInstance()): Boolean {
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return currentMinutes in START_MINUTES..END_MINUTES
    }

    fun statusMessage(now: Calendar = Calendar.getInstance()): String {
        return if (isOpen(now)) {
            "Place or edit orders before 8:00 PM today."
        } else {
            "Order changes are available daily from 9:00 AM to 8:00 PM."
        }
    }
}
