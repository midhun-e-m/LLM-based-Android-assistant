package com.example.llmosassistant.pccontrol

data class PcCommand(
    val action: String = "",
    val value: String = "",
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)