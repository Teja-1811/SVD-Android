package com.svd.svdagencies.ui.user

import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserDashboardResponse
import com.svd.svdagencies.data.model.user.UserSubscription
import com.svd.svdagencies.data.repository.UserDashboardObserver
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.user.adapter.SubscriptionItemAdapter
import com.svd.svdagencies.ui.user.adapter.SubscriptionPauseAdapter
import com.svd.svdagencies.utils.SessionManager
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

class UserSubscriptionFragment : Fragment(R.layout.user_subscription), UserDashboardObserver {

    private lateinit var cardActiveSubscription: MaterialCardView
    private lateinit var layoutNoSubscription: View
    private lateinit var tvSubPlanName: TextView
    private lateinit var tvPlanType: TextView
    private lateinit var tvSubStatus: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var tvNextRenewal: TextView
    private lateinit var tvDaysCompleted: TextView
    private lateinit var tvDaysRemaining: TextView
    private lateinit var subProgressBar: LinearProgressIndicator
    private lateinit var rvSubItems: RecyclerView
    private lateinit var tvTotalItemsQty: TextView
    private lateinit var tvSubPrice: TextView
    private lateinit var btnPauseSubscription: MaterialButton
    private lateinit var rvPauseHistory: RecyclerView
    private lateinit var tvPauseHistoryTitle: TextView

    private val itemsAdapter = SubscriptionItemAdapter()
    private val pauseAdapter = SubscriptionPauseAdapter()
    private lateinit var session: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        cardActiveSubscription = view.findViewById(R.id.cardActiveSubscription)
        layoutNoSubscription = view.findViewById(R.id.layoutNoSubscription)
        tvSubPlanName = view.findViewById(R.id.tvSubPlanName)
        tvPlanType = view.findViewById(R.id.tvPlanType)
        tvSubStatus = view.findViewById(R.id.tvSubStatus)
        tvStartDate = view.findViewById(R.id.tvStartDate)
        tvEndDate = view.findViewById(R.id.tvEndDate)
        tvNextRenewal = view.findViewById(R.id.tvNextRenewal)
        tvDaysCompleted = view.findViewById(R.id.tvDaysCompleted)
        tvDaysRemaining = view.findViewById(R.id.tvDaysRemaining)
        subProgressBar = view.findViewById(R.id.subProgressBar)
        rvSubItems = view.findViewById(R.id.rvSubItems)
        tvTotalItemsQty = view.findViewById(R.id.tvTotalItemsQty)
        tvSubPrice = view.findViewById(R.id.tvSubPrice)
        btnPauseSubscription = view.findViewById(R.id.btnPauseSubscription)
        rvPauseHistory = view.findViewById(R.id.rvPauseHistory)
        tvPauseHistoryTitle = view.findViewById(R.id.tvPauseHistoryTitle)

        rvSubItems.adapter = itemsAdapter
        rvPauseHistory.layoutManager = LinearLayoutManager(requireContext())
        rvPauseHistory.adapter = pauseAdapter

