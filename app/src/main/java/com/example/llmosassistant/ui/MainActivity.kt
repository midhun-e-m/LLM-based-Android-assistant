package com.example.llmosassistant.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.llmosassistant.R
import com.example.llmosassistant.auth.AuthGate
import com.example.llmosassistant.utils.SessionManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {

        if (intent?.action == Intent.ACTION_ASSIST) {
            setTheme(R.style.Theme_LLMOSAssistant_Assist)
        } else {
            setTheme(R.style.Theme_LLMOSAssistant)
        }

        super.onCreate(savedInstanceState)

        if (!AuthGate.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        if (savedInstanceState == null) {

            val existingSession = SessionManager.getSession(this)

            val fragment =
                if (existingSession != null) {
                    ChatFragment.newSession(existingSession)
                } else {
                    ChatFragment()
                }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }

        loadChatHistory()

        navigationView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_new_chat -> {

                    SessionManager.clearSession(this)

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ChatFragment())
                        .commit()

                }

                R.id.nav_settings -> {

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, SettingsFragment())
                        .commit()

                }

                R.id.nav_logout -> {

                    AuthGate.logout()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()

                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun loadChatHistory() {

        val uid = FirebaseAuth.getInstance().uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("chat_sessions")
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->

                snapshot ?: return@addSnapshotListener

                val menu = navigationView.menu
                val groupId = R.id.chat_history

                menu.removeGroup(groupId)

                snapshot.documents.forEach { doc ->

                    val title = doc.getString("title") ?: "New Chat"
                    val sessionId = doc.id

                    val item = menu.add(groupId, Menu.NONE, Menu.NONE, title)

                    item.setOnMenuItemClickListener {

                        SessionManager.saveSession(this, sessionId)

                        val fragment = ChatFragment.newSession(sessionId)

                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .commit()

                        drawerLayout.closeDrawer(GravityCompat.START)

                        true
                    }
                }
            }
    }


}
