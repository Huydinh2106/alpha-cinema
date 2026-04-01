package com.example.alphacinema

enum class MovieDetailTab(val title: String) {
    EPISODES("Tập phim"),
    CAST("Diễn viên"),
    RECOMMENDATIONS("Đề xuất")
}

enum class MovieDetailAction(val label: String) {
    FAVORITE("Yêu thích"),
    ADD_TO_LIST("Thêm vào"),
    RATE("Đánh giá"),
    COMMENT("Bình luận"),
    SHARE("Chia sẻ")
}

data class EpisodeUi(
    val id: String,
    val name: String,
    val duration: String
)

data class CastUi(
    val name: String,
    val role: String
)

data class RecommendedMovieUi(
    val id: String,
    val title: String,
    val year: String,
    val posterUrl: String
)

data class MovieDetailUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val year: String,
    val ageRating: String,
    val currentEpisode: String,
    val genres: List<String>,
    val description: String,
    val bannerUrl: String,
    val posterUrl: String,
    val episodes: List<EpisodeUi>,
    val cast: List<CastUi>,
    val recommendations: List<RecommendedMovieUi>
)
