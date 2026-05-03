package com.svd.svdagencies.ui.user

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserPlan
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.user.adapter.UserPlanAdapter
import java.util.Locale

class UserPlansFragment : Fragment(R.layout.user_plans) {

    private lateinit var rvPlans: RecyclerView
    private lateinit var progressPlans: ProgressBar
    private lateinit var tvPlansStatus: TextView
    private lateinit var adapter: UserPlanAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPlans = view.findViewById(R.id.rvPlans)
        progressPlans = view.findViewById(R.id.progressPlans)
        tvPlansStatus = view.findViewById(R.id.tvPlansStatus)

        adapter = UserPlanAdapter(
            onViewPlan = { plan -> showPlanDetails(plan) },
            onActivatePlan = { plan -> requestPlan(plan) }
        )

        rvPlans.layoutManager = LinearLayoutManager(requireContext())
        rvPlans.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        loadPlans()
    }

    private fun loadPlans() {
        progressPlans.visibility = View.VISIBLE
        tvPlansStatus.visibility = View.GONE
        rvPlans.visibility = View.GONE

        UserRepository.fetchPlans(
            onSuccess = { plans ->
                progressPlans.visibility = View.GONE
                if (plans.isEmpty()) {
                    tvPlansStatus.text = "No plans available at the moment."
                    tvPlansStatus.visibility = View.VISIBLE
                    rvPlans.visibility = View.GONE
                    return@fetchPlans
                }

                adapter.submitList(plans)
                rvPlans.visibility = View.VISIBLE
            },
            onError = { message ->
                progressPlans.visibility = View.GONE
                tvPlansStatus.text = message
                tvPlansStatus.visibility = View.VISIBLE
            }
        )
    }

    private fun showPlanDetails(plan: UserPlan) {
        val dialogView = layoutInflater.inflate(R.layout.user_plan_details, null)
        val tvName = dialogView.findViewById<TextView>(R.id.tvDialogPlanName)
        val tvPrice = dialogView.findViewById<TextView>(R.id.tvDialogPlanPrice)
        val tvDesc = dialogView.findViewById<TextView>(R.id.tvDialogPlanDescription)
        val tvItems = dialogView.findViewById<TextView>(R.id.tvDialogPlanItems)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnDialogClose)

        tvName.text = plan.name
        tvPrice.text = "Price: ? ${plan.price.toInt()} / month (? ${calcPerDelivery(plan.price)} per delivery)"
        tvDesc.text = plan.description ?: "No description available."

        val itemsSb = StringBuilder()
        if (plan.items.isNullOrEmpty()) {
            itemsSb.append("No items listed for this plan.")
        } else {
            plan.items.forEach { itemsSb.appendLine("• ${it.itemName} × ${formatQuantity(it.quantity)}") }
        }
        tvItems.text = itemsSb.toString().trim()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun requestPlan(plan: UserPlan) {
        val message = buildString {
            append("I want to buy the subscription plan: ${plan.name}.")
            if (!plan.description.isNullOrBlank()) {
                append("\n\nPlan details: ${plan.description}")
            }
            append("\n\nPlease help me activate this plan.")
        }
        val intent = Intent(requireContext(), ContactSupportActivity::class.java).apply {
            putExtra(ContactSupportActivity.EXTRA_PREFILL_SUBJECT, "Subscription request: ${plan.name}")
            putExtra(ContactSupportActivity.EXTRA_PREFILL_MESSAGE, message)
        }
        startActivity(intent)
    }

    private fun formatQuantity(value: Double): String =
        String.format(Locale.getDefault(), "%.0f", value)

    private fun calcPerDelivery(price: Double): Int = (price / 4).toInt()
}
