package com.example.alphacinema.data.repository

import com.example.alphacinema.data.model.SupportChatLinkItem
import com.example.alphacinema.data.model.SupportChatMemoryContext
import com.example.alphacinema.data.model.SupportChatMetadata
import com.example.alphacinema.data.model.SupportChatMovieItem
import com.example.alphacinema.data.model.SupportChatReply
import com.example.alphacinema.data.model.SupportMusicLookup
import com.example.alphacinema.data.model.SupportMusicSearchFailure
import com.example.alphacinema.data.model.SupportMusicSearchResult
import java.text.Normalizer
import java.util.Locale

internal object SupportMusicLinkPolicy {
    fun buildMusicReply(
        question: String,
        previousMovies: List<SupportChatMovieItem> = emptyList(),
        spotifyResult: SupportMusicSearchResult? = null,
        spotifyFailure: SupportMusicSearchFailure? = null,
        spotifyFailureDetail: String? = null
    ): SupportChatReply? {
        val lookup = buildMusicLookup(
            question = question,
            previousMovies = previousMovies
        ) ?: return null

        return buildMusicReply(
            lookup = lookup,
            spotifyResult = spotifyResult,
            spotifyFailure = spotifyFailure,
            spotifyFailureDetail = spotifyFailureDetail
        )
    }

    fun buildMusicLookup(
        question: String,
        previousMovies: List<SupportChatMovieItem> = emptyList()
    ): SupportMusicLookup? {
        val normalized = question.normalizeForLookup()
        if (!isMusicRequest(normalized, previousMovies.isNotEmpty())) return null

        val matchedMovie = findReferencedMovie(normalized, previousMovies)
        val target = matchedMovie?.title?.takeIf { it.isNotBlank() }
            ?: extractSearchTarget(normalized)?.toDisplayTitle()

        return SupportMusicLookup(
            target = target,
            query = target?.takeIf { it.isNotBlank() }?.let { "$it soundtrack" },
            matchedMovieSlug = matchedMovie?.slug
        )
    }

    fun buildMusicReply(
        lookup: SupportMusicLookup,
        spotifyResult: SupportMusicSearchResult? = null,
        spotifyFailure: SupportMusicSearchFailure? = null,
        spotifyFailureDetail: String? = null
    ): SupportChatReply {
        val target = lookup.target?.takeIf { it.isNotBlank() }
        if (target.isNullOrBlank()) {
            return SupportChatReply(
                text = "Ban gui them ten phim, minh se lay album Spotify cu the cho dung.",
                memory = SupportChatMemoryContext(
                    lastIntent = "music_request",
                    topics = listOf("music", "movies")
                )
            )
        }

        if (spotifyResult == null) {
            return SupportChatReply(
                text = spotifyFailureMessage(target, spotifyFailure, spotifyFailureDetail),
                memory = SupportChatMemoryContext(
                    lastIntent = "music_request",
                    topics = listOf("music", "movies"),
                    referencedMovieSlugs = listOfNotNull(lookup.matchedMovieSlug),
                    referencedMovieTitles = listOf(target)
                )
            )
        }

        val linkItem = SupportChatLinkItem(
            id = "spotify-${spotifyResult.id.toStableId()}",
            label = spotifyResult.name,
            subtitle = spotifyResult.toSubtitle(),
            url = spotifyResult.externalUrl,
            provider = "Spotify",
            uri = spotifyResult.uri?.takeIf { it.isNotBlank() },
            thumbnailUrl = spotifyResult.imageUrl?.takeIf { it.isNotBlank() },
            contentId = spotifyResult.id,
            contentType = spotifyResult.type.takeIf { it.isNotBlank() }
        )

        return SupportChatReply(
            text = "Minh da tim thay album Spotify cho nhac phim $target.",
            metadata = SupportChatMetadata(linkItems = listOf(linkItem)),
            memory = SupportChatMemoryContext(
                lastIntent = "music_request",
                topics = listOf("music", "movies"),
                referencedMovieSlugs = listOfNotNull(lookup.matchedMovieSlug),
                referencedMovieTitles = listOf(target)
            )
        )
    }

    private fun isMusicRequest(normalized: String, hasPreviousMovies: Boolean): Boolean {
        if (normalized.isBlank()) return false

        val hasSoundtrackHint = SOUNDTRACK_HINTS.any { normalized.contains(it) }
        val hasMusicAction = MUSIC_ACTION_HINTS.any { normalized.contains(it) }
        val hasMovieScope = MOVIE_SCOPE_HINTS.any { normalized.contains(it) }
        val hasContextReference = hasPreviousMovies && CONTEXTUAL_MOVIE_HINTS.any { normalized.contains(it) }
        val hasExplicitTarget = extractSearchTarget(normalized) != null

        return hasSoundtrackHint
            || (hasMusicAction && (hasMovieScope || hasContextReference || hasExplicitTarget))
    }

    private fun findReferencedMovie(
        normalized: String,
        previousMovies: List<SupportChatMovieItem>
    ): SupportChatMovieItem? {
        if (previousMovies.isEmpty()) return null

        previousMovies.firstOrNull { movie ->
            val movieTitle = movie.title.normalizeForLookup()
            movieTitle.isNotBlank() && normalized.contains(movieTitle)
        }?.let { return it }

        val referencesPreviousMovie = CONTEXTUAL_MOVIE_HINTS.any { normalized.contains(it) }
        return if (referencesPreviousMovie) previousMovies.first() else null
    }

