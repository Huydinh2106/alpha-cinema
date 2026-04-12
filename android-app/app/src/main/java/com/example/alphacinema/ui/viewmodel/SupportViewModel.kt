package com.example.alphacinema.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.model.SupportChatHistoryTurn
import com.example.alphacinema.data.model.SupportChatMessage
import com.example.alphacinema.data.model.SupportChatMemoryContext
import com.example.alphacinema.data.model.SupportChatMetadata
import com.example.alphacinema.data.model.SupportChatReply
import com.example.alphacinema.data.model.mergeWith
import com.example.alphacinema.data.model.SupportMessageSender
import com.example.alphacinema.data.repository.SupportRepository
import com.example.alphacinema.data.repository.SupportSuggestionPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SupportViewModel : ViewModel() {
    private val repository = SupportRepository()
    private val sessionId = UUID.randomUUID().toString()
    private var memorySnapshot = SupportChatMemoryContext()

    private val _messages = MutableStateFlow(
        listOf(
            SupportChatMessage(
                id = "welcome",
                text = "Xin chào, tôi là trợ lý AlphaCinema. Bạn có thể hỏi về phim, tài khoản hoặc cách sử dụng ứng dụng.",
                sender = SupportMessageSender.BOT,
                timestamp = currentTimeLabel()
            )
        )
    )
    val messages: StateFlow<List<SupportChatMessage>> = _messages.asStateFlow()

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
            text = "Đang trả lời...",
            sender = SupportMessageSender.LOADING
        )

        _messages.update { current ->
            current + userMessage + loadingMessage
        }

        viewModelScope.launch {
            val botReply = runCatching {
                repository.askQuestion(
                    question = question,
                    sessionId = sessionId,
                    history = conversationHistory,
                    memory = requestMemory
                )
            }.getOrElse {
                SupportChatReply(text = SupportRepository.FALLBACK_REPLY)
            }
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
        }
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
        messages: List<SupportChatMessage>
    ): SupportChatMemoryContext {
        val recentMessages = messages
            .filter { it.sender != SupportMessageSender.LOADING }
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
                latestUserQuestion.isBlank() -> memorySnapshot.lastIntent
                SupportSuggestionPolicy.analyzeRecommendationRequest(
                    question = latestUserQuestion,
                    memory = memorySnapshot
                ).shouldSuggestMovies -> "movie_recommendation"
                else -> "general_support"
            },
            topics = extractTopics(recentMessages),
            genres = extractGenres(recentMessages),
            referencedMovieSlugs = referencedMovies.mapNotNull { it.slug }.distinct().take(MAX_MEMORY_MOVIES),
            referencedMovieTitles = referencedMovies.map { it.title }.distinct().take(MAX_MEMORY_MOVIES)
        )

        return memorySnapshot.mergeWith(localMemory)
    }

    private fun buildConversationHistory(
        messages: List<SupportChatMessage>
    ): List<SupportChatHistoryTurn> {
        return messages
            .asSequence()
            .filter { it.sender != SupportMessageSender.LOADING }
            .mapNotNull { message ->
                val role = when (message.sender) {
                    SupportMessageSender.USER -> "user"
                    SupportMessageSender.BOT -> "assistant"
                    SupportMessageSender.LOADING -> null
                }
                role?.let {
                    SupportChatHistoryTurn(
                        role = it,
                        content = message.text.trim(),
                        timestamp = message.timestamp
                    )
                }
            }
            .filter { it.content.isNotBlank() }
            .toList()
            .takeLast(MAX_HISTORY_TURNS)
    }

    private fun extractTopics(messages: List<SupportChatMessage>): List<String> {
        val normalizedText = messages.joinToString(" ") { it.text.lowercase(Locale.ROOT) }
        val topics = buildList {
            if (normalizedText.contains("nội quy") || normalizedText.contains("dieu khoan")) add("app_policy")
            if (normalizedText.contains("tai khoan") || normalizedText.contains("đăng nhập") || normalizedText.contains("dang nhap")) add("account")
            if (normalizedText.contains("loi") || normalizedText.contains("bug") || normalizedText.contains("khong xem duoc")) add("troubleshooting")
            if (normalizedText.contains("phim")) add("movies")
        }
        return topics.distinct()
    }

    private fun extractGenres(messages: List<SupportChatMessage>): List<String> {
        val normalizedText = messages
            .filter { it.sender == SupportMessageSender.USER }
            .joinToString(" ") { it.text.lowercase(Locale.ROOT) }

        return GENRE_KEYWORDS.filter { (keyword, _) ->
            normalizedText.contains(keyword)
        }.map { it.second }
    }

    companion object {
        private const val MIN_REMEMBERED_QA_PAIRS = 3
        private const val MESSAGES_PER_QA_PAIR = 2
        private const val MAX_HISTORY_TURNS = (MIN_REMEMBERED_QA_PAIRS * MESSAGES_PER_QA_PAIR) + 2
        private const val MAX_MEMORY_SUMMARY_MESSAGES = MIN_REMEMBERED_QA_PAIRS * MESSAGES_PER_QA_PAIR
        private const val MAX_MEMORY_SUMMARY_CHARS = 500
        private const val MAX_MEMORY_MOVIES = 5

        private val GENRE_KEYWORDS = listOf(
            "kinh dị" to "Kinh dị",
            "kinh di" to "Kinh dị",
            "hành động" to "Hành động",
            "hanh dong" to "Hành động",
            "tình cảm" to "Tình cảm",
            "tinh cam" to "Tình cảm",
            "tâm lý" to "Tâm lý",
            "tam ly" to "Tâm lý",
            "viễn tưởng" to "Viễn tưởng",
            "vien tuong" to "Viễn tưởng",
            "hài" to "Hài hước",
            "hai" to "Hài hước",
            "anime" to "Anime"
        )

        private fun currentTimeLabel(): String {
            return SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(Date())
        }
    }
}
