package com.example.alphacinema.data.model

data class TmdbCreditsResponse(
    val id: Int?,
    val cast: List<TmdbCast>?
)

data class TmdbCast(
    val id: Int?,
    val name: String?,
    val original_name: String?,
    val character: String?,
    val profile_path: String?,
    val order: Int?
)

data class TmdbDetailResponse(
    val id: Int?,
    val overview: String?
)
