package com.svd.svdagencies.data.model.admin

import com.google.gson.annotations.SerializedName
import com.svd.svdagencies.data.model.admin.Items.AdminItem

data class SubscriptionDashboardResponse(
    @SerializedName("plans") val plansList: List<SubscriptionPlan>,
    @SerializedName("active_subscriptions") val activeSubscriptions: List<CustomerSubscription>,
    @SerializedName("de_activated_subscriptions") val deactivatedSubscriptions: List<CustomerSubscription>,
    @SerializedName("expired_subscriptions") val expiredSubscriptions: List<CustomerSubscription>,
    @SerializedName("expiring_soon") val expiringSoon: List<CustomerSubscription>,
    @SerializedName("items") val items: List<AdminItem>,
    @SerializedName("customers") val customers: List<SubscriptionCustomer>,
    @SerializedName("total_active") val totalActive: Int,
    @SerializedName("total_expired") val totalExpired: Int,
    @SerializedName("total_plans") val totalPlans: Int,
    @SerializedName("expiring_count") val expiringCount: Int
)

data class SubscriptionPlan(
    val id: Int,
    val name: String,
    val price: String,
    @SerializedName("duration_in_days") val durationInDays: Int,
    val description: String?,
    val items: List<SubscriptionPlanItem>? = null
)

data class SubscriptionPlanItem(
    val id: Int,
    @SerializedName("item_id") val itemId: Int,
    @SerializedName("item_name") val itemName: String,
    val quantity: Int
)

data class SubscriptionCustomer(
    val id: Int,
    val name: String,
    val phone: String,
    val area: String?
)

data class CustomerSubscription(
    val id: Int,
    @SerializedName("customer_id") val customerId: Int,
    val customer: String?,
    @SerializedName("plan_id") val planId: Int,
    val plan: String?,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("is_active") val isActive: Boolean
)

data class Delivery(
    @SerializedName("subscription_id") val subscriptionId: Int,
    @SerializedName("customer_id") val customerId: Int,
    val customer: String,
    val phone: String,
    @SerializedName("plan_id") val planId: Int,
    val plan: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String
)
