package com.svd.svdagencies.ui.customer.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R

data class CustomerOffer(
    val title: String,
    val subtitle: String,
    val actionText: String,
    val colorHex: String? = null
)

class CustomerOfferAdapter(private val offers: List<CustomerOffer>) :
    RecyclerView.Adapter<CustomerOfferAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.customer_offer_banner, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val offer = offers[position]
        holder.tvTitle.text = offer.title
        holder.tvSubtitle.text = offer.subtitle
        holder.btnAction.text = offer.actionText

        offer.colorHex?.let { colorStr ->
            runCatching {
                val color = Color.parseColor(colorStr)
                // Use background tint instead of replacing background to preserve rounded corners
                holder.layoutBanner.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                holder.btnAction.setTextColor(color)
            }
        }
    }

    override fun getItemCount(): Int = offers.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutBanner: View = view.findViewById(R.id.layoutBanner)
        val tvTitle: TextView = view.findViewById(R.id.tvBannerTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvBannerSubtitle)
        val btnAction: MaterialButton = view.findViewById(R.id.btnBannerAction)
    }
}
