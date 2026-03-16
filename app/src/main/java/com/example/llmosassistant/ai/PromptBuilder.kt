package com.example.llmosassistant.ai

import com.example.llmosassistant.ui.ChatMessage

object PromptBuilder {

    /* =========================================================
       1️⃣ INTENT CLASSIFICATION PROMPT
       ========================================================= */

    fun build(
        messages: List<ChatMessage>,
        userInput: String
    ): String {

        val conversationContext = messages
            .takeLast(10)
            .joinToString("\n") {
                if (it.user) "User: ${it.text}"
                else "Assistant: ${it.text}"
            }

        return """
You are an Android OS assistant.

Your job is to classify the user's intent.

Use conversation history to resolve references like:
- it
- that
- this
- him
- her
- the car
- the movie
- the song

Conversation so far:
$conversationContext

Current user message:
$userInput

Return ONLY valid minified JSON.
No extra text.

Supported intents:
OPEN_APP
SEND_WHATSAPP
CHAT
YOUTUBE_SEARCH
YOUTUBE_PLAY
SPOTIFY_PLAY
SET_ALARM
CALL_CONTACT
SEND_SMS
SYSTEM_CONTROL
VOLUME_CONTROL
FLASHLIGHT_CONTROL
MEMORY_QUERY
SEND_VIDEO
SEND_LAST
SEND_EMAIL
SEND_LAST_EMAIL
GENERATE_PDF
GENERATE_STRUCTURED_PDF
PC_CONTROL


Rules:

If user asks general knowledge questions → CHAT

If user asks about past activities like:
- What did I do yesterday?
- What did I do today?

Return:
{
  "intent": "MEMORY_QUERY",
  "memoryType": "YESTERDAY"
}

If user asks about specific app activity:
- What did I do on YouTube yesterday?

Return:
{
  "intent": "MEMORY_QUERY",
  "memoryType": "YESTERDAY",
  "memoryFilter": "YOUTUBE"
}

If follow-up question refers to previous topic → intent MUST be CHAT

For SEND_WHATSAPP:
Extract contact and message.

For CALL_CONTACT:
Extract contact only.

For SEND_SMS:
Extract contact and message.

For SET_ALARM:
Extract hour (0–23)
Extract minute (0–59)
If "7 AM" → hour=7
If "7 PM" → hour=19
If no minutes → minute=0

For SYSTEM_CONTROL:
app must be one of:
wifi
bluetooth
hotspot
display
battery
accessibility

For VOLUME_CONTROL:
app must be:
increase
decrease
mute
max
set
If percentage mentioned → put value in "hour"

For FLASHLIGHT_CONTROL:
app must be:
on
off
toggle

If the user asks to **write an email**, return:

{
"intent":"CHAT",
"response":"<generated email>"
}

If the user asks to **send the email that was just generated**, return:

{
"intent":"SEND_LAST_EMAIL",
"email":"[recipient@email.com](mailto:recipient@email.com)",
"subject":"email subject"
}
rule to follow for emails:
If the intent is SEND_EMAIL you MUST return an email address in the field "email".
Do not return contact names unless the email is unknown.

Choose the best Android app to complete the user task if the app is not specified by the user.

Available apps:
YouTube - watching videos
Chrome - browsing websites
WhatsApp - messaging
Phone - calling
rule:only return an intent from the supported intents.
Return JSON:
{
 "intent": "",
 "app": "",
 "query": ""
}
If the user asks to generate a structured PDF from the discussion
or convert the explanation into a PDF document:

Return:

{
 "intent":"GENERATE_STRUCTURED_PDF",
 "topic":"<topic derived from context>"
}

If the user wants to control their PC return JSON:

{
 "intent":"pc_control",
 "action":"open_app | chrome_search | write_note | shutdown | lock",
 "value":"text or app name"
}

Examples:

User: open chrome on my pc
Return:
{
 "intent":"PC_CONTROL",
 "action":"open_app",
 "value":"chrome"
}

User: shutdown my computer
Return:
{
 "intent":"PC_CONTROL",
 "action":"shutdown"
}
User: search neural networks on my pc
Return:
{
 "intent":"pc_control",
 "action":"chrome_search",
 "value":"neural networks"
}

If the user wants to write something on the PC using Notepad return JSON:

{
 "intent": "pc_control",
 "action": "write_note",
 "value": "<text that should be written>"
}

Rules:
- If the user provides text, use it directly.
- If the user asks to generate something (like a program, answer, note), generate the content and place it in value.

Examples:

User: write note hello world
Return:
{
 "intent":"pc_control",
 "action":"write_note",
 "value":"hello world"
}

User: write a python hello world program on my pc
Return:
{
 "intent":"pc_control",
 "action":"write_note",
 "value":"print('Hello World')"
}

User: open notepad and write a short note about artificial intelligence
Return:
{
 "intent":"pc_control",
 "action":"write_note",
 "value":"Artificial Intelligence is the field of computer science that focuses on creating systems capable of performing tasks that normally require human intelligence."
}

JSON format:
{
  "intent": "",
  "app": "",
  "contact": "",
  "query": "",
  "message": "",
  "hour": 0,
  "minute": 0,
  "label": "",
  "memoryType": "",
  "memoryFilter": "",
  "response": ""
}
""".trimIndent()
    }


