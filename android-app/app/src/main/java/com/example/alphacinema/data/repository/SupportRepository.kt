package com.example.alphacinema.data.repository

import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.local.SettingsManager
import com.example.alphacinema.data.model.SupportChatApiResponse
import com.example.alphacinema.data.model.FirestoreMovie
import com.example.alphacinema.data.model.SupportChatActionFactory
import com.example.alphacinema.data.model.SupportChatHistoryMessage
import com.example.alphacinema.data.model.SupportChatMemoryContext
import com.example.alphacinema.data.model.SupportChatMetadata
import com.example.alphacinema.data.model.SupportChatMovieItem
import com.example.alphacinema.data.model.SupportChatReply
import com.example.alphacinema.data.model.SupportChatRouteDestination
import com.example.alphacinema.data.model.SupportChatRequest
import com.example.alphacinema.data.model.SupportRecommendRequest
import com.example.alphacinema.data.model.mergeWith
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.Normalizer
import java.util.Locale

internal object SupportSuggestionPolicy {
    data class RecommendationRequest(
        val shouldSuggestMovies: Boolean,
        val isAlternativeRequest: Boolean,
        val shouldReusePreviousPreferences: Boolean
    )

    fun isRecommendationIntent(question: String): Boolean {
        val normalized = question.normalizeForLookup()
        return RECOMMENDATION_HINTS.any { hint -> normalized.contains(hint) }
    }

    fun analyzeRecommendationRequest(
        question: String,
        memory: SupportChatMemoryContext = SupportChatMemoryContext()
    ): RecommendationRequest {
        val normalized = question.normalizeForLookup()
        val explicitRecommendation = isRecommendationIntent(question)
        val followUpRecommendation = isRecommendationSession(memory)
            && RECOMMENDATION_FOLLOW_UP_HINTS.any { hint -> normalized.contains(hint) }

        return RecommendationRequest(
            shouldSuggestMovies = explicitRecommendation || followUpRecommendation,
            isAlternativeRequest = followUpRecommendation,
            shouldReusePreviousPreferences = followUpRecommendation
        )
    }

    fun shouldExposeMovieSuggestions(
        question: String,
        parsedIntent: ParsedSupportChatIntent,
        memory: SupportChatMemoryContext = SupportChatMemoryContext()
    ): Boolean {
        return parsedIntent == ParsedSupportChatIntent.RECOMMENDATION
            || parsedIntent == ParsedSupportChatIntent.MIXED
            || analyzeRecommendationRequest(question, memory).shouldSuggestMovies
    }

    fun shouldAvoidPreviouslySuggestedMovies(
        question: String,
        memory: SupportChatMemoryContext = SupportChatMemoryContext()
    ): Boolean {
        return analyzeRecommendationRequest(question, memory).isAlternativeRequest
    }

    private fun isRecommendationSession(memory: SupportChatMemoryContext): Boolean {
        val normalizedIntent = memory.lastIntent.orEmpty().normalizeForLookup()
        return normalizedIntent.contains("movie_recommendation")
            || normalizedIntent.contains("recommend")
            || normalizedIntent.contains("suggest")
            || normalizedIntent.contains("goi y")
            || normalizedIntent.contains("de xuat")
    }

