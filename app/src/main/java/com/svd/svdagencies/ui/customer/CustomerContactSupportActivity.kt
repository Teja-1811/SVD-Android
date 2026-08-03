package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.customer.CustomerContactResponse
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.ui.customer.CustomerStatementActivity
import com.svd.svdagencies.ui.customer.TermsConditionsActivity
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.showLoading
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerContactSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer)

        val drawerLayout = findViewById<DrawerLayout>(R.id.customerDrawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.customerNavigationView)
        findViewById<ImageButton>(R.id.btnCustomerMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navigationView.getHeaderView(0).findViewById<View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navigationView.setCheckedItem(R.id.nav_support)
        val session = SessionManager(this)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, CustomerMainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_terms -> {
                    openDrawerDestination(drawerLayout, TermsConditionsActivity::class.java)
                    true
                }
                R.id.nav_company -> {
                    openDrawerDestination(drawerLayout, CustomerCompanyDetailsActivity::class.java)
                    true
                }
                R.id.nav_support -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_queries -> {
                    openDrawerDestination(drawerLayout, CustomerRaisedQueriesActivity::class.java)
                    true
                }
                R.id.nav_statement -> {
                    openDrawerDestination(drawerLayout, CustomerStatementActivity::class.java)
                    true
                }
                R.id.nav_logout -> {
                    session.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        findViewById<ImageButton>(R.id.btnCustomerLogout).setOnClickListener {
            session.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<TextView>(R.id.tvCustomerToolbarTitle).text = "Contact Support"

        findViewById<BottomNavigationView>(R.id.customerBottomNav).visibility = View.GONE

        val container = findViewById<FrameLayout>(R.id.customerFragmentContainer)
        layoutInflater.inflate(R.layout.activity_contact_support, container, true)

        val btnCall = findViewById<View>(R.id.btnCall)
        val btnWhatsapp = findViewById<View>(R.id.btnWhatsapp)
        val btnEmail = findViewById<View>(R.id.btnEmail)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmit)

        val inputSubject = findViewById<TextInputEditText>(R.id.inputSubject)
        val inputMessage = findViewById<TextInputEditText>(R.id.inputMessage)
        val inputPhone = findViewById<TextInputEditText>(R.id.inputPhone)
        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmail)

        btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:+919392890375")
            startActivity(intent)
        }

        btnWhatsapp.setOnClickListener {
            val url = "https://api.whatsapp.com/send?phone=919392890375"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        btnEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:svdagencies12@gmail.com")
            startActivity(intent)
        }

        btnSubmit.setOnClickListener {
            val subject = inputSubject.text.toString().trim()
            val message = inputMessage.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val email = inputEmail.text.toString().trim()

            if (subject.isBlank() || message.isBlank() || phone.isBlank()) {
                Toast.makeText(this, "Please fill subject, message, and phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.showLoading(true, "Submitting...")

            val body = mapOf(
                "name" to "Customer_${session.getUserId()}",
                "phone" to phone,
                "email" to email,
                "subject" to subject,
                "message" to message
            )

            ApiClient.customerApi.submitContact(body).enqueue(object : Callback<CustomerContactResponse> {
                override fun onResponse(
                    call: Call<CustomerContactResponse>,
                    response: Response<CustomerContactResponse>
                ) {
                    btnSubmit.showLoading(false)

                    val payload = response.body()
                    if (response.isSuccessful && payload?.success == true) {
                        Toast.makeText(
                            this@CustomerContactSupportActivity,
                            payload.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        inputSubject.text?.clear()
                        inputMessage.text?.clear()
                        inputPhone.text?.clear()
                        inputEmail.text?.clear()
                        startActivity(Intent(this@CustomerContactSupportActivity, CustomerRaisedQueriesActivity::class.java))
                    } else {
                        Toast.makeText(
                            this@CustomerContactSupportActivity,
                            payload?.message ?: "Failed to submit support request",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<CustomerContactResponse>, t: Throwable) {
                    btnSubmit.showLoading(false)
                    Toast.makeText(
                        this@CustomerContactSupportActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    private fun openDrawerDestination(
        drawerLayout: DrawerLayout,
        activityClass: Class<out AppCompatActivity>
    ) {
        drawerLayout.closeDrawer(GravityCompat.START)
        drawerLayout.post {
            startActivity(Intent(this, activityClass))
        }
    }
}
