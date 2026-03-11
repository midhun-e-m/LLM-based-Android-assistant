package com.example.llmosassistant.data

data class ChatMessageEntity(
    val text: String = "",
    val user: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)