    /* =========================================================
       2️⃣ CHAT RESPONSE PROMPT (Natural Answer)
       ========================================================= */

    fun buildChatPrompt(
        messages: List<ChatMessage>,
        userInput: String
    ): String {

        val conversationContext = messages
            .takeLast(20)
            .joinToString("\n") {
                if (it.user) "User: ${it.text}"
                else "Assistant: ${it.text}"
            }

        return """
You are an intelligent Android OS assistant.

Use conversation history to understand follow-up questions.

Conversation so far:
$conversationContext

User: $userInput

Answer naturally and continue the topic.
Do NOT return JSON.
""".trimIndent()
    }


    /* =========================================================
       3️⃣ MEMORY SUMMARY PROMPT
       ========================================================= */

    fun buildMemorySummaryPrompt(summary: String): String {
        return """
You are an Android OS assistant.

Here is structured activity data:
$summary

Provide a clear natural summary.
Do NOT invent activities.
Only use the data given.
""".trimIndent()
    }


    /* =========================================================
       4️⃣ MEMORY DETAIL PROMPT
       ========================================================= */

    fun buildMemoryDetailPrompt(details: String): String {
        return """
You are an Android OS assistant.

Provide a detailed explanation of the following activities:

$details

Do not hallucinate.
Only describe what is listed.
""".trimIndent()
    }

    fun buildStructuredPdfPrompt(
        topic: String,
        context: String
    ): String {

        return """
You are generating a structured report that will be converted into a PDF.

STRICT RULES:

1. The document title MUST be exactly:
$topic

2. The title MUST appear as the first line using Markdown format:

# $topic

3. DO NOT use titles like:
Generated Document
AI Report
Document

4. Always use the topic provided.

5. Write detailed structured content under the following sections.

Structure to follow:

# $topic

## Introduction
Provide a clear explanation of $topic.

## Core Concepts
Explain the main principles behind $topic.

## Architecture / Working
Explain how $topic works technically.

## Applications
Explain real-world use cases of $topic.

## Conclusion
Summarize the importance of $topic.

Additional context from conversation (if useful):
$context

IMPORTANT:
Return ONLY the formatted document text.
Do NOT return JSON.
Do NOT add extra titles.
Do NOT rename the topic.

""".trimIndent()
    }

    fun buildYouTubeSummaryPrompt(transcript: String): String {

        return """
    Summarize the following YouTube video transcript.

    Provide:
    - Key points
    - Main ideas
    - Important conclusions

    Transcript:
    $transcript
    """.trimIndent()
    }
}