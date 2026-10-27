package com.svd.svdagencies.utils

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.customerData.CustomerItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class RouteCustomerFilters(
    private val activity: AppCompatActivity,
    private val route: AutoCompleteTextView,
    private val customer: AutoCompleteTextView,
    private val changed: () -> Unit
) {
    var routeId: Int? = null; private set
    var customerId: Int? = null; private set
    var customers: List<CustomerItem> = emptyList(); private set
    private var filtered: List<CustomerItem> = emptyList()
    init {
        route.setText("All routes",false); customer.setText("All customers",false)
        route.setOnClickListener { route.showDropDown() }; customer.setOnClickListener { customer.showDropDown() }
        customer.setOnItemClickListener { _,_,position,_ -> customerId=filtered.getOrNull(position-1)?.id; changed() }
        activity.lifecycleScope.launch {
            try {
                customers=(ApiClient.billsDashboardApi.getCustomersForBill("retailer").customers.orEmpty()+ApiClient.billsDashboardApi.getCustomersForBill("user").customers.orEmpty()).filter { it.frozen!=true }.distinctBy { it.id }
                val routes=customers.filter { it.route_id!=null }.distinctBy { it.route_id }.sortedBy { it.route_name }
                route.setAdapter(ArrayAdapter(activity,R.layout.delivery_dropdown_item,listOf("All routes")+routes.map { it.route_name ?: "Route ${it.route_id}" }))
                route.setOnItemClickListener { _,_,position,_ -> routeId=routes.getOrNull(position-1)?.route_id; customerId=null; customer.setText("All customers",false); updateCustomers(); changed() }
                updateCustomers(); changed()
            } catch(error: Exception) {
                if(error is CancellationException)throw error
                Toast.makeText(activity,"Unable to load customer filters",Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun updateCustomers() {
        filtered=customers.filter { routeId==null || it.route_id==routeId }.sortedBy { it.name }
        customer.setAdapter(ArrayAdapter(activity,R.layout.delivery_dropdown_item,listOf("All customers")+filtered.map { it.name.orEmpty() }))
    }
    fun clear() { routeId=null;customerId=null;route.setText("All routes",false);customer.setText("All customers",false);updateCustomers() }
}
