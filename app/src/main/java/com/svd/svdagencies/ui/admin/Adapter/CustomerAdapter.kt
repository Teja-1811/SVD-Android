package com.svd.svdagencies.ui.admin.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.data.model.admin.CustomerItem
import com.svd.svdagencies.databinding.ItemCustomerBinding
import com.svd.svdagencies.ui.admin.customer.CustomerProfileActivity

class CustomerAdapter(
    private var items: List<CustomerItem>
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

            btnView.setOnClickListener {
                val context = root.context
                val intent = Intent(context, CustomerProfileActivity::class.java).apply {
                    putExtra("CUSTOMER_DATA", c)
                }
                context.startActivity(intent)
            }
        }
    }

    fun update(list: List<CustomerItem>) {
        items = list
        notifyDataSetChanged()
    }
}
