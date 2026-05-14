package com.example.alphacinema.data.repository

import com.example.alphacinema.data.model.SupportChatMovieItem
import com.example.alphacinema.data.model.SupportMusicLookup
import com.example.alphacinema.data.model.SupportMusicSearchFailure
import com.example.alphacinema.data.model.SupportMusicSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportMusicLinkPolicyTest {

    @Test
    fun buildMusicReply_doesNotReturnSearchLinkWhenSpotifyApiResultIsMissing() {
        val reply = SupportMusicLinkPolicy.buildMusicReply(
            question = "cho xin nhac phim Titanic"
        )

        assertNotNull(reply)
        assertNull(reply?.metadata)
        assertTrue(reply?.text.orEmpty().contains("Spotify API"))
        assertEquals("music_request", reply?.memory?.lastIntent)
    }

    @Test
    fun buildMusicReply_reportsMissingCredentialsOnlyForMissingCredentialFailure() {
        val reply = SupportMusicLinkPolicy.buildMusicReply(
            lookup = SupportMusicLookup(
                target = "Interstellar",
                query = "Interstellar soundtrack"
            ),
            spotifyFailure = SupportMusicSearchFailure.MISSING_CREDENTIALS
        )

        assertNull(reply.metadata)
        assertTrue(reply.text.contains("Client ID/Secret"))
    }

    @Test
    fun buildMusicReply_reportsPremiumOwnerRequirementForSpotify403() {
        val reply = SupportMusicLinkPolicy.buildMusicReply(
            lookup = SupportMusicLookup(
                target = "Interstellar",
                query = "Interstellar soundtrack"
            ),
            spotifyFailure = SupportMusicSearchFailure.SEARCH_REQUEST_FAILED,
            spotifyFailureDetail = "HTTP 403 Active premium subscription required for the owner of the app."
        )

        assertNull(reply.metadata)
        assertTrue(reply.text.contains("tai khoan Spotify so huu app"))
        assertTrue(reply.text.contains("Premium"))
    }

    @Test
    fun buildMusicLookup_extractsTitleFromAlbumUiRequest() {
        val lookup = SupportMusicLinkPolicy.buildMusicLookup(
            question = "Mo giao dien album nhac phim Interstellar"
        )

        assertNotNull(lookup)
        assertEquals("Interstellar", lookup?.target)
        assertEquals("Interstellar soundtrack", lookup?.query)
    }

    @Test
    fun buildMusicReply_usesResolvedSpotifyAlbumWhenAvailable() {
        val reply = SupportMusicLinkPolicy.buildMusicReply(
            lookup = SupportMusicLookup(
                target = "Interstellar",
                query = "Interstellar soundtrack"
            ),
            spotifyResult = SupportMusicSearchResult(
                id = "43rA71vTeqO6zV9IunlIHe",
                name = "Interstellar (Original Motion Picture Soundtrack)",
                type = "album",
                uri = "spotify:album:43rA71vTeqO6zV9IunlIHe",
                externalUrl = "https://open.spotify.com/album/43rA71vTeqO6zV9IunlIHe",
                imageUrl = "https://i.scdn.co/image/example",
                artistNames = listOf("Hans Zimmer")
            )
        )

        val link = reply.metadata?.linkItems?.singleOrNull()
        assertEquals("Interstellar (Original Motion Picture Soundtrack)", link?.label)
        assertEquals("https://open.spotify.com/album/43rA71vTeqO6zV9IunlIHe", link?.url)
        assertEquals("spotify:album:43rA71vTeqO6zV9IunlIHe", link?.uri)
        assertEquals("43rA71vTeqO6zV9IunlIHe", link?.contentId)
        assertTrue(link?.subtitle.orEmpty().contains("ID 43rA71vTeqO6zV9IunlIHe"))
    }

    @Test
    fun buildMusicLookup_usesPreviousMovieForContextualRequest() {
        val lookup = SupportMusicLinkPolicy.buildMusicLookup(
            question = "cho xin nhac phim do",
            previousMovies = listOf(
                SupportChatMovieItem(
                    id = "la-la-land",
                    title = "La La Land",
                    slug = "la-la-land"
                )
            )
        )

        assertEquals("La La Land", lookup?.target)
        assertEquals("La La Land soundtrack", lookup?.query)
        assertEquals("la-la-land", lookup?.matchedMovieSlug)
    }

    @Test
    fun buildMusicReply_keepsPreviousMovieMemoryForResolvedContextualRequest() {
        val reply = SupportMusicLinkPolicy.buildMusicReply(
            lookup = SupportMusicLookup(
                target = "La La Land",
                query = "La La Land soundtrack",
                matchedMovieSlug = "la-la-land"
            ),
            spotifyResult = SupportMusicSearchResult(
                id = "4GNIhgEGXzWGAefgN5qjdU",
                name = "La La Land (Original Motion Picture Soundtrack)",
                type = "album",
                uri = "spotify:album:4GNIhgEGXzWGAefgN5qjdU",
                externalUrl = "https://open.spotify.com/album/4GNIhgEGXzWGAefgN5qjdU",
                artistNames = listOf("Justin Hurwitz")
            )
        )

        val link = reply?.metadata?.linkItems?.singleOrNull()
        assertEquals("https://open.spotify.com/album/4GNIhgEGXzWGAefgN5qjdU", link?.url)
        assertEquals(listOf("la-la-land"), reply?.memory?.referencedMovieSlugs)
        assertEquals(listOf("La La Land"), reply?.memory?.referencedMovieTitles)
    }

    @Test
    fun buildMusicReply_asksForMovieTitleWhenTargetIsMissing() {
        val reply = SupportMusicLinkPolicy.buildMusicReply(
            question = "cho xin nhac tu bo phim"
        )

        assertNotNull(reply)
        assertNull(reply?.metadata)
        assertTrue(reply?.text.orEmpty().contains("Spotify"))
    }
}
