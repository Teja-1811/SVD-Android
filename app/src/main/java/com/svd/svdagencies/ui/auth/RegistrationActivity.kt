package com.svd.svdagencies.ui.auth

import android.os.Bundle
import android.widget.ScrollView
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.CheckBox
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.base.BaseActivity
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.auth.RegisterRequest
import com.svd.svdagencies.data.api.auth.RegistrationResponse
import com.svd.svdagencies.utils.NetworkMessageUtils
import com.svd.svdagencies.utils.showLoading
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistrationActivity : BaseActivity() {
    private var hasOpenedTerms = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        hasOpenedTerms = savedInstanceState?.getBoolean("hasOpenedTerms") ?: false
        val termsAccepted = findViewById<CheckBox>(R.id.cbAcceptTerms)
        val termsInstruction = findViewById<TextView>(R.id.tvTermsInstruction)
        termsAccepted.isEnabled = hasOpenedTerms
        if (hasOpenedTerms) termsInstruction.text = "You can now accept the terms to continue."
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnReadTerms).setOnClickListener {
            val padding = (20 * resources.displayMetrics.density).toInt()
            val termsText = resources.openRawResource(R.raw.registration_terms).bufferedReader().use { it.readText() }
            val content = TextView(this).apply {
                text = HtmlCompat.fromHtml(termsText, HtmlCompat.FROM_HTML_MODE_COMPACT)
                textSize = 15f
                setTextColor(androidx.core.content.ContextCompat.getColor(this@RegistrationActivity, R.color.textColorPrimary))
                setPadding(padding, padding / 2, padding, padding)
                setTextIsSelectable(true)
            }
            val scroll = ScrollView(this).apply { addView(content) }
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Terms & Conditions")
                .setView(scroll)
                .setPositiveButton("Done", null)
                .create()
            dialog.setOnShowListener {
                dialog.window?.setLayout(
                    (resources.displayMetrics.widthPixels * 0.92).toInt(),
                    (resources.displayMetrics.heightPixels * 0.8).toInt()
                )
                hasOpenedTerms = true
                termsAccepted.isEnabled = true
                termsInstruction.text = "You can now accept the terms to continue."
            }
            dialog.show()
        }
        val typeGroup = findViewById<RadioGroup>(R.id.registrationTypeGroup)
        val name = findViewById<TextInputEditText>(R.id.etRegistrationName)
        val phone = findViewById<TextInputEditText>(R.id.etRegistrationPhone)
        val shop = findViewById<TextInputEditText>(R.id.etRegistrationShop)
        val area = findViewById<TextInputEditText>(R.id.etRegistrationArea)
        val password = findViewById<TextInputEditText>(R.id.etRegistrationPassword)
        val submit = findViewById<MaterialButton>(R.id.btnSubmitRegistration)

        submit.setOnClickListener {
            val accountType = if (typeGroup.checkedRadioButtonId == R.id.rbRetailer) "retailer" else "user"
            val enteredName = name.text?.toString()?.trim().orEmpty()
            val enteredPhone = phone.text?.toString()?.trim().orEmpty()
            val enteredPassword = password.text?.toString().orEmpty()
            if (enteredName.isBlank() || enteredPhone.isBlank() || enteredPassword.length < 6) {
                Toast.makeText(this, "Enter name, mobile number, and a 6-character password", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!hasOpenedTerms || !termsAccepted.isChecked) {
                Toast.makeText(this, "Open and accept the Terms and Conditions to continue", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            submit.showLoading(true, "Submitting...")
            ApiClient.authApi.register(
                RegisterRequest(enteredName, enteredPhone, enteredPassword, accountType, true, shop.text?.toString()?.trim().orEmpty(), area.text?.toString()?.trim().orEmpty())
            ).enqueue(object : Callback<RegistrationResponse> {
                override fun onResponse(call: Call<RegistrationResponse>, response: Response<RegistrationResponse>) {
                    submit.showLoading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.status == "success") {
                        Toast.makeText(this@RegistrationActivity, body.message ?: "Registration submitted", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegistrationActivity, body?.message ?: "Registration could not be submitted", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<RegistrationResponse>, error: Throwable) {
                    submit.showLoading(false)
                    Toast.makeText(this@RegistrationActivity, NetworkMessageUtils.friendlyMessage(error, "Registration failed"), Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("hasOpenedTerms", hasOpenedTerms)
        super.onSaveInstanceState(outState)
    }
}
