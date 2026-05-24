package com.example.alphacinema.data.model

import com.google.gson.annotations.SerializedName

data class SupportChatRequest(
    val question: String,
    @SerializedName("top_k")
    val topK: Int = 6,
    @SerializedName("top_n_recommendations")
    val topNRecommendations: Int = 5,
    @SerializedName("generation_model")
    val generationModel: String = "gpt-4o-mini",
    @SerializedName("session_id")
    val sessionId: String? = null,
    @SerializedName("remember_history")
    val rememberHistory: Boolean = true,
    @SerializedName("chat_history")
    val chatHistory: List<SupportChatHistoryMessage>? = null
)

data class SupportChatHistoryMessage(
    val role: String,
    val content: String
)

data class SupportChatApiResponse(
    val answer: String? = null,
    val intent: String? = null,
    val mode: String? = null,
    val sources: List<SupportChatSource> = emptyList(),
    val recommendations: List<SupportChatRecommendation> = emptyList(),
    @SerializedName("standalone_question")
    val standaloneQuestion: String? = null,
    @SerializedName("session_id")
    val sessionId: String? = null,
    @SerializedName("history_message_count")
    val historyMessageCount: Int? = null
)

data class SupportChatSource(
    val title: String? = null,
    val content: String? = null,
    val url: String? = null
)

data class SupportChatRecommendation(
    val id: String? = null,
    @SerializedName("movie_id")
    val movieId: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerializedName("origin_name")
    val originName: String? = null,
    val slug: String? = null,
    val year: String? = null,
    val poster: String? = null,
    @SerializedName("poster_url")
    val posterUrl: String? = null,
    @SerializedName("thumb_url")
    val thumbUrl: String? = null,
    val reason: String? = null
)

data class SupportRecommendRequest(
    val query: String,
    @SerializedName("top_n")
    val topN: Int = 5
)

data class SupportRecommendApiResponse(
    val recommendations: List<SupportChatRecommendation> = emptyList()
)

data class SupportChatMemoryContext(
    val summary: String? = null,
    @SerializedName("last_intent")
    val lastIntent: String? = null,
    val topics: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    @SerializedName("referenced_movie_slugs")
    val referencedMovieSlugs: List<String> = emptyList(),
    @SerializedName("referenced_movie_titles")
    val referencedMovieTitles: List<String> = emptyList(),
    @SerializedName("kids_mode_enabled")
    val kidsModeEnabled: Boolean? = null,
    val surface: String = "support_chat"
)

data class SupportChatResponseContract(
    val version: String = "2026-05-13",
    @SerializedName("include_intent")
    val includeIntent: Boolean = true,
    @SerializedName("supported_intents")
    val supportedIntents: List<String> = listOf(
        "policy",
        "movie",
        "recommendation",
        "mixed"
    ),
    @SerializedName("recommendation_key")
    val recommendationKey: String = "recommendations",
    @SerializedName("required_recommendation_fields")
    val requiredRecommendationFields: List<String> = listOf(
        "title",
        "slug"
    ),
    val rules: List<String> = listOf(
        "Return an answer for every successful /ask response.",
        "Use recommendations only when the answer includes movie suggestions.",
        "Preserve session_id for multi-turn conversations."
    )
)

enum class SupportMessageSender {
    USER,
    BOT,
    LOADING
}

enum class SupportChatActionType {
    PLAY_MOVIE,
    VIEW_DETAIL,
    SAVE_TO_LIST,
    WATCH_TRAILER
}

enum class SupportChatRouteDestination {
    PLAYER,
    DETAIL
}

data class SupportChatRoute(
    val destination: SupportChatRouteDestination,
    val movieId: String? = null,
    val slug: String? = null,
    val episodeId: String? = null,
    val deeplink: String? = null
)

data class SupportChatAction(
    val type: SupportChatActionType,
    val label: String,
    val enabled: Boolean = true,
    val primaryRoute: SupportChatRoute? = null,
    val fallbackRoute: SupportChatRoute? = null
)

data class SupportChatMovieItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String? = null,
    val year: String = "",
    val slug: String? = null,
    val movieId: String? = null,
    val actions: List<SupportChatAction> = emptyList()
)

