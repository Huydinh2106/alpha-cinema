package com.example.alphacinema.data.model

data class MovieListResponse(
    val status: Boolean,
    val items: List<MovieItem>?,
    val data: MovieData?, // Some API versions nest items under 'data'
    val pagination: Pagination?
)

data class MovieData(
    val items: List<MovieItem>?,
    val pagination: Pagination?
)

data class MovieItem(
    val _id: String?,
    val name: String,
    val slug: String,
    val origin_name: String?,
    val poster_url: String?,
    val thumb_url: String?,
    val year: Int?,
    val type: String?,
    val episode_current: String?,
    val quality: String?,
    val lang: String?,
    val ageRating: String? = null,
    val tmdb: TmdbInfo?,
    val category: List<CategoryInfo>?,
    val country: List<CountryInfo>?
) {
    // Helper to get full absolute URL if needed
    fun getFullPosterUrl(): String {
        val url = poster_url ?: return ""
        if (url.startsWith("http")) return url
        return "https://phimimg.com/$url"
    }

    // Helper to get average rating
    fun getRating(): String {
        return tmdb?.vote_average?.toString() ?: "N/A"
    }
}

data class MovieDetailResponse(
    val status: Boolean,
    val movie: MovieDetail?,
    val episodes: List<EpisodeServer>?
)

data class MovieDetail(
    val _id: String?,
    val name: String,
    val slug: String,
    val origin_name: String?,
    val content: String?,
    val type: String?,
    val status: String?,
    val poster_url: String?,
    val thumb_url: String?,
    val time: String?,
    val episode_current: String?,
    val episode_total: String?,
    val quality: String?,
    val lang: String?,
    val year: Int?,
    val actor: List<String>?,
    val director: List<String>?,
    val category: List<CategoryInfo>?,
    val country: List<CountryInfo>?,
    val tmdb: TmdbInfo?
) {
    fun getFullPosterUrl(): String {
        val url = poster_url ?: return ""
        if (url.startsWith("http")) return url
        return "https://phimimg.com/$url"
    }
}

data class EpisodeServer(
    val server_name: String,
    val server_data: List<Episode>
)

data class Episode(
    val name: String,
    val slug: String,
    val filename: String,
    val link_embed: String,
    val link_m3u8: String
)

data class TmdbInfo(
    val type: String?,
    val id: String?,
    val season: Int?,
    val vote_average: Double?,
    val vote_count: Int?
)

data class CategoryInfo(
    val name: String,
    val slug: String
)

data class CountryInfo(
    val name: String,
    val slug: String
)

data class Pagination(
    val totalItems: Int?,
    val totalItemsPerPage: Int?,
    val currentPage: Int?,
    val totalPages: Int?
)
