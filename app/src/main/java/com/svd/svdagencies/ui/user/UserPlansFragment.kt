package com.svd.svdagencies.ui.user

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserPlan
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.user.adapter.UserPlanAdapter

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

        adapter = UserPlanAdapter { plan ->
            Toast.makeText(
                requireContext(),
                "Selected ${plan.name}. Subscription flow coming soon.",
                Toast.LENGTH_SHORT
            ).show()
        }

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
}
