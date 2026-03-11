package com.example.llmosassistant.data

object AssistantMemory {

    var lastTextOutput: String? = null
    var lastYoutubeLink: String? = null

    fun saveText(text: String) {
        lastTextOutput = text
    }

    fun saveYoutube(link: String) {
        lastYoutubeLink = link
    }

    fun getText(): String? {
        return lastTextOutput
    }

    fun getYoutube(): String? {
        return lastYoutubeLink
    }
}