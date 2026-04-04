package com.example.alphacinema.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.model.SupportChatMessage
import com.example.alphacinema.data.model.SupportMessageSender
import com.example.alphacinema.data.repository.SupportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupportViewModel : ViewModel() {
    private val repository = SupportRepository()

    private val _messages = MutableStateFlow(
        listOf(
            SupportChatMessage(
                id = "welcome",
                text = "Xin ch\u00e0o, t\u00f4i l\u00e0 tr\u1ee3 l\u00fd AlphaCinema. B\u1ea1n c\u00f3 th\u1ec3 h\u1ecfi v\u1ec1 phim, t\u00e0i kho\u1ea3n ho\u1eb7c c\u00e1ch s\u1eed d\u1ee5ng \u1ee9ng d\u1ee5ng.",
                sender = SupportMessageSender.BOT,
                timestamp = currentTimeLabel()
            )
        )
    )
    val messages: StateFlow<List<SupportChatMessage>> = _messages.asStateFlow()

    fun sendMessage(userInput: String) {
        val question = userInput.trim()
        if (question.isBlank()) return

        val userMessage = createMessage(
            text = question,
            sender = SupportMessageSender.USER
        )
        val loadingMessage = createMessage(
            text = "\u0110ang tr\u1ea3 l\u1eddi...",
            sender = SupportMessageSender.LOADING
        )

        _messages.update { current ->
            current + userMessage + loadingMessage
        }

        viewModelScope.launch {
            val botReply = runCatching {
                repository.askQuestion(question)
            }.getOrElse {
                SupportRepository.FALLBACK_REPLY
            }

            val botMessage = loadingMessage.copy(
                text = botReply,
                sender = SupportMessageSender.BOT,
                timestamp = currentTimeLabel()
            )

            _messages.update { current ->
                current.map { message ->
                    if (message.id == loadingMessage.id) botMessage else message
                }
            }
        }
    }

    private fun createMessage(
        text: String,
        sender: SupportMessageSender
    ): SupportChatMessage {
        return SupportChatMessage(
            id = "${sender.name.lowercase(Locale.ROOT)}-${System.currentTimeMillis()}-${text.hashCode()}",
            text = text,
            sender = sender,
            timestamp = currentTimeLabel()
        )
    }

    companion object {
        private fun currentTimeLabel(): String {
            return SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date())
        }
    }
}
