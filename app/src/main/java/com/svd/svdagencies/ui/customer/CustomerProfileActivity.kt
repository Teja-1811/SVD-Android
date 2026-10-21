package com.svd.svdagencies.ui.customer

import android.os.Bundle
import android.view.View
import com.svd.svdagencies.utils.DeliveryCoordinates
import com.svd.svdagencies.utils.DeliveryLocationPicker
import com.svd.svdagencies.utils.DeliveryMaps
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import android.widget.Button
import android.widget.TextView
import android.graphics.Color
import android.text.InputType
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
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
    private var saving = false
    private lateinit var saveButton: MaterialButton
    private val fields = linkedMapOf<String, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16),dp(20),dp(16),dp(24))
            setBackgroundColor(Color.rgb(252,244,245))
        }
        val isUser = SessionManager(this).getRole() == UserRole.USER
        val account = section("Personal details")
        val profileFields = if (isUser) listOf("name" to "Full name", "phone" to "Phone number") else
            listOf("name" to "Name", "phone" to "Phone", "shop_name" to "Shop name", "area" to "Area", "city" to "City", "state" to "State", "pin_code" to "PIN code")
        profileFields.forEach { (key, label) ->
            val field = TextInputLayout(this).apply {
                hint = label
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(dp(12).toFloat(),dp(12).toFloat(),dp(12).toFloat(),dp(12).toFloat())
                boxBackgroundColor = Color.WHITE
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(14) }
            }
            val input = TextInputEditText(field.context).apply {
                isSingleLine = true
                inputType = when(key) {
                    "phone" -> InputType.TYPE_CLASS_PHONE
                    "pin_code" -> InputType.TYPE_CLASS_NUMBER
                    "name" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                    else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                }
                setTextColor(Color.rgb(24,59,71))
            }
            fields[key] = input
            field.addView(input, LinearLayout.LayoutParams(-1,-2))
            account.addView(field)
        }
        root.addView(card(account))
        if (isUser) {
            val location = section("Delivery location")
            locationButton = button("Set delivery location").apply {
                setOnClickListener { deliveryLocation.choose { pin -> showDeliveryPin(pin) } }
            }.also { location.addView(it) }
            mapButton = button("View in Google Maps").apply {
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(248,241,242))
                setTextColor(Color.rgb(193,43,43))
                visibility = View.GONE
                setOnClickListener { deliveryPin?.let { DeliveryMaps.open(this@CustomerProfileActivity, it.latitude, it.longitude) } }
            }.also { location.addView(it) }
            root.addView(card(location))
        }
        saveButton = button("Save changes").apply { setOnClickListener { save() } }
        root.addView(saveButton)
        PortalNavigation.installPage(this, ScrollView(this).apply {
            setBackgroundColor(Color.rgb(252,244,245)); isFillViewport = true; addView(root)
        }, "My Profile", R.id.nav_profile)
        load()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun section(title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18),dp(18),dp(18),dp(18))
        addView(TextView(this@CustomerProfileActivity).apply {
            text = title; textSize = 18f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(24,59,71))
        })
    }

    private fun card(body: View) = MaterialCardView(this).apply {
        radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
        strokeColor = Color.rgb(228,233,236); setCardBackgroundColor(Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(16) }
        addView(body)
    }

    private fun button(label: String) = MaterialButton(this).apply {
        text = label; isAllCaps = false; cornerRadius = dp(12); minHeight = dp(52)
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(12) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun load() = ApiClient.customerApi.getProfile().enqueue(object : Callback<Map<String, Any>> {
        override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
            if (isFinishing || isDestroyed) return
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
        locationButton?.text = "Change delivery location"
        mapButton?.visibility = View.VISIBLE
    }

    private fun save() {
        if (saving) return
        if (fields["name"]?.text.isNullOrBlank()) { fields["name"]?.error = "Enter your name"; return }
        if (fields["phone"]?.text.isNullOrBlank()) { fields["phone"]?.error = "Enter your phone number"; return }
        saving = true
        saveButton.isEnabled = false
        saveButton.text = "Saving…"
        val body = fields.mapValues { it.value.text.toString().trim() }
        ApiClient.customerApi.updateProfile(body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (isFinishing || isDestroyed) return
                saving = false; saveButton.isEnabled = true; saveButton.text = "Save changes"
                if (response.isSuccessful) Toast.makeText(this@CustomerProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show() else failure() }
            override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) {
                if (isFinishing || isDestroyed) return
                saving = false; saveButton.isEnabled = true; saveButton.text = "Save changes"
                failure()
            }
        })
    }
    private fun failure() { if (!isFinishing && !isDestroyed) Toast.makeText(this, "Unable to update profile", Toast.LENGTH_SHORT).show() }
}
