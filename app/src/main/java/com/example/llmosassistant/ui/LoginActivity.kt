package com.example.llmosassistant.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.llmosassistant.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient

    private val RC_GOOGLE_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // -------- Loading UI --------
        val loadingOverlay = findViewById<FrameLayout>(R.id.loadingOverlay)
        val loadingSpinner = findViewById<ProgressBar>(R.id.loadingSpinner)

        // Tint spinner (British green)
        loadingSpinner.indeterminateDrawable.setTint(0xFF0A6E4F.toInt())

        fun showLoading() {
            loadingOverlay.visibility = View.VISIBLE
        }

        fun hideLoading() {
            loadingOverlay.visibility = View.GONE
        }

        // ---------- Email / Password ----------
        val email = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)

        loginBtn.setOnClickListener {
            val e = email.text.toString().trim()
            val p = password.text.toString().trim()

            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading() // ✅ added

            auth.signInWithEmailAndPassword(e, p)
                .addOnSuccessListener {
                    goToMain()
                }
                .addOnFailureListener {
                    auth.createUserWithEmailAndPassword(e, p)
                        .addOnSuccessListener {
                            goToMain()
                        }
                        .addOnFailureListener { err ->
                            hideLoading() // ✅ added
                            Toast.makeText(this, err.message, Toast.LENGTH_LONG).show()
                        }
                }
        }

        // ---------- Google Sign-In ----------
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleClient = GoogleSignIn.getClient(this, gso)

        findViewById<SignInButton>(R.id.googleSignInBtn).apply {
            setSize(SignInButton.SIZE_WIDE)
            setOnClickListener {
                showLoading() // ✅ added
                startActivityForResult(
                    googleClient.signInIntent,
                    RC_GOOGLE_SIGN_IN
                )
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential =
                    GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        goToMain()
                    }
                    .addOnFailureListener {
                        findViewById<FrameLayout>(R.id.loadingOverlay).visibility = View.GONE
                        Toast.makeText(
                            this,
                            it.message ?: "Google sign-in failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } catch (e: ApiException) {
                findViewById<FrameLayout>(R.id.loadingOverlay).visibility = View.GONE
                Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
