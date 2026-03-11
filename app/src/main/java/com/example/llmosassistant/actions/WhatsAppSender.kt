package com.example.llmosassistant.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppSender {

    fun send(context: Context, phone: String, message: String) {

        val url = "https://wa.me/$phone?text=" +
                URLEncoder.encode(message, "UTF-8")

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)

        context.startActivity(intent)
    }
}