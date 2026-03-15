package com.example.llmosassistant.utils

import android.content.Context

object SessionManager {

    private const val PREF = "assistant_session"
    private const val KEY_SESSION = "current_session"

    fun saveSession(context: Context, sessionId: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSION, sessionId)
            .apply()
    }

    fun getSession(context: Context): String? {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_SESSION, null)
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SESSION)
            .apply()
    }
}