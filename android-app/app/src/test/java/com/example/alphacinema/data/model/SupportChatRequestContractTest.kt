package com.example.alphacinema.data.model

import com.google.gson.Gson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportChatRequestContractTest {

    private val gson = Gson()

    @Test
    fun supportChatRequest_serializesMemoryContractFields() {
        val request = SupportChatRequest(
            question = "Noi quy cua app la gi?",
            topK = 6,
            topNRecommendations = 5,
            generationModel = "gpt-4o-mini",
            sessionId = "session-123",
            rememberHistory = true,
            chatHistory = listOf(
                SupportChatHistoryMessage(
                    role = "user",
                    content = "Noi quy cua app la gi?"
                )
            )
        )

        val json = gson.toJson(request)

        assertTrue(json.contains("\"session_id\":\"session-123\""))
        assertTrue(json.contains("\"remember_history\":true"))
        assertTrue(json.contains("\"generation_model\":\"gpt-4o-mini\""))
        assertTrue(json.contains("\"top_k\":6"))
        assertTrue(json.contains("\"top_n_recommendations\":5"))
        assertTrue(json.contains("\"chat_history\""))
        assertFalse(json.contains("\"history\""))
        assertFalse(json.contains("\"memory\""))
    }

    @Test
    fun supportChatRequest_allowsNullSessionForFirstTurn() {
        val request = SupportChatRequest(
            question = "Goi y phim cho toi",
            sessionId = null
        )

        val json = gson.toJson(request)

        assertTrue(json.contains("\"question\":\"Goi y phim cho toi\""))
        assertTrue(json.contains("\"top_k\":6"))
        assertTrue(json.contains("\"top_n_recommendations\":5"))
        assertTrue(json.contains("\"generation_model\":\"gpt-4o-mini\""))
        assertTrue(json.contains("\"remember_history\":true"))
        assertFalse(json.contains("\"session_id\""))
    }
}
