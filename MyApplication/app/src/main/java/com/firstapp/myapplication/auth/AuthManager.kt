package com.firstapp.myapplication.auth

import android.content.Context

object AuthManager {
    private const val PREFS = "progr3ss_prefs"
    private const val KEY_AT = "access_token"
    private const val KEY_RT = "refresh_token"

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var accessToken: String?
        get() = prefs?.getString(KEY_AT, null)
        private set(value) {
            prefs?.edit()?.putString(KEY_AT, value)?.apply()
        }

    var refreshToken: String?
        get() = prefs?.getString(KEY_RT, null)
        private set(value) {
            prefs?.edit()?.putString(KEY_RT, value)?.apply()
        }

    fun save(tokensAccess: String?, tokensRefresh: String?) {
        accessToken = tokensAccess
        refreshToken = tokensRefresh
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}
