package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserOffer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UserOfferAdapter(
    private val onUseOffer: (UserOffer) -> Unit
) : RecyclerView.Adapter<UserOfferAdapter.OfferViewHolder>() {

    private val offers = mutableListOf<UserOffer>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_offer_card, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        holder.bind(offers[position])
    }

    override fun getItemCount(): Int = offers.size

    fun submitList(list: List<UserOffer>) {
        offers.clear()
        offers.addAll(list)
        notifyDataSetChanged()
    }

    inner class OfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvOfferTitle)
        private val code = view.findViewById<TextView>(R.id.tvOfferCode)
        private val description = view.findViewById<TextView>(R.id.tvOfferDescription)
        private val summary = view.findViewById<TextView>(R.id.tvOfferSummary)
        private val expiry = view.findViewById<TextView>(R.id.tvOfferExpiry)
        private val btnUseOffer = view.findViewById<MaterialButton>(R.id.btnUseOffer)

        fun bind(offer: UserOffer) {
            title.text = offer.name
            code.text = offer.offerType ?: "Special Offer"
            description.text = offer.description?.takeIf { it.isNotBlank() }
                ?: "Use this live offer while adding items to your order."
            summary.text = buildSummary(offer)
            expiry.text = formatExpiry(offer.endDate, offer.price)
            btnUseOffer.setOnClickListener { onUseOffer(offer) }
        }

        private fun buildSummary(offer: UserOffer): String {
            val first = offer.items.firstOrNull()
            if (first == null) {
                return "Offer amount ${offer.price?.let { "? ${it.toInt()}" } ?: "available now"}"
            }
            return "${first.itemName}: buy ${first.buyQty} and get ${first.offerQty}"
        }

        private fun formatExpiry(endDate: String?, price: Double?): String {
            val expiryText = if (endDate.isNullOrBlank()) {
                "Ends soon"
            } else {
                LocalDate.parse(endDate.substringBefore("T"))
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    .let { "Expires on $it" }
            }
            val priceText = price?.takeIf { it > 0 }?.let { " • ? ${it.toInt()}" } ?: ""
            return expiryText + priceText
        }
    }
}
