package com.example.llmosassistant.youtube

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class YouTubeTranscriptClient {

    private val client = OkHttpClient()

    fun getTranscript(videoId: String, callback: (String?) -> Unit) {

        val url =
            "https://youtubetranscript.p.rapidapi.com/?video_id=$videoId"

        val request = Request.Builder()
            .url(url)
            .addHeader("X-RapidAPI-Key", "YOUR_RAPIDAPI_KEY")
            .addHeader("X-RapidAPI-Host", "youtubetranscript.p.rapidapi.com")
            .build()

        Thread {

            try {

                val response = client.newCall(request).execute()

                val body = response.body?.string()

                val json = JSONObject(body ?: "")

                val transcriptArray = json.getJSONArray("transcript")

                val builder = StringBuilder()

                for (i in 0 until transcriptArray.length()) {

                    val line = transcriptArray
                        .getJSONObject(i)
                        .getString("text")

                    builder.append(line).append(" ")
                }

                callback(builder.toString())

            } catch (e: Exception) {

                callback(null)
            }

        }.start()
    }
}