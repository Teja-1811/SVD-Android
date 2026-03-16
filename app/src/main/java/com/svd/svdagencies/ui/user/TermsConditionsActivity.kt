package com.svd.svdagencies.ui.user

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.svd.svdagencies.R

class TermsConditionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_terms_conditions)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        setupAccordion(R.id.sectionOperator, R.id.operatorContent)
        setupAccordion(R.id.sectionOrders, R.id.ordersContent)
        setupAccordion(R.id.sectionContact, R.id.contactContent)
    }

    private fun setupAccordion(headerId: Int, contentId: Int) {
        val header = findViewById<TextView>(headerId)
        val content = findViewById<View>(contentId)
        
        header.setOnClickListener {
            if (content.visibility == View.VISIBLE) {
                content.visibility = View.GONE
            } else {
                content.visibility = View.VISIBLE
            }
        }
    }
}
