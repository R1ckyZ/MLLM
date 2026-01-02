package com.example.mllm.baidu

import android.content.Context
import android.util.Log
import com.baidu.speech.EventListener
import com.baidu.speech.EventManager
import com.baidu.speech.EventManagerFactory
import com.baidu.speech.asr.SpeechConstant
import org.json.JSONObject

class BaiduAsrClient(context: Context) {
    private val tag = "BaiduAsrClient"
    private val eventManager: EventManager =
        EventManagerFactory.create(
            context.applicationContext,
            "asr",
        )
    private var running = false
    private var lastText: String = ""
    private var finalSent = false

    var config: BaiduConfig = BaiduConfig("", "", "")

    private var onPartial: ((String) -> Unit)? = null
    private var onFinal: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    private val listener =
        EventListener { name, params, _, _, _ ->
            Log.d(tag, "event=$name params=$params")
            when (name) {
                SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL -> {
                    val (text, isFinal) = parseResult(params)
                    if (text.isNotBlank()) {
                        lastText = text
                        if (isFinal) {
                            running = false
                            finalSent = true
                            onFinal?.invoke(text)
                        } else {
                            onPartial?.invoke(text)
                        }
                    }
                }
                SpeechConstant.CALLBACK_EVENT_ASR_FINISH -> {
                    val error = parseError(params)
                    if (error != null) {
                        running = false
                        onError?.invoke(error)
                    } else if (!finalSent && lastText.isNotBlank()) {
                        running = false
                        finalSent = true
                        onFinal?.invoke(lastText)
                    }
                }
                SpeechConstant.CALLBACK_EVENT_ASR_EXIT -> {
                    running = false
                }
            }
        }

    init {
        eventManager.registerListener(listener)
    }

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (running) return
        if (config.appId.isBlank() || config.appKey.isBlank() || config.secretKey.isBlank()) {
            onError("Baidu config missing.")
            return
        }
        this.onPartial = onPartial
        this.onFinal = onFinal
        this.onError = onError
        lastText = ""
        finalSent = false
        running = true

        val params = JSONObject()
        params.put(SpeechConstant.APP_ID, config.appId)
        params.put(SpeechConstant.APP_KEY, config.appKey)
        params.put(SpeechConstant.SECRET, config.secretKey)
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false)
        params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN)
        params.put(SpeechConstant.PID, 1537)
        eventManager.send(SpeechConstant.ASR_START, params.toString(), null, 0, 0)
    }

    fun stop() {
        if (!running) return
        eventManager.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0)
    }

    fun cancel() {
        running = false
        eventManager.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0)
    }

    fun release() {
        cancel()
        eventManager.unregisterListener(listener)
    }

    private fun parseResult(params: String?): Pair<String, Boolean> {
        if (params.isNullOrBlank()) return "" to false
        return try {
            val json = JSONObject(params)
            val resultType = json.optString("result_type", "")
            val bestResult = json.optString("best_result", "")
            val results = json.optJSONArray("results_recognition")
            val text =
                when {
                    bestResult.isNotBlank() -> bestResult
                    results != null && results.length() > 0 -> results.optString(0)
                    else -> ""
                }
            text to (resultType == "final_result")
        } catch (e: Exception) {
            Log.e(tag, "parseResult failed", e)
            "" to false
        }
    }

    private fun parseError(params: String?): String? {
        if (params.isNullOrBlank()) return null
        return try {
            val json = JSONObject(params)
            val error = json.optInt("error", 0)
            if (error == 0) return null
            val subError = json.optInt("sub_error", 0)
            val desc = json.optString("desc", "ASR error")
            if (
                error == 7 && subError == 7001
            ) {
                return "The input time is too short, please record again."
            }
            "ASR error: $error/$subError $desc"
        } catch (e: Exception) {
            Log.e(tag, "parseError failed", e)
            "ASR error: parse failure"
        }
    }
}
