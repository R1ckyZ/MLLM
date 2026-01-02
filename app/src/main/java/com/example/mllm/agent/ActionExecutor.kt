package com.example.mllm.agent

import com.example.mllm.device.DeviceController
import com.example.mllm.device.DeviceInputController
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class ActionResult(
    val success: Boolean,
    val shouldFinish: Boolean,
    val message: String? = null,
)

class ActionExecutor(
    private val appContext: android.content.Context,
    private val confirmSensitiveAction: suspend (String) -> Boolean,
    private val onTakeover: suspend (String) -> Unit,
) {
    suspend fun execute(
        action: ParsedAction,
        screenWidth: Int,
        screenHeight: Int,
    ): ActionResult {
        if (action.metadata == "finish") {
            return ActionResult(true, true, action.fields["message"] as? String)
        }
        val actionName = action.actionName ?: return ActionResult(false, true, "Missing action")
        return when (actionName) {
            "Launch" -> handleLaunch(action)
            "Tap" -> handleTap(action, screenWidth, screenHeight)
            "Type", "Type_Name" -> handleType(action)
            "Swipe" -> handleSwipe(action, screenWidth, screenHeight)
            "Back" -> handleBack()
            "Home" -> handleHome()
            "Double Tap" -> handleDoubleTap(action, screenWidth, screenHeight)
            "Long Press" -> handleLongPress(action, screenWidth, screenHeight)
            "Wait" -> handleWait(action)
            "Take_over" -> handleTakeover(action)
            "Note" -> ActionResult(true, false)
            "Call_API" -> ActionResult(true, false)
            "Interact" -> ActionResult(true, true, "User interaction required")
            else -> ActionResult(false, false, "Unknown action: $actionName")
        }
    }

    private suspend fun handleLaunch(action: ParsedAction): ActionResult {
        val appName = action.fields["app"] as? String ?: return ActionResult(false, false, "No app")
        val launched = DeviceController.launchApp(appContext, appName)
        return if (launched) ActionResult(true, false) else ActionResult(false, false, "App not found")
    }

    private suspend fun handleTap(
        action: ParsedAction,
        width: Int,
        height: Int,
    ): ActionResult {
        val element = action.fields["element"] as? List<*> ?: return ActionResult(false, false, "No element")
        val coords = toAbsolutePoint(element, width, height) ?: return ActionResult(false, false, "Bad coordinates")
        val sensitiveMessage = action.fields["message"] as? String
        if (!sensitiveMessage.isNullOrBlank()) {
            val ok = confirmSensitiveAction(sensitiveMessage)
            if (!ok) {
                return ActionResult(false, true, "User cancelled sensitive operation")
            }
        }
        DeviceController.tap(coords.first, coords.second)
        return ActionResult(true, false)
    }

    private suspend fun handleDoubleTap(
        action: ParsedAction,
        width: Int,
        height: Int,
    ): ActionResult {
        val element = action.fields["element"] as? List<*> ?: return ActionResult(false, false, "No element")
        val coords = toAbsolutePoint(element, width, height) ?: return ActionResult(false, false, "Bad coordinates")
        DeviceController.doubleTap(coords.first, coords.second)
        return ActionResult(true, false)
    }

    private suspend fun handleLongPress(
        action: ParsedAction,
        width: Int,
        height: Int,
    ): ActionResult {
        val element = action.fields["element"] as? List<*> ?: return ActionResult(false, false, "No element")
        val coords = toAbsolutePoint(element, width, height) ?: return ActionResult(false, false, "Bad coordinates")
        DeviceController.longPress(coords.first, coords.second)
        return ActionResult(true, false)
    }

    private suspend fun handleSwipe(
        action: ParsedAction,
        width: Int,
        height: Int,
    ): ActionResult {
        val start = action.fields["start"] as? List<*> ?: return ActionResult(false, false, "No start")
        val end = action.fields["end"] as? List<*> ?: return ActionResult(false, false, "No end")
        val startCoords = toAbsolutePoint(start, width, height)
        val endCoords = toAbsolutePoint(end, width, height)
        if (startCoords == null || endCoords == null) {
            return ActionResult(false, false, "Bad coordinates")
        }
        DeviceController.swipe(
            startCoords.first,
            startCoords.second,
            endCoords.first,
            endCoords.second,
        )
        return ActionResult(true, false)
    }

    private suspend fun handleType(action: ParsedAction): ActionResult {
        val text = action.fields["text"] as? String ?: ""
        val ok = DeviceInputController.typeText(text)
        if (ok) {
            return ActionResult(true, false)
        }
        DeviceInputController.copyToClipboard(appContext, text)
        return ActionResult(false, false, "Input failed (copied to clipboard)")
    }

    private suspend fun handleBack(): ActionResult {
        DeviceController.back()
        return ActionResult(true, false)
    }

    private suspend fun handleHome(): ActionResult {
        DeviceController.home()
        return ActionResult(true, false)
    }

    private suspend fun handleWait(action: ParsedAction): ActionResult {
        val durationRaw = action.fields["duration"]?.toString() ?: "1 seconds"
        val seconds = durationRaw.replace("seconds", "").trim().toDoubleOrNull() ?: 1.0
        delay((seconds * 1000).roundToInt().toLong())
        return ActionResult(true, false)
    }

    private suspend fun handleTakeover(action: ParsedAction): ActionResult {
        val message = action.fields["message"] as? String ?: "User intervention required"
        onTakeover(message)
        return ActionResult(true, true, message)
    }

    private fun toAbsolutePoint(
        element: List<*>,
        width: Int,
        height: Int,
    ): Pair<Int, Int>? {
        if (element.size < 2) return null
        val x = (element[0] as? Number)?.toDouble() ?: return null
        val y = (element[1] as? Number)?.toDouble() ?: return null
        val absX = (x / 1000.0 * width).roundToInt()
        val absY = (y / 1000.0 * height).roundToInt()
        return absX to absY
    }
}
