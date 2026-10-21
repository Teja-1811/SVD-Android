package com.svd.svdagencies.utils

import android.Manifest
import android.content.DialogInterface
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.gson.JsonObject
import com.svd.svdagencies.data.api.auth.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class DeliveryCoordinates(val latitude: Double, val longitude: Double) {
    fun json() = JsonObject().apply {
        addProperty("delivery_latitude", latitude)
        addProperty("delivery_longitude", longitude)
    }
}

interface DeliveryLocationApi {
    @GET("api/customer/delivery-location/")
    fun get(): Call<JsonObject>
    @POST("api/customer/delivery-location/")
    fun save(@Body point: JsonObject): Call<JsonObject>
}

object DeliveryMaps {
    fun open(context: Context, latitude: Double, longitude: Double) {
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))
        } catch (_: ActivityNotFoundException) {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            catch (_: ActivityNotFoundException) { Toast.makeText(context, "Install Google Maps or a browser to view this pin.", Toast.LENGTH_LONG).show() }
        }
    }
}

/** Saves a coordinate pin on the account; no postal address or background tracking. */
class DeliveryLocationPicker(caller: ActivityResultCaller, private val context: () -> Context) {
    private var afterPermission: (() -> Unit)? = null
    private val permission = caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) afterPermission?.invoke()
        else Toast.makeText(context(), "Location permission was declined. You can enter the pin coordinates instead.", Toast.LENGTH_LONG).show()
    }

    fun choose(onDismissed: () -> Unit = {}, onSelected: (DeliveryCoordinates) -> Unit) {
        val ctx = context()
        val form = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
        }
        fun dp(value: Int) = (value * ctx.resources.displayMetrics.density).toInt()
        form.addView(TextView(ctx).apply {
            text = "Delivery pin"; textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.rgb(24,59,71))
            setPadding(0,dp(20),0,dp(6))
        })
        form.addView(TextView(ctx).apply {
            text = "Choose your location or enter map coordinates."
            textSize = 13f; setTextColor(android.graphics.Color.rgb(100,121,133))
            setPadding(0,0,0,dp(16))
        })
        val inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        val current = MaterialButton(ctx).apply {
            text = "Use current location"; isAllCaps = false; cornerRadius = dp(12); minHeight = dp(48)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(8) }
        }
        form.addView(current)
        fun coordinateField(label: String): EditText {
            val box = com.google.android.material.textfield.TextInputLayout(ctx).apply {
                hint = label
                boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(dp(12).toFloat(),dp(12).toFloat(),dp(12).toFloat(),dp(12).toFloat())
                boxBackgroundColor = android.graphics.Color.WHITE
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(12) }
            }
            val input = com.google.android.material.textfield.TextInputEditText(box.context).apply {
                this.inputType = inputType; isSingleLine = true; textSize = 16f
            }
            box.addView(input, LinearLayout.LayoutParams(-1,-2)); form.addView(box)
            return input
        }
        val latitude = coordinateField("Latitude")
        val longitude = coordinateField("Longitude")
        val preview = MaterialButton(ctx).apply {
            text = "Preview in Google Maps"; isAllCaps = false; cornerRadius = dp(12); minHeight = dp(48)
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(252,244,245))
            setTextColor(android.graphics.Color.rgb(195,43,43))
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(16); bottomMargin = dp(12) }
        }
        form.addView(preview)
        fun point(): DeliveryCoordinates? {
            val lat = latitude.text.toString().trim().toDoubleOrNull()
            val lng = longitude.text.toString().trim().toDoubleOrNull()
            if (lat == null || !lat.isFinite() || lat !in -90.0..90.0) { latitude.error = "Enter latitude between -90 and 90"; return null }
            if (lng == null || !lng.isFinite() || lng !in -180.0..180.0) { longitude.error = "Enter longitude between -180 and 180"; return null }
            return DeliveryCoordinates(lat, lng)
        }
        preview.setOnClickListener { point()?.let { DeliveryMaps.open(ctx, it.latitude, it.longitude) } }
        val dialog = WhiteMaterialAlertDialogBuilder(ctx)
            .setView(form).setNegativeButton("Cancel", null).setPositiveButton("Save and continue", null).create()
        val api = ApiClient.retrofit.create(DeliveryLocationApi::class.java)
        val handler = Handler(Looper.getMainLooper())
        val manager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var listener: LocationListener? = null
        var timeout: Runnable? = null
        fun stopLocation() {
            listener?.let { try { manager.removeUpdates(it) } catch (_: SecurityException) {} }
            listener = null
            timeout?.let { handler.removeCallbacks(it) }
            timeout = null
            current.isEnabled = true
            current.text = "Use current location"
        }
        fun capture() {
            if (!dialog.isShowing) return
            stopLocation()
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { manager.isProviderEnabled(it) }
            if (providers.isEmpty()) {
                Toast.makeText(ctx, "Turn on Location in phone settings, or enter the pin coordinates.", Toast.LENGTH_LONG).show(); return
            }
            current.isEnabled = false
            current.text = "Finding your location…"
            listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!dialog.isShowing) { stopLocation(); return }
                    latitude.setText(String.format(java.util.Locale.US, "%.6f", location.latitude))
                    longitude.setText(String.format(java.util.Locale.US, "%.6f", location.longitude))
                    stopLocation()
                }
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
                @Deprecated("Legacy location callback")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            providers.forEach { provider ->
                try { manager.requestSingleUpdate(provider, listener!!, Looper.getMainLooper()) }
                catch (_: SecurityException) { /* A coarse grant may only allow the network provider. */ }
                catch (_: IllegalArgumentException) { /* Provider became unavailable. */ }
            }
            timeout = Runnable {
                stopLocation()
                if (dialog.isShowing) Toast.makeText(ctx, "Could not find your location. Retry outdoors or enter the pin coordinates.", Toast.LENGTH_LONG).show()
            }.also { handler.postDelayed(it, 20000) }
        }
        afterPermission = { capture() }
        current.setOnClickListener {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) capture()
            else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        val loadCall = api.get()
        dialog.setOnDismissListener { onDismissed(); stopLocation(); afterPermission = null; loadCall.cancel() }
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.rgb(100,121,133))
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).apply {
                setTextColor(android.graphics.Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(211,45,45))
                isAllCaps = false
            }
            loadCall.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (!dialog.isShowing || latitude.text.isNotBlank() || longitude.text.isNotBlank()) return
                    val body = response.body() ?: return
                    body.get("delivery_latitude")?.takeUnless { it.isJsonNull }?.let { latitude.setText(it.asString) }
                    body.get("delivery_longitude")?.takeUnless { it.isJsonNull }?.let { longitude.setText(it.asString) }
                }
                override fun onFailure(call: Call<JsonObject>, error: Throwable) = Unit
            })
            val save = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            save.setOnClickListener {
                val selected = point() ?: return@setOnClickListener
                stopLocation()
                save.isEnabled = false
                api.save(selected.json()).enqueue(object : Callback<JsonObject> {
                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                        if (!dialog.isShowing) return
                        save.isEnabled = true
                        if (response.isSuccessful && response.body()?.get("success")?.asBoolean == true) {
                            dialog.dismiss(); onSelected(selected)
                        } else Toast.makeText(ctx, "Could not save the delivery pin. Please retry.", Toast.LENGTH_LONG).show()
                    }
                    override fun onFailure(call: Call<JsonObject>, error: Throwable) {
                        if (!dialog.isShowing) return
                        save.isEnabled = true
                        Toast.makeText(ctx, "Could not save the delivery pin. Please retry.", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }
        dialog.show()
    }
}
