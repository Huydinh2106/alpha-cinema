package com.example.alphacinema.data.model

data class SupportChatRequest(
    val question: String,
    val top_k: Int = 4
)

enum class SupportMessageSender {
    USER,
    BOT,
    LOADING
}

data class SupportChatMessage(
    val id: String,
    val text: String,
    val sender: SupportMessageSender,
    val timestamp: String
)
