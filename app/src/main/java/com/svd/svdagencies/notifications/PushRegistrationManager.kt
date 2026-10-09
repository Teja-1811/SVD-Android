package com.svd.svdagencies.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.svd.svdagencies.BuildConfig
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.PushDeviceRegisterRequest
import com.svd.svdagencies.data.model.PushDeviceResponse
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object PushRegistrationManager {
    private const val TAG = "PushRegistration"
    const val NOTIFICATION_PERMISSION_REQUEST = 4201

    fun requestPermissionIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST
            )
        }
    }

    fun registerCurrentDevice(context: Context) {
        val session = SessionManager(context)
        if (session.getToken().isNullOrBlank()) {
            Log.d(TAG, "Skipping push registration: no active token.")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken ->
                if (fcmToken.isNullOrBlank()) {
                    Log.w(TAG, "Firebase returned an empty token.")
                    return@addOnSuccessListener
                }
                registerToken(context, fcmToken)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Unable to get Firebase token.", error)
            }
    }

    private fun registerToken(context: Context, fcmToken: String) {
        val request = PushDeviceRegisterRequest(
            token = fcmToken,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            appVersion = BuildConfig.VERSION_NAME,
            userAgent = "SVD Android ${BuildConfig.VERSION_NAME}; SDK ${Build.VERSION.SDK_INT}; ${Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)}"
        )

        ApiClient.pushApi.registerDevice(request).enqueue(object : Callback<PushDeviceResponse> {
            override fun onResponse(
                call: Call<PushDeviceResponse>,
                response: Response<PushDeviceResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "Push device registered.")
                } else {
                    Log.w(TAG, "Push registration failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<PushDeviceResponse>, t: Throwable) {
                Log.w(TAG, "Push registration request failed.", t)
            }
        })
    }
}
