package com.svd.svdagencies.ui.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R

class CompanyDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_company_details)

        findViewById<MaterialButton>(R.id.btnOpenMap).setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:0,0?q=Sri+Vijaya+Durga+Milk+Agencies+Gundugolanu")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }
    }
}
