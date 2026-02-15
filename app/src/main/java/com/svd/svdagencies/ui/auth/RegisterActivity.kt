package com.svd.svdagencies.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etNumber = findViewById<TextInputEditText>(R.id.etNumber)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etCPassword = findViewById<TextInputEditText>(R.id.etCPassword)
        val etAddress = findViewById<TextInputEditText>(R.id.etAddress)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val number = etNumber.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val cPassword = etCPassword.text.toString().trim()
            val address = etAddress.text.toString().trim()

            if (name.isEmpty() || number.isEmpty() || email.isEmpty() || password.isEmpty() || cPassword.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != cPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Implement registration logic here
            Toast.makeText(this, "Registration logic to be implemented", Toast.LENGTH_SHORT).show()
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}