package com.svd.svdagencies.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.auth.LoginRequest
import com.svd.svdagencies.data.api.auth.LoginResponse
import com.svd.svdagencies.notifications.PushRegistrationManager
import com.svd.svdagencies.ui.admin.AdminDashboardActivity
import com.svd.svdagencies.ui.customer.CustomerMainActivity
import com.svd.svdagencies.ui.delivery.DeliveryCreateBillActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import com.svd.svdagencies.utils.showLoading
import com.svd.svdagencies.base.BaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val session = SessionManager(this)

        session.getRole()?.let { role ->
            PushRegistrationManager.requestPermissionIfNeeded(this)
            PushRegistrationManager.registerCurrentDevice(this)
            navigateToDashboard(role)
            finish()
            return
        }

        val logo = findViewById<ImageView>(R.id.login_logo)
        val userLayout = findViewById<TextInputLayout>(R.id.username_layout)
        val passLayout = findViewById<TextInputLayout>(R.id.password_layout)
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        logo.startAnimation(fadeIn)
        userLayout.startAnimation(slideUp)
        passLayout.startAnimation(slideUp)
        btnLogin.startAnimation(slideUp)

        val api = ApiClient.authApi

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.showLoading(true, "Signing In...")

            val request = LoginRequest(phone = username, password = password)
            Log.d("Login", "Sending request: phone=$username")

            api.login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    btnLogin.showLoading(false)

                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Login", "Error response body: $errorBody")

                        val errorMessage = try {
                            val jsonObject = Gson().fromJson(errorBody, JsonObject::class.java)
                            jsonObject.get("message")?.asString
                                ?: jsonObject.get("detail")?.asString
                                ?: "Error: ${response.code()}"
                        } catch (e: Exception) {
                            "Login Failed: ${response.code()}"
                        }

                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                        return
                    }

                    val body = response.body()
                    if (body != null && body.status == "success" && body.token != null) {
                        session.saveSession(body.token, body.role ?: "", body.userId ?: -1)
                        PushRegistrationManager.requestPermissionIfNeeded(this@LoginActivity)
                        PushRegistrationManager.registerCurrentDevice(this@LoginActivity)
                        navigateToDashboard(body.role ?: "")
                        finish()
                    } else {
                        val msg = body?.message ?: "Login failed"
                        Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnLogin.showLoading(false)
                    Log.e("Login", "Network error", t)
                    Toast.makeText(
                        this@LoginActivity,
                        NetworkMessageUtils.friendlyMessage(t, "Login failed"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun navigateToDashboard(role: String) {
        val intent = when (role) {
            UserRole.ADMIN -> Intent(this, AdminDashboardActivity::class.java)
            UserRole.CUSTOMER -> Intent(this, CustomerMainActivity::class.java)
            UserRole.DELIVERY -> Intent(this, DeliveryCreateBillActivity::class.java)
            else -> {
                Toast.makeText(this, "Invalid role received", Toast.LENGTH_SHORT).show()
                null
            }
        }
        intent?.let { startActivity(it) }
    }
}
