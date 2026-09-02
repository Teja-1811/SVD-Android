package com.svd.svdagencies.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.BuildConfig
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.AdminDashboardActivity
import com.svd.svdagencies.ui.customer.CustomerMainActivity
import com.svd.svdagencies.ui.delivery.DeliveryCreateBillActivity
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole
import com.svd.svdagencies.base.BaseActivity

class WelcomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)
        session.getRole()?.let { role ->
            val intent = when (role) {
                UserRole.ADMIN -> Intent(this, AdminDashboardActivity::class.java)
                UserRole.CUSTOMER -> Intent(this, CustomerMainActivity::class.java)
                UserRole.DELIVERY -> Intent(this, DeliveryCreateBillActivity::class.java)
                else -> null
            }
            intent?.let {
                startActivity(it)
                finish()
                return
            }
        }

        setContentView(R.layout.activity_welcome)
        findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieWelcome)?.setFailureListener { e ->
            android.util.Log.e("Lottie", "Failed to load welcome animation", e)
        }
        findViewById<android.widget.TextView>(R.id.tvVersion).text = "Version ${BuildConfig.VERSION_NAME}"

        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener {
            showAuthChoiceDialog()
        }
    }

    private fun showAuthChoiceDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_auth_choice, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
        }

        dialog.show()
    }
}
