package com.example.alphacinema.data.repository

import com.example.alphacinema.data.model.SupportChatMemoryContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportSuggestionPolicyTest {

    @Test
    fun shouldExposeMovieSuggestions_forDislikeFollowUpAfterRecommendation() {
        val memory = SupportChatMemoryContext(
            lastIntent = "movie_recommendation",
            genres = listOf("Kinh di"),
            referencedMovieSlugs = listOf("movie-a"),
            referencedMovieTitles = listOf("Movie A")
        )

        assertTrue(
            SupportSuggestionPolicy.shouldExposeMovieSuggestions(
                question = "khong thich",
                parsedIntent = ParsedSupportChatIntent.UNKNOWN,
                memory = memory
            )
        )
    }

    @Test
    fun shouldExposeMovieSuggestions_forContextualFollowUpAfterRecommendation() {
        val memory = SupportChatMemoryContext(
            lastIntent = "movie_recommendation",
            genres = listOf("Tam ly")
        )

        assertTrue(
            SupportSuggestionPolicy.shouldExposeMovieSuggestions(
                question = "Con phim Han thi sao?",
                parsedIntent = ParsedSupportChatIntent.UNKNOWN,
                memory = memory
            )
        )
    }

    @Test
    fun shouldAvoidPreviouslySuggestedMovies_forDislikeFollowUpAfterRecommendation() {
        val memory = SupportChatMemoryContext(
            lastIntent = "movie_recommendation",
            referencedMovieSlugs = listOf("movie-a")
        )

        assertTrue(
            SupportSuggestionPolicy.shouldAvoidPreviouslySuggestedMovies(
                question = "phim khac di",
                memory = memory
            )
        )
    }

    @Test
    fun shouldNotTreatDislikeAsRecommendationWithoutPreviousRecommendation() {
        assertFalse(
            SupportSuggestionPolicy.shouldExposeMovieSuggestions(
                question = "khong thich",
                parsedIntent = ParsedSupportChatIntent.UNKNOWN
            )
        )
    }
}
