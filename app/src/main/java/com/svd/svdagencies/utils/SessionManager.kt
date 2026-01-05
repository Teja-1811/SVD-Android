package com.svd.svdagencies.utils

import android.content.Context
import android.util.Log

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("svd_session", Context.MODE_PRIVATE)

    fun saveSession(token: String, role: String, userId: Int) {
        Log.d("SessionManager", "Saving session: Token=$token, Role=$role, User=$userId")
        prefs.edit()
            .putString("token", token)
            .putString("role", role)
            .putInt("user_id", userId)
            .apply()
    }

    fun getToken(): String? {
        val token = prefs.getString("token", null)
        Log.d("SessionManager", "getToken: $token")
        return token
    }

    fun getRole(): String? = prefs.getString("role", null)

    fun getUserId(): Int = prefs.getInt("user_id", -1)

    fun logout() {
        Log.d("SessionManager", "Clearing session")
        prefs.edit().clear().apply()
    }
}
