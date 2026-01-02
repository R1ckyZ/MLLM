package com.example.mllm.overlay

import android.content.Context
import android.content.Intent
import com.example.mllm.LANGUAGE_CODE_ZH
import com.example.mllm.baidu.BaiduConfig

object OverlayController {
    @Volatile
    private var service: OverlayService? = null

    @Volatile
    private var lastVisibility: Pair<Boolean, Boolean>? = null

    @Volatile
    private var restorePanelOnFinish: Boolean = false

    @Volatile
    private var hidePanelAfterSend: Boolean = false

    var onTaskRequested: ((String) -> Unit)? = null
    var onTaskCancelRequested: (() -> Unit)? = null
    var baiduConfig: BaiduConfig = BaiduConfig("", "", "")
    var wakeupEnabled: Boolean = false
    var bubbleSizeDp: Int = 56
    var languageCode: String = LANGUAGE_CODE_ZH

    fun updateWakeupEnabled(enabled: Boolean) {
        wakeupEnabled = enabled
        service?.setWakeupEnabled(enabled)
    }

    fun updateBubbleSize(sizeDp: Int) {
        bubbleSizeDp = sizeDp
        service?.setBubbleSizeDp(sizeDp)
    }

    fun updateLanguage(code: String) {
        languageCode = code
        service?.updateLanguage(code)
    }

    fun bind(service: OverlayService) {
        this.service = service
        service.setBubbleSizeDp(bubbleSizeDp)
    }

    fun unbind(service: OverlayService) {
        if (this.service == service) {
            this.service = null
        }
    }

    fun ensureService(context: Context) {
        context.startService(Intent(context, OverlayService::class.java))
    }

    fun showPanel(text: String) {
        service?.showPanel(text)
    }

    fun updatePanel(text: String) {
        service?.updatePanel(text)
    }

    fun appendMessage(
        role: String,
        text: String,
    ) {
        service?.appendMessage(role, text)
    }

    fun clearMessages() {
        service?.clearMessages()
    }

    fun setCancelVisible(visible: Boolean) {
        service?.setCancelVisible(visible)
    }

    fun hidePanel() {
        service?.hidePanel()
    }

    fun setPanelVisible(visible: Boolean) {
        service?.setPanelVisible(visible)
    }

    fun markPanelHiddenForTask() {
        restorePanelOnFinish = true
    }

    fun consumeRestorePanelOnFinish(): Boolean {
        val restore = restorePanelOnFinish
        restorePanelOnFinish = false
        return restore
    }

    fun requestHidePanelAfterSend() {
        hidePanelAfterSend = true
    }

    fun consumeHidePanelAfterSend(): Boolean {
        val hide = hidePanelAfterSend
        hidePanelAfterSend = false
        return hide
    }

    fun hideOverlaysForCapture() {
        val bubbleVisible = service?.isBubbleVisible() ?: false
        val panelVisible = service?.isPanelVisible() ?: false
        lastVisibility = bubbleVisible to panelVisible
        service?.setBubbleVisible(false)
        service?.setPanelVisible(false)
    }

    fun restoreOverlaysAfterCapture() {
        val visibility = lastVisibility ?: return
        service?.setBubbleVisible(visibility.first)
        service?.setPanelVisible(visibility.second)
        lastVisibility = null
    }
}
