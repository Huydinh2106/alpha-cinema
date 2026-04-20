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
            topK = 4,
            topNRecommendations = 3,
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
        assertTrue(json.contains("\"top_n_recommendations\":3"))
        assertTrue(json.contains("\"chat_history\""))
        assertFalse(json.contains("\"history\""))
        assertFalse(json.contains("\"memory\""))
    }
}
