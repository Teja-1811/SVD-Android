package com.svd.svdagencies.ui.admin.Adapter

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.admin.CustomerItem
import com.svd.svdagencies.databinding.ItemCustomerBinding
import com.svd.svdagencies.ui.admin.customer.CustomerProfileActivity
import java.net.URLEncoder

class CustomerAdapter(
    private var items: List<CustomerItem>,
    private val onFreezeClick: ((CustomerItem) -> Unit)? = null,
    private val onBalanceClick: ((CustomerItem) -> Unit)? = null
) : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCustomerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemCustomerBinding.inflate(
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
                // Use red background for consistency with profile logic, or keep gray
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

            btnView.setOnClickListener {
                val context = root.context
                val intent = Intent(context, CustomerProfileActivity::class.java).apply {
                    putExtra("CUSTOMER_DATA", c)
                }
                context.startActivity(intent)
            }

            btnWhatsapp.setOnClickListener {
                val context = root.context
                val phoneNumber = c.phone
                val dueAmount = "₹ %.2f".format(c.due)
                val message = "Hello ${c.name},\nYour pending balance at ${c.shop_name} is $dueAmount.\nPlease pay at your earliest convenience."

                if (!phoneNumber.isNullOrEmpty()) {
                    try {
                        val url = "https://api.whatsapp.com/send?phone=+91$phoneNumber&text=${URLEncoder.encode(message, "UTF-8")}"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(url)
                            setPackage("com.whatsapp")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback if WhatsApp is not installed or other error
                        try {
                            // Try browser fallback
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

    fun update(list: List<CustomerItem>) {
        items = list
        notifyDataSetChanged()
    }
}
