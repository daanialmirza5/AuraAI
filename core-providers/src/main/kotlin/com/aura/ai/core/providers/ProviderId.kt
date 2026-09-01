package com.aura.ai.core.providers

enum class ProviderId(
    val key: String,
) {
    Gemini("gemini"),
    OpenAI("openai"),
    Claude("claude"),
    Ollama("ollama"),
    LocalModel("local"),
}
