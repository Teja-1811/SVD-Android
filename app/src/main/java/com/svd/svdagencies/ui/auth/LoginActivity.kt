package com.svd.svdagencies.ui.auth

import android.content.Intent
import android.os.Bundle
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
        setContentView(R.layout.activity_login)

        // ================= AUTO LOGIN =================
        val session = SessionManager(this)

        session.getRole()?.let { role ->
            when (role) {
                UserRole.ADMIN ->
                    startActivity(Intent(this, AdminDashboardActivity::class.java))

                UserRole.CUSTOMER ->
                    startActivity(Intent(this, CustomerMainActivity::class.java))

                UserRole.DELIVERY ->
                    startActivity(Intent(this, DeliveryDashboardActivity::class.java))
            }
            finish()
            return
        }
        // ==============================================

        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        val api = ApiClient.retrofit.create(AuthApi::class.java)

        btnLogin.setOnClickListener {

            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validation
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Enter username and password",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val request = LoginRequest(
                phone = username,
                password = password
            )

            api.login(request).enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    if (!response.isSuccessful) {
                        // Check if it's a 400 Bad Request which usually contains the error message
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            this@LoginActivity,
                            "Login Failed: ${response.code()} ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val body = response.body()

                    if (body?.status == "success" &&
                        body.token != null &&
                        body.role != null &&
                        body.user_id != null
                    ) {

                        // Save session
                        session.saveSession(body.token, body.role, body.user_id)

                        when (body.role) {
                            UserRole.ADMIN ->
                                startActivity(
                                    Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                                )

                            UserRole.CUSTOMER ->
                                startActivity(
                                    Intent(this@LoginActivity, CustomerMainActivity::class.java)
                                )

                            UserRole.DELIVERY ->
                                startActivity(
                                    Intent(this@LoginActivity, DeliveryDashboardActivity::class.java)
                                )
                        }
                        finish()

                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            body?.message ?: "Login failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}
