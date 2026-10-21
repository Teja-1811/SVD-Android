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
        // Discover all exported UPI handlers, including apps that do not mark
        // their payment activity as a default handler. Launch remains explicit.
        val candidates = mutableListOf<Pair<android.content.pm.ResolveInfo, Intent>>()
        fun discover(probe: Intent) {
            manager.queryIntentActivities(probe, 0).forEach { candidates.add(it to probe) }
        }
        discover(intent)
        val knownPackages = listOf("com.google.android.apps.nbu.paisa.user", "com.phonepe.app",
            "net.one97.paytm", "in.org.npci.upiapp", "com.samsung.android.spay")
        knownPackages.forEach { discover(Intent(intent).setPackage(it)) }
        // Keep the exact URI that each app declares it can handle.
        listOf(
            Triple("com.google.android.apps.nbu.paisa.user", "gpay", "upi"),
            Triple("com.phonepe.app", "phonepe", "pay"),
            Triple("net.one97.paytm", "paytmmp", "pay")
        ).forEach { (packageName, scheme, host) ->
            val appUri = uri.buildUpon().scheme(scheme).authority(host)
                .path(if (scheme == "gpay") "/pay" else "").build()
            discover(Intent(Intent.ACTION_VIEW, appUri).setPackage(packageName))
        }
        val activities = candidates
            .filter { (result, _) ->
                val info = result.activityInfo
                val packageName = info.packageName.lowercase(Locale.ROOT)
                val label = result.loadLabel(manager).toString().lowercase(Locale.ROOT)
                info.enabled && info.applicationInfo.enabled && info.exported &&
                    !packageName.contains("whatsapp") && !label.contains("whatsapp")
            }
            .distinctBy { it.first.activityInfo.packageName }
            .sortedBy { it.first.loadLabel(manager).toString() }
        if (activities.isEmpty()) {
            Toast.makeText(context, "Install Google Pay, PhonePe, Paytm or another supported UPI app. WhatsApp payments are unavailable.", Toast.LENGTH_LONG).show()
            onCancelled()
            return
        }
        var launched = false
        fun launch(index: Int) {
            if (launched) return
            val (resolved, supportedIntent) = activities[index]
            val activity = resolved.activityInfo
            // Recheck at the launch boundary as well as when presenting the list.
            if (activity.packageName.lowercase(Locale.ROOT).contains("whatsapp")) return
            try {
                val selected = Intent(supportedIntent).setComponent(ComponentName(activity.packageName, activity.name))
                if (context !is android.app.Activity) selected.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(selected)
                launched = true
                onLaunched()
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(context, "This UPI app is unavailable. Choose another payment app.", Toast.LENGTH_LONG).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, "Could not open this UPI app. Choose another payment app.", Toast.LENGTH_LONG).show()
            }
        }
        val adapter = object : android.widget.BaseAdapter() {
            override fun getCount() = activities.size
            override fun getItem(position: Int) = activities[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val density = context.resources.displayMetrics.density
                fun dp(value: Int) = (value * density).toInt()
                val row = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(20), dp(12), dp(20), dp(12))
                    minimumHeight = dp(68)
                }
                val app = activities[position].first
                row.addView(android.widget.ImageView(context).apply {
                    setImageDrawable(app.loadIcon(manager))
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                }, android.widget.LinearLayout.LayoutParams(dp(40), dp(40)))
                row.addView(android.widget.TextView(context).apply {
                    text = app.loadLabel(manager)
                    textSize = 16f
                    setTextColor(androidx.core.content.ContextCompat.getColor(context, com.svd.svdagencies.R.color.textColorPrimary))
                }, android.widget.LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(16) })
                return row
            }
        }
        WhiteMaterialAlertDialogBuilder(context).setTitle("Choose a UPI payment app")
            .setAdapter(adapter) { _, index -> launch(index) }
            .setNegativeButton("Cancel", null)
            .setOnDismissListener { if (!launched) onCancelled() }.show()
    }
}