        btnPauseSubscription.setOnClickListener {
            UserRepository.getCachedDashboard()?.subscription?.let { sub ->
                showPauseDialog(sub)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        UserRepository.registerObserver(this)
    }

    override fun onStop() {
        UserRepository.unregisterObserver(this)
        super.onStop()
    }

    override fun onDashboardUpdated(data: UserDashboardResponse) {
        updateSubscription(data.subscription)
        if (data.subscriptionPauses.isEmpty()) {
            rvPauseHistory.visibility = View.GONE
            tvPauseHistoryTitle.visibility = View.GONE
        } else {
            rvPauseHistory.visibility = View.VISIBLE
            tvPauseHistoryTitle.visibility = View.VISIBLE
            pauseAdapter.submitList(data.subscriptionPauses)
        }
    }

    private fun showPauseDialog(subscription: UserSubscription) {
        val dialogView = layoutInflater.inflate(R.layout.user_subscription_pause, null)
        val etPauseDate = dialogView.findViewById<TextInputEditText>(R.id.etPauseDate)
        val etResumeDate = dialogView.findViewById<TextInputEditText>(R.id.etResumeDate)
        val etReason = dialogView.findViewById<TextInputEditText>(R.id.etReason)
        val tilReason = dialogView.findViewById<TextInputLayout>(R.id.tilReason)

        val datePicker = { editText: TextInputEditText ->
            val builder = MaterialDatePicker.Builder.datePicker()
            builder.setTitleText("Select Date")
            val picker = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                val date = Date(selection)
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                editText.setText(format.format(date))
            }
            picker.show(childFragmentManager, picker.toString())
        }

        etPauseDate.setOnClickListener { datePicker(etPauseDate) }
        etResumeDate.setOnClickListener { datePicker(etResumeDate) }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            positive.setOnClickListener {
                val pauseDate = etPauseDate.text?.toString()?.trim()
                val resumeDate = etResumeDate.text?.toString()?.trim()
                val reason = etReason.text?.toString()?.trim()

                if (pauseDate.isNullOrBlank() || resumeDate.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Please select both dates", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                tilReason.error = null

                UserRepository.pauseSubscription(
                    userId = session.getUserId(),
                    subscriptionId = subscription.id ?: return@setOnClickListener,
                    pauseDate = pauseDate,
                    resumeDate = resumeDate,
                    reason = reason,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Pause request submitted", Toast.LENGTH_SHORT).show()
                        UserRepository.fetchDashboard(session.getUserId())
                        dialog.dismiss()
                    },
                    onError = { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        dialog.show()
    }

    private fun updateSubscription(subscription: UserSubscription) {
        if (!subscription.isActive) {
            layoutNoSubscription.visibility = View.VISIBLE
            cardActiveSubscription.visibility = View.GONE
            return
        }

        layoutNoSubscription.visibility = View.GONE
        cardActiveSubscription.visibility = View.VISIBLE

        tvSubPlanName.text = subscription.planName ?: "Subscription"
        tvPlanType.text = if (subscription.durationInDays > 0) {
            "${subscription.durationInDays} days"
        } else {
            "Subscription"
        }

        if (subscription.isActive) {
            tvSubStatus.text = "ACTIVE"
            tvSubStatus.setBackgroundResource(R.drawable.bg_status_green)
        } else {
            tvSubStatus.text = "INACTIVE"
            tvSubStatus.setBackgroundResource(R.drawable.bg_status_red)
        }

        tvStartDate.text = formatDate(subscription.startDate)
        tvEndDate.text = formatDate(subscription.endDate)
        tvNextRenewal.text = formatDate(subscription.endDate)

        val (progress, completedDays, remainingDays) = calculateProgress(subscription)
        subProgressBar.progress = progress
        tvDaysCompleted.text = "$completedDays Days Completed"
        tvDaysRemaining.text = "$remainingDays Days Left"

        val totalQty = subscription.items.sumOf { it.quantity }
        tvTotalItemsQty.text = "Total Qty: ${formatQuantity(totalQty)}"
        itemsAdapter.submitList(subscription.items)

        tvSubPrice.text = formatCurrency(subscription.price)
    }

    private fun calculateProgress(subscription: UserSubscription): Triple<Int, Int, Int> {
        val start = parseDate(subscription.startDate)
        val end = parseDate(subscription.endDate)

        if (start == null || end == null || end.isBefore(start)) {
            return Triple(0, 0, 0)
        }

        val now = LocalDate.now().coerceAtMost(end)
        val totalDays = (ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceAtLeast(1)
        val completedDays = (ChronoUnit.DAYS.between(start, now) + 1).toInt().coerceIn(0, totalDays)
        val remainingDays = (totalDays - completedDays).coerceAtLeast(0)
        val percent = ((completedDays.toDouble() / totalDays.toDouble()) * 100).toInt().coerceIn(0, 100)
        return Triple(percent, completedDays, remainingDays)
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value.substringBefore("T"))
        } catch (ex: Exception) {
            null
        }
    }

    private fun formatDate(value: String?): String {
        return parseDate(value)?.toString() ?: "N/A"
    }

    private fun formatCurrency(amount: Double): String {
        return String.format(Locale.getDefault(), "₹ %.2f", amount)
    }

    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            quantity.toString()
        }
    }
}
