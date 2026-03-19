package com.svd.svdagencies.ui.user

import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserDashboardResponse
import com.svd.svdagencies.data.model.user.UserPlan
import com.svd.svdagencies.data.model.user.UserSubscription
import com.svd.svdagencies.data.repository.UserDashboardObserver
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.user.adapter.SubscriptionItemAdapter
import com.svd.svdagencies.ui.user.adapter.UserPlanAdapter
import com.svd.svdagencies.utils.SessionManager
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

class UserSubscriptionFragment : Fragment(R.layout.user_subscription), UserDashboardObserver {

    private lateinit var swipeRefresh: SwipeRefreshLayout
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
    private lateinit var layoutDates: View
    private lateinit var layoutProgress: View
    private lateinit var rvSubItems: RecyclerView
    private lateinit var tvTotalItemsQty: TextView
    private lateinit var tvSubPrice: TextView
    private lateinit var btnPauseSubscription: MaterialButton
    private lateinit var btnResumeSubscriptionCard: MaterialButton
    private lateinit var progressBar: View
    
    // Available Plans
    private lateinit var rvAvailablePlans: RecyclerView
    private lateinit var progressAvailablePlans: ProgressBar
    private lateinit var availablePlansAdapter: UserPlanAdapter

    private var isLoading = false
    
    // No sub / Paused view elements
    private lateinit var ivNoSubIcon: ImageView
    private lateinit var tvNoSubTitle: TextView
    private lateinit var tvNoSubMsg: TextView
    private lateinit var btnExplorePlans: MaterialButton
    private lateinit var btnResumeSubscription: MaterialButton

    private val itemsAdapter = SubscriptionItemAdapter()
    private lateinit var session: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
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
        
        layoutDates = view.findViewById(R.id.layoutSubscriptionDates)
        layoutProgress = view.findViewById(R.id.layoutSubscriptionProgress)
        
        rvSubItems = view.findViewById(R.id.rvSubItems)
        tvTotalItemsQty = view.findViewById(R.id.tvTotalItemsQty)
        tvSubPrice = view.findViewById(R.id.tvSubPrice)
        btnPauseSubscription = view.findViewById(R.id.btnPauseSubscription)
        btnResumeSubscriptionCard = view.findViewById(R.id.btnResumeSubscriptionCard)
        progressBar = view.findViewById(R.id.progressBar)

        ivNoSubIcon = view.findViewById(R.id.ivNoSubIcon)
        tvNoSubTitle = view.findViewById(R.id.tvNoSubTitle)
        tvNoSubMsg = view.findViewById(R.id.tvNoSubMsg)
        btnExplorePlans = view.findViewById(R.id.btnExplorePlans)
        btnResumeSubscription = view.findViewById(R.id.btnResumeSubscription)

        // Available Plans Setup
        rvAvailablePlans = view.findViewById(R.id.rvAvailablePlans)
        progressAvailablePlans = view.findViewById(R.id.progressAvailablePlans)
        availablePlansAdapter = UserPlanAdapter(
            onViewPlan = { plan -> showPlanDetails(plan) },
            onActivatePlan = { plan -> activatePlan(plan) }
        )
        rvAvailablePlans.adapter = availablePlansAdapter

        rvSubItems.adapter = itemsAdapter

        btnPauseSubscription.setOnClickListener {
            showPauseDialog()
        }

        btnResumeSubscriptionCard.setOnClickListener {
            resumeSubscription()
        }

        btnResumeSubscription.setOnClickListener {
            resumeSubscription()
        }

        swipeRefresh.setOnRefreshListener {
            loadSubscriptionData()
            loadAvailablePlans()
        }

