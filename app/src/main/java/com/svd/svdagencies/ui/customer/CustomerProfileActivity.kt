package com.svd.svdagencies.ui.customer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.base.BaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerProfileActivity : BaseActivity() {
    private val fields = linkedMapOf<String, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        listOf("name" to "Name", "phone" to "Phone", "shop_name" to "Shop name", "area" to "Area", "city" to "City", "state" to "State", "pin_code" to "PIN code").forEach { (key, hint) ->
            fields[key] = EditText(this).apply { this.hint = hint }; root.addView(fields[key])
        }
        root.addView(Button(this).apply { text = "Save profile"; setOnClickListener { save() } })
        setContentView(root); load()
    }

    @Suppress("UNCHECKED_CAST")
    private fun load() = ApiClient.customerApi.getProfile().enqueue(object : Callback<Map<String, Any>> {
        override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
            val profile = response.body()?.get("profile") as? Map<String, Any> ?: return failure()
            fields.forEach { (key, input) -> input.setText(profile[key]?.toString().orEmpty()) }
        }
        override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) = failure()
    })

    private fun save() {
        val body = fields.mapValues { it.value.text.toString().trim() }
        ApiClient.customerApi.updateProfile(body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) { if (response.isSuccessful) Toast.makeText(this@CustomerProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show() else failure() }
            override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) = failure()
        })
    }
    private fun failure() = Toast.makeText(this, "Unable to update profile", Toast.LENGTH_SHORT).show()
}
