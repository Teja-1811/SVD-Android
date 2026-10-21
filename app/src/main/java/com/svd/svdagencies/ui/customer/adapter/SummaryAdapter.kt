package com.svd.svdagencies.ui.customer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.customer.ProductResponse

class SummaryAdapter(
    private val cartQuantities: HashMap<Int, Double>,
    private val allProducts: MutableMap<Int, ProductResponse>
) : RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>() {

    // RecyclerView must render a stable snapshot, not the cart changed by another tab.
    private var items = cartQuantities.mapNotNull { (id, quantity) ->
        allProducts[id]?.let { it to quantity }
    }

    fun refresh() {
        items = cartQuantities.mapNotNull { (id, quantity) -> allProducts[id]?.let { it to quantity } }
        notifyDataSetChanged()
    }

    inner class SummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvProductTotal: TextView = view.findViewById(R.id.tvProductTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.customer_order_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {

        val (product, quantity) = items[position]

        // 🏷 Show name + (crates, pcs)
        val qtyDescription = product.getCrateAndPcsDescription(quantity)
        holder.tvProductName.text = "${product.name} - ($qtyDescription)"

        // 💰 Total price = price × total pieces
        val total = product.calculateTotal(quantity)

        holder.tvProductTotal.text =
            "₹${String.format("%.2f", total)}"
    }

    override fun getItemCount(): Int = items.size
}