data class SupportChatLinkItem(
    val id: String,
    val label: String,
    val subtitle: String = "",
    val url: String,
    val provider: String = "",
    val uri: String? = null,
    val thumbnailUrl: String? = null,
    val contentId: String? = null,
    val contentType: String? = null
)

data class SupportChatMetadata(
    val movieItems: List<SupportChatMovieItem> = emptyList(),
    val actions: List<SupportChatAction> = emptyList(),
    val linkItems: List<SupportChatLinkItem> = emptyList()
)

data class SupportChatMessage(
    val id: String,
    val text: String,
    val sender: SupportMessageSender,
    val timestamp: String,
    val metadata: SupportChatMetadata? = null
)

data class SupportChatSession(
    val id: String,
    val title: String,
    val updatedAtMillis: Long,
    val messages: List<SupportChatMessage> = emptyList()
)

data class SupportChatReply(
    val text: String,
    val metadata: SupportChatMetadata? = null,
    val memory: SupportChatMemoryContext? = null,
    val sessionId: String? = null,
    val historyMessageCount: Int? = null
)

fun SupportChatAction.resolveRoute(): SupportChatRoute? = primaryRoute ?: fallbackRoute

fun SupportChatMemoryContext.mergeWith(newer: SupportChatMemoryContext?): SupportChatMemoryContext {
    if (newer == null) return this

    return SupportChatMemoryContext(
        summary = newer.summary ?: summary,
        lastIntent = newer.lastIntent ?: lastIntent,
        topics = (topics + newer.topics).distinct(),
        genres = (genres + newer.genres).distinct(),
        referencedMovieSlugs = (referencedMovieSlugs + newer.referencedMovieSlugs).distinct(),
        referencedMovieTitles = (referencedMovieTitles + newer.referencedMovieTitles).distinct(),
        kidsModeEnabled = newer.kidsModeEnabled ?: kidsModeEnabled,
        surface = newer.surface.ifBlank { surface }
    )
}

fun SupportChatMovieItem.primaryAction(): SupportChatAction? {
    return actions.firstOrNull { it.type == SupportChatActionType.PLAY_MOVIE }
        ?: actions.firstOrNull()
}

internal object SupportChatActionFactory {
    fun createWatchMovieAction(
        slug: String?,
        movieId: String?,
        preferredDestination: SupportChatRouteDestination? = SupportChatRouteDestination.PLAYER,
        playable: Boolean? = null,
        episodeId: String? = null,
        deeplink: String? = null
    ): SupportChatAction {
        val resolvedSlug = slug.orNormalizedIdentifier()
            ?: movieId.orNormalizedIdentifier()
            ?: extractSlugFromDeeplink(deeplink)
        val resolvedMovieId = movieId.orNormalizedIdentifier() ?: resolvedSlug
        val detailRoute = resolvedSlug?.let {
            SupportChatRoute(
                destination = SupportChatRouteDestination.DETAIL,
                movieId = resolvedMovieId,
                slug = it,
                deeplink = deeplink
            )
        }
        val shouldOpenPlayer = playable != false && preferredDestination != SupportChatRouteDestination.DETAIL
        val primaryRoute = when {
            shouldOpenPlayer && resolvedSlug != null -> SupportChatRoute(
                destination = SupportChatRouteDestination.PLAYER,
                movieId = resolvedMovieId,
                slug = resolvedSlug,
                episodeId = episodeId,
                deeplink = deeplink
            )
            detailRoute != null -> detailRoute
            else -> null
        }

        return SupportChatAction(
            type = SupportChatActionType.PLAY_MOVIE,
            label = "Xem phim",
            enabled = primaryRoute != null || detailRoute != null,
            primaryRoute = primaryRoute ?: detailRoute,
            fallbackRoute = if (primaryRoute?.destination == SupportChatRouteDestination.PLAYER) {
                detailRoute
            } else {
                null
            }
        )
    }

    private fun String?.orNormalizedIdentifier(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun extractSlugFromDeeplink(deeplink: String?): String? {
        val trimmed = deeplink?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        return trimmed
            .substringAfterLast("/")
            .substringBefore("?")
            .trim()
            .takeIf { it.isNotEmpty() }
    }
}
