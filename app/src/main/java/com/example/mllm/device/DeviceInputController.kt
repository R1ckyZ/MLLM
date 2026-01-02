package com.example.mllm.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

object DeviceInputController {
    fun typeText(text: String): Boolean {
        val service = DeviceAccessibilityService.instance ?: return false
        val root = service.rootInActiveWindow ?: return false
        var target: AccessibilityNodeInfo? = null
        return try {
            target = findEditableNode(root) ?: return false
            if (!target.isFocused) {
                target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            val args =
                Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
            target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (_: Throwable) {
            false
        } finally {
            target?.recycle()
            root.recycle()
        }
    }

    fun clearText(): Boolean = typeText("")

    fun copyToClipboard(
        context: Context,
        text: String,
    ): Boolean {
        val manager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
        val clip = ClipData.newPlainText("mllm_input", text)
        manager.setPrimaryClip(clip)
        return true
    }

    private fun findEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused?.isEditable == true) {
            return focused
        }
        focused?.recycle()
        return findFirstEditableAny(root)
    }

    private fun findFirstEditableAny(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val match = findFirstEditableAny(child)
            if (match != null) {
                child.recycle()
                return match
            }
            child.recycle()
        }
        return null
    }
}
