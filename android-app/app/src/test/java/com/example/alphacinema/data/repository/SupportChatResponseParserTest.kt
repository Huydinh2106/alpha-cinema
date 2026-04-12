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
              "answer": "Bạn có thể xem: Úng Kính Ma Quái (2026)",
              "memory": {
                "summary": "User prefers horror suggestions.",
                "last_intent": "movie_recommendation",
                "topics": ["movies"],
                "genres": ["Kinh dị"],
                "referenced_movie_slugs": ["ung-kinh-ma-quai-2026"]
              },
              "movies": [
                {
                  "id": "ung-kinh-ma-quai-2026",
                  "name": "Úng Kính Ma Quái",
                  "year": 2026,
                  "poster": "https://example.com/poster.jpg",
                  "slug": "ung-kinh-ma-quai-2026",
                  "playable": true
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Bạn có thể xem: Úng Kính Ma Quái (2026)", payload.text)
        assertEquals(ParsedSupportChatIntent.MOVIE_RECOMMENDATION, payload.intent)
        assertEquals("User prefers horror suggestions.", payload.memory?.summary)
        assertEquals(listOf("Kinh dị"), payload.memory?.genres)
        assertEquals(1, payload.movieSuggestions.size)
        assertEquals("Úng Kính Ma Quái", payload.movieSuggestions.first().title)
        assertEquals("ung-kinh-ma-quai-2026", payload.movieSuggestions.first().slug)
    }

    @Test
    fun parseTextOnlyReply_keepsLegacyTextOnlyBehavior() {
        val payload = SupportChatResponseParser.parse(
            """
            {
              "answer": "Xin chào, tôi vẫn hỗ trợ như trước."
            }
            """.trimIndent()
        )

        assertEquals("Xin chào, tôi vẫn hỗ trợ như trước.", payload.text)
        assertTrue(payload.movieSuggestions.isEmpty())
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

    @Test
    fun supportRepository_doesNotExposeMovieSuggestionsForAppPolicyQuestion() {
        assertFalse(
            SupportSuggestionPolicy.shouldExposeMovieSuggestions(
                question = "Nội quy của app là gì?",
                parsedIntent = ParsedSupportChatIntent.UNKNOWN
            )
        )
    }

    @Test
    fun supportRepository_exposesMovieSuggestionsForRecommendationQuestion() {
        assertTrue(
            SupportSuggestionPolicy.shouldExposeMovieSuggestions(
                question = "Gợi ý cho tôi một bộ phim kinh dị đi",
                parsedIntent = ParsedSupportChatIntent.UNKNOWN
            )
        )
    }
}
