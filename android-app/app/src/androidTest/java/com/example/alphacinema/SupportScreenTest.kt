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
import com.example.alphacinema.data.model.SupportChatLinkItem
import com.example.alphacinema.data.model.SupportChatMessage
import com.example.alphacinema.data.model.SupportChatMetadata
import com.example.alphacinema.data.model.SupportChatMovieItem
import com.example.alphacinema.data.model.SupportChatRouteDestination
import com.example.alphacinema.data.model.SupportMessageSender
import com.example.alphacinema.data.model.resolveRoute
import com.example.alphacinema.ui.support.MessageBubble
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

    @Test
    fun rendersAndClicksSpotifyLinkForMusicReply() {
        var clickedLink: SupportChatLinkItem? = null

        composeRule.setContent {
            AlphaCinemaTheme {
                MessageBubble(
                    message = spotifyLinkMessage(),
                    onLinkClick = { clickedLink = it }
                )
            }
        }

        composeRule.onNodeWithTag("support_link_spotify-titanic").assertIsDisplayed()
        composeRule.onNodeWithText("Titanic: Music From The Motion Picture").assertIsDisplayed()
        composeRule.onNodeWithTag("support_link_spotify-titanic").performClick()
        composeRule.runOnIdle {
            assertNotNull(clickedLink)
            assertEquals("https://open.spotify.com/album/3Xx4fZfIuNDA6oLQ4uM5X5", clickedLink?.url)
            assertEquals("spotify:album:3Xx4fZfIuNDA6oLQ4uM5X5", clickedLink?.uri)
        }
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

    private fun spotifyLinkMessage(): SupportChatMessage {
        return SupportChatMessage(
            id = "bot-spotify",
            text = "Spotify link ready.",
            sender = SupportMessageSender.BOT,
            timestamp = "12:01",
            metadata = SupportChatMetadata(
                linkItems = listOf(
                    SupportChatLinkItem(
                        id = "spotify-titanic",
                        label = "Titanic: Music From The Motion Picture",
                        subtitle = "Album Spotify - ID 3Xx4fZfIuNDA6oLQ4uM5X5",
                        url = "https://open.spotify.com/album/3Xx4fZfIuNDA6oLQ4uM5X5",
                        provider = "Spotify",
                        uri = "spotify:album:3Xx4fZfIuNDA6oLQ4uM5X5",
                        contentId = "3Xx4fZfIuNDA6oLQ4uM5X5",
                        contentType = "album"
                    )
                )
            )
        )
    }
}
