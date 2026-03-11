package com.example.llmosassistant.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ChatRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    fun createNewSession(callback: (String) -> Unit) {

        val userId = getUserId() ?: return

        val sessionId = UUID.randomUUID().toString()

        val session = ChatSession(
            sessionId = sessionId,
            startedAt = System.currentTimeMillis(),
            title = "New Chat"
        )

        firestore.collection("users")
            .document(userId)
            .collection("chat_sessions")
            .document(sessionId)
            .set(session)
            .addOnSuccessListener {
                callback(sessionId)
            }
    }

    fun saveMessage(sessionId: String, message: ChatMessageEntity) {

        val userId = getUserId() ?: return

        firestore.collection("users")
            .document(userId)
            .collection("chat_sessions")
            .document(sessionId)
            .collection("messages")
            .add(message)
    }

    fun loadMessages(
        sessionId: String,
        callback: (List<ChatMessageEntity>) -> Unit
    ) {

        val userId = getUserId() ?: return

        firestore.collection("users")
            .document(userId)
            .collection("chat_sessions")
            .document(sessionId)
            .collection("messages")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener {
                callback(it.toObjects(ChatMessageEntity::class.java))
            }
    }

    // 🔹 Update title when first message arrives
    fun updateSessionTitle(sessionId: String, title: String) {

        val userId = getUserId() ?: return

        firestore.collection("users")
            .document(userId)
            .collection("chat_sessions")
            .document(sessionId)
            .update("title", title)
    }

    fun listenForMessages(
        sessionId: String,
        callback: (List<ChatMessageEntity>) -> Unit
    ) {

        val userId = getUserId() ?: return

        firestore.collection("users")
            .document(userId)
            .collection("chat_sessions")
            .document(sessionId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot != null) {
                    callback(snapshot.toObjects(ChatMessageEntity::class.java))
                }
            }
    }
}