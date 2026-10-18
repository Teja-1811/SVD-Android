package com.svd.svdagencies.ui.admin.routes

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.AdminRoute
import com.svd.svdagencies.data.api.admin.AdminRoutesResponse
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminRoutesActivity : AdminBaseActivity() {
    private lateinit var routesList: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_routes_dashboard)
        setupAdminLayout("Routes")
        swipeRefresh = findViewById(R.id.swipeRefresh)
        routesList = findViewById(R.id.routesList)
        findViewById<MaterialButton>(R.id.btnAddRoute).setOnClickListener { showRouteDialog() }
        swipeRefresh.setOnRefreshListener { loadRoutes() }
        loadRoutes()
    }

    override fun onResume() {
        super.onResume()
        loadRoutes()
    }

    private fun loadRoutes() {
        ApiClient.adminRoutesApi.listRoutes().enqueue(object : Callback<AdminRoutesResponse> {
            override fun onResponse(call: Call<AdminRoutesResponse>, response: Response<AdminRoutesResponse>) {
                swipeRefresh.isRefreshing = false
                if (!response.isSuccessful) return showMessage("Unable to load routes")
                routesList.removeAllViews()
                response.body()?.routes.orEmpty().forEach(::addRouteRow)
            }
            override fun onFailure(call: Call<AdminRoutesResponse>, error: Throwable) { swipeRefresh.isRefreshing = false; showMessage("Unable to load routes") }
        })
    }

    private fun addRouteRow(route: AdminRoute) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 18, 0, 18) }
        row.addView(TextView(this).apply { text = "${route.name} (${route.customer_count} customers)" }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply { text = "Edit"; setOnClickListener { showRouteDialog(route) } })
        row.addView(Button(this).apply { text = "Delete"; setOnClickListener { deleteRoute(route) } })
        routesList.addView(row, ViewGroup.LayoutParams(-1, -2))
    }

    private fun showRouteDialog(existing: AdminRoute? = null) {
        val form = layoutInflater.inflate(R.layout.admin_dialog_route_form, null)
        val input = form.findViewById<EditText>(R.id.etRouteName).apply {
            setText(existing?.name.orEmpty())
        }
        WhiteMaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) "Add route" else "Edit route")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> saveRoute(existing, input.text.toString()) }
            .show()
    }

    private fun saveRoute(existing: AdminRoute?, name: String) {
        val body = mapOf("name" to name)
        val request = if (existing == null) ApiClient.adminRoutesApi.addRoute(body) else ApiClient.adminRoutesApi.updateRoute(existing.id, body)
        request.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) loadRoutes() else showMessage("Unable to save route")
            }
            override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) = showMessage("Unable to save route")
        })
    }

    private fun deleteRoute(route: AdminRoute) {
        WhiteMaterialAlertDialogBuilder(this).setMessage("Delete ${route.name}?").setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                ApiClient.adminRoutesApi.deleteRoute(route.id).enqueue(object : Callback<Map<String, Any>> {
                    override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) { if (response.isSuccessful) loadRoutes() else showMessage("Unable to delete route") }
                    override fun onFailure(call: Call<Map<String, Any>>, error: Throwable) = showMessage("Unable to delete route")
                })
            }.show()
    }

    private fun showMessage(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
