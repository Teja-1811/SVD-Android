package com.svd.svdagencies.ui.user

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserDashboardResponse
import com.svd.svdagencies.data.repository.UserDashboardObserver
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.user.adapter.SubscriptionHistoryAdapter
import com.svd.svdagencies.ui.user.adapter.SubscriptionPauseAdapter
import com.svd.svdagencies.ui.user.adapter.UserOfferAdapter

class UserHomeFragment : Fragment(R.layout.user_home), UserDashboardObserver {

    private lateinit var tvUserName: TextView
    private lateinit var rvSpecialOffers: RecyclerView
    private lateinit var rvSubscriptionHistory: RecyclerView
    private lateinit var rvSubscriptionPauses: RecyclerView
    private lateinit var tvHistoryEmpty: TextView
    private lateinit var tvPausesEmpty: TextView

    private val offerAdapter = UserOfferAdapter()
    private val historyAdapter = SubscriptionHistoryAdapter()
    private val pauseAdapter = SubscriptionPauseAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvUserName = view.findViewById(R.id.tvUserName)
        rvSpecialOffers = view.findViewById(R.id.rvSpecialOffers)
        rvSubscriptionHistory = view.findViewById(R.id.rvSubscriptionHistory)
        rvSubscriptionPauses = view.findViewById(R.id.rvSubscriptionPauses)
        tvHistoryEmpty = view.findViewById(R.id.tvHistoryEmpty)
        tvPausesEmpty = view.findViewById(R.id.tvPausesEmpty)

        rvSpecialOffers.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvSpecialOffers.adapter = offerAdapter

        rvSubscriptionHistory.layoutManager = LinearLayoutManager(requireContext())
        rvSubscriptionHistory.adapter = historyAdapter

        rvSubscriptionPauses.layoutManager = LinearLayoutManager(requireContext())
        rvSubscriptionPauses.adapter = pauseAdapter
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
        updateUI(data)
    }

    private fun updateUI(data: UserDashboardResponse) {
        tvUserName.text = data.customer.name

        offerAdapter.submitList(data.offers)
        historyAdapter.submitList(data.subscriptionHistory)
        pauseAdapter.submitList(data.subscriptionPauses)

        val hasHistory = data.subscriptionHistory.isNotEmpty()
        tvHistoryEmpty.isVisible = !hasHistory
        rvSubscriptionHistory.isVisible = hasHistory

        val hasPauses = data.subscriptionPauses.isNotEmpty()
        tvPausesEmpty.isVisible = !hasPauses
        rvSubscriptionPauses.isVisible = hasPauses
    }
}
