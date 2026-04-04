package com.example.alphacinema.data.repository

import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.model.SupportChatRequest
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SupportRepository {
    private val api = RetrofitClient.supportChatApi

    suspend fun askQuestion(question: String): String {
        return withContext(Dispatchers.IO) {
            // Chatbot API integration lives here so the UI stays free of networking logic.
            val response = api.askQuestion(
                SupportChatRequest(
                    question = question,
                    top_k = 4
                )
            )

            val rawBody = response.body()?.string().orEmpty()
            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            // Response parsing/formatting is centralized here for easier future API upgrades.
            cleanChatbotReply(rawBody)
        }
    }

    private fun cleanChatbotReply(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return FALLBACK_REPLY
        }

        val extracted = extractJsonText(trimmed) ?: trimmed

        return extracted
            .removeSurrounding("\"")
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace(Regex("^(answer|response|reply|message)\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
            .ifBlank { FALLBACK_REPLY }
    }

    private fun extractJsonText(raw: String): String? {
        return runCatching {
            val element = JsonParser.parseString(raw)
            extractTextFromElement(element)
        }.getOrNull()
    }

    private fun extractTextFromElement(element: JsonElement?): String? {
        element ?: return null
        if (element.isJsonNull) return null

        return when {
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                if (primitive.isString) primitive.asString else primitive.toString()
            }

            element.isJsonArray -> {
                element.asJsonArray
                    .asSequence()
                    .mapNotNull(::extractTextFromElement)
                    .firstOrNull { it.isNotBlank() }
            }

            element.isJsonObject -> {
                val jsonObject = element.asJsonObject
                val preferredKeys = listOf(
                    "answer",
                    "response",
                    "reply",
                    "message",
                    "text",
                    "result",
                    "data",
                    "content"
                )

                preferredKeys
                    .asSequence()
                    .mapNotNull { key ->
                        jsonObject.get(key)?.let(::extractTextFromElement)
                    }
                    .firstOrNull { it.isNotBlank() }
                    ?: jsonObject.entrySet()
                        .asSequence()
                        .mapNotNull { (_, value) -> extractTextFromElement(value) }
                        .firstOrNull { it.isNotBlank() }
            }

            else -> null
        }
    }

    companion object {
        const val FALLBACK_REPLY = "Xin l\u1ed7i, hi\u1ec7n kh\u00f4ng th\u1ec3 tr\u1ea3 l\u1eddi"
    }
}
