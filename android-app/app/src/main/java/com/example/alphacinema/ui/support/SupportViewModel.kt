package com.example.alphacinema.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.local.SettingsManager
import com.example.alphacinema.data.model.SupportChatHistoryMessage
import com.example.alphacinema.data.model.SupportChatMessage
import com.example.alphacinema.data.model.SupportChatMemoryContext
import com.example.alphacinema.data.model.SupportChatMetadata
import com.example.alphacinema.data.model.SupportChatReply
import com.example.alphacinema.data.model.SupportChatMovieItem
import com.example.alphacinema.data.model.SupportMessageSender
import com.example.alphacinema.data.model.mergeWith
import com.example.alphacinema.data.repository.SpotifyRepository
import com.example.alphacinema.data.repository.SupportMusicLinkPolicy
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
    private val settingsManager: SettingsManager,
    private val spotifyRepository: SpotifyRepository
) : ViewModel() {
    constructor() : this(
        repository = SupportRepository(),
        settingsManager = SettingsManager.getInstance(),
        spotifyRepository = SpotifyRepository()
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

        val previousMessages = _messages.value
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

        _messages.update { current ->
            current + userMessage + loadingMessage
        }
        persistConversation()

        viewModelScope.launch {
            val botReply = createLocalReplyIfAvailable(
                question = question,
                previousMessages = previousMessages
            ) ?: runCatching {
                repository.askQuestion(
                    question = enrichQuestionWithMovieContext(question, previousMessages),
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

    private fun enrichQuestionWithMovieContext(
        question: String,
        previousMessages: List<SupportChatMessage>
    ): String {
        val normalized = normalizeForLookup(question)
        val movies = previousSuggestedMovies(previousMessages)
        if (movies.isEmpty() || !isContextualMovieFollowUp(normalized)) return question

        val movieContext = movies.joinToString("; ") { movie ->
            buildString {
                append(movie.title)
                if (movie.year.isNotBlank()) append(" (${movie.year})")
                if (!movie.slug.isNullOrBlank()) append(" - ${movie.slug}")
            }
        }

        return """
            $question

            Ngữ cảnh: người dùng đang hỏi tiếp về các phim đã gợi ý trước đó: $movieContext.
            Nếu trả lời so sánh/lựa chọn, chỉ chọn trong danh sách này trừ khi người dùng yêu cầu phim khác.
        """.trimIndent()
    }

    private suspend fun createLocalReplyIfAvailable(
        question: String,
        previousMessages: List<SupportChatMessage>
    ): SupportChatReply? {
        val normalized = normalizeForLookup(question)
        val previousMovies = previousSuggestedMovies(previousMessages)

        SupportMusicLinkPolicy.buildMusicLookup(
            question = question,
            previousMovies = previousMovies
        )?.let { lookup ->
            val spotifyOutcome = lookup.query
                ?.let { query -> spotifyRepository.searchTopAlbumOutcome(query) }
            return SupportMusicLinkPolicy.buildMusicReply(
                lookup = lookup,
                spotifyResult = spotifyOutcome?.result,
                spotifyFailure = spotifyOutcome?.failure,
                spotifyFailureDetail = spotifyOutcome?.detail
            ).copy(sessionId = currentSessionId)
        }

        if (previousMovies.isNotEmpty() && isLocalMovieFollowUp(normalized)) {
            val selectedMovie = selectMovieForFollowUp(normalized, previousMovies)
            val reason = when {
                normalized.contains("ban be") || normalized.contains("xem chung") || normalized.contains("nhom") -> {
                    "dễ chọn làm phim xem chung vì nằm ngay trong nhóm gợi ý vừa rồi và phù hợp để cả nhóm quyết nhanh."
                }
                normalized.contains("nhe") || normalized.contains("gia dinh") -> {
                    "là lựa chọn an toàn hơn trong nhóm gợi ý; bạn vẫn nên mở chi tiết phim để kiểm tra độ tuổi và nội dung."
                }
                normalized.contains("hay nhat") || normalized.contains("nen xem") || normalized.contains("phim nao") -> {
                    "là lựa chọn mình ưu tiên nhất trong các phim vừa gợi ý."
                }
                else -> "là lựa chọn hợp lý nhất trong các phim vừa gợi ý."
            }

            return SupportChatReply(
                text = "Trong các phim vừa gợi ý, mình chọn ${selectedMovie.title}. Phim này $reason",
                metadata = SupportChatMetadata(movieItems = listOf(selectedMovie)),
                memory = SupportChatMemoryContext(
                    lastIntent = "recommendation",
                    topics = listOf("movies"),
                    referencedMovieSlugs = listOfNotNull(selectedMovie.slug),
                    referencedMovieTitles = listOf(selectedMovie.title)
                ),
                sessionId = currentSessionId
            )
        }

        val localAnswer = localSupportAnswer(normalized) ?: return null
        return SupportChatReply(
            text = localAnswer,
            sessionId = currentSessionId,
            memory = SupportChatMemoryContext(
                lastIntent = "policy",
                topics = listOf("support")
            )
        )
    }

    private fun previousSuggestedMovies(messages: List<SupportChatMessage>): List<SupportChatMovieItem> {
        return messages
            .asReversed()
            .asSequence()
            .filter { it.sender == SupportMessageSender.BOT }
            .flatMap { it.metadata?.movieItems.orEmpty().asSequence() }
            .distinctBy { it.slug ?: it.movieId ?: it.id }
            .take(MAX_MOVIE_CONTEXT_ITEMS)
            .toList()
    }

    private fun isContextualMovieFollowUp(normalized: String): Boolean {
        return listOf(
            "cac phim do",
            "may phim do",
            "phim do",
            "trong do",
            "trong cac phim",
            "vua goi y",
            "o tren",
            "phim nao",
            "bo nao",
            "cai nao"
        ).any { normalized.contains(it) }
    }

    private fun isLocalMovieFollowUp(normalized: String): Boolean {
        return isContextualMovieFollowUp(normalized) && listOf(
            "phim nao",
            "bo nao",
            "cai nao",
            "nen xem",
            "hay nhat",
            "xem chung",
            "ban be",
            "nhom",
            "gia dinh",
            "nhe"
        ).any { normalized.contains(it) }
    }

    private fun selectMovieForFollowUp(
        normalized: String,
        movies: List<SupportChatMovieItem>
    ): SupportChatMovieItem {
        val friendlyKeywords = listOf("ban be", "xem chung", "nhom", "gia dinh", "nhe")
        if (friendlyKeywords.any { normalized.contains(it) }) {
            return movies.firstOrNull { movie ->
                val text = normalizeForLookup("${movie.title} ${movie.subtitle}")
                listOf("hai", "phieu luu", "hanh dong", "gia dinh", "anime").any { text.contains(it) }
            } ?: movies.first()
        }

        return movies.first()
    }

    private fun localSupportAnswer(normalized: String): String? {
        return when {
            listOf("premium", "basic", "couple", "thanh vien", "nang cap", "gia goi", "goi thanh vien")
                .any { normalized.contains(it) } -> {
                """
                AlphaCinema hiện có 3 gói:
                • Basic: xem phim không giới hạn, lưu yêu thích, chất lượng HD.
                • Couple: thêm tạo phòng xem chung, đồng bộ thời gian xem và chat trong phòng.
                • Premium: nhiều phòng xem chung, mời bạn bè bằng link, Full HD / 4K và không quảng cáo.

                Nếu bạn muốn nâng cấp, mở màn Tài khoản rồi chọn Nâng cấp gói.
                """.trimIndent()
            }

            listOf("xem chung", "tao phong", "phong xem", "moi ban", "dong bo")
                .any { normalized.contains(it) } -> {
                """
                Để xem chung, bạn mở phim muốn xem rồi chọn tính năng Xem chung. Sau đó tạo phòng, gửi link/mã phòng cho bạn bè và bắt đầu phát phim. Gói Couple hỗ trợ xem chung cơ bản, còn Premium phù hợp hơn nếu bạn tạo nhiều phòng hoặc xem theo nhóm.
                """.trimIndent()
            }

            listOf("dang nhap", "dang ky", "quen mat khau", "tai khoan", "mat khau")
                .any { normalized.contains(it) } -> {
                """
                Với tài khoản, bạn có thể vào màn Tài khoản để đăng nhập hoặc đăng ký. Nếu quên mật khẩu, hãy chọn luồng đăng nhập rồi dùng chức năng khôi phục mật khẩu nếu app đang bật Firebase Auth. Sau khi đăng nhập, dữ liệu như gói thành viên, yêu thích và đang xem sẽ được đồng bộ tốt hơn.
                """.trimIndent()
            }

            listOf("yeu thich", "danh sach", "luu phim", "dang xem")
                .any { normalized.contains(it) } -> {
                """
                Bạn có thể lưu phim vào Yêu thích hoặc Danh sách phim từ màn chi tiết phim. Mục Đang xem giúp quay lại các phim đang xem dở. Nếu chưa đăng nhập, app có thể yêu cầu đăng nhập để lưu dữ liệu ổn định hơn.
                """.trimIndent()
            }

            listOf("loi", "khong xem duoc", "bi lag", "cham", "load", "man hinh den")
                .any { normalized.contains(it) } -> {
                """
                Nếu phim không phát hoặc bị chậm, bạn thử các bước này: kiểm tra mạng, thoát vào lại phim, đổi tập/nguồn nếu có, tắt VPN hoặc mở lại app. Nếu vẫn lỗi, hãy gửi góp ý kèm tên phim và tập đang xem để dễ kiểm tra.
                """.trimIndent()
            }

            listOf("thanh toan", "gia han", "huy goi", "huy gia han", "doi goi")
                .any { normalized.contains(it) } -> {
                """
                Bạn có thể quản lý gói trong màn Tài khoản. Tại đó có thông tin gói hiện tại, ngày hết hạn, quyền lợi, đổi gói và hủy gia hạn. Khi hủy gia hạn, gói vẫn còn hiệu lực đến ngày hết hạn đã hiển thị.
                """.trimIndent()
            }

            listOf("lien he", "gop y", "chinh sach", "ho tro")
                .any { normalized.contains(it) } -> {
                "Bạn có thể vào mục Góp ý hoặc Chính sách trong màn Tài khoản. Nếu cần hỗ trợ nhanh, hãy mô tả rõ lỗi, tên phim, thiết bị và thời điểm gặp vấn đề để AlphaCinema xử lý dễ hơn."
            }

            else -> null
        }
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
        private const val MAX_MOVIE_CONTEXT_ITEMS = 5
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
