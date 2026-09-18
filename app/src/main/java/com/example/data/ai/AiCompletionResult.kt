package com.example.data.ai

/**
 * Hasil dari satu pemanggilan chat completion:
 * bisa berupa teks langsung dan/atau daftar tool call (function calling).
 */
data class AiCompletionResult(
    val content: String,
    val toolCalls: List<AiToolCall> = emptyList()
)
