package com.svd.svdagencies.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.BuildConfig
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.AdminDashboardActivity
import com.svd.svdagencies.ui.customer.CustomerMainActivity
import com.svd.svdagencies.ui.delivery.DeliveryDashboardActivity
import com.svd.svdagencies.ui.user.UserMainActivity
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)
        session.getRole()?.let { role ->
            val intent = when (role) {
                UserRole.ADMIN -> Intent(this, AdminDashboardActivity::class.java)
                UserRole.CUSTOMER -> Intent(this, CustomerMainActivity::class.java)
                UserRole.USER -> Intent(this, UserMainActivity::class.java)
                UserRole.DELIVERY -> Intent(this, DeliveryDashboardActivity::class.java)
                else -> null
            }
            intent?.let {
                startActivity(it)
                finish()
                return
            }
        }

        setContentView(R.layout.activity_welcome)
        findViewById<android.widget.TextView>(R.id.tvVersion).text = "Version ${BuildConfig.VERSION_NAME}"

        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener {
            showAuthChoiceDialog()
        }
    }

    private fun showAuthChoiceDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_auth_choice, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
        }

        dialogView.findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        dialog.show()
    }
}
