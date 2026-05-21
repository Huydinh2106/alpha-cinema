package com.example.alphacinema.data.model

import com.google.gson.annotations.SerializedName

data class SpotifyTokenResponse(
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("token_type")
    val tokenType: String? = null,
    @SerializedName("expires_in")
    val expiresIn: Long? = null
)

data class SpotifySearchResponse(
    val albums: SpotifyAlbumPage? = null
)

data class SpotifyAlbumPage(
    val items: List<SpotifyAlbumObject> = emptyList()
)

data class SpotifyAlbumObject(
    val id: String? = null,
    val name: String? = null,
    val uri: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    val artists: List<SpotifyArtist> = emptyList(),
    @SerializedName("external_urls")
    val externalUrls: SpotifyExternalUrls? = null,
    @SerializedName("album_type")
    val albumType: String? = null,
    @SerializedName("release_date")
    val releaseDate: String? = null
)

data class SpotifyExternalUrls(
    val spotify: String? = null
)

data class SpotifyImage(
    val url: String? = null,
    val height: Int? = null,
    val width: Int? = null
)

data class SpotifyArtist(
    val name: String? = null
)
