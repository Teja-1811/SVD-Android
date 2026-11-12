package com.svd.svdagencies.ui.customer

import android.os.Bundle
import android.view.View
import android.view.Gravity
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
        val root = com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(28))
            setBackgroundColor(getColor(R.color.window_background))
        }
        val isUser = SessionManager(this).getRole() == UserRole.USER
        val identity = section(
            if (isUser) "Personal details" else "Business details",
            if (isUser) "Keep your contact information current." else "Contact and shop information."
        )
        val identityFields = if (isUser) {
            listOf("name" to "Full name", "phone" to "Phone number")
        } else {
            listOf("name" to "Contact name", "phone" to "Phone number", "shop_name" to "Shop name")
        }
        identityFields.forEach { (key, label) -> addProfileField(identity, key, label) }
        root.addView(card(identity))
        if (!isUser) {
            val address = section("Address", "Used for delivery and account records.")
            listOf("area" to "Area", "city" to "City", "state" to "State", "pin_code" to "PIN code")
                .forEach { (key, label) -> addProfileField(address, key, label) }
            root.addView(card(address))
        }
        if (isUser) {
            val location = section("Delivery location", "Set the location where your orders should be delivered.")
            locationButton = button("Set delivery location").apply {
                setOnClickListener { deliveryLocation.choose { pin -> showDeliveryPin(pin) } }
            }.also { location.addView(it) }
            mapButton = button("View in Google Maps").apply {
                backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this@CustomerProfileActivity, com.svd.svdagencies.R.color.surface_background))
                setTextColor(com.google.android.material.color.MaterialColors.getColor(this@CustomerProfileActivity, com.google.android.material.R.attr.colorPrimary, getColor(R.color.textColorPrimary)))
                visibility = View.GONE
                setOnClickListener { deliveryPin?.let { DeliveryMaps.open(this@CustomerProfileActivity, it.latitude, it.longitude) } }
            }.also { location.addView(it) }
            root.addView(card(location))
        }
        saveButton = button("Save changes").apply {
            minHeight = dp(56)
            setIconResource(R.drawable.ic_check)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconPadding = dp(8)
            setOnClickListener { save() }
        }
        root.addView(saveButton)
        PortalNavigation.installPage(this, ScrollView(this, null, android.R.attr.scrollViewStyle).apply {
            setBackgroundColor(getColor(R.color.window_background)); isFillViewport = true; addView(root)
        }, "My Profile", R.id.nav_profile)
        load()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun addProfileField(section: LinearLayout, key: String, label: String) {
        val field = TextInputLayout(this).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat())
            boxBackgroundColor = androidx.core.content.ContextCompat.getColor(this@CustomerProfileActivity, R.color.surface_background)
            boxStrokeColor = androidx.core.content.ContextCompat.getColor(this@CustomerProfileActivity, R.color.card_stroke_color)
            boxStrokeWidth = dp(1)
            boxStrokeWidthFocused = dp(2)
            boxCollapsedPaddingTop = dp(8)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }
        }
        val input = TextInputEditText(field.context).apply {
            isSingleLine = true
            textSize = 16f
            minHeight = dp(64)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), 0)
            inputType = when (key) {
                "phone" -> InputType.TYPE_CLASS_PHONE
                "pin_code" -> InputType.TYPE_CLASS_NUMBER
                "name" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            setTextColor(androidx.core.content.ContextCompat.getColor(this@CustomerProfileActivity, R.color.textColorPrimary))
        }
        fields[key] = input
        field.addView(input, LinearLayout.LayoutParams(-1, -2))
        section.addView(field)
    }

    private fun section(title: String, subtitle: String? = null) = com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(20))
        addView(TextView(this@CustomerProfileActivity).apply {
            text = title; textSize = 18f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(androidx.core.content.ContextCompat.getColor(this@CustomerProfileActivity, com.svd.svdagencies.R.color.textColorPrimary))
        })
        subtitle?.let { value ->
            addView(TextView(this@CustomerProfileActivity).apply {
                text = value
                textSize = 13f
                setTextColor(androidx.core.content.ContextCompat.getColor(this@CustomerProfileActivity, R.color.textColorSecondary))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) }
            })
        }
    }

    private fun card(body: View) = MaterialCardView(this).apply {
        radius = dp(20).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
        strokeColor = androidx.core.content.ContextCompat.getColor(this@CustomerProfileActivity, com.svd.svdagencies.R.color.card_stroke_color); setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this@CustomerProfileActivity, com.svd.svdagencies.R.color.surface_background))
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(18) }
        addView(body)
    }

    private fun button(label: String) = MaterialButton(this).apply {
        text = label; isAllCaps = false; cornerRadius = dp(16); minHeight = dp(52)
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(2); bottomMargin = dp(8) }
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
