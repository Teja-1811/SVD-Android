package com.svd.svdagencies.ui.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager

class CustomerCompanyDetailsActivity : AppCompatActivity() {

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

        findViewById<TextView>(R.id.tvCustomerToolbarTitle).text = "Company Details"

        findViewById<BottomNavigationView>(R.id.customerBottomNav).visibility = View.GONE

        val container = findViewById<FrameLayout>(R.id.customerFragmentContainer)
        layoutInflater.inflate(R.layout.user_company_details, container, true)

        findViewById<MaterialButton>(R.id.btnOpenMap).setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:0,0?q=Sri+Vijaya+Durga+Milk+Agencies+Gundugolanu")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }
    }
}
