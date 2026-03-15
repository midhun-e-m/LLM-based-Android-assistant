package com.example.llmosassistant.ai

data class IntentResult(
    val intent: String,

    // App-related (OPEN_APP)
    val app: String? = null,

    // WhatsApp-related (SEND_WHATSAPP)
    val contact: String? = null,
    val message: String? = null,

    // YouTube-related (SEARCH / PLAY)
    val query: String? = null,


    //mail
    val email: String? = null,
    val subject: String? = null,

    //ALARM
    val hour: Int? = null,
    val minute: Int? = null,
    val label: String? = null,

    // Assistant reply
    val memoryType: String? = null,
    val memoryFilter: String? = null,

    val topic: String? =  null,

    //PC control

    val action:String?=null,
    val value:String?=null,

    val response: String
)
