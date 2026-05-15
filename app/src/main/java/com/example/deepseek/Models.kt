package com.example.deepseek

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Role { user, assistant, system }

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val ts: Long = System.currentTimeMillis()
)

@Serializable
data class AppSettings(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-chat",
    val systemPrompt: String = "You are a helpful assistant."
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7
)

@Serializable
data class ApiMessage(
    val role: String,
    val content: String
)

@Serializable
data class StreamChunk(
    val choices: List<StreamChoice> = emptyList()
)

@Serializable
data class StreamChoice(
    val delta: Delta = Delta(),
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class Delta(
    val role: String? = null,
    val content: String? = null
)
