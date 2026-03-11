package com.example.llmosassistant.ai

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import com.example.llmosassistant.BuildConfig
import com.example.llmosassistant.data.AssistantMemory

class LLMClient {

    private val client = OkHttpClient()
    private val gson = Gson()

    private val apiKey= BuildConfig.GROQ_API_KEY

    private val url = "https://api.groq.com/openai/v1/chat/completions"
    private val model = "llama-3.3-70b-versatile"

    fun process(prompt: String, callback: (IntentResult) -> Unit) {

        val requestBody = """
        {
        "model": "$model",
        "messages": [
            { "role": "system", "content": "You are an Android OS assistant." },
            { "role": "user", "content": ${gson.toJson(prompt)} }
        ],
        "temperature": 0
        }
    """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(
                RequestBody.create(
                    "application/json".toMediaType(),
                    requestBody
                )
            )
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback(
                    IntentResult(
                        intent = "CHAT",
                        response = "Network error"
                    )
                )
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string()
                //debug
                println("HTTP CODE: ${response.code}")
                println("LLM RAW BODY: $body")

                if (!response.isSuccessful || body == null) {
                    callback(
                        IntentResult(
                            intent = "CHAT",
                            response = "LLM error"
                        )
                    )
                    return
                }

                try {
                    val root = gson.fromJson(body, Map::class.java)

                    val content =
                        ((root["choices"] as List<*>)[0] as Map<*, *>)
                            .let { it["message"] as Map<*, *> }
                            .let { it["content"] as String }

                    // 🔐 SAFETY: ensure it's JSON
                    if (!content.trim().startsWith("{")) {

                        AssistantMemory.saveText(content)

                        callback(
                            IntentResult(
                                intent = "CHAT",
                                response = content

                            )
                        )
                        return
                    }

                    val result =
                        gson.fromJson(content, IntentResult::class.java)

                    // save response to assistant memory
                    if(result.response != null){
                        AssistantMemory.saveText(result.response)
                    }

                    callback(result)

                } catch (e: Exception) {
                    callback(
                        IntentResult(
                            intent = "CHAT",
                            response = "Could not parse command"
                        )
                    )
                }
            }
        })
    }
}
