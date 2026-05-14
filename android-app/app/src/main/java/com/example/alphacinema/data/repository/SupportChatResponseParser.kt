package com.example.alphacinema.data.repository

import com.example.alphacinema.data.model.SupportChatMemoryContext
import com.example.alphacinema.data.model.SupportChatRouteDestination
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Locale

internal data class ParsedSupportChatMovieSuggestion(
    val id: String? = null,
    val title: String = "",
    val subtitle: String = "",
    val posterUrl: String? = null,
    val year: String = "",
    val slug: String? = null,
    val movieId: String? = null,
    val preferredDestination: SupportChatRouteDestination? = null,
    val episodeId: String? = null,
    val deeplink: String? = null,
    val playable: Boolean? = null
)

internal data class ParsedSupportChatPayload(
    val text: String,
    val intent: ParsedSupportChatIntent = ParsedSupportChatIntent.UNKNOWN,
    val memory: SupportChatMemoryContext? = null,
    val movieSuggestions: List<ParsedSupportChatMovieSuggestion> = emptyList(),
    val sessionId: String? = null,
    val historyMessageCount: Int? = null
)

internal enum class ParsedSupportChatIntent {
    POLICY,
    MOVIE,
    RECOMMENDATION,
    MIXED,
    UNKNOWN
}

internal object SupportChatResponseParser {
    fun parse(raw: String): ParsedSupportChatPayload {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return ParsedSupportChatPayload(text = SupportRepository.FALLBACK_REPLY)
        }

        val root = runCatching { JsonParser.parseString(trimmed) }.getOrNull()
        val intent = root?.let(::extractIntent) ?: ParsedSupportChatIntent.UNKNOWN
        val memory = root?.let(::extractMemoryContext)
        val sessionId = root?.let(::extractSessionId)
        val historyMessageCount = root?.let(::extractHistoryMessageCount)
        val movieSuggestions = root
            ?.let(::extractMovieSuggestions)
            .orEmpty()
            .distinctBy(::suggestionKey)
        val text = cleanReplyText(extractTextFromElement(root) ?: trimmed)
        val resolvedText = when {
            text.isNotBlank() -> text
            movieSuggestions.isNotEmpty() -> "Bạn có thể xem:"
            else -> SupportRepository.FALLBACK_REPLY
        }

