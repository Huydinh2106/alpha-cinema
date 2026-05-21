package com.example.alphacinema.data.model

data class SupportMusicLookup(
    val target: String?,
    val query: String?,
    val matchedMovieSlug: String? = null
)

data class SupportMusicSearchResult(
    val id: String,
    val name: String,
    val type: String,
    val uri: String?,
    val externalUrl: String,
    val imageUrl: String? = null,
    val artistNames: List<String> = emptyList()
)

enum class SupportMusicSearchFailure {
    MISSING_CREDENTIALS,
    TOKEN_REQUEST_FAILED,
    SEARCH_REQUEST_FAILED,
    EMPTY_RESULT
}

data class SupportMusicSearchOutcome(
    val result: SupportMusicSearchResult? = null,
    val failure: SupportMusicSearchFailure? = null,
    val detail: String? = null
)
