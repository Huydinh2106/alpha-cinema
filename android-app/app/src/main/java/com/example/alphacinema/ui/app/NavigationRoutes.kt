package com.example.alphacinema.ui.app

import kotlinx.serialization.Serializable

@Serializable
data object MainRoute

@Serializable
data class MovieDetailNavRoute(val slug: String)

@Serializable
data class PlayerNavRoute(val slug: String, val episodeId: String? = null)

@Serializable
data class MovieListNavRoute(val title: String, val filterKindName: String, val slug: String)

@Serializable
data object AdminNavRoute

@Serializable
data class WatchPartyNavRoute(val roomId: String)