    private fun String.normalizeForLookup(): String {
        if (isBlank()) return ""
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "d")
            .replace("Đ", "D")
            .lowercase(Locale.ROOT)
            .trim()
    }

    private val RECOMMENDATION_HINTS = listOf(
        "goi y",
        "de xuat",
        "de cu",
        "recommend",
        "suggest",
        "tu van phim",
        "xem gi",
        "xem phim gi",
        "nen xem",
        "co phim nao",
        "phim nao hay",
        "phim nao",
        "gioi thieu"
    )

    private val RECOMMENDATION_FOLLOW_UP_HINTS = listOf(
        "khong thich",
        "khong hop",
        "chua ung",
        "thi sao",
        "doi phim khac",
        "goi y lai",
        "phim khac",
        "khac di",
        "khac nua",
        "doi bo khac",
        "thu bo khac",
        "goi y bo khac",
        "de xuat phim khac"
    )
}

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
            val recommendationRequest = SupportSuggestionPolicy.analyzeRecommendationRequest(
                question = question,
                memory = requestMemory
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
                return@withContext buildOfflineReply(
                    question = question,
                    sessionId = sessionId,
                    isKidsMode = isKidsMode,
                    memory = requestMemory
                )
            }

            val apiResponse = runCatching {
                gson.fromJson(rawBody, SupportChatApiResponse::class.java)
            }.getOrNull()
            val parsed = SupportChatResponseParser.parse(rawBody)
            val shouldExposeSuggestions = parsed.movieSuggestions.isNotEmpty()
                || SupportSuggestionPolicy.shouldExposeMovieSuggestions(
                    question = question,
                    parsedIntent = parsed.intent,
                    memory = requestMemory
                )
            val enrichedMovies = if (shouldExposeSuggestions) {
                enrichMovieSuggestions(
                    parsedSuggestions = parsed.movieSuggestions,
                    isKidsMode = isKidsMode
                )
            } else {
                emptyList()
            }
            val filteredEnrichedMovies = if (recommendationRequest.isAlternativeRequest) {
                enrichedMovies.filterNot { it.matchesReferencedMovie(requestMemory) }
            } else {
                enrichedMovies
            }
            val endpointMovies = if (shouldExposeSuggestions && filteredEnrichedMovies.size < MAX_SUGGESTIONS) {
                fetchRecommendationEndpointMovies(
                    question = question,
                    isKidsMode = isKidsMode,
                    excludeSlugs = buildSet {
                        addAll(filteredEnrichedMovies.mapNotNull { it.slug })
                        addAll(filteredEnrichedMovies.mapNotNull { it.movieId })
                        if (recommendationRequest.isAlternativeRequest) {
                            addAll(requestMemory.referencedMovieSlugs)
                        }
                    },
                    limit = MAX_SUGGESTIONS - filteredEnrichedMovies.size
                )
            } else {
                emptyList()
            }
            val fallbackExcludeSlugs = buildSet {
                if (recommendationRequest.isAlternativeRequest) {
                    addAll(requestMemory.referencedMovieSlugs)
                }
                addAll(filteredEnrichedMovies.mapNotNull { it.slug })
                addAll(filteredEnrichedMovies.mapNotNull { it.movieId })
                addAll(endpointMovies.mapNotNull { it.slug })
                addAll(endpointMovies.mapNotNull { it.movieId })
            }
            val suggestedCount = filteredEnrichedMovies.size + endpointMovies.size
            val fallbackMovies = if (shouldExposeSuggestions && suggestedCount < MAX_SUGGESTIONS) {
                buildFallbackSuggestions(
                    question = question,
                    isKidsMode = isKidsMode,
                    memory = requestMemory,
                    excludeSlugs = fallbackExcludeSlugs,
                    limit = MAX_SUGGESTIONS - suggestedCount
                )
            } else {
                emptyList()
            }
            val movieItems = (filteredEnrichedMovies + endpointMovies + fallbackMovies)
                .distinctBy { it.slug ?: it.movieId ?: it.id }
                .take(MAX_SUGGESTIONS)
            val metadata = movieItems.takeIf { it.isNotEmpty() }
                ?.let { SupportChatMetadata(movieItems = it) }
            val usedSupplementalRecommendations = filteredEnrichedMovies.isEmpty() && movieItems.isNotEmpty()

            SupportChatReply(
                text = resolveReplyText(
                    parsedText = apiResponse?.answer?.takeIf { it.isNotBlank() } ?: parsed.text,
                    metadata = metadata,
                    preferRecommendationIntro = recommendationRequest.shouldSuggestMovies && usedSupplementalRecommendations
                ),
                metadata = metadata,
                memory = requestMemory.mergeWith(parsed.memory),
                sessionId = apiResponse?.sessionId ?: parsed.sessionId,
                historyMessageCount = apiResponse?.historyMessageCount ?: parsed.historyMessageCount
            )
        }
    }

    private suspend fun fetchRecommendationEndpointMovies(
        question: String,
        isKidsMode: Boolean,
        excludeSlugs: Set<String>,
        limit: Int
    ): List<SupportChatMovieItem> {
        if (limit <= 0) return emptyList()

        val rawBody = runCatching {
            val response = api.recommend(
                SupportRecommendRequest(
                    query = question,
                    topN = limit
                )
            )
            val body = response.body()?.string().orEmpty()
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            body
        }.getOrNull() ?: return emptyList()

        val parsed = SupportChatResponseParser.parse(rawBody)
        return enrichMovieSuggestions(
            parsedSuggestions = parsed.movieSuggestions,
            isKidsMode = isKidsMode
        ).filterNot { movie ->
            val keys = listOfNotNull(movie.slug, movie.movieId, movie.id)
            keys.any { it in excludeSlugs }
        }.take(limit)
    }

    private suspend fun buildOfflineReply(
        question: String,
        sessionId: String,
        isKidsMode: Boolean,
        memory: SupportChatMemoryContext
    ): SupportChatReply {
        val fallbackMovies = buildFallbackSuggestions(
            question = question,
            isKidsMode = isKidsMode,
            memory = memory,
            excludeSlugs = memory.referencedMovieSlugs.toSet()
                .takeIf { SupportSuggestionPolicy.shouldAvoidPreviouslySuggestedMovies(question, memory) }
                .orEmpty()
        )
        val metadata = fallbackMovies.takeIf { it.isNotEmpty() }
            ?.let { SupportChatMetadata(movieItems = it) }

        return SupportChatReply(
            text = resolveReplyText(FALLBACK_REPLY, metadata),
            metadata = metadata,
            memory = memory,
            sessionId = sessionId
        )
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

    private suspend fun buildFallbackSuggestions(
        question: String,
        isKidsMode: Boolean,
        memory: SupportChatMemoryContext = SupportChatMemoryContext(),
        excludeSlugs: Set<String> = emptySet(),
        limit: Int = MAX_SUGGESTIONS
    ): List<SupportChatMovieItem> {
        if (limit <= 0) return emptyList()

        val recommendationRequest = SupportSuggestionPolicy.analyzeRecommendationRequest(
            question = question,
            memory = memory
        )
        if (!recommendationRequest.shouldSuggestMovies) return emptyList()

        val normalizedExcludeSlugs = excludeSlugs
            .mapNotNull { it.normalizedValue() }
            .toSet()
        val genre = extractRequestedGenre(question)
            ?: memory.genres.lastOrNull().takeIf { recommendationRequest.shouldReusePreviousPreferences }
        val type = extractRequestedType(question)
        val searchKeyword = extractSearchKeyword(question)
            ?.takeUnless { recommendationRequest.shouldReusePreviousPreferences }

        val candidates = when {
            genre != null -> firestoreRepository.getMoviesByCategory(
                category = genre,
                isKidsMode = isKidsMode,
                limit = limit,
                excludeSlugs = normalizedExcludeSlugs
            )
            type != null -> firestoreRepository.getMoviesByType(
                type = type,
                isKidsMode = isKidsMode,
                limit = limit,
                excludeSlugs = normalizedExcludeSlugs
            )
            searchKeyword != null -> firestoreRepository.searchMovies(
                keyword = searchKeyword,
                isKidsMode = isKidsMode,
                limit = limit + normalizedExcludeSlugs.size
            )
            else -> firestoreRepository.getMovies(
                isKidsMode = isKidsMode,
                limit = limit + normalizedExcludeSlugs.size
            )
        }.filterNot { it.slug in normalizedExcludeSlugs }
            .take(limit)

        return candidates.map { movie ->
            SupportChatMovieItem(
                id = movie.slug.ifBlank { movie.title.normalizeForLookup() },
                title = movie.title,
                subtitle = movie.originName,
                posterUrl = movie.posterUrl.normalizedValue(),
                year = movie.year.takeIf { it > 0 }?.toString().orEmpty(),
                slug = movie.slug,
                movieId = movie.slug,
                actions = listOf(
                    SupportChatActionFactory.createWatchMovieAction(
                        slug = movie.slug,
                        movieId = movie.slug
                    )
                )
            )
        }
    }

    private fun resolveReplyText(
        parsedText: String,
        metadata: SupportChatMetadata?,
        preferRecommendationIntro: Boolean = false
    ): String {
        val cleanedText = parsedText.trim()
        return when {
            preferRecommendationIntro && metadata?.movieItems?.isNotEmpty() == true -> {
                "Mình gợi ý vài phim phù hợp để bạn xem ngay:"
            }
            cleanedText.isNotBlank() && cleanedText != FALLBACK_REPLY -> cleanedText
            metadata?.movieItems?.isNotEmpty() == true -> "Mình gợi ý vài phim bạn có thể xem ngay:"
            else -> FALLBACK_REPLY
        }
    }

    private fun extractRequestedGenre(question: String): String? {
        val normalized = question.normalizeForLookup()
        return GENRE_KEYWORDS.firstOrNull { (keyword, _) ->
            normalized.contains(keyword)
        }?.second
    }

    private fun extractRequestedType(question: String): String? {
        val normalized = question.normalizeForLookup()
        return TYPE_KEYWORDS.firstOrNull { (keyword, _) ->
            normalized.contains(keyword)
        }?.second
    }

    private fun extractSearchKeyword(question: String): String? {
        val normalized = question.normalizeForLookup()
        val keyword = normalized
            .split(Regex("\\s+"))
            .filterNot { it in STOP_WORDS }
            .joinToString(" ")
            .trim()
        return keyword.takeIf { it.length >= 2 }
    }

    private fun String?.normalizedValue(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun String.normalizeForLookup(): String {
        if (isBlank()) return ""
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "d")
            .replace("Đ", "D")
            .lowercase(Locale.ROOT)
            .trim()
    }

    private fun SupportChatMovieItem.matchesReferencedMovie(
        memory: SupportChatMemoryContext
    ): Boolean {
        val referencedSlugs = memory.referencedMovieSlugs
            .mapNotNull { it.normalizedValue() }
            .toSet()
        val referencedTitles = memory.referencedMovieTitles
            .map { it.normalizeForLookup() }
            .toSet()

        return slug.normalizedValue() in referencedSlugs
            || movieId.normalizedValue() in referencedSlugs
            || title.normalizeForLookup() in referencedTitles
    }

    companion object {
        const val FALLBACK_REPLY = "Xin lỗi, hiện không thể trả lời"
        private const val MAX_SUGGESTIONS = 5
        private const val MAX_HISTORY_TURNS = 8

        private val GENRE_KEYWORDS = listOf(
            "kinh di" to "Kinh dị",
            "hanh dong" to "Hành động",
            "tinh cam" to "Tình cảm",
            "tam ly" to "Tâm lý",
            "vien tuong" to "Viễn tưởng",
            "hai huoc" to "Hài hước",
            "vo thuat" to "Võ thuật",
            "co trang" to "Cổ trang",
            "chien tranh" to "Chiến tranh",
            "the thao" to "Thể thao"
        )

        private val TYPE_KEYWORDS = listOf(
            "phim bo" to "series",
            "series" to "series",
            "phim le" to "single",
            "le" to "single",
            "anime" to "hoathinh",
            "hoat hinh" to "hoathinh",
            "tv show" to "tvshows",
            "gameshow" to "tvshows"
        )

        private val STOP_WORDS = setOf(
            "goi",
            "y",
            "de",
            "xuat",
            "cho",
            "toi",
            "minh",
            "mot",
            "bo",
            "phim",
            "di",
            "nhe",
            "voi",
            "xem",
            "ngay"
        )
    }
}
