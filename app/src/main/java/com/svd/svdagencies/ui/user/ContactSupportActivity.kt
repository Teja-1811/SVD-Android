package com.svd.svdagencies.ui.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R

class ContactSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_contact_support)

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
