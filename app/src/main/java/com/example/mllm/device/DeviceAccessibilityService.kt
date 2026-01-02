package com.example.mllm.device

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class DeviceAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: used for gesture dispatch and window info access.
    }

    override fun onInterrupt() {
        // No-op.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
    }

    companion object {
        @Volatile
        var instance: DeviceAccessibilityService? = null
            private set
    }
}
