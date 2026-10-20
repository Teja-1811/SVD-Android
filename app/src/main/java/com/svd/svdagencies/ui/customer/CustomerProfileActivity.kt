package com.svd.svdagencies.ui.customer

import android.os.Bundle
import android.view.View
import com.svd.svdagencies.utils.DeliveryCoordinates
import com.svd.svdagencies.utils.DeliveryLocationPicker
import com.svd.svdagencies.utils.DeliveryMaps
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ScrollView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.base.BaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerProfileActivity : BaseActivity() {
    private val deliveryLocation = DeliveryLocationPicker(this) { this }
    private var deliveryPin: DeliveryCoordinates? = null
    private var locationButton: Button? = null
    private var mapButton: Button? = null
    private val fields = linkedMapOf<String, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        val isUser = SessionManager(this).getRole() == UserRole.USER
        val profileFields = if (isUser) listOf("name" to "Name", "phone" to "Phone") else
            listOf("name" to "Name", "phone" to "Phone", "shop_name" to "Shop name", "area" to "Area", "city" to "City", "state" to "State", "pin_code" to "PIN code")
        profileFields.forEach { (key, hint) ->
            fields[key] = EditText(this).apply { this.hint = hint }; root.addView(fields[key])
        }
        if (isUser) {
            locationButton = Button(this).apply {
                text = "Set delivery location"
                setOnClickListener { deliveryLocation.choose { pin -> showDeliveryPin(pin) } }
            }.also { root.addView(it) }
            mapButton = Button(this).apply {
                text = "View delivery pin in Google Maps"
                visibility = View.GONE
                setOnClickListener { deliveryPin?.let { DeliveryMaps.open(this@CustomerProfileActivity, it.latitude, it.longitude) } }
            }.also { root.addView(it) }
        }
        root.addView(Button(this).apply { text = "Save profile"; setOnClickListener { save() } })
        PortalNavigation.installPage(this, ScrollView(this).apply { addView(root) }, "My Profile", R.id.nav_profile)
        load()
    }

    @Suppress("UNCHECKED_CAST")
    private fun load() = ApiClient.customerApi.getProfile().enqueue(object : Callback<Map<String, Any>> {
        override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
            val profile = response.body()?.get("profile") as? Map<String, Any> ?: return failure()
            fields.forEach { (key, input) -> input.setText(profile[key]?.toString().orEmpty()) }
            val latitude = profile["delivery_latitude"]?.toString()?.toDoubleOrNull()
            val longitude = profile["delivery_longitude"]?.toString()?.toDoubleOrNull()
            if (latitude != null && longitude != null) showDeliveryPin(DeliveryCoordinates(latitude, longitude))
        }
        override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) = failure()
    })

    private fun showDeliveryPin(pin: DeliveryCoordinates) {
        deliveryPin = pin
        locationButton?.text = "Change saved delivery location"
        mapButton?.visibility = View.VISIBLE
    }

    private fun save() {
        val body = fields.mapValues { it.value.text.toString().trim() }
        ApiClient.customerApi.updateProfile(body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) { if (response.isSuccessful) Toast.makeText(this@CustomerProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show() else failure() }
            override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) = failure()
        })
    }
    private fun failure() = Toast.makeText(this, "Unable to update profile", Toast.LENGTH_SHORT).show()
}
