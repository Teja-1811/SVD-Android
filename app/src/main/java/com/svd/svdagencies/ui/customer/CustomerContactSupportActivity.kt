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
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager

class CustomerContactSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer)

        val drawerLayout = findViewById<DrawerLayout>(R.id.customerDrawerLayout)
        findViewById<ImageButton>(R.id.btnCustomerMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val session = SessionManager(this)
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
        layoutInflater.inflate(R.layout.user_contact_support, container, true)

        val btnCall = findViewById<MaterialButton>(R.id.btnCall)
        val btnWhatsapp = findViewById<MaterialButton>(R.id.btnWhatsapp)
        val btnEmail = findViewById<MaterialButton>(R.id.btnEmail)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmit)

        val inputSubject = findViewById<TextInputEditText>(R.id.inputSubject)
        val inputMessage = findViewById<TextInputEditText>(R.id.inputMessage)
        val inputContact = findViewById<TextInputEditText>(R.id.inputContact)

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
            val subject = inputSubject.text.toString()
            val message = inputMessage.text.toString()
            if (subject.isBlank() || message.isBlank()) {
                Toast.makeText(this, "Please fill in subject and message", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Support request sent successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
