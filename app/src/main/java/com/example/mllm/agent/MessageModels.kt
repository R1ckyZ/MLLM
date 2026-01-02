package com.example.mllm.agent

import org.json.JSONArray
import org.json.JSONObject

data class ContentPart(
    val type: String,
    val text: String? = null,
    val imageUrl: String? = null,
)

data class ModelMessage(
    val role: String,
    val content: Any,
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("role", role)
        when (content) {
            is String -> obj.put("content", content)
            is List<*> -> {
                val array = JSONArray()
                content.filterIsInstance<ContentPart>().forEach { part ->
                    val partObj = JSONObject()
                    partObj.put("type", part.type)
                    if (part.type == "text") {
                        partObj.put("text", part.text ?: "")
                    } else if (part.type == "image_url") {
                        val imageObj = JSONObject()
                        imageObj.put("url", part.imageUrl ?: "")
                        partObj.put("image_url", imageObj)
                    }
                    array.put(partObj)
                }
                obj.put("content", array)
            }
            else -> obj.put("content", content.toString())
        }
        return obj
    }
}

object MessageBuilder {
    fun createSystemMessage(content: String): ModelMessage {
        return ModelMessage(role = "system", content = content)
    }

    fun createUserMessage(
        text: String,
        imageBase64: String?,
    ): ModelMessage {
        val content = mutableListOf<ContentPart>()
        if (!imageBase64.isNullOrBlank()) {
            content.add(
                ContentPart(
                    type = "image_url",
                    imageUrl = "data:image/png;base64,$imageBase64",
                ),
            )
        }
        content.add(ContentPart(type = "text", text = text))
        return ModelMessage(role = "user", content = content)
    }

    fun createAssistantMessage(content: String): ModelMessage {
        return ModelMessage(role = "assistant", content = content)
    }

    fun removeImagesFromMessage(message: ModelMessage): ModelMessage {
        val content = message.content
        if (content is List<*>) {
            val filtered = content.filterIsInstance<ContentPart>().filter { it.type == "text" }
            return message.copy(content = filtered)
        }
        return message
    }

    fun buildScreenInfo(currentApp: String): String {
        val obj = JSONObject()
        obj.put("current_app", currentApp)
        return obj.toString()
    }
}
