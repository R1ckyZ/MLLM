package com.example.mllm.agent

data class ParsedAction(
    val metadata: String,
    val fields: Map<String, Any?>,
) {
    val actionName: String? get() = fields["action"] as? String
}

object ActionParser {
    fun parseThinkingAndAction(content: String): Pair<String, String> {
//        val json = extractJsonObject(content)
//        if (json != null && json.has("type")) {
//            val thinking = json.optString("think", "").trim()
//            return thinking to json.toString()
//        }
        if (content.contains("finish(message=")) {
            val parts = content.split("finish(message=", limit = 2)
            return parts[0].trim() to "finish(message=" + parts[1]
        }
        if (content.contains("do(action=")) {
            val parts = content.split("do(action=", limit = 2)
            return parts[0].trim() to "do(action=" + parts[1]
        }
        if (content.contains("<answer>")) {
            val parts = content.split("<answer>", limit = 2)
            val thinking = parts[0].replace("<think>", "").replace("</think>", "").trim()
            val action = parts.getOrNull(1)?.replace("</answer>", "")?.trim().orEmpty()
            return thinking to action
        }
        return "" to content
    }

    fun parseAction(response: String): ParsedAction {
//        parseJsonAction(response)?.let { return it }
        val actionText = extractActionText(response)
        if (actionText.startsWith("finish")) {
            val message = parseFinishMessage(actionText)
            return ParsedAction("finish", mapOf("message" to message))
        }
        if (actionText.startsWith("do")) {
            val fields = parseCallArgs(actionText)
            return ParsedAction("do", fields)
        }
        return ParsedAction("finish", mapOf("message" to actionText))
    }

    private fun extractActionText(text: String): String {
        var result = text.trim()
        if (result.contains("<answer>")) {
            result = result.split("<answer>", limit = 2)[1]
            if (result.contains("</answer>")) {
                result = result.split("</answer>", limit = 2)[0]
            }
        }
        result = result
            .replace("</answer>", "")
            .replace("</think>", "")
            .replace("<think>", "")
            .replace("<answer>", "")
            .trim()
        val startIdx = listOf("finish(", "do(")
            .map { result.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull()
        if (startIdx != null) {
            result = result.substring(startIdx)
        }
        val endIdx = result.lastIndexOf(")")
        if (endIdx >= 0) {
            result = result.substring(0, endIdx + 1)
        }
        return result
    }

    private fun parseFinishMessage(text: String): String {
        val start = text.indexOf("(")
        val end = text.lastIndexOf(")")
        if (start == -1 || end <= start) {
            return ""
        }
        val args = text.substring(start + 1, end).trim()
        val key = "message="
        val idx = args.indexOf(key)
        if (idx == -1) {
            return ""
        }
        val raw = args.substring(idx + key.length).trim()
        if (raw.isEmpty()) return ""
        val quote = raw.first()
        if (quote == '"' || quote == '\'') {
            val lastQuote = raw.lastIndexOf(quote)
            return if (lastQuote > 0) raw.substring(1, lastQuote) else raw.substring(1)
        }
        return raw
    }

//    private fun parseJsonAction(text: String): ParsedAction? {
//        val obj = extractJsonObject(text) ?: return null
//        val type = obj.optString("type", "").trim().uppercase()
//        val payload = obj.optJSONObject("payload")
//        return when (type) {
//            "FINISH" -> {
//                val message = payload?.optString("message", "")?.trim().orEmpty()
//                ParsedAction("finish", mapOf("message" to message))
//            }
//            "DO" -> {
//                if (payload == null) return null
//                val actionName = payload.optString("action", "").trim()
//                if (actionName.isBlank()) return null
//                val params = payload.optJSONObject("params")
//                val fields = mutableMapOf<String, Any?>("action" to actionName)
//                if (params != null) {
//                    val keys = params.keys()
//                    while (keys.hasNext()) {
//                        val key = keys.next()
//                        fields[key] = parseJsonValue(params.opt(key))
//                    }
//                }
//                ParsedAction("do", fields)
//            }
//            else -> null
//        }
//    }
//
//    private fun parseJsonValue(value: Any?): Any? {
//        return when (value) {
//            null -> null
//            is JSONArray -> {
//                val list = mutableListOf<Any?>()
//                for (i in 0 until value.length()) {
//                    list.add(parseJsonValue(value.opt(i)))
//                }
//                list
//            }
//            is JSONObject -> value.toString()
//            is Number, is Boolean, is String -> value
//            else -> value.toString()
//        }
//    }
//
//    private fun extractJsonObject(text: String): JSONObject? {
//        val trimmed = text.trim()
//        val direct = runCatching { JSONObject(trimmed) }.getOrNull()
//        if (direct != null) return direct
//        val start = trimmed.indexOf("{")
//        val end = trimmed.lastIndexOf("}")
//        if (start >= 0 && end > start) {
//            val slice = trimmed.substring(start, end + 1)
//            return runCatching { JSONObject(slice) }.getOrNull()
//        }
//        return null
//    }

    private fun parseCallArgs(callText: String): Map<String, Any?> {
        val start = callText.indexOf("(")
        val end = callText.lastIndexOf(")")
        if (start == -1 || end <= start) {
            return emptyMap()
        }
        val argsBody = callText.substring(start + 1, end)
        val parts = splitArgs(argsBody)
        val result = mutableMapOf<String, Any?>()
        for (part in parts) {
            val idx = part.indexOf("=")
            if (idx <= 0) continue
            val key = part.substring(0, idx).trim()
            val valueRaw = part.substring(idx + 1).trim()
            result[key] = parseValue(valueRaw)
        }
        return result
    }

    private fun splitArgs(text: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = '\u0000'
        var bracketDepth = 0
        for (ch in text) {
            when {
                inQuotes -> {
                    current.append(ch)
                    if (ch == quoteChar) {
                        inQuotes = false
                    }
                }
                ch == '"' || ch == '\'' -> {
                    inQuotes = true
                    quoteChar = ch
                    current.append(ch)
                }
                ch == '[' -> {
                    bracketDepth += 1
                    current.append(ch)
                }
                ch == ']' -> {
                    bracketDepth = (bracketDepth - 1).coerceAtLeast(0)
                    current.append(ch)
                }
                ch == ',' && bracketDepth == 0 -> {
                    val part = current.toString().trim()
                    if (part.isNotEmpty()) {
                        result.add(part)
                    }
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        val part = current.toString().trim()
        if (part.isNotEmpty()) {
            result.add(part)
        }
        return result
    }

    private fun parseValue(value: String): Any? {
        val trimmed = value.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val inner = trimmed.substring(1, trimmed.length - 1)
            if (inner.isBlank()) return emptyList<Int>()
            return inner.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
        }
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))
        ) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        trimmed.toIntOrNull()?.let { return it }
        trimmed.toDoubleOrNull()?.let { return it }
        return trimmed
    }
}
