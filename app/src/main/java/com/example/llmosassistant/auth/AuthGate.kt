package com.example.llmosassistant.auth

import com.google.firebase.auth.FirebaseAuth

object AuthGate {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun userId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User not logged in")
    }

    fun logout() {
        auth.signOut()
    }
}
