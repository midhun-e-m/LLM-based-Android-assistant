package com.example.llmosassistant.data

data class ActivityLog(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)