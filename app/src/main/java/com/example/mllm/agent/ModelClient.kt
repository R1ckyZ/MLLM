package com.example.mllm.agent

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionContentPart
import com.openai.models.chat.completions.ChatCompletionContentPartImage
import com.openai.models.chat.completions.ChatCompletionContentPartText
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ModelConfig(
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val maxTokens: Int = 3000,
    val temperature: Double = 0.1,
    val topP: Double = 0.85,
    val frequencyPenalty: Double = 0.2,
    val reasoningEffort: com.openai.models.ReasoningEffort? = null,
)

data class ModelResponse(
    val thinking: String,
    val action: String,
    val rawContent: String,
)

class ModelClient(private val config: ModelConfig) {
    private val client: OpenAIClient =
        OpenAIOkHttpClient.builder()
            .apiKey(config.apiKey)
            .baseUrl(ensureTrailingSlash(config.baseUrl))
            .build()

    suspend fun request(messages: List<ModelMessage>): ModelResponse {
        return withContext(Dispatchers.IO) {
            val paramsBuilder =
                ChatCompletionCreateParams.builder()
                    .model(ChatModel.Companion.of(config.modelName))
                    .messages(messages.map { it.toChatMessageParam() })
                    .maxTokens(config.maxTokens.toLong())
                    .temperature(config.temperature)
                    .topP(config.topP)
                    .frequencyPenalty(config.frequencyPenalty)
            if (config.reasoningEffort != null) {
                paramsBuilder.reasoningEffort(config.reasoningEffort)
            }
            val completion = client.chat().completions().create(paramsBuilder.build())
            val content =
                completion.choices().firstOrNull()
                    ?.message()
                    ?.content()
                    ?.orElse("")
                    ?: ""
            val (thinking, action) = ActionParser.parseThinkingAndAction(content)
            ModelResponse(thinking, action, content)
        }
    }

    private fun ModelMessage.toChatMessageParam(): ChatCompletionMessageParam {
        return when (role.lowercase()) {
            "system" ->
                ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder()
                        .content(content.toString())
                        .build(),
                )
            "assistant" ->
                ChatCompletionMessageParam.ofAssistant(
                    ChatCompletionAssistantMessageParam.builder()
                        .content(content.toString())
                        .build(),
                )
            "user" -> {
                when (val c = content) {
                    is String ->
                        ChatCompletionMessageParam.ofUser(
                            ChatCompletionUserMessageParam.builder()
                                .content(c)
                                .build(),
                        )
                    is List<*> -> {
                        val parts =
                            c.filterIsInstance<ContentPart>().mapNotNull { part ->
                                when (part.type) {
                                    "text" ->
                                        part.text?.let {
                                            ChatCompletionContentPart.ofText(
                                                ChatCompletionContentPartText.builder().text(it).build(),
                                            )
                                        }
                                    "image_url" ->
                                        part.imageUrl?.let {
                                            ChatCompletionContentPart.ofImageUrl(
                                                ChatCompletionContentPartImage.builder()
                                                    .imageUrl(
                                                        ChatCompletionContentPartImage.ImageUrl.builder()
                                                            .url(it)
                                                            .build(),
                                                    )
                                                    .build(),
                                            )
                                        }
                                    else -> null
                                }
                            }
                        ChatCompletionMessageParam.ofUser(
                            ChatCompletionUserMessageParam.builder()
                                .contentOfArrayOfContentParts(parts)
                                .build(),
                        )
                    }
                    else ->
                        ChatCompletionMessageParam.ofUser(
                            ChatCompletionUserMessageParam.builder()
                                .content(c.toString())
                                .build(),
                        )
                }
            }
            else ->
                ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                        .content(content.toString())
                        .build(),
                )
        }
    }

    private fun ensureTrailingSlash(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