    private fun extractSearchTarget(normalized: String): String? {
        var text = normalized
        TARGET_PHRASES.forEach { phrase ->
            text = text.replace(phrase, " ")
        }
        text = text
            .replace(Regex("\\b(${TARGET_STOP_WORDS.joinToString("|")})\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return text.takeIf { it.length >= MIN_TARGET_LENGTH && it !in CONTEXT_ONLY_TARGETS }
    }

    private fun SupportMusicSearchResult.toSubtitle(): String {
        val typeLabel = type.toDisplayTitle().ifBlank { "Album" }
        return buildList {
            add("$typeLabel Spotify")
            if (artistNames.isNotEmpty()) {
                add(artistNames.take(2).joinToString(", "))
            }
            add("ID $id")
        }.joinToString(" - ")
    }

    private fun spotifyFailureMessage(
        target: String,
        failure: SupportMusicSearchFailure?,
        detail: String?
    ): String {
        if (detail?.contains("Active premium subscription required", ignoreCase = true) == true) {
            return "Spotify dang chan Search API vi tai khoan Spotify so huu app tren Developer Dashboard chua duoc nhan la Premium. Hay nang cap dung tai khoan owner cua app hoac tao Spotify app moi bang tai khoan Premium, doi vai gio de Spotify dong bo roi thu lai. Chi tiet: $detail"
        }

        val baseMessage = when (failure) {
            SupportMusicSearchFailure.MISSING_CREDENTIALS -> {
                "Ban build hien tai chua co Spotify Client ID/Secret, nen chua lay duoc album cu the cho nhac phim $target."
            }
            SupportMusicSearchFailure.TOKEN_REQUEST_FAILED -> {
                "Da co Spotify Client ID/Secret, nhung chua lay duoc access token tu Spotify. Hay kiem tra mang tren thiet bi hoac Spotify Accounts API roi thu lai."
            }
            SupportMusicSearchFailure.SEARCH_REQUEST_FAILED -> {
                "Da xac thuc duoc Spotify, nhung Spotify Search API chua tra duoc album cho nhac phim $target. Hay kiem tra mang tren thiet bi roi thu lai."
            }
            SupportMusicSearchFailure.EMPTY_RESULT -> {
                "Spotify API khong tra ve album phu hop cho nhac phim $target."
            }
            null -> {
                "Minh chua lay duoc album cu the tu Spotify API cho nhac phim $target."
            }
        }
        return detail?.takeIf { it.isNotBlank() }?.let { "$baseMessage Chi tiet: $it" }
            ?: baseMessage
    }

    private fun String.toStableId(): String {
        return normalizeForLookup()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "music" }
    }

    private fun String.toDisplayTitle(): String {
        return trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
                }
            }
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

    private const val MIN_TARGET_LENGTH = 2

    private val SOUNDTRACK_HINTS = listOf(
        "nhac phim",
        "nhac trong phim",
        "nhac tu phim",
        "nhac tu bo phim",
        "nhac cua phim",
        "bai hat phim",
        "bai hat trong phim",
        "ca khuc trong phim",
        "soundtrack",
        "ost",
        "theme song"
    )

    private val MUSIC_ACTION_HINTS = listOf(
        "cho xin nhac",
        "xin nhac",
        "tim nhac",
        "nghe nhac",
        "nhac tu",
        "nhac trong",
        "nhac cua",
        "bai nhac",
        "bai hat",
        "ca khuc",
        "soundtrack",
        "ost"
    )

    private val MOVIE_SCOPE_HINTS = listOf(
        "phim",
        "bo phim",
        "movie",
        "film"
    )

    private val CONTEXTUAL_MOVIE_HINTS = listOf(
        "phim do",
        "bo phim do",
        "phim nay",
        "bo nay",
        "trong do",
        "vua goi y",
        "o tren"
    )

    private val TARGET_PHRASES = listOf(
        "cho xin nhac",
        "xin nhac",
        "tim nhac",
        "nghe nhac",
        "mo giao dien album",
        "giao dien album",
        "nhac phim",
        "nhac trong phim",
        "nhac tu phim",
        "nhac tu bo phim",
        "nhac cua phim",
        "bai hat trong phim",
        "bai hat phim",
        "ca khuc trong phim",
        "soundtrack",
        "theme song"
    )

    private val TARGET_STOP_WORDS = listOf(
        "cho",
        "xin",
        "minh",
        "toi",
        "em",
        "anh",
        "chi",
        "ban",
        "link",
        "duong",
        "dan",
        "spotify",
        "album",
        "giao",
        "dien",
        "tren",
        "mo",
        "giup",
        "nhe",
        "di",
        "voi",
        "duoc",
        "khong",
        "tu",
        "trong",
        "cua",
        "bo",
        "mot",
        "phim",
        "movie",
        "film",
        "nhac",
        "bai",
        "hat",
        "ca",
        "khuc",
        "ost",
        "theme",
        "song"
    )

    private val CONTEXT_ONLY_TARGETS = setOf(
        "do",
        "nay",
        "tren"
    )
}
