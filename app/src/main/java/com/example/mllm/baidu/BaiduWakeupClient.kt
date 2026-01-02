package com.example.mllm.baidu

import android.content.Context
import android.util.Log
import com.baidu.speech.EventListener
import com.baidu.speech.EventManager
import com.baidu.speech.EventManagerFactory
import com.baidu.speech.asr.SpeechConstant
import org.json.JSONObject

class BaiduWakeupClient(context: Context) {
    private val tag = "BaiduWakeupClient"
    private val eventManager: EventManager =
        EventManagerFactory.create(
            context.applicationContext,
            "wp",
        )
    private var running = false

    var config: BaiduConfig = BaiduConfig("", "", "")
    var onWakeup: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val listener =
        EventListener { name, params, _, _, _ ->
            Log.d(tag, "event=$name params=$params")
            when (name) {
                SpeechConstant.CALLBACK_EVENT_WAKEUP_SUCCESS -> {
                    val word = parseWakeupWord(params)
                    if (word.isNotBlank()) {
                        onWakeup?.invoke(word)
                    }
                }
                SpeechConstant.CALLBACK_EVENT_WAKEUP_ERROR -> {
                    val error = parseWakeupError(params)
                    if (error.isNotBlank()) {
                        onError?.invoke(error)
                    }
                }
                SpeechConstant.CALLBACK_EVENT_WAKEUP_STOPED -> {
                    running = false
                }
            }
        }

    init {
        eventManager.registerListener(listener)
    }

    fun start() {
        if (running) return
        if (config.appId.isBlank() || config.appKey.isBlank() || config.secretKey.isBlank()) {
            onError?.invoke("Baidu config missing.")
            return
        }
        running = true
        val params = JSONObject()
        params.put(SpeechConstant.APP_ID, config.appId)
        params.put(SpeechConstant.APP_KEY, config.appKey)
        params.put(SpeechConstant.SECRET, config.secretKey)
        params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin")
        eventManager.send(SpeechConstant.WAKEUP_START, params.toString(), null, 0, 0)
    }

    fun stop() {
        if (!running) return
        eventManager.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0)
        running = false
    }

    fun release() {
        stop()
        eventManager.unregisterListener(listener)
    }

    private fun parseWakeupWord(params: String?): String {
        if (params.isNullOrBlank()) return ""
        return try {
            val json = JSONObject(params)
            val errorCode = json.optInt("errorCode", 0)
            if (errorCode != 0) {
                ""
            } else {
                json.optString("word", "")
            }
        } catch (e: Exception) {
            Log.e(tag, "parseWakeupWord failed", e)
            ""
        }
    }

    private fun parseWakeupError(params: String?): String {
        if (params.isNullOrBlank()) return "Wakeup error"
        return try {
            val json = JSONObject(params)
            val errorCode = json.optInt("error", 0)
            val desc = json.optString("desc", "Wakeup error")
            "Wakeup error: $errorCode $desc"
        } catch (e: Exception) {
            Log.e(tag, "parseWakeupError failed", e)
            "Wakeup error: parse failure"
        }
    }
}
