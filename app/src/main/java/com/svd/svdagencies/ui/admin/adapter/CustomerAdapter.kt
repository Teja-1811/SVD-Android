package com.svd.svdagencies.ui.admin.adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.customerData.CustomerItem
import com.svd.svdagencies.databinding.AdminCustomerCardBinding
import com.svd.svdagencies.ui.admin.customer.CustomerProfileActivity
import java.net.URLEncoder

class CustomerAdapter(
    private var items: List<CustomerItem>,
    private val onFreezeClick: ((CustomerItem) -> Unit)? = null,
    private val onBalanceClick: ((CustomerItem) -> Unit)? = null
) : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: AdminCustomerCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            AdminCustomerCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val c = items[position]

        holder.binding.apply {
            checkAllowCredit.tag = c.id
            checkAllowCredit.isChecked = c.allow_credit
            checkAllowCredit.isEnabled = true
            checkAllowCredit.visibility = if (c.user_type == "delivery") android.view.View.GONE else android.view.View.VISIBLE
            checkAllowCredit.setOnClickListener {
                val id = c.id ?: return@setOnClickListener
                val requested = checkAllowCredit.isChecked
                checkAllowCredit.isEnabled = false
                com.svd.svdagencies.data.api.auth.ApiClient.adminCustomerDashboard.updateCredit(id, mapOf("allow_credit" to requested))
                    .enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
                        override fun onResponse(call: retrofit2.Call<com.google.gson.JsonObject>, response: retrofit2.Response<com.google.gson.JsonObject>) {
                            val body = response.body()
                            val success = response.isSuccessful && body?.get("success")?.asBoolean == true
                            if (success) c.allow_credit = body?.get("allow_credit")?.asBoolean ?: requested
                            if (checkAllowCredit.tag == id) {
                                checkAllowCredit.isChecked = c.allow_credit
                                checkAllowCredit.isEnabled = true
                            }
                            if (!success) Toast.makeText(root.context, "Unable to save credit setting.", Toast.LENGTH_SHORT).show()
                        }
                        override fun onFailure(call: retrofit2.Call<com.google.gson.JsonObject>, error: Throwable) {
                            if (checkAllowCredit.tag == id) {
                                checkAllowCredit.isChecked = c.allow_credit
                                checkAllowCredit.isEnabled = true
                            }
                            Toast.makeText(root.context, "Unable to save credit setting.", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            txtCustomerName.text = "${c.serial_no}. ${c.name}"
            txtShopName.text = c.shop_name
            txtRoute.text = "Route: ${c.route_name?.takeIf { it.isNotBlank() } ?: "-"}"
            txtPhone.text = c.phone
            txtBalance.text = "₹ %.2f".format(c.due)

            // Dynamic Balance Coloring
            val due = c.due ?: 0.0
            if (due >= 0) {
                txtBalance.setTextColor(ContextCompat.getColor(root.context, R.color.brand_red))
            } else {
                txtBalance.setTextColor(ContextCompat.getColor(root.context, R.color.icon_green))
            }

            // Freeze button visual state
            if (c.frozen == true) {
                txtStatus.text = "Frozen"
                txtStatus.setTextColor(ContextCompat.getColor(root.context, R.color.icon_red))
                // Frozen State: Unfreeze (Green)
                btnPassword.setIconResource(R.drawable.ic_unlock)
                btnPassword.text = "Unfreeze"
                btnPassword.contentDescription = "Unfreeze customer"
                btnPassword.iconTint = ColorStateList.valueOf(
                    ContextCompat.getColor(root.context, R.color.icon_green)
                )
            } else {
                txtStatus.text = "Active"
                txtStatus.setTextColor(ContextCompat.getColor(root.context, R.color.icon_green))
                // Active State: Freeze (Red/Gray)
                btnPassword.setIconResource(R.drawable.ic_lock)
                btnPassword.text = "Freeze"
                btnPassword.contentDescription = "Freeze customer"
                btnPassword.iconTint = ColorStateList.valueOf(
                    ContextCompat.getColor(root.context, R.color.icon_red)
                )
            }

            btnPassword.setOnClickListener {
                onFreezeClick?.invoke(c)
            }

            btnPay.setOnClickListener {
                onBalanceClick?.invoke(c)
            }

            // Click on Eye Button to view profile
            btnView.setOnClickListener {
                navigateToProfile(root.context, c)
            }

            // Also make the whole card clickable for profile view
            root.setOnClickListener {
                navigateToProfile(root.context, c)
            }

            btnWhatsapp.visibility = if (c.user_type == "delivery") android.view.View.GONE else android.view.View.VISIBLE
            btnWhatsapp.setOnClickListener {
                val activity = root.context as? androidx.appcompat.app.AppCompatActivity ?: return@setOnClickListener
                c.id?.let { id ->
                    com.svd.svdagencies.utils.CustomerDeletionAction.confirm(activity, id, c.name ?: "Customer") {
                        items = items.filterNot { it.id == id }
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun navigateToProfile(context: android.content.Context, customer: CustomerItem) {
        val intent = Intent(context, CustomerProfileActivity::class.java).apply {
            putExtra("CUSTOMER_DATA", customer)
        }
        context.startActivity(intent)
    }

    fun update(list: List<CustomerItem>) {
        items = list
        notifyDataSetChanged()
    }
}
