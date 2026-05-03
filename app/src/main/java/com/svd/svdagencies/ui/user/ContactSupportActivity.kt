package com.svd.svdagencies.ui.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.customer.CustomerContactResponse
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ContactSupportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PREFILL_SUBJECT = "prefill_subject"
        const val EXTRA_PREFILL_MESSAGE = "prefill_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user)

        val drawerLayout = findViewById<DrawerLayout>(R.id.userDrawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.userNavigationView)
        findViewById<ImageButton>(R.id.btnUserMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navigationView.getHeaderView(0).findViewById<View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navigationView.setCheckedItem(R.id.nav_support)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terms -> {
                    startActivity(Intent(this, TermsConditionsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_company -> {
                    startActivity(Intent(this, CompanyDetailsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_support -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_queries -> {
                    startActivity(Intent(this, RaisedQueriesActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }

        val session = SessionManager(this)
        findViewById<ImageButton>(R.id.btnUserLogout).setOnClickListener {
            session.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<TextView>(R.id.tvUserToolbarTitle).text = "Contact Support"

        findViewById<SwipeRefreshLayout>(R.id.userSwipeRefresh).isEnabled = false
        findViewById<BottomNavigationView>(R.id.userBottomNav).visibility = View.GONE
        val progressBar = findViewById<ProgressBar>(R.id.userProgress)
        progressBar.visibility = View.GONE
        findViewById<TextView>(R.id.tvUserStatusMessage).visibility = View.GONE

        val container = findViewById<FrameLayout>(R.id.userFragmentContainer)
        layoutInflater.inflate(R.layout.activity_contact_support, container, true)

        val btnCall = findViewById<View>(R.id.btnCall)
        val btnWhatsapp = findViewById<View>(R.id.btnWhatsapp)
        val btnEmail = findViewById<View>(R.id.btnEmail)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmit)

        val inputSubject = findViewById<TextInputEditText>(R.id.inputSubject)
        val inputMessage = findViewById<TextInputEditText>(R.id.inputMessage)
        val inputPhone = findViewById<TextInputEditText>(R.id.inputPhone)
        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmail)

        inputSubject.setText(intent.getStringExtra(EXTRA_PREFILL_SUBJECT).orEmpty())
        inputMessage.setText(intent.getStringExtra(EXTRA_PREFILL_MESSAGE).orEmpty())

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

            if (subject.isEmpty() || message.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill subject, message, and phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSubmit.isEnabled = false

            val body = mapOf(
                "name" to "User_${session.getUserId()}",
                "phone" to phone,
                "email" to email,
                "subject" to subject,
                "message" to message
            )

            ApiClient.customerApi.submitContact(body).enqueue(object : Callback<CustomerContactResponse> {
                override fun onResponse(call: Call<CustomerContactResponse>, response: Response<CustomerContactResponse>) {
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true

                    val payload = response.body()
                    if (response.isSuccessful && payload?.success == true) {
                        Toast.makeText(
                            this@ContactSupportActivity,
                            payload.message ?: "Support request sent successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        inputSubject.text?.clear()
                        inputMessage.text?.clear()
                        inputPhone.text?.clear()
                        inputEmail.text?.clear()
                    } else {
                        Toast.makeText(
                            this@ContactSupportActivity,
                            payload?.message ?: "Failed to send: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<CustomerContactResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    Toast.makeText(this@ContactSupportActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
