package com.svd.svdagencies.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.AuthApi
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.auth.LoginRequest
import com.svd.svdagencies.data.api.auth.LoginResponse
import com.svd.svdagencies.ui.admin.AdminDashboardActivity
import com.svd.svdagencies.ui.customer.CustomerMainActivity
import com.svd.svdagencies.ui.delivery.DeliveryDashboardActivity
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        val session = SessionManager(this)

        session.getRole()?.let { role ->
            when (role) {
                UserRole.ADMIN -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                UserRole.CUSTOMER -> startActivity(Intent(this, CustomerMainActivity::class.java))
                UserRole.DELIVERY -> startActivity(Intent(this, DeliveryDashboardActivity::class.java))
            }
            finish()
            return
        }

        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        val api = ApiClient.retrofit.create(AuthApi::class.java)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Using 'phone' field as expected by LoginRequest
            val request = LoginRequest(phone = username, password = password)
            Log.d("Login", "Sending request: phone=$username")

            api.login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Login", "Error response: $errorBody")
                        Toast.makeText(this@LoginActivity, "Login Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val body = response.body()
                    Log.d("Login", "Response: $body")

                    if (body != null && body.status == "success" && body.token != null) {
                        session.saveSession(body.token, body.role ?: "", body.user_id ?: -1)

                        when (body.role) {
                            UserRole.ADMIN -> startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
                            UserRole.CUSTOMER -> startActivity(Intent(this@LoginActivity, CustomerMainActivity::class.java))
                            UserRole.DELIVERY -> startActivity(Intent(this@LoginActivity, DeliveryDashboardActivity::class.java))
                            else -> Toast.makeText(this@LoginActivity, "Invalid role received", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, body?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("Login", "Network error", t)
                    Toast.makeText(this@LoginActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