        // Initial load
        loadSubscriptionData()
        loadAvailablePlans()
    }

    private fun loadSubscriptionData() {
        if (isLoading) return
        setLoading(true)
        UserRepository.fetchCurrentSubscription(
            customerId = session.getUserId(),
            onSuccess = { subscription ->
                val isActive = subscription.planName != null &&
                        subscription.planName != "No active subscription"

                val isPaused = UserRepository.getCachedDashboard()
                    ?.subscriptionPauses
                    ?.any { it.isActivePause }
                    ?: false

                updateUI(subscription, isPaused, isActive)
                setLoading(false)
            },
            onError = { error ->
                if (isAdded) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
                setLoading(false)
            }
        )
    }

    private fun loadAvailablePlans() {
        progressAvailablePlans.visibility = View.VISIBLE
        rvAvailablePlans.visibility = View.GONE

        UserRepository.fetchPlans(
            onSuccess = { plans ->
                progressAvailablePlans.visibility = View.GONE
                if (plans.isNotEmpty()) {
                    availablePlansAdapter.submitList(plans)
                    rvAvailablePlans.visibility = View.VISIBLE
                }
            },
            onError = { _ ->
                progressAvailablePlans.visibility = View.GONE
            }
        )
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
        loadSubscriptionData()
    }

    private fun updateUI(subscription: UserSubscription, isPaused: Boolean, isActive: Boolean) {
        tvSubPlanName.text = subscription.planName ?: "Active Plan"
        tvPlanType.text = subscription.description ?: "Subscription Details"

        val totalQty = subscription.items.sumOf { it.quantity }
        tvTotalItemsQty.text = "Total Qty: ${formatQuantity(totalQty)}"
        itemsAdapter.submitList(subscription.items)
        tvSubPrice.text = formatCurrency(subscription.price)

        if (subscription.startDate != null && subscription.endDate != null) {
            layoutDates.visibility = View.VISIBLE
            layoutProgress.visibility = View.VISIBLE
            tvStartDate.text = formatDate(subscription.startDate)
            tvEndDate.text = formatDate(subscription.endDate)
            val (progress, completedDays, remainingDays) = calculateProgress(subscription)
            subProgressBar.progress = progress
            tvDaysCompleted.text = "$completedDays Days Completed"
            tvDaysRemaining.text = "$remainingDays Days Left"
        } else {
            layoutDates.visibility = View.GONE
            layoutProgress.visibility = View.GONE
        }

        if (isPaused) {
            layoutNoSubscription.visibility = View.GONE
            cardActiveSubscription.visibility = View.VISIBLE
            tvSubStatus.text = "PAUSED"
            tvSubStatus.setBackgroundResource(R.drawable.bg_status_red)
            tvNextRenewal.text = "Subscription is paused"
            btnPauseSubscription.visibility = View.GONE
            btnResumeSubscriptionCard.visibility = View.VISIBLE
        } else if (!isActive) {
            cardActiveSubscription.visibility = View.GONE
            layoutNoSubscription.visibility = View.VISIBLE
            ivNoSubIcon.setImageResource(R.drawable.ic_subscription)
            ivNoSubIcon.alpha = 0.2f
            tvNoSubTitle.text = "No Active Subscription"
            tvNoSubMsg.text = "You don't have an active subscription right now. Explore our plans to start your daily milk delivery."
            btnExplorePlans.visibility = View.VISIBLE
            btnResumeSubscription.visibility = View.GONE
        } else {
            layoutNoSubscription.visibility = View.GONE
            cardActiveSubscription.visibility = View.VISIBLE

            tvSubStatus.text = "ACTIVE"
            tvSubStatus.setBackgroundResource(R.drawable.bg_status_green)
            btnPauseSubscription.visibility = View.VISIBLE
            btnResumeSubscriptionCard.visibility = View.GONE

            tvNextRenewal.text = if (subscription.endDate != null) {
                "Ends on: ${formatDate(subscription.endDate)}"
            } else {
                "Subscription is currently active"
            }
        }
    }

    private fun showPauseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.user_subscription_pause, null)
        val etReason = dialogView.findViewById<TextInputEditText>(R.id.etReason)
        val tilReason = dialogView.findViewById<TextInputLayout>(R.id.tilReason)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btnSubmit)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val reason = etReason.text?.toString()?.trim()
            if (reason.isNullOrBlank()) {
                tilReason.error = "Reason is required to pause"
                return@setOnClickListener
            }
            tilReason.error = null

            UserRepository.pauseResumeSubscription(
                userId = session.getUserId(),
                action = "pause",
                reason = reason,
                onSuccess = {
                    Toast.makeText(requireContext(), "Subscription paused", Toast.LENGTH_SHORT).show()
                    UserRepository.fetchDashboard(
                        userId = session.getUserId(),
                        onSuccess = { loadSubscriptionData() },
                        onError = { loadSubscriptionData() }
                    )
                    dialog.dismiss()
                },
                onError = { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            )
        }
        dialog.show()
    }

    private fun resumeSubscription() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Resume Subscription")
            .setMessage("Are you sure you want to resume your subscription and start deliveries?")
            .setPositiveButton("Yes, Resume") { _, _ ->
                UserRepository.pauseResumeSubscription(
                    userId = session.getUserId(),
                    action = "resume",
                    onSuccess = {
                        Toast.makeText(requireContext(), "Subscription resumed", Toast.LENGTH_SHORT).show()
                        UserRepository.fetchDashboard(
                            userId = session.getUserId(),
                            onSuccess = { loadSubscriptionData() },
                            onError = { loadSubscriptionData() }
                        )
                    },
                    onError = { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun calculateProgress(subscription: UserSubscription): Triple<Int, Int, Int> {
        val start = parseDate(subscription.startDate)
        val end = parseDate(subscription.endDate)
        if (start == null || end == null || end.isBefore(start)) return Triple(0, 0, 0)

        val now = LocalDate.now().coerceAtMost(end)
        val totalDays = (ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceAtLeast(1)
        val completedDays = (ChronoUnit.DAYS.between(start, now) + 1).toInt().coerceIn(0, totalDays)
        val remainingDays = (totalDays - completedDays).coerceAtLeast(0)
        val percent = ((completedDays.toDouble() / totalDays.toDouble()) * 100).toInt().coerceIn(0, 100)
        return Triple(percent, completedDays, remainingDays)
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try { LocalDate.parse(value.substringBefore("T")) } catch (ex: Exception) { null }
    }

    private fun formatDate(value: String?): String = parseDate(value)?.toString() ?: "N/A"

    private fun formatCurrency(amount: Double): String = String.format(Locale.getDefault(), "₹ %.2f", amount)

    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
    }

    private fun setLoading(state: Boolean) {
        isLoading = state
        progressBar.visibility = if (state) View.VISIBLE else View.GONE
        if (!state) swipeRefresh.isRefreshing = false
        btnPauseSubscription.isEnabled = !state
        btnResumeSubscriptionCard.isEnabled = !state
        btnResumeSubscription.isEnabled = !state
    }

    private fun showPlanDetails(plan: UserPlan) {
        val dialogView = layoutInflater.inflate(R.layout.user_plan_details, null)
        val tvName = dialogView.findViewById<TextView>(R.id.tvDialogPlanName)
        val tvPrice = dialogView.findViewById<TextView>(R.id.tvDialogPlanPrice)
        val tvDesc = dialogView.findViewById<TextView>(R.id.tvDialogPlanDescription)
        val tvItems = dialogView.findViewById<TextView>(R.id.tvDialogPlanItems)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnDialogClose)

        tvName.text = plan.name
        tvPrice.text = "Price: \u20B9 ${plan.price.toInt()} / month"
        tvDesc.text = plan.description ?: "No description available."
        
        val itemsSb = StringBuilder()
        if (plan.items.isNullOrEmpty()) {
            itemsSb.append("No items listed for this plan.")
        } else {
            plan.items.forEach { itemsSb.appendLine("• ${it.itemName}  × ${formatQuantity(it.quantity.toDouble())}") }
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

    private fun activatePlan(plan: UserPlan) {
        Toast.makeText(requireContext(), "Activation for ${plan.name} coming soon.", Toast.LENGTH_SHORT).show()
    }
}
