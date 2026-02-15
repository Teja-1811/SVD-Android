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

        val logo = findViewById<ImageView>(R.id.login_logo)
        val userLabel = findViewById<TextView>(R.id.username_label)
        val userLayout = findViewById<TextInputLayout>(R.id.username_layout)
        val passLabel = findViewById<TextView>(R.id.password_label)
        val passLayout = findViewById<TextInputLayout>(R.id.password_layout)
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // Animation
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        logo.startAnimation(fadeIn)
        userLabel.startAnimation(slideUp)
        userLayout.startAnimation(slideUp)
        passLabel.startAnimation(slideUp)
        passLayout.startAnimation(slideUp)
        btnLogin.startAnimation(slideUp)

        // Using the dedicated authApi which bypasses the AuthInterceptor
        val api = ApiClient.authApi

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Signing In..."

            val request = LoginRequest(phone = username, password = password)
            Log.d("Login", "Sending request: phone=$username")

            api.login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Sign In"
                    
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
                        session.saveSession(body.token, body.role ?: "", body.user_id ?: -1)

                        when (body.role) {
                            UserRole.ADMIN -> startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
                            UserRole.CUSTOMER -> startActivity(Intent(this@LoginActivity, CustomerMainActivity::class.java))
                            UserRole.DELIVERY -> startActivity(Intent(this@LoginActivity, DeliveryDashboardActivity::class.java))
                            else -> Toast.makeText(this@LoginActivity, "Invalid role received", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    } else {
                        val msg = body?.message ?: "Login failed"
                        Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Sign In"
                    Log.e("Login", "Network error", t)
                    Toast.makeText(this@LoginActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}