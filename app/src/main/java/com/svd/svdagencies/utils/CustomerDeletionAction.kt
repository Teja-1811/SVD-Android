package com.svd.svdagencies.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import com.svd.svdagencies.data.api.auth.ApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

object CustomerDeletionAction {
    fun confirm(activity: AppCompatActivity, id: Int, name: String, onDeleted: () -> Unit) {
        WhiteMaterialAlertDialogBuilder(activity)
            .setTitle("Delete $name?")
            .setMessage("Bills and payments will be transferred to your admin account. The customer account will be deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                activity.lifecycleScope.launch {
                    try {
                        val result = ApiClient.adminCustomerDashboard.deleteCustomer(id, mapOf("confirm_customer_id" to id))
                        if (result.get("success")?.asBoolean == true) onDeleted()
                        else Toast.makeText(activity, result.get("message")?.asString ?: "Unable to delete customer", Toast.LENGTH_LONG).show()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        Toast.makeText(activity, NetworkMessageUtils.friendlyMessage(e, "Unable to delete customer"), Toast.LENGTH_LONG).show()
                    }
                }
            }.show()
    }
}
