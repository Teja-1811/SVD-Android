package com.svd.svdagencies.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.WelcomeProduct
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.view.LayoutInflater
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder as MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.BuildConfig
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.AdminDashboardActivity
import com.svd.svdagencies.ui.customer.CustomerMainActivity
import com.svd.svdagencies.ui.customer.UserPortalActivity
import com.svd.svdagencies.ui.delivery.DeliveryBillToCustomerActivity
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
                UserRole.USER -> Intent(this, UserPortalActivity::class.java)
                UserRole.DELIVERY -> Intent(this, DeliveryBillToCustomerActivity::class.java)
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

        val productsView = findViewById<RecyclerView>(R.id.welcomeProducts)
        productsView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        loadWelcomeProducts()

        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener {
            showAuthChoiceDialog()
        }
    }

    private fun loadWelcomeProducts() {
        val section = findViewById<View>(R.id.welcomeProductsSection)
        val status = findViewById<TextView>(R.id.welcomeProductsStatus)
        val productsView = findViewById<RecyclerView>(R.id.welcomeProducts)
        status.setText(R.string.welcome_products_loading)
        status.isClickable = false
        status.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val products = ApiClient.productApi.getWelcomeProducts()
                if (products.isEmpty()) {
                    section.visibility = View.GONE
                } else {
                    productsView.adapter = WelcomeProductsAdapter(products)
                    productsView.visibility = View.VISIBLE
                    status.visibility = View.GONE
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                status.setText(R.string.welcome_products_retry)
                status.setOnClickListener { loadWelcomeProducts() }
            }
        }
    }

    private class WelcomeProductsAdapter(private val products: List<WelcomeProduct>) :
        RecyclerView.Adapter<WelcomeProductsAdapter.Holder>() {
        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.productImage)
            val name: TextView = view.findViewById(R.id.productName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_welcome_product, parent, false)
        )

        override fun getItemCount() = products.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val product = products[position]
            holder.name.text = product.name
            Glide.with(holder.itemView)
                .load(ApiClient.getImageUrl(product.image))
                .placeholder(R.drawable.ic_milk_placeholder)
                .error(R.drawable.ic_milk_placeholder)
                .fitCenter()
                .into(holder.image)
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

        dialogView.findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, RegistrationActivity::class.java))
        }

        dialog.show()
    }
}
