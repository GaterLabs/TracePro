package com.example.data.ai

data class AiConfig(
    val endpoint: String = "https://ai.drakor.pp.ua/v1",
    val apiKey: String = "freellmapi-f657ed0085e8e45b7282037af89d6712e8fb0db6860fcf90",
    val model: String = "auto",
    val customPersona: String = "",
    val temperature: Double = 0.7,
    val isEnabled: Boolean = true
)

data class AiChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessagePayload>,
    val temperature: Double = 0.7
)

data class OpenAiMessagePayload(
    val role: String,
    val content: String
)

data class OpenAiChatResponse(
    val id: String? = null,
    val choices: List<OpenAiChoice>? = null,
    val error: OpenAiError? = null
)

data class OpenAiChoice(
    val message: OpenAiMessagePayload? = null,
    val finish_reason: String? = null
)

data class OpenAiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
