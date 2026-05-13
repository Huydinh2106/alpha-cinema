package com.example.alphacinema.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.local.SettingsManager
import com.example.alphacinema.data.model.SupportChatHistoryMessage
import com.example.alphacinema.data.model.SupportChatMessage
import com.example.alphacinema.data.model.SupportChatMemoryContext
import com.example.alphacinema.data.model.SupportChatMetadata
import com.example.alphacinema.data.model.SupportChatReply
import com.example.alphacinema.data.model.SupportMessageSender
import com.example.alphacinema.data.model.mergeWith
import com.example.alphacinema.data.repository.SupportRepository
import com.example.alphacinema.data.repository.SupportSuggestionPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import java.util.UUID

class SupportViewModel(
    private val repository: SupportRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    constructor() : this(
        repository = SupportRepository(),
        settingsManager = SettingsManager.getInstance()
    )

    private var currentSessionId = settingsManager.getSupportChatSessionId()
        ?: UUID.randomUUID().toString()
    private var memorySnapshot = SupportChatMemoryContext()

    private val _messages = MutableStateFlow(
        settingsManager.getSupportChatMessages()
            .filterNot { it.sender == SupportMessageSender.LOADING || it.id == WELCOME_MESSAGE_ID }
    )
    val messages: StateFlow<List<SupportChatMessage>> = _messages.asStateFlow()

    init {
        memorySnapshot = buildMemoryContext(
            messages = _messages.value,
            baseMemory = SupportChatMemoryContext()
        )
        persistConversation()
    }

    fun sendMessage(userInput: String) {
        val question = userInput.trim()
        if (question.isBlank()) return

        val userMessage = createMessage(
            text = question,
            sender = SupportMessageSender.USER
        )
        val conversationHistory = buildConversationHistory(_messages.value + userMessage)
        val requestMemory = buildMemoryContext(_messages.value + userMessage)
        val loadingMessage = createMessage(
            text = "\u0110ang tr\u1ea3 l\u1eddi...",
            sender = SupportMessageSender.LOADING
        )

        _messages.update { current ->
            current + userMessage + loadingMessage
        }
        persistConversation()

        viewModelScope.launch {
            val botReply = runCatching {
                repository.askQuestion(
                    question = question,
                    sessionId = currentSessionId,
                    history = conversationHistory,
                    memory = requestMemory,
                    includeChatHistory = conversationHistory.isNotEmpty()
                )
            }.getOrElse {
                SupportChatReply(
                    text = SupportRepository.FALLBACK_REPLY,
                    sessionId = currentSessionId
                )
            }

            currentSessionId = botReply.sessionId?.takeIf { it.isNotBlank() } ?: currentSessionId
            memorySnapshot = memorySnapshot.mergeWith(botReply.memory)

            val botMessage = loadingMessage.copy(
                text = botReply.text,
                sender = SupportMessageSender.BOT,
                timestamp = currentTimeLabel(),
                metadata = botReply.metadata
            )

            _messages.update { current ->
                current.map { message ->
                    if (message.id == loadingMessage.id) botMessage else message
                }
            }
            persistConversation()
        }
    }

    fun startNewConversation() {
        currentSessionId = UUID.randomUUID().toString()
        memorySnapshot = SupportChatMemoryContext()
        _messages.value = emptyList()
        persistConversation()
    }

    private fun createMessage(
        text: String,
        sender: SupportMessageSender,
        metadata: SupportChatMetadata? = null
    ): SupportChatMessage {
        return SupportChatMessage(
            id = "${sender.name.lowercase(Locale.ROOT)}-${System.currentTimeMillis()}-${text.hashCode()}",
            text = text,
            sender = sender,
            timestamp = currentTimeLabel(),
            metadata = metadata
        )
    }

    private fun buildMemoryContext(
        messages: List<SupportChatMessage>,
        baseMemory: SupportChatMemoryContext = memorySnapshot
    ): SupportChatMemoryContext {
        val recentMessages = conversationMessages(messages)
            .takeLast(MAX_HISTORY_TURNS)
        val latestUserQuestion = recentMessages
            .lastOrNull { it.sender == SupportMessageSender.USER }
            ?.text
            .orEmpty()
        val referencedMovies = recentMessages
            .flatMap { it.metadata?.movieItems.orEmpty() }
        val summary = recentMessages
            .takeLast(MAX_MEMORY_SUMMARY_MESSAGES)
            .joinToString("\n") { message ->
                val role = if (message.sender == SupportMessageSender.USER) "User" else "Assistant"
                "$role: ${message.text.trim()}"
            }
            .trim()
            .take(MAX_MEMORY_SUMMARY_CHARS)
            .ifBlank { null }

        val localMemory = SupportChatMemoryContext(
            summary = summary,
            lastIntent = when {
                latestUserQuestion.isBlank() -> baseMemory.lastIntent
                SupportSuggestionPolicy.analyzeRecommendationRequest(
                    question = latestUserQuestion,
                    memory = baseMemory
                ).shouldSuggestMovies -> "movie_recommendation"
                else -> "general_support"
            },
            topics = extractTopics(recentMessages),
            genres = extractGenres(recentMessages),
            referencedMovieSlugs = referencedMovies.mapNotNull { it.slug }.distinct().take(MAX_MEMORY_MOVIES),
            referencedMovieTitles = referencedMovies.map { it.title }.distinct().take(MAX_MEMORY_MOVIES)
        )

        return baseMemory.mergeWith(localMemory)
    }

    private fun buildConversationHistory(
        messages: List<SupportChatMessage>
    ): List<SupportChatHistoryMessage> {
        return conversationMessages(messages)
            .asSequence()
            .mapNotNull { message ->
                val role = when (message.sender) {
                    SupportMessageSender.USER -> "user"
                    SupportMessageSender.BOT -> "assistant"
                    SupportMessageSender.LOADING -> null
                }
                role?.let {
                    SupportChatHistoryMessage(
                        role = it,
                        content = message.text.trim()
                    )
                }
            }
            .filter { it.content.isNotBlank() }
            .toList()
            .takeLast(MAX_HISTORY_TURNS)
    }

    private fun extractTopics(messages: List<SupportChatMessage>): List<String> {
        val normalizedText = normalizeForLookup(messages.joinToString(" ") { it.text })
        val topics = buildList {
            if (normalizedText.contains("noi quy") || normalizedText.contains("dieu khoan")) add("app_policy")
            if (normalizedText.contains("tai khoan") || normalizedText.contains("dang nhap")) add("account")
            if (normalizedText.contains("loi") || normalizedText.contains("bug") || normalizedText.contains("khong xem duoc")) add("troubleshooting")
            if (normalizedText.contains("phim")) add("movies")
        }
        return topics.distinct()
    }

    private fun extractGenres(messages: List<SupportChatMessage>): List<String> {
        val normalizedText = normalizeForLookup(
            messages
            .filter { it.sender == SupportMessageSender.USER }
            .joinToString(" ") { it.text.lowercase(Locale.ROOT) }
        )

        return GENRE_KEYWORDS.filter { (keyword, _) ->
            normalizedText.contains(keyword)
        }.map { it.second }
    }

    private fun conversationMessages(messages: List<SupportChatMessage>): List<SupportChatMessage> {
        return messages.filter { message ->
            message.sender != SupportMessageSender.LOADING && message.id != WELCOME_MESSAGE_ID
        }
    }

    private fun persistConversation() {
        settingsManager.saveSupportChatConversation(
            sessionId = currentSessionId,
            messages = _messages.value.filterNot { it.sender == SupportMessageSender.LOADING || it.id == WELCOME_MESSAGE_ID }
        )
    }

    private fun normalizeForLookup(value: String): String {
        if (value.isBlank()) return ""
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("\u0111", "d")
            .replace("\u0110", "D")
            .lowercase(Locale.ROOT)
            .trim()
    }

    companion object {
        private const val MIN_REMEMBERED_QA_PAIRS = 3
        private const val MESSAGES_PER_QA_PAIR = 2
        private const val MAX_HISTORY_TURNS = (MIN_REMEMBERED_QA_PAIRS * MESSAGES_PER_QA_PAIR) + 2
        private const val MAX_MEMORY_SUMMARY_MESSAGES = MIN_REMEMBERED_QA_PAIRS * MESSAGES_PER_QA_PAIR
        private const val MAX_MEMORY_SUMMARY_CHARS = 500
        private const val MAX_MEMORY_MOVIES = 5
        private const val WELCOME_MESSAGE_ID = "welcome"

        private val GENRE_KEYWORDS = listOf(
            "kinh di" to "Kinh di",
            "hanh dong" to "Hanh dong",
            "tinh cam" to "Tinh cam",
            "tam ly" to "Tam ly",
            "vien tuong" to "Vien tuong",
            "hai" to "Hai huoc",
            "anime" to "Anime"
        )

        private fun currentTimeLabel(): String {
            return SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(Date())
        }
    }
}
