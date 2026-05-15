package com.example.deepseek

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _streaming = MutableStateFlow(false)
    val streaming: StateFlow<Boolean> = _streaming.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            _settings.value = Storage.settings(ctx).first()
            _messages.value = Storage.history(ctx).first()
        }
    }

    fun updateSettings(s: AppSettings) {
        _settings.value = s
        viewModelScope.launch { Storage.saveSettings(ctx, s) }
    }

    fun clearHistory() {
        _messages.value = emptyList()
        viewModelScope.launch { Storage.saveHistory(ctx, emptyList()) }
    }

    fun dismissError() { _error.value = null }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        _streaming.value = false
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _streaming.value) return
        val s = _settings.value
        if (s.apiKey.isBlank()) {
            _error.value = "请先在设置里填写 API Key。"
            return
        }

        val userMsg = ChatMessage(role = "user", content = trimmed)
        val assistantPlaceholder = ChatMessage(role = "assistant", content = "")
        val newHistory = _messages.value + userMsg + assistantPlaceholder
        _messages.value = newHistory
        persistHistory()

        val historyForApi = newHistory.dropLast(1) // exclude placeholder
        _streaming.value = true

        streamJob = DeepSeekApi.stream(s, historyForApi)
            .onEach { event ->
                when (event) {
                    is DeepSeekApi.StreamEvent.Delta -> appendToLastAssistant(event.text)
                    is DeepSeekApi.StreamEvent.Done -> finishStream()
                    is DeepSeekApi.StreamEvent.Error -> {
                        _error.value = event.message
                        finishStream()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun appendToLastAssistant(piece: String) {
        val list = _messages.value.toMutableList()
        val idx = list.indexOfLast { it.role == "assistant" }
        if (idx >= 0) {
            list[idx] = list[idx].copy(content = list[idx].content + piece)
            _messages.value = list
        }
    }

    private fun finishStream() {
        _streaming.value = false
        // Drop trailing empty assistant message if the call failed before any tokens
        val list = _messages.value
        if (list.isNotEmpty() && list.last().role == "assistant" && list.last().content.isBlank()) {
            _messages.value = list.dropLast(1)
        }
        persistHistory()
    }

    private fun persistHistory() {
        val snapshot = _messages.value
        viewModelScope.launch { Storage.saveHistory(ctx, snapshot) }
    }
}
