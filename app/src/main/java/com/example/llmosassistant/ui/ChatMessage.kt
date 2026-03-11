package com.example.llmosassistant.ui

import java.io.File

data class ChatMessage(
    val text: String?=null,
    val user: Boolean,
    val imageUrl: String?=null,
    val pdfFile: File? = null,
    val type: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT,
    BULLET_LIST,
    SECTION,
    ERROR
}
