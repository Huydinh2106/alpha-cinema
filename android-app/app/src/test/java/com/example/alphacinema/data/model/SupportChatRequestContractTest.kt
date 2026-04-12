package com.example.alphacinema.data.model

import com.google.gson.Gson
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportChatRequestContractTest {

    private val gson = Gson()

    @Test
    fun supportChatRequest_serializesMemoryContractFields() {
        val request = SupportChatRequest(
            question = "Nội quy của app là gì?",
            topK = 4,
            sessionId = "session-123",
            history = listOf(
                SupportChatHistoryTurn(
                    role = "user",
                    content = "Nội quy của app là gì?",
                    timestamp = "10:30"
                )
            ),
            memory = SupportChatMemoryContext(
                summary = "User is asking about app policy.",
                lastIntent = "general_support",
                topics = listOf("app_policy"),
                kidsModeEnabled = false
            )
        )

        val json = gson.toJson(request)

        assertTrue(json.contains("\"session_id\":\"session-123\""))
        assertTrue(json.contains("\"memory\""))
        assertTrue(json.contains("\"last_intent\":\"general_support\""))
        assertTrue(json.contains("\"response_contract\""))
        assertTrue(json.contains("\"structured_movies_only_for_recommendation\":true"))
    }
}
