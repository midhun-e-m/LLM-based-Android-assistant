package com.example.llmosassistant.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.llmosassistant.R
import com.example.llmosassistant.actions.ActionExecutor
import com.example.llmosassistant.ai.LLMClient
import com.example.llmosassistant.ai.PromptBuilder
import com.example.llmosassistant.data.ActivityLog
import com.example.llmosassistant.data.ChatMessageEntity
import com.example.llmosassistant.data.ChatRepository
import com.example.llmosassistant.data.MemoryRepository
import com.example.llmosassistant.utils.buildMemorySummary
import com.example.llmosassistant.voice.VoiceInputManager
import com.example.llmosassistant.youtube.YouTubeApiClient
import com.example.llmosassistant.ai.ImageGenerationClient
import com.example.llmosassistant.utils.SessionManager
import com.example.llmosassistant.youtube.YouTubeTranscriptClient;

class ChatFragment : Fragment() {

    companion object {

        fun newSession(sessionId: String): ChatFragment {

            val fragment = ChatFragment()

            val args = Bundle()
            args.putString("sessionId", sessionId)

            fragment.arguments = args

            return fragment
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter

    private val messages = mutableListOf<ChatMessage>()

    private val chatRepository = ChatRepository()
    private val llmClient = LLMClient()
    private val youtubeApiClient = YouTubeApiClient()
    private val memoryRepository = MemoryRepository()

    private lateinit var actionExecutor: ActionExecutor
    private lateinit var voiceManager: VoiceInputManager

    private var currentSessionId: String? = null
    private var lastDetailedLogs: List<ActivityLog>? = null
    private var lastIntentWasMemoryQuery = false

    private var lastYoutubeVideoId: String? = null

    private val contactPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.chatRecyclerView)
        val inputField = view.findViewById<EditText>(R.id.messageInput)
        val sendButton = view.findViewById<ImageButton>(R.id.sendButton)
        val micButton = view.findViewById<ImageButton>(R.id.micButton)

        val suggestionContainer = view.findViewById<View>(R.id.suggestionContainer)

        fun startChat() {
            suggestionContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        currentSessionId = arguments?.getString("sessionId")

        currentSessionId?.let {
            SessionManager.saveSession(requireContext(), it)
        }

        if (currentSessionId == null) {

            chatRepository.createNewSession { sessionId ->

                currentSessionId = sessionId

                SessionManager.saveSession(
                    requireContext(),
                    sessionId
                )

                chatRepository.listenForMessages(sessionId) { history ->

                    requireActivity().runOnUiThread {

                        startChat()

                        messages.clear()

                        history.forEach { msg ->
                            messages.add(
                                ChatMessage(
                                    text = msg.text,
                                    user = msg.user
                                )
                            )
                        }

                        adapter.notifyDataSetChanged()
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }

        } else {

            chatRepository.listenForMessages(currentSessionId!!) { history ->

                requireActivity().runOnUiThread {

                    startChat()

                    messages.clear()

                    history.forEach { msg ->

                        if (msg.user) {

                            messages.add(
                                ChatMessage(
                                    text = msg.text,
                                    user = true
                                )
                            )

                        } else {

                            messages.add(
                                ChatMessage(
                                    text = msg.text,
                                    user = false
                                )
                            )
                        }
                    }

                    adapter.notifyDataSetChanged()

                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }



        actionExecutor = ActionExecutor(requireContext())

        voiceManager = VoiceInputManager(requireActivity()) {
            handleUserInput(it)
        }

        sendButton.setOnClickListener {
            it.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(80)
                .withEndAction {

                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .duration = 80

                }
            val text = inputField.text.toString()
            if (text.isNotBlank()) {

                suggestionContainer.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                inputField.text.clear()
                handleUserInput(text)
            }
        }

        micButton.setOnClickListener {
            voiceManager.startListening()
        }
    }

    // =========================================================
    // MAIN INPUT HANDLER
    // =========================================================

    private fun handleUserInput(input: String) {

        addUserMessage(input)

        val cleaned = input.trim().lowercase()

        // ================= MEMORY FOLLOW-UP =================

        if (lastIntentWasMemoryQuery) {
            val isFollowUp =
                cleaned.contains("tell me more") ||
                        cleaned.contains("expand") ||
                        cleaned.contains("details")

            if (isFollowUp) {
                lastDetailedLogs?.let { logs ->
                    if (logs.isEmpty()) {
                        streamAssistantMessage("There are no additional details available.")
                        return
                    }

                    val detailedText = logs.joinToString("\n") {
                        "• ${it.title}: ${it.description}"
                    }

                    streamAssistantMessage(
                        "Here are the detailed activities:\n\n$detailedText"
                    )
                    return
                }
            }
        }
        if (cleaned.contains("summarize") && cleaned.contains("video")) {

            val videoId = lastYoutubeVideoId

            if (videoId == null) {

                streamAssistantMessage("No video found to summarize.")
                return
            }

            streamAssistantMessage("Analyzing the video...")

            val transcriptClient = YouTubeTranscriptClient()

            transcriptClient.getTranscript(videoId) { transcript ->

                if (transcript == null) {

                    requireActivity().runOnUiThread {
                        streamAssistantMessage("Could not fetch video transcript.")
                    }

                    return@getTranscript
                }

                val prompt =
                    PromptBuilder.buildYouTubeSummaryPrompt(transcript)

                llmClient.process(prompt) { result ->

                    requireActivity().runOnUiThread {

                        result.response?.let {

                            streamAssistantMessage(it)

                        } ?: streamAssistantMessage("Failed to generate summary.")
                    }
                }
            }

            return
        }

        // ================= LOCAL MEMORY QUERY =================

        val isToday = cleaned.contains("today")
        val isYesterday = cleaned.contains("yesterday")

        val isActivityQuestion =
            cleaned.contains("what did i") ||
                    cleaned.contains("what videos did i") ||
                    cleaned.contains("what messages did i")

        if (isActivityQuestion && (isToday || isYesterday)) {

            val memoryType = if (isYesterday) "YESTERDAY" else "TODAY"

            handleMemoryQuery(
                memoryType = memoryType,
                filter = when {
                    cleaned.contains("youtube") -> "YOUTUBE_PLAY"
                    cleaned.contains("whatsapp") -> "WHATSAPP_MESSAGE"
                    cleaned.contains("spotify") -> "SPOTIFY_PLAY"
                    cleaned.contains("call") -> "CALL"
                    else -> null
                }
            )
            return
        }
        // ================= IMAGE GENERATION =================

        if (
            cleaned.contains("generate") &&
            cleaned.contains("image")
        ) {

            var prompt = cleaned

            prompt = prompt
                .replace("generate an image of", "")
                .replace("generate image of", "")
                .replace("generate an image", "")
                .replace("generate image", "")
                .replace("create an image of", "")
                .replace("create image of", "")
                .replace("draw", "")
                .trim()

            if (prompt.isNotBlank()) {

                val imageClient = ImageGenerationClient()

                imageClient.generateImage(prompt) { url ->

                    android.util.Log.d("IMAGE_API", "Returned URL: $url")

                    requireActivity().runOnUiThread {

                        if (url != null) {

                            messages.add(
                                ChatMessage(
                                    text = null,
                                    imageUrl = url,
                                    user = false
                                )
                            )

                            adapter.notifyItemInserted(messages.size - 1)
                            recyclerView.smoothScrollToPosition(messages.size - 1)

                        } else {

                            streamAssistantMessage("Image generation failed")

                        }
                    }
                }

                return
            }
        }

        // ================= LLM INTENT =================

        val intentPrompt =
            PromptBuilder.build(messages.dropLast(1), input)

        llmClient.process(intentPrompt) { result ->

            requireActivity().runOnUiThread {

                when (result.intent) {

                    "CHAT" -> {
                        lastIntentWasMemoryQuery = false

                        val chatPrompt =
                            PromptBuilder.buildChatPrompt(
                                messages.dropLast(1),
                                input
                            )

                        llmClient.process(chatPrompt) { chatResponse ->
                            requireActivity().runOnUiThread {
                                chatResponse.response?.let {
                                    streamAssistantMessage(it)
                                }
                            }
                        }
                    }

                    "MEMORY_QUERY" -> {
                        result.memoryType?.let {
                            handleMemoryQuery(
                                it.uppercase(),
                                result.memoryFilter
                            )
                        }
                    }

                    else -> {

                        lastIntentWasMemoryQuery = false

                        result.response?.let {
                            streamAssistantMessage(it)
                        }

                        when (result.intent) {

                            "OPEN_APP" ->
                                actionExecutor.execute(result.intent, result.app)

                            "SEND_WHATSAPP" ->
                                actionExecutor.sendWhatsAppMessage(
                                    result.contact ?: return@runOnUiThread,
                                    result.message ?: return@runOnUiThread
                                )

                            "YOUTUBE_PLAY" -> {
                                val query = result.query ?: return@runOnUiThread

                                youtubeApiClient.getFirstVideo(query) { video ->
                                    video?.let {
                                        lastYoutubeVideoId = it.id
                                        actionExecutor.playYouTubeVideo(it.id)
                                        memoryRepository.logActivity(
                                            "YOUTUBE_PLAY",
                                            "Played YouTube Video",
                                            "Title: ${it.title}"
                                        )
                                    }
                                }
                            }

                            "YOUTUBE_SEARCH"->
                                actionExecutor.searchYouTube(
                                    result.query?:return@runOnUiThread
                                )


                            "SPOTIFY_PLAY" ->
                                actionExecutor.playSpotifySong(
                                    result.query ?: return@runOnUiThread
                                )

                            "SET_ALARM" ->
                                actionExecutor.setAlarm(
                                    result.hour ?: return@runOnUiThread,
                                    result.minute ?: 0,
                                    result.label
                                )

                            "SYSTEM_CONTROL" ->
                                actionExecutor.controlSystem(result.app)

                            "VOLUME_CONTROL" ->
                                actionExecutor.controlVolume(
                                    result.app,
                                    result.hour
                                )

                            "FLASHLIGHT_CONTROL" ->
                                actionExecutor.controlFlashlight(result.query?: "toggle")

                            "SEND_VIDEO" ->
                                actionExecutor.sendLastYoutubeVideo(
                                    result.contact ?: return@runOnUiThread
                                )

                            "SEND_LAST" ->
                                actionExecutor.sendLastAssistantResponse(
                                    result.contact ?: return@runOnUiThread
                                )
                            "SEND_EMAIL" ->
                                actionExecutor.sendEmail(
                                    result.email ?: return@runOnUiThread,
                                    result.subject ?: "AI Assistant Message",
                                    result.message ?: ""
                                )
                            "SEND_LAST_EMAIL" ->
                                actionExecutor.sendLastResponseAsEmail(
                                    result.email ?: return@runOnUiThread,
                                    result.subject ?: "AI Assistant Message"
                                )

                            "CALL_CONTACT" ->
                                actionExecutor.callContactByName(
                                    result.contact ?: return@runOnUiThread
                                )

                            "SEND_SMS" ->
                                actionExecutor.sendSmsByContactName(
                                    result.contact ?: return@runOnUiThread,
                                    result.message ?: return@runOnUiThread
                                )

                            "GENERATE_STRUCTURED_PDF",
                                 "GENERATE_PDF"-> {

                                val topic =
                                    result.query
                                        ?: "Generated Document"

                                generateStructuredPdfFromContext(topic)
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================
    // MEMORY HANDLER
    // =========================================================

    private fun handleMemoryQuery(
        memoryType: String,
        filter: String?
    ) {

        val fetchFunction = when (memoryType) {
            "YESTERDAY" -> memoryRepository::getYesterdayActivities
            "TODAY" -> memoryRepository::getTodayActivities
            else -> null
        } ?: return

        fetchFunction { logs ->

            var filteredLogs = logs

            filter?.let { type ->
                filteredLogs = logs.filter {
                    it.type.equals(type, ignoreCase = true)
                }
            }

            lastDetailedLogs = filteredLogs
            lastIntentWasMemoryQuery = true

            if (filteredLogs.isEmpty()) {
                streamAssistantMessage("You have no recorded activities for that period.")
                return@fetchFunction
            }

            val summaryText = buildMemorySummary(filteredLogs)
            streamAssistantMessage(summaryText)


        }
    }
    private fun generateStructuredPdfFromContext(topic: String) {

        val contextText = messages
            .filter { !it.user }
            .takeLast(10)
            .joinToString("\n") { it.text ?: "" }

        val prompt = PromptBuilder.buildStructuredPdfPrompt(
            topic,
            contextText
        )

        llmClient.process(prompt) { result ->

            requireActivity().runOnUiThread {

                val structuredText = result.response ?: return@runOnUiThread

                val file = actionExecutor.generateStructuredPdf(
                    topic,
                    structuredText
                )

                if (file != null) {

                    adapter.addMessage(
                        ChatMessage(
                            text = "📄 $topic PDF generated",
                            user = false,
                            pdfFile = file
                        )
                    )

                    recyclerView.scrollToPosition(messages.size - 1)

                } else {

                    streamAssistantMessage("Failed to generate PDF.")
                }
            }
        }
    }

    // =========================================================
    // USER MESSAGE
    // =========================================================

    private fun addUserMessage(text: String) {
        val message = ChatMessage(text, true)
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)

        recyclerView.post {
            recyclerView.smoothScrollToPosition(messages.size - 1)
        }

        currentSessionId?.let { sessionId ->
            val entity = ChatMessageEntity(
                text = text,
                user = true,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.saveMessage(sessionId, entity)

            if (messages.count { it.user } == 1) {

                val title = text
                    .split(" ")
                    .take(6)
                    .joinToString(" ")

                chatRepository.updateSessionTitle(sessionId, title)
            }
        }
    }

    // =========================================================
    // STREAMING EFFECT
    // =========================================================

    private fun streamAssistantMessage(fullText: String) {

        recyclerView.visibility = View.VISIBLE

        val message = ChatMessage("", false)
        messages.add(message)
        val position = messages.size - 1
        adapter.notifyItemInserted(position)

        recyclerView.smoothScrollToPosition(position)

        val handler = Handler(Looper.getMainLooper())
        var index = 0

        val runnable = object : Runnable {

            override fun run() {

                if (index <= fullText.length) {

                    adapter.updateMessage(
                        position,
                        fullText.substring(0, index)
                    )

                    recyclerView.smoothScrollToPosition(position)

                    index++

                    handler.postDelayed(this, 12)

                } else {

                    // ✅ Save assistant message to Firestore
                    currentSessionId?.let { sessionId ->

                        val entity = ChatMessageEntity(
                            text = fullText,
                            user = false,
                            timestamp = System.currentTimeMillis()
                        )

                        chatRepository.saveMessage(sessionId, entity)
                    }
                }
            }
        }

        handler.post(runnable)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == 999 && resultCode == Activity.RESULT_OK) {
            data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )?.firstOrNull()?.let {
                handleUserInput(it)
            }
        }
    }
}