        return ParsedSupportChatPayload(
            text = resolvedText,
            intent = intent,
            memory = memory,
            movieSuggestions = movieSuggestions,
            sessionId = sessionId,
            historyMessageCount = historyMessageCount
        )
    }

    private fun extractMovieSuggestions(element: JsonElement?): List<ParsedSupportChatMovieSuggestion> {
        element ?: return emptyList()
        if (element.isJsonNull || element.isJsonPrimitive) return emptyList()

        return when {
            element.isJsonArray -> {
                val array = element.asJsonArray
                val directItems = array.mapNotNull { item ->
                    item.takeIf { it.isJsonObject }?.asJsonObject?.let(::parseMovieSuggestion)
                }
                if (directItems.isNotEmpty()) {
                    directItems
                } else {
                    array.flatMap(::extractMovieSuggestions)
                }
            }

            element.isJsonObject -> {
                val jsonObject = element.asJsonObject
                val preferredCollections = MOVIE_COLLECTION_KEYS.asSequence()
                    .flatMap { key -> extractMovieSuggestions(jsonObject.get(key)).asSequence() }
                    .toList()
                if (preferredCollections.isNotEmpty()) {
                    preferredCollections
                } else {
                    parseMovieSuggestion(jsonObject)?.let(::listOf)
                        ?: jsonObject.entrySet()
                            .asSequence()
                            .flatMap { (_, value) -> extractMovieSuggestions(value).asSequence() }
                            .toList()
                }
            }

            else -> emptyList()
        }
    }

    private fun parseMovieSuggestion(jsonObject: JsonObject): ParsedSupportChatMovieSuggestion? {
        jsonObject.getAsJsonObjectOrNull("movie")?.let { nestedMovie ->
            return parseMovieSuggestion(nestedMovie)
        }

        val title = stringValue(jsonObject, TITLE_KEYS)
        val slug = stringValue(jsonObject, SLUG_KEYS)
        val movieId = stringValue(jsonObject, MOVIE_ID_KEYS)
        val posterUrl = normalizePosterUrl(stringValue(jsonObject, POSTER_KEYS))
        val subtitle = stringValue(jsonObject, SUBTITLE_KEYS).orEmpty().ifBlank {
            stringList(jsonObject, REASON_KEYS + CATEGORY_KEYS)
                .take(2)
                .joinToString(" • ")
        }
        val year = stringValue(jsonObject, YEAR_KEYS).orEmpty()
        val deeplink = stringValue(jsonObject, DEEPLINK_KEYS)
        val episodeId = stringValue(jsonObject, EPISODE_KEYS)
        val playable = booleanValue(jsonObject, PLAYABLE_KEYS)
            ?: if (!stringValue(jsonObject, PLAYABLE_LINK_KEYS).isNullOrBlank()) true else null
        val preferredDestination = jsonObject.getAsJsonObjectOrNull("route")?.let(::destinationFromObject)
            ?: destinationFromString(stringValue(jsonObject, ROUTE_KEYS))
            ?: destinationFromString(deeplink)
            ?: if (playable == false) SupportChatRouteDestination.DETAIL else null

        val identifier = stringValue(jsonObject, IDENTIFIER_KEYS)
        val looksLikeMovie = title.orEmpty().isNotBlank()
            || slug.orEmpty().isNotBlank()
            || movieId.orEmpty().isNotBlank()
            || posterUrl.orEmpty().isNotBlank()
            || deeplink.orEmpty().isNotBlank()

        if (!looksLikeMovie) return null

        return ParsedSupportChatMovieSuggestion(
            id = identifier,
            title = title.orEmpty(),
            subtitle = subtitle,
            posterUrl = posterUrl,
            year = year,
            slug = slug,
            movieId = movieId,
            preferredDestination = preferredDestination,
            episodeId = episodeId,
            deeplink = deeplink,
            playable = playable
        )
    }

    private fun extractTextFromElement(element: JsonElement?): String? {
        element ?: return null
        if (element.isJsonNull) return null

        return when {
            element.isJsonPrimitive -> element.asJsonPrimitive.takeIf { it.isString }?.asString

            element.isJsonArray -> {
                element.asJsonArray.asSequence()
                    .mapNotNull { item ->
                        item.takeUnless { it.isJsonObject && parseMovieSuggestion(it.asJsonObject) != null }
                            ?.let(::extractTextFromElement)
                    }
                    .firstOrNull { it.isNotBlank() }
            }

            element.isJsonObject -> {
                val jsonObject = element.asJsonObject
                TEXT_KEYS.asSequence()
                    .mapNotNull { key -> jsonObject.get(key)?.let(::extractTextFromElement) }
                    .firstOrNull { it.isNotBlank() }
                    ?: jsonObject.entrySet()
                        .asSequence()
                        .filterNot { (key, _) -> key in MOVIE_COLLECTION_KEYS }
                        .mapNotNull { (_, value) ->
                            value.takeUnless { it.isJsonObject && parseMovieSuggestion(it.asJsonObject) != null }
                                ?.let(::extractTextFromElement)
                        }
                        .firstOrNull { it.isNotBlank() }
            }

            else -> null
        }
    }

    private fun extractIntent(element: JsonElement?): ParsedSupportChatIntent {
        element ?: return ParsedSupportChatIntent.UNKNOWN
        if (!element.isJsonObject) return ParsedSupportChatIntent.UNKNOWN

        val intentValue = stringValue(element.asJsonObject, INTENT_KEYS).normalizeForMatching()
        return when {
            intentValue == "policy" || intentValue.contains("app_policy") -> {
                ParsedSupportChatIntent.POLICY
            }
            intentValue == "movie" -> {
                ParsedSupportChatIntent.MOVIE
            }
            intentValue == "mixed" -> {
                ParsedSupportChatIntent.MIXED
            }
            intentValue.contains("recommend")
                || intentValue.contains("suggest")
                || intentValue.contains("movie_recommendation")
                || intentValue.contains("goi y")
                || intentValue.contains("de xuat") -> {
                ParsedSupportChatIntent.RECOMMENDATION
            }
            else -> ParsedSupportChatIntent.UNKNOWN
        }
    }

    private fun extractMemoryContext(element: JsonElement?): SupportChatMemoryContext? {
        element ?: return null
        if (!element.isJsonObject) return null

        val memoryObject = element.asJsonObject.getAsJsonObjectOrNull("memory") ?: return null
        return SupportChatMemoryContext(
            summary = stringValue(memoryObject, listOf("summary")),
            lastIntent = stringValue(memoryObject, listOf("last_intent", "lastIntent")),
            topics = stringList(memoryObject, listOf("topics")),
            genres = stringList(memoryObject, listOf("genres")),
            referencedMovieSlugs = stringList(
                memoryObject,
                listOf("referenced_movie_slugs", "referencedMovieSlugs")
            ),
            referencedMovieTitles = stringList(
                memoryObject,
                listOf("referenced_movie_titles", "referencedMovieTitles")
            ),
            kidsModeEnabled = booleanValue(
                memoryObject,
                listOf("kids_mode_enabled", "kidsModeEnabled")
            ),
            surface = stringValue(memoryObject, listOf("surface")).orEmpty().ifBlank { "support_chat" }
        )
    }

    private fun extractSessionId(element: JsonElement?): String? {
        element ?: return null
        if (!element.isJsonObject) return null
        return stringValue(element.asJsonObject, listOf("session_id", "sessionId"))
    }

    private fun extractHistoryMessageCount(element: JsonElement?): Int? {
        element ?: return null
        if (!element.isJsonObject) return null
        return intValue(element.asJsonObject, listOf("history_message_count", "historyMessageCount"))
    }

    private fun cleanReplyText(raw: String): String {
        return raw
            .removeSurrounding("\"")
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace(Regex("^(answer|response|reply|message)\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun destinationFromObject(jsonObject: JsonObject): SupportChatRouteDestination? {
        return destinationFromString(stringValue(jsonObject, DESTINATION_KEYS))
            ?: destinationFromString(stringValue(jsonObject, ROUTE_KEYS))
            ?: destinationFromString(stringValue(jsonObject, DEEPLINK_KEYS))
    }

    private fun destinationFromString(value: String?): SupportChatRouteDestination? {
        val normalized = value.normalizeForMatching()
        if (normalized.isBlank()) return null
        return when {
            normalized.contains("detail") || normalized.contains("chi tiet") -> {
                SupportChatRouteDestination.DETAIL
            }
            normalized.contains("player")
                || normalized.contains("play")
                || normalized.contains("watch")
                || normalized.contains("xem") -> {
                SupportChatRouteDestination.PLAYER
            }
            else -> null
        }
    }

    private fun suggestionKey(suggestion: ParsedSupportChatMovieSuggestion): String {
        return suggestion.slug
            ?: suggestion.movieId
            ?: suggestion.id
            ?: suggestion.title.normalizeForMatching()
    }

    private fun stringValue(jsonObject: JsonObject, keys: List<String>): String? {
        return keys.asSequence()
            .mapNotNull { key -> jsonObject.get(key)?.asStringOrNull() }
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
    }

    private fun booleanValue(jsonObject: JsonObject, keys: List<String>): Boolean? {
        return keys.asSequence()
            .mapNotNull { key -> jsonObject.get(key)?.asBooleanOrNull() }
            .firstOrNull()
    }

    private fun intValue(jsonObject: JsonObject, keys: List<String>): Int? {
        return keys.asSequence()
            .mapNotNull { key -> jsonObject.get(key)?.asIntOrNull() }
            .firstOrNull()
    }

    private fun stringList(jsonObject: JsonObject, keys: List<String>): List<String> {
        return keys.asSequence()
            .mapNotNull { key -> jsonObject.get(key) }
            .flatMap { element ->
                when {
                    element.isJsonArray -> element.asJsonArray.asSequence()
                        .mapNotNull { it.asStringOrNull()?.trim() }
                    else -> listOfNotNull(element.asStringOrNull()?.trim()).asSequence()
                }
            }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun JsonObject.getAsJsonObjectOrNull(memberName: String): JsonObject? {
        return get(memberName)?.takeIf { it.isJsonObject }?.asJsonObject
    }

    private fun JsonElement.asStringOrNull(): String? {
        if (!isJsonPrimitive) return null
        val primitive = asJsonPrimitive
        return when {
            primitive.isString -> primitive.asString
            primitive.isNumber || primitive.isBoolean -> primitive.toString()
            else -> null
        }
    }

    private fun JsonElement.asBooleanOrNull(): Boolean? {
        if (!isJsonPrimitive) return null
        val primitive = asJsonPrimitive
        if (primitive.isBoolean) return primitive.asBoolean
        if (!primitive.isString) return null

        return when (primitive.asString.trim().lowercase(Locale.ROOT)) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
    }

    private fun JsonElement.asIntOrNull(): Int? {
        if (!isJsonPrimitive) return null
        val primitive = asJsonPrimitive
        return when {
            primitive.isNumber -> runCatching { primitive.asInt }.getOrNull()
            primitive.isString -> primitive.asString.trim().toIntOrNull()
            else -> null
        }
    }

    private fun normalizePosterUrl(url: String?): String? {
        val trimmed = url?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        return if (trimmed.startsWith("http", ignoreCase = true)) {
            trimmed
        } else {
            "https://phimimg.com/$trimmed"
        }
    }

    private fun String?.normalizeForMatching(): String {
        if (this.isNullOrBlank()) return ""
        val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "d")
            .replace("Đ", "D")
            .lowercase(Locale.ROOT)
            .trim()
    }

    private val TEXT_KEYS = listOf(
        "answer",
        "response",
        "reply",
        "message",
        "text",
        "content",
        "result"
    )

    private val MOVIE_COLLECTION_KEYS = listOf(
        "movies",
        "movieItems",
        "movie_items",
        "items",
        "suggestions",
        "recommendations",
        "results"
    )

    private val IDENTIFIER_KEYS = listOf("id", "_id", "movieId", "movie_id")
    private val TITLE_KEYS = listOf("title", "name", "movieTitle", "movie_name")
    private val SUBTITLE_KEYS = listOf("subtitle", "originName", "origin_name", "reason")
    private val REASON_KEYS = listOf("reasons", "why_recommended", "whyRecommended")
    private val CATEGORY_KEYS = listOf("categories", "category", "genres", "genre")
    private val POSTER_KEYS = listOf("poster", "posterUrl", "poster_url", "thumbUrl", "thumb_url", "image")
    private val YEAR_KEYS = listOf("year", "releaseYear", "release_year")
    private val SLUG_KEYS = listOf("slug", "movieSlug", "movie_slug")
    private val MOVIE_ID_KEYS = listOf("movieId", "movie_id", "_id", "id")
    private val INTENT_KEYS = listOf("intent", "queryType", "query_type", "type")
    private val ROUTE_KEYS = listOf("route", "watchRoute", "playRoute", "play_route")
    private val DEEPLINK_KEYS = listOf("deeplink", "deepLink", "deep_link", "url")
    private val DESTINATION_KEYS = listOf("destination", "screen", "target")
    private val EPISODE_KEYS = listOf("episodeId", "episode_id")
    private val PLAYABLE_KEYS = listOf(
        "hasPlayback",
        "has_playback",
        "hasWatchLink",
        "has_watch_link",
        "canPlay",
        "can_play",
        "playable",
        "hasStream",
        "has_stream"
    )
    private val PLAYABLE_LINK_KEYS = listOf("link_m3u8", "link_embed", "watchUrl", "watch_url")
}
