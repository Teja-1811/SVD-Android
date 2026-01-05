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

    inner class SummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvProductTotal: TextView = view.findViewById(R.id.tvProductTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary_product, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {

        val productId = cartQuantities.keys.toList()[position]
        val quantity = cartQuantities[productId] ?: 0.0
        val product = allProducts[productId] ?: return

        val pcs = product.pcs_count

        // 🔥 Total pieces (crate qty × pcs count)
        val totalPieces = quantity * pcs

        // 👍 Format qty (hide .0)
        val qtyString =
            if (quantity == quantity.toInt().toDouble())
                quantity.toInt().toString()
            else
                quantity.toString()

        // 🏷 Show name + qty + total pieces
        holder.tvProductName.text =
            "${product.name} - (${totalPieces.toInt()} pcs)"

        // 💰 Total price = price × total pieces
        val total = totalPieces * product.selling_price

        holder.tvProductTotal.text =
            "₹${String.format("%.2f", total)}"
    }

    override fun getItemCount(): Int = cartQuantities.size
}
