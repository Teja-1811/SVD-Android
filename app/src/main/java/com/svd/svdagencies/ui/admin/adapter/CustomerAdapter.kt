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
            txtCustomerName.text = "${c.serial_no}. ${c.name}"
            txtShopName.text = c.shop_name
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
                // Frozen State: Unfreeze (Green)
                btnPassword.setImageResource(R.drawable.ic_unlock)
                btnPassword.setBackgroundResource(R.drawable.bg_icon_btn_green)
                btnPassword.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(root.context, R.color.icon_green)
                )
            } else {
                // Active State: Freeze (Red/Gray)
                btnPassword.setImageResource(R.drawable.ic_lock)
                btnPassword.setBackgroundResource(R.drawable.bg_icon_btn_red)
                btnPassword.imageTintList = ColorStateList.valueOf(
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

            btnWhatsapp.setOnClickListener {
                val context = root.context
                val phoneNumber = c.phone
                
                val absDue = Math.abs(due)
                val formattedDue = "₹ %.2f".format(absDue)
                
                val statusMessage = if (due >= 0) {
                    "Your pending balance is $formattedDue."
                } else {
                    "You have a wallet balance of $formattedDue."
                }

                // Removed shop name from the message as requested
                val message = "Hello ${c.name},\n$statusMessage\n\nVisit us: https://svd-dqw3.onrender.com/\nThank you for your business!"

                if (!phoneNumber.isNullOrEmpty()) {
                    try {
                        val url = "https://api.whatsapp.com/send?phone=+91$phoneNumber&text=${URLEncoder.encode(message, "UTF-8")}"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(url)
                            setPackage("com.whatsapp")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val url = "https://api.whatsapp.com/send?phone=+91$phoneNumber&text=${URLEncoder.encode(message, "UTF-8")}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
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
