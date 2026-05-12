package com.example.alphacinema.ui.account
enum class FilterKind {
    ALL, MOVIE_TYPE, GENRE
}

data class SearchFilter(
    val label: String,
    val kind: FilterKind,
    val slug: String? = null
)
