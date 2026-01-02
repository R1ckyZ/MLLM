package com.example.mllm.device

data class Screenshot(
    val base64Data: String,
    val width: Int,
    val height: Int,
    val isSensitive: Boolean = false,
)
