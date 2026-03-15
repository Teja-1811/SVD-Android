package com.svd.svdagencies.ui.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserOffer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UserOfferAdapter : RecyclerView.Adapter<UserOfferAdapter.OfferViewHolder>() {

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
        private val expiry = view.findViewById<TextView>(R.id.tvOfferExpiry)

        fun bind(offer: UserOffer) {
            title.text = offer.name
            code.text = offer.offerType ?: "Special Offer"
            expiry.text = formatExpiry(offer.endDate)
        }

        private fun formatExpiry(endDate: String?): String {
            return if (endDate.isNullOrBlank()) {
                "Ends soon"
            } else {
                LocalDate.parse(endDate.substringBefore("T")).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    .let { "Expires on $it" }
            }
        }
    }
}
