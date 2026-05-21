package com.example.alphacinema.data.repository

import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.local.SettingsManager
import com.example.alphacinema.data.model.FirestoreMovie
import com.example.alphacinema.data.model.SupportChatActionFactory
import com.example.alphacinema.data.model.SupportChatApiResponse
import com.example.alphacinema.data.model.SupportChatHistoryMessage
import com.example.alphacinema.data.model.SupportChatMemoryContext
import com.example.alphacinema.data.model.SupportChatMetadata
import com.example.alphacinema.data.model.SupportChatMovieItem
import com.example.alphacinema.data.model.SupportChatReply
import com.example.alphacinema.data.model.SupportChatRequest
import com.example.alphacinema.data.model.SupportChatRouteDestination
import com.example.alphacinema.data.model.mergeWith
import com.google.gson.Gson
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SupportRepository {
    private val api = RetrofitClient.supportChatApi
    private val firestoreRepository = FirestoreRepository()
    private val gson = Gson()

    suspend fun askQuestion(
        question: String,
        sessionId: String,
        history: List<SupportChatHistoryMessage> = emptyList(),
        memory: SupportChatMemoryContext = SupportChatMemoryContext(),
        includeChatHistory: Boolean = false
    ): SupportChatReply {
        return withContext(Dispatchers.IO) {
            val isKidsMode = runCatching {
                SettingsManager.getInstance().isKidsModeEnabled.value
            }.getOrDefault(false)
            val requestMemory = memory.mergeWith(
                SupportChatMemoryContext(kidsModeEnabled = isKidsMode)
            )

            val rawBody = runCatching {
                val response = api.askQuestion(
                    SupportChatRequest(
                        question = question,
                        topK = 6,
                        topNRecommendations = MAX_SUGGESTIONS,
                        generationModel = "gpt-4o-mini",
                        sessionId = sessionId,
                        rememberHistory = true,
                        chatHistory = history
                            .takeLast(MAX_HISTORY_TURNS)
                            .takeIf { includeChatHistory && it.isNotEmpty() }
                    )
                )
                val body = response.body()?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                body
            }.getOrElse {
                return@withContext SupportChatReply(
                    text = FALLBACK_REPLY,
                    memory = requestMemory,
                    sessionId = sessionId
                )
            }

            val apiResponse = runCatching {
                gson.fromJson(rawBody, SupportChatApiResponse::class.java)
            }.getOrNull()
            val parsed = SupportChatResponseParser.parse(rawBody)
            val movieItems = enrichMovieSuggestions(
                parsedSuggestions = parsed.movieSuggestions,
                isKidsMode = isKidsMode
            )
                .distinctBy { it.slug ?: it.movieId ?: it.id }
                .take(MAX_SUGGESTIONS)
            val metadata = movieItems.takeIf { it.isNotEmpty() }
                ?.let { SupportChatMetadata(movieItems = it) }

            SupportChatReply(
                text = resolveReplyText(
                    parsedText = apiResponse?.answer?.takeIf { it.isNotBlank() } ?: parsed.text
                ),
                metadata = metadata,
                memory = requestMemory.mergeWith(parsed.memory),
                sessionId = apiResponse?.sessionId ?: parsed.sessionId,
                historyMessageCount = apiResponse?.historyMessageCount ?: parsed.historyMessageCount
            )
        }
    }

    private suspend fun enrichMovieSuggestions(
        parsedSuggestions: List<ParsedSupportChatMovieSuggestion>,
        isKidsMode: Boolean
    ): List<SupportChatMovieItem> {
        if (parsedSuggestions.isEmpty()) return emptyList()

        return parsedSuggestions.mapNotNull { suggestion ->
            val resolvedMovie = findMatchingMovie(
                slug = suggestion.slug,
                movieId = suggestion.movieId,
                title = suggestion.title,
                isKidsMode = isKidsMode
            )
            toSupportChatMovieItem(
                suggestion = suggestion,
                resolvedMovie = resolvedMovie
            )
        }
    }

    private suspend fun findMatchingMovie(
        slug: String?,
        movieId: String?,
        title: String,
        isKidsMode: Boolean
    ): FirestoreMovie? {
        val identifier = slug.normalizedValue() ?: movieId.normalizedValue()
        if (identifier != null) {
            val bySlug = firestoreRepository.getMovieBySlug(identifier)
            if (bySlug != null && (!isKidsMode || bySlug.isKidsFriendly)) {
                return bySlug
            }
        }

        if (title.isBlank()) return null

        val candidates = firestoreRepository.searchMovies(
            keyword = title,
            isKidsMode = isKidsMode,
            limit = 5
        )
        val normalizedTitle = title.normalizeForLookup()
        return candidates.firstOrNull { movie ->
            movie.title.normalizeForLookup() == normalizedTitle
                || movie.originName.normalizeForLookup() == normalizedTitle
        } ?: candidates.firstOrNull()
    }

    private fun toSupportChatMovieItem(
        suggestion: ParsedSupportChatMovieSuggestion,
        resolvedMovie: FirestoreMovie?
    ): SupportChatMovieItem? {
        val resolvedSlug = suggestion.slug.normalizedValue()
            ?: suggestion.movieId.normalizedValue()
            ?: resolvedMovie?.slug?.normalizedValue()
        val resolvedMovieId = suggestion.movieId.normalizedValue()
            ?: resolvedSlug
        val resolvedTitle = suggestion.title.ifBlank {
            resolvedMovie?.title.orEmpty()
        }

        if (resolvedTitle.isBlank() && resolvedSlug == null) return null

        val action = SupportChatActionFactory.createWatchMovieAction(
            slug = resolvedSlug,
            movieId = resolvedMovieId,
            preferredDestination = suggestion.preferredDestination ?: SupportChatRouteDestination.PLAYER,
            playable = suggestion.playable,
            episodeId = suggestion.episodeId,
            deeplink = suggestion.deeplink
        )

        return SupportChatMovieItem(
            id = resolvedSlug
                ?: resolvedMovieId
                ?: suggestion.id.normalizedValue()
                ?: resolvedTitle.normalizeForLookup(),
            title = resolvedTitle,
            subtitle = suggestion.subtitle.ifBlank { resolvedMovie?.originName.orEmpty() },
            posterUrl = suggestion.posterUrl.normalizedValue()
                ?: resolvedMovie?.posterUrl?.normalizedValue(),
            year = suggestion.year.ifBlank {
                resolvedMovie?.year?.takeIf { it > 0 }?.toString().orEmpty()
            },
            slug = resolvedSlug,
            movieId = resolvedMovieId,
            actions = listOf(action)
        )
    }

    private fun resolveReplyText(parsedText: String): String {
        val cleanedText = SupportChatResponseParser.cleanDisplayText(parsedText)
        return cleanedText.takeIf { it.isNotBlank() } ?: FALLBACK_REPLY
    }

    private fun String?.normalizedValue(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun String.normalizeForLookup(): String {
        if (isBlank()) return ""
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("\u0111", "d")
            .replace("\u0110", "D")
            .lowercase(Locale.ROOT)
            .trim()
    }

    companion object {
        const val FALLBACK_REPLY = "Xin lỗi, hiện không thể trả lời"
        private const val MAX_SUGGESTIONS = 5
        private const val MAX_HISTORY_TURNS = 8
    }
}
