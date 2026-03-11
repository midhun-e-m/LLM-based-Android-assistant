package com.example.llmosassistant.ai

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class ImageGenerationClient {

    private val client = OkHttpClient()

    private val apiKey = "sk-uKQfzj6Op4BR7Mu7oMWddnGMUEWlWztaH2bBTdN9MgYD4c4d"

    fun generateImage(prompt: String, callback: (String?) -> Unit) {

        val json = """
        {
          "text_prompts":[{"text":"$prompt"}],
          "cfg_scale":7,
          "height":512,
          "width":512,
          "samples":1,
          "steps":30
        }
        """.trimIndent()

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json
        )

        val request = Request.Builder()
            .url("https://api.stability.ai/v1/generation/stable-diffusion-v1-6/text-to-image")
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string()

                if (body == null) {
                    callback(null)
                    return
                }

                try {

                    val json = JSONObject(body)

                    val artifacts = json.getJSONArray("artifacts")

                    val base64 = artifacts.getJSONObject(0).getString("base64")

                    val imageUrl = "data:image/png;base64,$base64"

                    callback(imageUrl)

                } catch (e: Exception) {

                    callback(null)

                }
            }
        })
    }
}