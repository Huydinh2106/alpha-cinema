package com.example.alphacinema.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.local.SettingsManager
import com.example.alphacinema.data.model.SupportChatHistoryMessage
import com.example.alphacinema.data.model.SupportChatMemoryContext
import com.example.alphacinema.data.model.SupportChatMessage
import com.example.alphacinema.data.model.SupportChatMetadata
import com.example.alphacinema.data.model.SupportChatReply
import com.example.alphacinema.data.model.SupportChatSession
import com.example.alphacinema.data.model.SupportMessageSender
import com.example.alphacinema.data.model.mergeWith
import com.example.alphacinema.data.repository.SupportRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupportViewModel(
    private val repository: SupportRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    constructor() : this(
        repository = SupportRepository(),
        settingsManager = SettingsManager.getInstance()
    )

    private val initialSessionId = settingsManager.getSupportChatSessionId()
        ?: UUID.randomUUID().toString()
    private val initialMessages = settingsManager.getSupportChatMessages()
        .filterNot { it.sender == SupportMessageSender.LOADING || it.id == WELCOME_MESSAGE_ID }

    private val _currentSessionId = MutableStateFlow(initialSessionId)
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private var memorySnapshot = SupportChatMemoryContext()

    private val _messages = MutableStateFlow(initialMessages)
    val messages: StateFlow<List<SupportChatMessage>> = _messages.asStateFlow()

    private val _chatSessions = MutableStateFlow(
        loadInitialChatSessions(
            sessionId = initialSessionId,
            messages = initialMessages
        )
    )
    val chatSessions: StateFlow<List<SupportChatSession>> = _chatSessions.asStateFlow()

    init {
        memorySnapshot = buildMemoryContext(
            messages = _messages.value,
            baseMemory = SupportChatMemoryContext()
        )
        persistConversation()
    }

    fun sendMessage(userInput: String) {
        val question = userInput.trim()
        if (question.isBlank() || hasPendingResponse()) return

        val previousMessages = _messages.value.filterNot { it.sender == SupportMessageSender.LOADING }
        val userMessage = createMessage(
            text = question,
            sender = SupportMessageSender.USER
        )
        val conversationHistory = buildConversationHistory(previousMessages)
        val requestMemory = buildMemoryContext(previousMessages + userMessage)
        val loadingMessage = createMessage(
            text = "\u0110ang tr\u1ea3 l\u1eddi...",
            sender = SupportMessageSender.LOADING
        )
        val requestSessionId = _currentSessionId.value

        _messages.update {
            previousMessages + userMessage + loadingMessage
        }
        persistConversation()

        viewModelScope.launch {
            val botReply = runCatching {
                repository.askQuestion(
                    question = question,
                    sessionId = requestSessionId,
                    history = conversationHistory,
                    memory = requestMemory,
                    includeChatHistory = conversationHistory.isNotEmpty()
                )
            }.getOrElse {
                SupportChatReply(
                    text = SupportRepository.FALLBACK_REPLY,
                    sessionId = requestSessionId
                )
            }

            val resolvedSessionId = botReply.sessionId?.takeIf { it.isNotBlank() } ?: requestSessionId
            if (resolvedSessionId != requestSessionId) {
                _chatSessions.update { sessions -> sessions.filterNot { it.id == requestSessionId } }
            }
            _currentSessionId.value = resolvedSessionId
            memorySnapshot = memorySnapshot.mergeWith(botReply.memory)

            val botMessage = loadingMessage.copy(
                text = botReply.text,
                sender = SupportMessageSender.BOT,
                timestamp = currentTimeLabel(),
                metadata = botReply.metadata
            )

            _messages.update { current ->
                var replacedLoadingMessage = false
                val resolvedMessages = current.mapNotNull { message ->
                    when {
                        message.id == loadingMessage.id -> {
                            replacedLoadingMessage = true
                            botMessage
                        }
                        message.sender == SupportMessageSender.LOADING -> null
                        else -> message
                    }
                }
                if (replacedLoadingMessage) {
                    resolvedMessages
                } else {
                    resolvedMessages + botMessage
                }
            }
            persistConversation()
        }
    }

    fun startNewConversation() {
        _currentSessionId.value = UUID.randomUUID().toString()
        memorySnapshot = SupportChatMemoryContext()
        _messages.value = emptyList()
        persistConversation()
    }

    fun openConversation(sessionId: String) {
        val session = _chatSessions.value.firstOrNull { it.id == sessionId } ?: return
        val restoredMessages = session.messages.filterNot {
            it.sender == SupportMessageSender.LOADING || it.id == WELCOME_MESSAGE_ID
        }

        _currentSessionId.value = session.id
        _messages.value = restoredMessages
        memorySnapshot = buildMemoryContext(
            messages = restoredMessages,
            baseMemory = SupportChatMemoryContext()
        )
        saveActiveConversation(restoredMessages)
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
                        content = message.toHistoryContent()
                    )
                }
            }
            .filter { it.content.isNotBlank() }
            .toList()
            .takeLast(MAX_HISTORY_TURNS)
    }

    private fun SupportChatMessage.toHistoryContent(): String {
        val base = text.trim()
        val movieContext = metadata?.movieItems
            .orEmpty()
            .take(MAX_MOVIE_CONTEXT_ITEMS)
            .joinToString("; ") { movie ->
                buildString {
                    append(movie.title)
                    if (movie.year.isNotBlank()) append(" (${movie.year})")
                    if (!movie.slug.isNullOrBlank()) append(" - ${movie.slug}")
                }
            }

        return if (movieContext.isBlank()) {
            base
        } else {
            "$base\nCác phim đã gợi ý: $movieContext"
        }
    }

    private fun conversationMessages(messages: List<SupportChatMessage>): List<SupportChatMessage> {
        return messages.filter { message ->
            message.sender != SupportMessageSender.LOADING && message.id != WELCOME_MESSAGE_ID
        }
    }

    private fun hasPendingResponse(): Boolean {
        return _messages.value.any { it.sender == SupportMessageSender.LOADING }
    }

    private fun persistConversation() {
        val messages = _messages.value.filterNot {
            it.sender == SupportMessageSender.LOADING || it.id == WELCOME_MESSAGE_ID
        }
        saveActiveConversation(messages)

        if (messages.isNotEmpty()) {
            val updatedSession = createSession(
                sessionId = _currentSessionId.value,
                messages = messages,
                updatedAtMillis = System.currentTimeMillis()
            )
            _chatSessions.update { sessions ->
                (listOf(updatedSession) + sessions.filterNot { it.id == updatedSession.id })
                    .filter { it.messages.isNotEmpty() }
                    .sortedByDescending { it.updatedAtMillis }
                    .take(MAX_SAVED_SESSIONS)
            }
        }

        settingsManager.saveSupportChatSessions(_chatSessions.value)
    }

    private fun saveActiveConversation(messages: List<SupportChatMessage>) {
        settingsManager.saveSupportChatConversation(
            sessionId = _currentSessionId.value,
            messages = messages
        )
    }

    private fun loadInitialChatSessions(
        sessionId: String,
        messages: List<SupportChatMessage>
    ): List<SupportChatSession> {
        val storedSessions = settingsManager.getSupportChatSessions()
            .filter { it.messages.isNotEmpty() }
        if (messages.isEmpty()) {
            return storedSessions
                .sortedByDescending { it.updatedAtMillis }
                .take(MAX_SAVED_SESSIONS)
        }

        val activeSession = createSession(
            sessionId = sessionId,
            messages = messages,
            updatedAtMillis = System.currentTimeMillis()
        )

        return (listOf(activeSession) + storedSessions.filterNot { it.id == sessionId })
            .sortedByDescending { it.updatedAtMillis }
            .take(MAX_SAVED_SESSIONS)
    }

    private fun createSession(
        sessionId: String,
        messages: List<SupportChatMessage>,
        updatedAtMillis: Long
    ): SupportChatSession {
        return SupportChatSession(
            id = sessionId,
            title = conversationTitle(messages),
            updatedAtMillis = updatedAtMillis,
            messages = messages
        )
    }

    private fun conversationTitle(messages: List<SupportChatMessage>): String {
        return messages
            .firstOrNull { it.sender == SupportMessageSender.USER }
            ?.text
            ?.cleanTitle()
            ?: messages
                .firstOrNull { it.sender == SupportMessageSender.BOT }
                ?.text
                ?.cleanTitle()
            ?: "Cuộc trò chuyện mới"
    }

    private fun String.cleanTitle(): String {
        return trim()
            .replace(Regex("\\s+"), " ")
            .take(MAX_SESSION_TITLE_CHARS)
            .ifBlank { "Cuộc trò chuyện mới" }
    }

    companion object {
        private const val MIN_REMEMBERED_QA_PAIRS = 3
        private const val MESSAGES_PER_QA_PAIR = 2
        private const val MAX_HISTORY_TURNS = (MIN_REMEMBERED_QA_PAIRS * MESSAGES_PER_QA_PAIR) + 2
        private const val MAX_MEMORY_SUMMARY_MESSAGES = MIN_REMEMBERED_QA_PAIRS * MESSAGES_PER_QA_PAIR
        private const val MAX_MEMORY_SUMMARY_CHARS = 500
        private const val MAX_MEMORY_MOVIES = 5
        private const val MAX_MOVIE_CONTEXT_ITEMS = 5
        private const val MAX_SAVED_SESSIONS = 30
        private const val MAX_SESSION_TITLE_CHARS = 56
        private const val WELCOME_MESSAGE_ID = "welcome"

        private fun currentTimeLabel(): String {
            return SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(Date())
        }
    }
}
