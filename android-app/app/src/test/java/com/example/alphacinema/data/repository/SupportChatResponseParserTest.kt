package com.example.alphacinema.data.repository

import com.example.alphacinema.data.model.SupportChatActionFactory
import com.example.alphacinema.data.model.SupportChatRouteDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportChatResponseParserTest {

    @Test
    fun parseStructuredMovieSuggestions_returnsTextAndMovieMetadata() {
        val payload = SupportChatResponseParser.parse(
            """
            {
              "intent": "movie_recommendation",
              "answer": "Ban co the xem: Ung Kinh Ma Quai (2026)",
              "session_id": "session-123",
              "history_message_count": 4,
              "memory": {
                "summary": "User prefers horror suggestions.",
                "last_intent": "movie_recommendation",
                "topics": ["movies"],
                "genres": ["Kinh di"],
                "referenced_movie_slugs": ["ung-kinh-ma-quai-2026"]
              },
              "recommendations": [
                {
                  "id": "ung-kinh-ma-quai-2026",
                  "name": "Ung Kinh Ma Quai",
                  "year": 2026,
                  "poster": "https://example.com/poster.jpg",
                  "slug": "ung-kinh-ma-quai-2026",
                  "playable": true
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Ban co the xem: Ung Kinh Ma Quai (2026)", payload.text)
        assertEquals(ParsedSupportChatIntent.RECOMMENDATION, payload.intent)
        assertEquals("User prefers horror suggestions.", payload.memory?.summary)
        assertEquals(listOf("Kinh di"), payload.memory?.genres)
        assertEquals("session-123", payload.sessionId)
        assertEquals(4, payload.historyMessageCount)
        assertEquals(1, payload.movieSuggestions.size)
        assertEquals("Ung Kinh Ma Quai", payload.movieSuggestions.first().title)
        assertEquals("ung-kinh-ma-quai-2026", payload.movieSuggestions.first().slug)
    }

    @Test
    fun parseUpdatedAskResponse_recognizesBackendIntentAndRecommendations() {
        val payload = SupportChatResponseParser.parse(
            """
            {
              "intent": "recommendation",
              "mode": "answer",
              "answer": "Mình gợi ý vài phim hợp tâm trạng của bạn.",
              "sources": [],
              "recommendations": [
                {
                  "title": "Lat Mat 7",
                  "slug": "lat-mat-7",
                  "poster_url": "https://phimimg.com/lat-mat-7.jpg",
                  "year": 2024
                }
              ],
              "session_id": "alpha-session",
              "history_message_count": 2
            }
            """.trimIndent()
        )

        assertEquals(ParsedSupportChatIntent.RECOMMENDATION, payload.intent)
        assertEquals("Mình gợi ý vài phim hợp tâm trạng của bạn.", payload.text)
        assertEquals("alpha-session", payload.sessionId)
        assertEquals(2, payload.historyMessageCount)
        assertEquals(1, payload.movieSuggestions.size)
        assertEquals("Lat Mat 7", payload.movieSuggestions.first().title)
        assertEquals("lat-mat-7", payload.movieSuggestions.first().slug)
    }

    @Test
    fun parseTextOnlyReply_keepsLegacyTextOnlyBehavior() {
        val payload = SupportChatResponseParser.parse(
            """
            {
              "answer": "Xin chao, toi van ho tro nhu truoc."
            }
            """.trimIndent()
        )

        assertEquals("Xin chao, toi van ho tro nhu truoc.", payload.text)
        assertTrue(payload.movieSuggestions.isEmpty())
    }

    @Test
    fun parseTextOnlyReply_cleansMarkdownAndImageReferences() {
        val payload = SupportChatResponseParser.parse(
            """
            {
              "answer": "[Image #1]. - **Bac Si Watson** – Kich tinh, bi an. – **Nu than tinh yeu** – Lang man sau sac."
            }
            """.trimIndent()
        )

        assertEquals(
            "Bac Si Watson - Kich tinh, bi an.\n\nNu than tinh yeu - Lang man sau sac.",
            payload.text
        )
        assertFalse(payload.text.contains("**"))
        assertFalse(payload.text.contains("[Image"))
    }

    @Test
    fun watchActionFactory_fallsBackToDetailWhenPlaybackUnavailable() {
        val action = SupportChatActionFactory.createWatchMovieAction(
            slug = "ung-kinh-ma-quai-2026",
            movieId = "ung-kinh-ma-quai-2026",
            preferredDestination = SupportChatRouteDestination.PLAYER,
            playable = false
        )

        assertEquals(SupportChatRouteDestination.DETAIL, action.primaryRoute?.destination)
        assertEquals("ung-kinh-ma-quai-2026", action.primaryRoute?.slug)
    }

}
