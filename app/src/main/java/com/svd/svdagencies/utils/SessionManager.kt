package com.svd.svdagencies.utils

import android.content.Context
import android.util.Log
import com.svd.svdagencies.data.api.auth.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SessionManager(private val context: Context) {

    private val prefs =
        context.getSharedPreferences("svd_session", Context.MODE_PRIVATE)

    fun saveSession(token: String, role: String, userId: Int) {
        synchronized(sessionLock) {
            Log.d("SessionManager", "Saving signed-in account")
            prefs.edit()
                .putString("token", token)
                .putString("role", role)
                .putInt("user_id", userId)
                .putBoolean("notification_permission_pending", true)
                .apply()
            if (role == UserRole.ADMIN) KeepAliveWorker.startNow(context)
        }
    }

    fun getToken(): String? {
        val token = prefs.getString("token", null)
        return token
    }

    fun getRole(): String? = prefs.getString("role", null)

    fun getUserId(): Int = prefs.getInt("user_id", -1)

    fun logout() {
        val token = getToken()
        if (!token.isNullOrBlank() && invalidateIfTokenMatches(token)) {
            // This uses the captured login token, so a delayed logout cannot
            // revoke a newer login on the same phone.
            ApiClient.authApi.logout("Token $token").enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) = Unit
                override fun onFailure(call: Call<Void>, error: Throwable) = Unit
            })
        } else if (token.isNullOrBlank()) {
            synchronized(sessionLock) {
                if (getToken().isNullOrBlank()) {
                    KeepAliveWorker.stop(context)
                    prefs.edit().clear().apply()
                }
            }
        }
    }

    fun invalidateIfTokenMatches(expected: String): Boolean = synchronized(sessionLock) {
        if (getToken() != expected) return@synchronized false
        KeepAliveWorker.stop(context)
        prefs.edit().clear().apply()
        true
    }

    fun consumeNotificationPermissionPrompt(): Boolean {
        if (!prefs.getBoolean("notification_permission_pending", false)) return false
        prefs.edit().putBoolean("notification_permission_pending", false).apply()
        return true
    }

    companion object {
        private val sessionLock = Any()
    }

    fun saveSelectedRoute(id: Int?, name: String?) {
        prefs.edit()
            .putInt("selected_route_id", id ?: -1)
            .putString("selected_route_name", name)
            .apply()
    }

    fun getSelectedRouteId(): Int? {
        val id = prefs.getInt("selected_route_id", -1)
        return if (id == -1) null else id
    }

    fun getSelectedRouteName(): String? = prefs.getString("selected_route_name", null)
}
