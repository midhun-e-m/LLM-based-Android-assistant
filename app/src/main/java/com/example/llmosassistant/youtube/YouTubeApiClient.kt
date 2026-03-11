package com.example.llmosassistant.youtube

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import com.example.llmosassistant.BuildConfig
import com.example.llmosassistant.data.AssistantMemory

class YouTubeApiClient {

    private val client = OkHttpClient()

    private val apiKey=BuildConfig.YOUTUBE_API_KEY

    fun getFirstVideo(
        query: String,
        callback: (YouTubeVideo?) -> Unit
    ) {

        val url =
            "https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet" +
                    "&type=video" +
                    "&maxResults=1" +
                    "&q=${query.replace(" ", "+")}" +
                    "&key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string() ?: run {
                    callback(null)
                    return
                }

                try {
                    val json = JSONObject(body)
                    val items = json.getJSONArray("items")

                    if (items.length() == 0) {
                        callback(null)
                        return
                    }

                    val firstItem = items.getJSONObject(0)

                    val videoId =
                        firstItem.getJSONObject("id")
                            .getString("videoId")

                    val snippet = firstItem.getJSONObject("snippet")

                    val title = snippet.getString("title")
                    val channel = snippet.getString("channelTitle")
                    val description = snippet.getString("description")
                    val videoUrl = "https://www.youtube.com/watch?v=$videoId"

                    AssistantMemory.saveYoutube(videoUrl)

                    val video = YouTubeVideo(
                        id = videoId,
                        title = title,
                        channel = channel,
                        description = description,
                        link=videoUrl
                    )

                    callback(video)

                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }
}
