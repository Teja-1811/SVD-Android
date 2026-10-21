package com.svd.svdagencies.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import java.util.Locale

/** Launch only the selected payment activity; never fall back to a generic chooser. */
object UpiPaymentLauncher {
    fun open(context: Context, rawUri: String, onCancelled: () -> Unit = {}, onLaunched: () -> Unit = {}) {
        val original = Uri.parse(rawUri)
        if (original.scheme != "upi" || original.host != "pay") {
            Toast.makeText(context, "Could not prepare UPI payment details.", Toast.LENGTH_LONG).show()
            onCancelled()
            return
        }
        val uri = original.buildUpon().clearQuery().apply {
            original.queryParameterNames.filter { it != "pa" && it != "pn" }.forEach { key ->
                original.getQueryParameters(key).forEach { value -> appendQueryParameter(key, value) }
            }
            appendQueryParameter("pa", PaymentConfig.UPI_ID)
            appendQueryParameter("pn", PaymentConfig.PAYEE_NAME)
        }.build()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val manager = context.packageManager
        val activities = manager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { result ->
                val info = result.activityInfo
                val packageName = info.packageName.lowercase(Locale.ROOT)
                val label = result.loadLabel(manager).toString().lowercase(Locale.ROOT)
                info.enabled && info.exported && !packageName.contains("whatsapp") && !label.contains("whatsapp")
            }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(manager).toString() }
        if (activities.isEmpty()) {
            Toast.makeText(context, "Install Google Pay, PhonePe, Paytm or another supported UPI app. WhatsApp payments are unavailable.", Toast.LENGTH_LONG).show()
            onCancelled()
            return
        }
        var launched = false
        fun launch(index: Int) {
            if (launched) return
            val activity = activities[index].activityInfo
            // Recheck at the launch boundary as well as when presenting the list.
            if (activity.packageName.lowercase(Locale.ROOT).contains("whatsapp")) return
            try {
                context.startActivity(Intent(intent).setComponent(ComponentName(activity.packageName, activity.name)))
                launched = true
                onLaunched()
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(context, "This UPI app is unavailable. Choose another payment app.", Toast.LENGTH_LONG).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, "Could not open this UPI app. Choose another payment app.", Toast.LENGTH_LONG).show()
            }
        }
        WhiteMaterialAlertDialogBuilder(context).setTitle("Choose a UPI payment app")
            .setItems(activities.map { it.loadLabel(manager).toString() }.toTypedArray()) { _, index -> launch(index) }
            .setNegativeButton("Cancel", null)
            .setOnDismissListener { if (!launched) onCancelled() }.show()
    }
}
