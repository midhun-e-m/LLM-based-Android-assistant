package com.example.llmosassistant.data

data class ChatSession(
    val sessionId: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val title: String = "New Chat"
)