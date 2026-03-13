package com.svd.svdagencies.ui.admin

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.svd.svdagencies.data.model.admin.*
import com.svd.svdagencies.databinding.AdminSubscriptionsBinding
import com.svd.svdagencies.databinding.AdminSubscriptionPlanAddBinding
import com.svd.svdagencies.databinding.AdminSubscriptionAssignBinding
import com.svd.svdagencies.databinding.AdminSubscriptionPaymentAddBinding
import com.svd.svdagencies.ui.admin.adapter.SubscriptionPlanAdapter
import com.svd.svdagencies.ui.admin.adapter.SubscriptionRowAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class AdminSubscriptionsActivity : AdminBaseActivity() {

    private lateinit var binding: AdminSubscriptionsBinding
    
    private lateinit var planAdapter: SubscriptionPlanAdapter
    private lateinit var activeAdapter: SubscriptionRowAdapter
    private lateinit var deactivatedAdapter: SubscriptionRowAdapter
    private lateinit var expiredAdapter: SubscriptionRowAdapter
    private lateinit var expiringSoonAdapter: SubscriptionRowAdapter

    private var allPlans: List<SubscriptionPlan> = emptyList()
    private var allCustomers: List<SubscriptionCustomer> = emptyList()
    private var allItems: List<AdminItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminSubscriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup the centralized admin layout (toolbar, drawer, etc.)
        setupAdminLayout("Subscriptions")
        
        setupRecyclerViews()
        setupListeners()
        setupStatCards()
        
        loadData()
    }

    private fun setupStatCards() {
        binding.statActive.tvStatLabel.text = "Active"
        binding.statExpired.tvStatLabel.text = "Expired"
        binding.statPlans.tvStatLabel.text = "Total Plans"
        binding.statExpiringSoon.tvStatLabel.text = "Expiring Soon"
        
        binding.statActive.ivStatIcon.setImageResource(R.drawable.ic_subscription)
        binding.statExpired.ivStatIcon.setImageResource(R.drawable.ic_subscription)
        binding.statPlans.ivStatIcon.setImageResource(R.drawable.ic_subscription)
        binding.statExpiringSoon.ivStatIcon.setImageResource(R.drawable.ic_subscription)
    }

    private fun setupRecyclerViews() {
        // Plans List
        planAdapter = SubscriptionPlanAdapter(emptyList(), { plan ->
            showEditPlanDialog(plan)
        }, { plan ->
            showAddItemDialog(plan)
        }, { plan, item ->
            showEditItemDialog(plan, item)
        }, { _, item ->
            confirmDeleteItem(item)
        })
        binding.rvPlans.apply {
            layoutManager = LinearLayoutManager(this@AdminSubscriptionsActivity)
            adapter = planAdapter
        }

        // Active Subscriptions
        activeAdapter = SubscriptionRowAdapter(emptyList(), SubscriptionRowAdapter.SubscriptionType.ACTIVE, { sub ->
            showPaymentDialog(sub)
        }, { sub ->
            toggleSubscription(sub)
        })
        binding.rvActiveSubscriptions.apply {
            layoutManager = LinearLayoutManager(this@AdminSubscriptionsActivity)
            adapter = activeAdapter
        }

        // Deactivated Subscriptions
        deactivatedAdapter = SubscriptionRowAdapter(emptyList(), SubscriptionRowAdapter.SubscriptionType.DEACTIVATED, { _ ->
            // Handled via toggle in adapter
        }, { sub ->
            toggleSubscription(sub)
        })
        binding.rvDeactivatedSubscriptions.apply {
            layoutManager = LinearLayoutManager(this@AdminSubscriptionsActivity)
            adapter = deactivatedAdapter
        }

        // Expired Subscriptions
        expiredAdapter = SubscriptionRowAdapter(emptyList(), SubscriptionRowAdapter.SubscriptionType.EXPIRED, {}, {})
        binding.rvExpiredSubscriptions.apply {
            layoutManager = LinearLayoutManager(this@AdminSubscriptionsActivity)
            adapter = expiredAdapter
        }

        // Expiring Soon
        expiringSoonAdapter = SubscriptionRowAdapter(emptyList(), SubscriptionRowAdapter.SubscriptionType.EXPIRING_SOON, {}, {})
        binding.rvExpiringSoonSubscriptions.apply {
            layoutManager = LinearLayoutManager(this@AdminSubscriptionsActivity)
            adapter = expiringSoonAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }

        binding.btnAddPlan.setOnClickListener {
            showAddPlanDialog()
        }

        binding.btnAssignSubscription.setOnClickListener {
            if (allCustomers.isEmpty() || allPlans.isEmpty()) {
                fetchCustomersAndPlansForDialog()
            } else {
                showAssignSubscriptionDialog()
            }
        }
        
        binding.btnSubscriptionHistory.setOnClickListener {
            startActivity(Intent(this, AdminSubscriptionHistoryActivity::class.java))
        }
        
        binding.btnTodayDeliveries.setOnClickListener {
            startActivity(Intent(this, AdminUserDeliveryActivity::class.java))
        }
    }

    private fun showAddPlanDialog() {
        val dialogBinding = AdminSubscriptionPlanAddBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvPlanDialogTitle.text = "Add New Subscription Plan"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val planData = buildPlanPayload(dialogBinding) ?: return@setOnClickListener
                createPlan(planData)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditPlanDialog(plan: SubscriptionPlan) {
        val dialogBinding = AdminSubscriptionPlanAddBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvPlanDialogTitle.text = "Edit Subscription Plan"
        dialogBinding.etPlanName.setText(plan.name)
        dialogBinding.etPlanPrice.setText(plan.price)
        dialogBinding.etPlanDuration.setText(plan.durationInDays.toString())
        dialogBinding.etPlanDescription.setText(plan.description)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val planData = buildPlanPayload(dialogBinding) ?: return@setOnClickListener
                updatePlan(plan.id, planData)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun buildPlanPayload(dialogBinding: AdminSubscriptionPlanAddBinding): Map<String, Any>? {
        dialogBinding.tilPlanName.error = null
        dialogBinding.tilPlanPrice.error = null
        dialogBinding.tilPlanDuration.error = null

        val name = dialogBinding.etPlanName.text?.toString()?.trim().orEmpty()
        val priceText = dialogBinding.etPlanPrice.text?.toString()?.trim().orEmpty()
        val durationText = dialogBinding.etPlanDuration.text?.toString()?.trim().orEmpty()
        val description = dialogBinding.etPlanDescription.text?.toString()?.trim().orEmpty()

        var hasError = false

        if (name.isEmpty()) {
            dialogBinding.tilPlanName.error = "Enter plan name"
            hasError = true
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0.0) {
            dialogBinding.tilPlanPrice.error = "Enter a valid price"
            hasError = true
        }

        val duration = durationText.toIntOrNull()
        if (duration == null || duration <= 0) {
            dialogBinding.tilPlanDuration.error = "Enter valid duration"
            hasError = true
        }

        if (hasError) {
            return null
        }

        return mapOf(
            "name" to name,
            "price" to price!!,
            "duration_days" to duration!!,
            "description" to description
        )
    }

    private fun showAddItemDialog(plan: SubscriptionPlan) {
        if (allItems.isEmpty()) {
            Toast.makeText(this, "Items are still loading. Please try again.", Toast.LENGTH_SHORT).show()
            loadData()
            return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.admin_subscription_add_plan_item, null)
        val etName = view.findViewById<AutoCompleteTextView>(R.id.etItemName)
        val etQty = view.findViewById<EditText>(R.id.etStock) 
        bindItemDropdown(etName)
        
        AlertDialog.Builder(this)
            .setTitle("Add Item to ${plan.name}")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val qty = etQty.text.toString()
                if (name.isNotEmpty() && qty.isNotEmpty()) {
                    val data = mapOf("item_name" to name, "quantity" to qty.toInt())
                    addItemToPlan(plan.id, data)
                } else {
                    Toast.makeText(this, "Select an item and enter quantity", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditItemDialog(plan: SubscriptionPlan, item: SubscriptionPlanItem) {
        if (allItems.isEmpty()) {
            Toast.makeText(this, "Items are still loading. Please try again.", Toast.LENGTH_SHORT).show()
            loadData()
            return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.admin_subscription_add_plan_item, null)
        val etName = view.findViewById<AutoCompleteTextView>(R.id.etItemName)
        val etQty = view.findViewById<EditText>(R.id.etStock)
        bindItemDropdown(etName)
        etName.setText(item.itemName, false)
        etQty.setText(item.quantity.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit Item")
            .setView(view)
            .setPositiveButton("Update") { _, _ ->
                val data = mapOf("item_name" to etName.text.toString().trim(), "quantity" to etQty.text.toString().toInt())
                updatePlanItem(item.id, data)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteItem(item: SubscriptionPlanItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete ${item.itemName}?")
            .setPositiveButton("Delete") { _, _ -> deletePlanItem(item.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createPlan(planData: Map<String, Any>) {
        ApiClient.subscriptionApi.createPlan(planData).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminSubscriptionsActivity, "Plan created", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
        })
    }

    private fun updatePlan(id: Int, planData: Map<String, Any>) {
        ApiClient.subscriptionApi.updatePlan(id, planData).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminSubscriptionsActivity, "Plan updated", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
        })
    }

    private fun addItemToPlan(id: Int, data: Map<String, Any>) {
        ApiClient.subscriptionApi.addItemToPlan(id, data).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminSubscriptionsActivity, "Item added", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
        })
    }

    private fun updatePlanItem(id: Int, data: Map<String, Any>) {
        ApiClient.subscriptionApi.updatePlanItem(id, data).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminSubscriptionsActivity, "Item updated", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
        })
    }

    private fun deletePlanItem(id: Int) {
        ApiClient.subscriptionApi.deletePlanItem(id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminSubscriptionsActivity, "Item deleted", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        ApiClient.subscriptionApi.getSubscriptionDashboard().enqueue(object : Callback<SubscriptionDashboardResponse> {
            override fun onResponse(call: Call<SubscriptionDashboardResponse>, response: Response<SubscriptionDashboardResponse>) {
                binding.swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    val data = response.body() ?: return
                    
                    allPlans = data.plansList
                    allCustomers = data.customers
                    allItems = data.items
                    
                    planAdapter.updateData(data.plansList)
                    activeAdapter.updateData(data.activeSubscriptions)
                    deactivatedAdapter.updateData(data.deactivatedSubscriptions)
                    expiredAdapter.updateData(data.expiredSubscriptions)
                    expiringSoonAdapter.updateData(data.expiringSoon)
                    
                    binding.statActive.tvStatValue.text = data.totalActive.toString()
                    binding.statExpired.tvStatValue.text = data.totalExpired.toString()
                    binding.statPlans.tvStatValue.text = data.totalPlans.toString()
                    binding.statExpiringSoon.tvStatValue.text = data.expiringCount.toString()
                }
            }
            override fun onFailure(call: Call<SubscriptionDashboardResponse>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }

    private fun bindItemDropdown(itemField: AutoCompleteTextView) {
        val itemNames = allItems
            .map { it.name.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, itemNames)
        itemField.setAdapter(adapter)
        itemField.setOnClickListener { itemField.showDropDown() }
        itemField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) itemField.showDropDown()
        }
    }

    private fun fetchCustomersAndPlansForDialog() {
        ApiClient.subscriptionApi.getSubscriptionDashboard().enqueue(object : Callback<SubscriptionDashboardResponse> {
            override fun onResponse(call: Call<SubscriptionDashboardResponse>, response: Response<SubscriptionDashboardResponse>) {
                if (response.isSuccessful) {
                    allPlans = response.body()?.plansList ?: emptyList()
                    allCustomers = response.body()?.customers ?: emptyList()
                    showAssignSubscriptionDialog()
                }
            }
            override fun onFailure(call: Call<SubscriptionDashboardResponse>, t: Throwable) {}
        })
    }

    private fun showAssignSubscriptionDialog() {
        val dialogBinding = AdminSubscriptionAssignBinding.inflate(LayoutInflater.from(this))
        
        val customerNames = allCustomers.map { it.name }
        val planNames = allPlans.map { it.name }
        
        dialogBinding.spinnerCustomer.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, customerNames)
        dialogBinding.spinnerPlan.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, planNames)

        dialogBinding.etStartDate.setOnClickListener {
            showDatePicker(dialogBinding.etStartDate)
        }

        AlertDialog.Builder(this)
            .setTitle("Assign Subscription")
            .setView(dialogBinding.root)
            .setPositiveButton("Assign") { _, _ ->
                val customer = allCustomers[dialogBinding.spinnerCustomer.selectedItemPosition]
                val plan = allPlans[dialogBinding.spinnerPlan.selectedItemPosition]
                val startDate = dialogBinding.etStartDate.text.toString()

                if (startDate.isNotEmpty()) {
                    val data = mapOf(
                        "customer_id" to customer.id,
                        "plan_id" to plan.id,
                        "start_date" to startDate
                    )
                    assignSubscription(data)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            editText.setText(String.format("%d-%02d-%02d", year, month + 1, day))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun assignSubscription(data: Map<String, Any>) {
        ApiClient.subscriptionApi.assignSubscription(data).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminSubscriptionsActivity, "Subscription assigned", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
        })
    }

    private fun toggleSubscription(sub: CustomerSubscription) {
        ApiClient.subscriptionApi.toggleSubscription(sub.id).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    loadData()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
        })
    }

    private fun showPaymentDialog(sub: CustomerSubscription) {
        val dialogBinding = AdminSubscriptionPaymentAddBinding.inflate(LayoutInflater.from(this))
        
        AlertDialog.Builder(this)
            .setTitle("Add Payment")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val amount = dialogBinding.etAmount.text.toString()
                if (amount.isNotEmpty()) {
                    val data = mapOf(
                        "subscription_id" to sub.id,
                        "customer_id" to sub.customerId,
                        "amount" to amount.toDouble(),
                        "note" to dialogBinding.etNote.text.toString()
                    )
                    addPayment(sub.id, data)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addPayment(subscriptionId: Int, data: Map<String, Any>) {
        ApiClient.subscriptionApi.recordPayment(subscriptionId, data).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminSubscriptionsActivity, "Payment added", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
        })
    }
}
