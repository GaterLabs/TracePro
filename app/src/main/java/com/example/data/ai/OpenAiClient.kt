package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OpenAiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun generateChatCompletion(
        config: AiConfig,
        messages: List<AiChatMessage>,
        enableTools: Boolean = false
    ): Result<AiCompletionResult> = withContext(Dispatchers.IO) {
        try {
            val url = formatChatEndpoint(config.endpoint)
            
            val jsonRoot = JSONObject()
            jsonRoot.put("model", config.model.ifBlank { "auto" })
            jsonRoot.put("temperature", config.temperature)

            val messagesArray = JSONArray()
            for (msg in messages) {
                val msgObj = JSONObject()
                msgObj.put("role", msg.role)
                msgObj.put("content", msg.content)
                messagesArray.put(msgObj)
            }
            jsonRoot.put("messages", messagesArray)

            // Inject Tools JSON schema if Agent mode is enabled
            if (enableTools && config.isAgentModeEnabled) {
                try {
                    val tools = AiToolDefinitions.getToolsJsonArray()
                    if (tools.length() > 0) {
                        jsonRoot.put("tools", tools)
                        jsonRoot.put("tool_choice", "auto")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val requestBody = jsonRoot.toString().toRequestBody(JSON_MEDIA_TYPE)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")

            if (config.apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMessage = try {
                    val errJson = JSONObject(responseBody)
                    val errorObj = errJson.optJSONObject("error")
                    errorObj?.optString("message") ?: "HTTP Error ${response.code}: $responseBody"
                } catch (e: Exception) {
                    "HTTP Error ${response.code}: ${response.message}"
                }
                return@withContext Result.failure(Exception(errorMessage))
            }

            val respJson = JSONObject(responseBody)
            val choices = respJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.optJSONObject("message")
                val content = message?.optString("content") ?: ""

                val toolCallsList = mutableListOf<AiToolCall>()
                val rawToolCalls = message?.optJSONArray("tool_calls")
                if (rawToolCalls != null) {
                    for (i in 0 until rawToolCalls.length()) {
                        val tcObj = rawToolCalls.optJSONObject(i) ?: continue
                        val tcId = tcObj.optString("id", "call_${System.currentTimeMillis()}_$i")
                        val funcObj = tcObj.optJSONObject("function") ?: continue
                        val funcName = funcObj.optString("name", "")
                        val funcArgs = funcObj.optString("arguments", "{}")
                        if (funcName.isNotBlank()) {
                            toolCallsList.add(AiToolCall(id = tcId, name = funcName, argumentsJson = funcArgs))
                        }
                    }
                }

                Result.success(AiCompletionResult(content = content.trim(), toolCalls = toolCallsList))
            } else {
                Result.failure(Exception("Format respons AI tidak memuat choices yang valid."))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Domain/Host '${e.message}' tidak ditemukan (DNS unresolved). Pastikan domain sudah memiliki A/CNAME Record yang aktif dan terpropagasi di DNS server."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Koneksi timeout ke server. Pastikan server/gateway Anda sedang aktif."))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("Koneksi ditolak oleh server (${e.message}). Pastikan port dan protokol (http/https) sesuai."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal terhubung ke AI Gateway"))
        }
    }

    suspend fun testConnection(config: AiConfig): Result<String> = withContext(Dispatchers.IO) {
        val testMessages = listOf(
            AiChatMessage(role = "system", content = "Kamu adalah asisten tes."),
            AiChatMessage(role = "user", content = "Balas satu kata: 'TERHUBUNG'")
        )
        val result = generateChatCompletion(config, testMessages, enableTools = false)
        result.map { reply ->
            "Koneksi Berhasil! Model '${config.model}' merespons: ${reply.content}"
        }
    }

    private fun formatChatEndpoint(endpoint: String): String {
        var clean = endpoint.trim().removeSuffix("/")
        return if (clean.endsWith("/chat/completions")) {
            clean
        } else if (clean.endsWith("/v1")) {
            "$clean/chat/completions"
        } else {
            "$clean/v1/chat/completions"
        }
    }
}
