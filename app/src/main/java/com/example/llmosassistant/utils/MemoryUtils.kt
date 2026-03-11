package com.example.llmosassistant.utils

import com.example.llmosassistant.data.ActivityLog

fun buildMemorySummary(logs: List<ActivityLog>): String {

    val grouped = logs.groupBy { it.type }

    val builder = StringBuilder()

    grouped.forEach { (type, items) ->

        val count = items.size

        when (type) {

            "YOUTUBE_PLAY" ->
                builder.append("You played $count YouTube video(s).\n")

            "WHATSAPP_MESSAGE" ->
                builder.append("You sent $count WhatsApp message(s).\n")

            "CALL" ->
                builder.append("You made $count call(s).\n")

            "SPOTIFY_PLAY" ->
                builder.append("You played $count Spotify song(s).\n")

            "FLASHLIGHT" ->
                builder.append("You used the flashlight $count time(s).\n")

            "VOLUME_CONTROL" ->
                builder.append("You adjusted volume $count time(s).\n")

            "SYSTEM_CONTROL" ->
                builder.append("You opened $count system setting(s).\n")

            "OPEN_APP" ->
                builder.append("You opened $count app(s).\n")

            else ->
                builder.append("You performed $count action(s) of type $type.\n")
        }
    }

    return builder.toString()
}

