package com.example.deepseek

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

object DeepSeekApi {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // SSE: keep-alive
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    sealed interface StreamEvent {
        data class Delta(val text: String) : StreamEvent
        data object Done : StreamEvent
        data class Error(val message: String) : StreamEvent
    }

    fun stream(
        settings: AppSettings,
        history: List<ChatMessage>
    ): Flow<StreamEvent> = callbackFlow {
        val apiMessages = buildList {
            if (settings.systemPrompt.isNotBlank()) {
                add(ApiMessage("system", settings.systemPrompt))
            }
            history.forEach { add(ApiMessage(it.role, it.content)) }
        }

        val body = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(model = settings.model, messages = apiMessages, stream = true)
        )

        val url = settings.baseUrl.trimEnd('/') + "/v1/chat/completions"
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("Accept", "text/event-stream")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val source = EventSources.createFactory(client).newEventSource(
            req,
            object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (data == "[DONE]") {
                        trySend(StreamEvent.Done)
                        close()
                        return
                    }
                    runCatching {
                        val chunk = json.decodeFromString(StreamChunk.serializer(), data)
                        val piece = chunk.choices.firstOrNull()?.delta?.content
                        if (!piece.isNullOrEmpty()) trySend(StreamEvent.Delta(piece))
                        if (chunk.choices.firstOrNull()?.finishReason != null) {
                            trySend(StreamEvent.Done)
                            close()
                        }
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    trySend(StreamEvent.Done)
                    close()
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    val msg = buildString {
                        append("请求失败")
                        if (response != null) append("，HTTP ${response.code}")
                        if (t != null) append("：${t.message}")
                        val errBody = response?.body?.string()
                        if (!errBody.isNullOrBlank()) append("\n$errBody")
                    }
                    trySend(StreamEvent.Error(msg))
                    close()
                }
            }
        )

        awaitClose { source.cancel() }
    }
}
