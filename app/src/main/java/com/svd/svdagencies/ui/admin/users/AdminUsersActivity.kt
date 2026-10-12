package com.svd.svdagencies.ui.admin.users

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.svd.svdagencies.data.api.admin.AdminUser
import com.svd.svdagencies.data.api.admin.AdminUsersResponse
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminUsersActivity : AdminBaseActivity() {
    private lateinit var usersList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        root.addView(Button(this).apply { text = "Add user"; setOnClickListener { showUserDialog() } })
        usersList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(usersList) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadUsers()
    }

    override fun onResume() { super.onResume(); loadUsers() }

    private fun loadUsers() {
        ApiClient.adminUsersApi.listUsers().enqueue(object : Callback<AdminUsersResponse> {
            override fun onResponse(call: Call<AdminUsersResponse>, response: Response<AdminUsersResponse>) {
                if (!response.isSuccessful) return showMessage("Unable to load users")
                usersList.removeAllViews(); response.body()?.users.orEmpty().forEach(::addUserRow)
            }
            override fun onFailure(call: Call<AdminUsersResponse>, error: Throwable) = showMessage("Unable to load users")
        })
    }

    private fun addUserRow(user: AdminUser) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 18, 0, 18) }
        row.addView(TextView(this).apply { text = "${user.name}\n${user.phone} • Due ₹${"%.2f".format(user.due)}" }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply { text = "Edit"; setOnClickListener { showUserDialog(user) } })
        row.addView(Button(this).apply { text = if (user.frozen) "Unfreeze" else "Freeze"; setOnClickListener { toggleFreeze(user) } })
        usersList.addView(row, ViewGroup.LayoutParams(-1, -2))
    }

    private fun showUserDialog(existing: AdminUser? = null) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 16, 48, 0) }
        val name = EditText(this).apply { hint = "Name"; setText(existing?.name.orEmpty()) }
        val phone = EditText(this).apply { hint = "Phone"; setText(existing?.phone.orEmpty()) }
        val shop = EditText(this).apply { hint = "Shop name (optional)"; setText(existing?.shop_name.orEmpty()) }
        form.addView(name); form.addView(phone); form.addView(shop)
        AlertDialog.Builder(this).setTitle(if (existing == null) "Add user" else "Edit user").setView(form)
            .setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                val body = mutableMapOf("name" to name.text.toString(), "phone" to phone.text.toString(), "shop_name" to shop.text.toString())
                existing?.let { body["user_id"] = it.id.toString() }
                ApiClient.adminUsersApi.saveUser(body).enqueue(object : Callback<Map<String, Any>> {
                    override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) { if (response.isSuccessful) loadUsers() else showMessage("Unable to save user") }
                    override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) = showMessage("Unable to save user")
                })
            }.show()
    }

    private fun toggleFreeze(user: AdminUser) {
        ApiClient.adminUsersApi.toggleFreeze(user.id).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) { if (response.isSuccessful) loadUsers() else showMessage("Unable to update user") }
            override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) = showMessage("Unable to update user")
        })
    }

    private fun showMessage(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
