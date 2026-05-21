package com.example.alphacinema.data.repository

import android.util.Base64
import com.example.alphacinema.BuildConfig
import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.api.SpotifyApiService
import com.example.alphacinema.data.api.SpotifyAuthApiService
import com.example.alphacinema.data.model.SpotifyAlbumObject
import com.example.alphacinema.data.model.SpotifySearchResponse
import com.example.alphacinema.data.model.SupportMusicSearchFailure
import com.example.alphacinema.data.model.SupportMusicSearchOutcome
import com.example.alphacinema.data.model.SupportMusicSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.nio.charset.StandardCharsets

class SpotifyRepository(
    private val spotifyApi: SpotifyApiService = RetrofitClient.spotifyApi,
    private val spotifyAuthApi: SpotifyAuthApiService = RetrofitClient.spotifyAuthApi,
    private val clientId: String = BuildConfig.SPOTIFY_CLIENT_ID,
    private val clientSecret: String = BuildConfig.SPOTIFY_CLIENT_SECRET,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val tokenMutex = Mutex()
    private var cachedToken: CachedSpotifyToken? = null

    suspend fun searchTopAlbum(query: String): SupportMusicSearchResult? {
        return searchTopAlbumOutcome(query).result
    }

    suspend fun searchTopAlbumOutcome(query: String): SupportMusicSearchOutcome {
        val cleanedQuery = query.trim()
        if (cleanedQuery.isBlank()) {
            return SupportMusicSearchOutcome(failure = SupportMusicSearchFailure.EMPTY_RESULT)
        }

        return withContext(Dispatchers.IO) {
            val accessTokenOutcome = accessTokenOutcome()
            val accessToken = accessTokenOutcome.token ?: return@withContext SupportMusicSearchOutcome(
                failure = accessTokenOutcome.failure ?: SupportMusicSearchFailure.TOKEN_REQUEST_FAILED,
                detail = accessTokenOutcome.detail
            )
            val firstSearch = searchAlbums(accessToken, cleanedQuery)
            val firstResponse = firstSearch.response
            var failureDetail = firstSearch.detail
            val body = when {
                firstResponse?.isSuccessful == true -> firstResponse.body()
                firstResponse?.code() == HTTP_UNAUTHORIZED -> {
                    cachedToken = null
                    val refreshedTokenOutcome = accessTokenOutcome()
                    val refreshedToken = refreshedTokenOutcome.token
                    if (refreshedToken == null) {
                        failureDetail = refreshedTokenOutcome.detail
                        null
                    } else {
                        val retrySearch = searchAlbums(refreshedToken, cleanedQuery)
                        failureDetail = retrySearch.detail
                        retrySearch.response
                            ?.takeIf { it.isSuccessful }
                            ?.body()
                    }
                }
                else -> null
            }

            val result = body?.albums?.items
                .orEmpty()
                .firstNotNullOfOrNull { album -> album.toSupportMusicSearchResult() }
            SupportMusicSearchOutcome(
                result = result,
                failure = if (result == null) {
                    if (body == null) {
                        SupportMusicSearchFailure.SEARCH_REQUEST_FAILED
                    } else {
                        SupportMusicSearchFailure.EMPTY_RESULT
                    }
                } else {
                    null
                },
                detail = failureDetail
            )
        }
    }

    private suspend fun searchAlbums(
        accessToken: String,
        query: String
    ): SpotifyApiCallOutcome {
        return runCatching {
            val response = spotifyApi.search(
                authorization = "Bearer $accessToken",
                query = query,
                type = "album",
                limit = 1
            )
            SpotifyApiCallOutcome(
                response = response,
                detail = response.takeUnless { it.isSuccessful }?.toFailureDetail()
            )
        }.getOrElse { throwable ->
            SpotifyApiCallOutcome(detail = throwable.toFailureDetail())
        }
    }

    private suspend fun accessTokenOrNull(): String? {
        return accessTokenOutcome().token
    }

    private suspend fun accessTokenOutcome(): AccessTokenOutcome {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return AccessTokenOutcome(failure = SupportMusicSearchFailure.MISSING_CREDENTIALS)
        }

        val currentToken = cachedToken
        if (currentToken != null && currentToken.expiresAtMillis > clockMillis()) {
            return AccessTokenOutcome(token = currentToken.value)
        }

        return tokenMutex.withLock {
            val tokenAfterLock = cachedToken
            if (tokenAfterLock != null && tokenAfterLock.expiresAtMillis > clockMillis()) {
                return@withLock AccessTokenOutcome(token = tokenAfterLock.value)
            }

            val credentials = "$clientId:$clientSecret"
            val encodedCredentials = Base64.encodeToString(
                credentials.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )
            val tokenCall = runCatching {
                spotifyAuthApi.requestClientCredentialsToken(
                    authorization = "Basic $encodedCredentials"
                )
            }
            val response = tokenCall.getOrNull()

            val body = response?.takeIf { it.isSuccessful }?.body()
                ?: return@withLock AccessTokenOutcome(
                    failure = SupportMusicSearchFailure.TOKEN_REQUEST_FAILED,
                    detail = tokenCall.exceptionOrNull()?.toFailureDetail() ?: response?.toFailureDetail()
                )
            val accessToken = body.accessToken?.takeIf { it.isNotBlank() }
                ?: return@withLock AccessTokenOutcome(
                    failure = SupportMusicSearchFailure.TOKEN_REQUEST_FAILED,
                    detail = "Token response did not include access_token."
                )
            val expiresInSeconds = body.expiresIn ?: DEFAULT_TOKEN_TTL_SECONDS
            val usableTtlSeconds = (expiresInSeconds - TOKEN_REFRESH_SKEW_SECONDS)
                .coerceAtLeast(MIN_TOKEN_TTL_SECONDS)
            val freshToken = CachedSpotifyToken(
                value = accessToken,
                expiresAtMillis = clockMillis() + (usableTtlSeconds * MILLIS_PER_SECOND)
            )
            cachedToken = freshToken
            AccessTokenOutcome(token = freshToken.value)
        }
    }

    private fun SpotifyAlbumObject.toSupportMusicSearchResult(): SupportMusicSearchResult? {
        val resolvedId = id?.trim().orEmpty()
        val resolvedName = name?.trim().orEmpty()
        val resolvedUrl = externalUrls?.spotify?.trim().orEmpty()
        if (resolvedId.isBlank() || resolvedName.isBlank() || resolvedUrl.isBlank()) return null

        return SupportMusicSearchResult(
            id = resolvedId,
            name = resolvedName,
            type = albumType?.trim().orEmpty().ifBlank { "album" },
            uri = uri?.trim()?.takeIf { it.isNotBlank() },
            externalUrl = resolvedUrl,
            imageUrl = images.firstOrNull()?.url?.trim()?.takeIf { it.isNotBlank() },
            artistNames = artists
                .mapNotNull { artist -> artist.name?.trim()?.takeIf { it.isNotBlank() } }
                .distinct()
        )
    }

    private data class CachedSpotifyToken(
        val value: String,
        val expiresAtMillis: Long
    )

    private data class SpotifyApiCallOutcome(
        val response: Response<SpotifySearchResponse>? = null,
        val detail: String? = null
    )

    private data class AccessTokenOutcome(
        val token: String? = null,
        val failure: SupportMusicSearchFailure? = null,
        val detail: String? = null
    )

    private fun Response<*>.toFailureDetail(): String {
        val errorText = runCatching { errorBody()?.string().orEmpty() }
            .getOrDefault("")
            .replace(Regex("\\s+"), " ")
            .take(MAX_FAILURE_DETAIL_LENGTH)
        return buildString {
            append("HTTP ")
            append(code())
            if (message().isNotBlank()) {
                append(" ")
                append(message())
            }
            if (errorText.isNotBlank()) {
                append(": ")
                append(errorText)
            }
        }
    }

    private fun Throwable.toFailureDetail(): String {
        val type = this::class.java.simpleName.ifBlank { "Exception" }
        val text = message.orEmpty()
            .replace(Regex("\\s+"), " ")
            .take(MAX_FAILURE_DETAIL_LENGTH)
        return if (text.isBlank()) type else "$type: $text"
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val DEFAULT_TOKEN_TTL_SECONDS = 3600L
        private const val TOKEN_REFRESH_SKEW_SECONDS = 60L
        private const val MIN_TOKEN_TTL_SECONDS = 60L
        private const val MILLIS_PER_SECOND = 1000L
        private const val MAX_FAILURE_DETAIL_LENGTH = 180
    }
}
