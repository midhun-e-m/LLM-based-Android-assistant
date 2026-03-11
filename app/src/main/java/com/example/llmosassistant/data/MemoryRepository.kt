package com.example.llmosassistant.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MemoryRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    fun logActivity(
        type: String,
        title: String,
        description: String
    ) {
        val userId = getUserId() ?: return

        val log = ActivityLog(
            type = type,
            title = title,
            description = description,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(userId)
            .collection("activity_logs")
            .add(log)
    }

    fun getYesterdayActivities(
        callback: (List<ActivityLog>) -> Unit
    ) {
        val userId = getUserId() ?: return

        val calendar = Calendar.getInstance()

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)

        val end = calendar.timeInMillis

        firestore.collection("users")
            .document(userId)
            .collection("activity_logs")
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .get()
            .addOnSuccessListener { result ->
                val logs = result.toObjects(ActivityLog::class.java)
                callback(logs)
            }
    }
    fun getTodayActivities(
        callback: (List<ActivityLog>) -> Unit
    ) {

        val userId = getUserId() ?: return

        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()

        firestore.collection("users")
            .document(userId)
            .collection("activity_logs")
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .get()
            .addOnSuccessListener { result ->
                val logs = result.toObjects(ActivityLog::class.java)
                callback(logs)
            }
    }
}