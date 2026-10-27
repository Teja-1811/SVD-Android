package com.svd.svdagencies.ui

import android.app.DatePickerDialog
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.JsonObject
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.AdminReportsApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.utils.CompactLinearLayout
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.Calendar
import java.util.Locale

interface BusinessSettingsApi {
    @GET("api/business-settings/") suspend fun read(): JsonObject
    @POST("api/business-settings/") suspend fun save(@Body body: JsonObject): JsonObject
}

/** Admin-only server-backed settings. Each section saves independently. */
class SettingsBusinessUi(private val activity: ToolsActivity, private val root: LinearLayout) {
    private val api by lazy { ApiClient.retrofit.create(BusinessSettingsApi::class.java) }
    private val gstApi by lazy { ApiClient.retrofit.create(AdminReportsApi::class.java) }
    private val fields = linkedMapOf<String, TextInputEditText>()
    private lateinit var gstin: TextInputEditText
    private lateinit var effective: MaterialButton
    private lateinit var enabled: SwitchMaterial
    private lateinit var status: TextView
    private lateinit var saveBusiness: MaterialButton
    private lateinit var saveGst: MaterialButton
    private var busy = false
    private var loaded = false
    private var gstLoaded = false
    private var effectiveDate = ""

    fun attach() {
        if (SessionManager(activity).getRole() != UserRole.ADMIN) { root.visibility = View.GONE; return }
        val panel = activity.layoutInflater.inflate(R.layout.include_business_settings, root, false)
        root.addView(panel)
        for ((key, id) in listOf("upi_id" to R.id.businessUpi, "pan" to R.id.businessPan,
                "fssai" to R.id.businessFssai, "trade_licence" to R.id.businessTrade,
                "lin" to R.id.businessLin, "details" to R.id.businessOther)) {
            fields[key] = panel.findViewById(id)
        }
        gstin = panel.findViewById(R.id.businessGstin)
        effective = panel.findViewById(R.id.businessEffective)
        enabled = panel.findViewById(R.id.businessGstEnabled)
        status = panel.findViewById(R.id.businessStatus)
        saveBusiness = panel.findViewById(R.id.businessSave)
        saveGst = panel.findViewById(R.id.businessSaveGst)
        saveBusiness.setOnClickListener { if (!busy) save(false) }
        saveGst.setOnClickListener { if (!busy) save(true) }
        effective.setOnClickListener { selectDate() }
        panel.findViewById<MaterialButton>(R.id.businessRefresh).setOnClickListener { if (!busy) load() }
        load()
    }

    private fun setBusy(value: Boolean) {
        busy = value
        saveBusiness.isEnabled = !value && loaded; saveGst.isEnabled = !value && gstLoaded
    }
    private fun load() {
        setBusy(true); status.text = "Loading settings…"
        activity.lifecycleScope.launch {
            try {
                val data = api.read()
                val business = data.getAsJsonObject("business")
                fields.forEach { (key, view) -> view.setText(value(business, key)) }
                showGst(data.getAsJsonObject("gst"))
                loaded = true; status.text = ""
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                status.text = message(error)
                // Keep existing GST registration usable while the additive certificate endpoint deploys.
                if (error is HttpException && error.code() == 404) {
                    try { showGst(gstApi.gst(emptyMap()).getAsJsonObject("config")) }
                    catch (fallback: Exception) { if (fallback is CancellationException) throw fallback }
                }
            } finally { setBusy(false) }
        }
    }
    private fun showGst(gst: JsonObject) {
        gstin.setText(value(gst, "gstin")); effectiveDate = value(gst, "effective_from")
        effective.text = effectiveDate.ifBlank { "Select date" }
        enabled.isChecked = gst.get("enabled")?.asBoolean ?: false
        gstLoaded = true
    }
    private fun selectDate() {
        val date = Calendar.getInstance()
        val parts = effectiveDate.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size == 3) date.set(parts[0], parts[1] - 1, parts[2])
        DatePickerDialog(activity, { _, y, m, d ->
            effectiveDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            effective.text = effectiveDate
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun save(gst: Boolean) {
        if (!(if (gst) gstLoaded else loaded) || SessionManager(activity).getRole() != UserRole.ADMIN) return
        val body = JsonObject().apply {
            if (gst) {
                addProperty("gstin", gstin.text.toString().trim().uppercase(Locale.US))
                addProperty("effective_from", effectiveDate); addProperty("enabled", enabled.isChecked)
            } else fields.forEach { (key, edit) -> addProperty(key, edit.text.toString().trim()) }
        }
        setBusy(true); status.text = "Saving…"
        activity.lifecycleScope.launch {
            try {
                if (gst) gstApi.settings(body) else api.save(body)
                status.text = if (gst) "GST settings saved" else "Certificate details saved"
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                status.text = message(error)
            } finally { setBusy(false) }
        }
    }
    private fun value(data: JsonObject, key: String) = data.get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
    private fun message(error: Exception): String = if (error is HttpException) {
        if (error.code() == 404) "Business settings require the server update. Refresh after deployment."
        else "Unable to save or load settings (${error.code()}). ${error.response()?.errorBody()?.string()?.take(300).orEmpty()}"
    } else "Unable to connect. Refresh to try again."
    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
}
