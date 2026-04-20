package com.example.alphacinema

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.alphacinema.data.model.SupportChatAction
import com.example.alphacinema.data.model.SupportChatActionFactory
import com.example.alphacinema.data.model.SupportChatMessage
import com.example.alphacinema.data.model.SupportChatMetadata
import com.example.alphacinema.data.model.SupportChatMovieItem
import com.example.alphacinema.data.model.SupportChatRouteDestination
import com.example.alphacinema.data.model.SupportMessageSender
import com.example.alphacinema.data.model.resolveRoute
import com.example.alphacinema.ui.theme.AlphaCinemaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupportScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersWatchMovieButtonForSuggestedMovie() {
        composeRule.setContent {
            AlphaCinemaTheme {
                MessageBubble(message = suggestedMovieMessage())
            }
        }

        composeRule.onNodeWithText("Bạn có thể xem: Úng Kính Ma Quái (2026)").assertIsDisplayed()
        composeRule.onNodeWithTag("support_movie_action_ung-kinh-ma-quai-2026").assertIsDisplayed()
    }

    @Test
    fun clickingWatchMovieButtonEmitsPlayerRouteForCorrectMovie() {
        var clickedAction: SupportChatAction? = null

        composeRule.setContent {
            AlphaCinemaTheme {
                MessageBubble(
                    message = suggestedMovieMessage(),
                    onMovieAction = { clickedAction = it }
                )
            }
        }

        composeRule.onNodeWithTag("support_movie_action_ung-kinh-ma-quai-2026").performClick()
        composeRule.runOnIdle {
            assertNotNull(clickedAction)
            assertEquals("ung-kinh-ma-quai-2026", clickedAction?.resolveRoute()?.slug)
            assertEquals(SupportChatRouteDestination.PLAYER, clickedAction?.resolveRoute()?.destination)
        }
    }

    @Test
    fun clickingWatchMovieButtonFallsBackToDetailRouteWhenPlaybackUnavailable() {
        var clickedAction: SupportChatAction? = null
        val fallbackAction = SupportChatActionFactory.createWatchMovieAction(
            slug = "ung-kinh-ma-quai-2026",
            movieId = "ung-kinh-ma-quai-2026",
            preferredDestination = SupportChatRouteDestination.PLAYER,
            playable = false
        )

        composeRule.setContent {
            AlphaCinemaTheme {
                MessageBubble(
                    message = suggestedMovieMessage(action = fallbackAction),
                    onMovieAction = { clickedAction = it }
                )
            }
        }

        composeRule.onNodeWithTag("support_movie_action_ung-kinh-ma-quai-2026").performClick()
        composeRule.runOnIdle {
            assertNotNull(clickedAction)
            assertEquals(SupportChatRouteDestination.DETAIL, clickedAction?.resolveRoute()?.destination)
            assertEquals("ung-kinh-ma-quai-2026", clickedAction?.resolveRoute()?.slug)
        }
    }

    @Test
    fun legacyTextOnlyMessageStillRendersWithoutWatchButton() {
        composeRule.setContent {
            AlphaCinemaTheme {
                MessageBubble(
                    message = SupportChatMessage(
                        id = "legacy-message",
                        text = "Tin nhắn text-only cũ vẫn phải render bình thường.",
                        sender = SupportMessageSender.BOT,
                        timestamp = "12:00"
                    )
                )
            }
        }

        composeRule.onNodeWithText("Tin nhắn text-only cũ vẫn phải render bình thường.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Xem phim").assertCountEquals(0)
    }

    private fun suggestedMovieMessage(
        action: SupportChatAction = SupportChatActionFactory.createWatchMovieAction(
            slug = "ung-kinh-ma-quai-2026",
            movieId = "ung-kinh-ma-quai-2026",
            preferredDestination = SupportChatRouteDestination.PLAYER,
            playable = true
        )
    ): SupportChatMessage {
        return SupportChatMessage(
            id = "bot-suggestion",
            text = "Bạn có thể xem: Úng Kính Ma Quái (2026)",
            sender = SupportMessageSender.BOT,
            timestamp = "12:00",
            metadata = SupportChatMetadata(
                movieItems = listOf(
                    SupportChatMovieItem(
                        id = "ung-kinh-ma-quai-2026",
                        title = "Úng Kính Ma Quái",
                        subtitle = "Horror lens",
                        year = "2026",
                        slug = "ung-kinh-ma-quai-2026",
                        movieId = "ung-kinh-ma-quai-2026",
                        actions = listOf(action)
                    )
                )
            )
        )
    }
}
