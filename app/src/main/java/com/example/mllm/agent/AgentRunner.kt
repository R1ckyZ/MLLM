package com.example.mllm.agent

import android.content.Context
import com.example.mllm.device.DeviceController
import com.example.mllm.device.ScreenshotController
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StepResult(
    val success: Boolean,
    val finished: Boolean,
    val action: ParsedAction?,
    val thinking: String,
    val message: String?,
)

class AgentRunner(
    private val context: Context,
    private val screenshotController: ScreenshotController,
    private val modelClient: ModelClient,
    private val systemPromptFixed: String,
    private val systemRulesProvider: () -> String,
    private val extraUserContextProvider: () -> String?,
    private val confirmSensitiveAction: suspend (String) -> Boolean,
    private val takeoverCallback: suspend (String) -> Unit,
) {
    private val contextMessages = mutableListOf<ModelMessage>()
    private var stepCount = 0

    suspend fun runTask(
        task: String,
        maxSteps: Int,
        onStep: suspend (StepResult) -> Unit,
    ) {
        reset()
        currentCoroutineContext().ensureActive()
        var result = executeStep(task, isFirst = true)
        onStep(result)
        if (result.finished) return

        while (stepCount < maxSteps) {
            currentCoroutineContext().ensureActive()
            result = executeStep(null, isFirst = false)
            onStep(result)
            if (result.finished) return
        }
        onStep(
            StepResult(
                success = false,
                finished = true,
                action = null,
                thinking = "",
                message = "Max steps reached",
            ),
        )
    }

    private suspend fun executeStep(
        userPrompt: String?,
        isFirst: Boolean,
    ): StepResult {
        currentCoroutineContext().ensureActive()
        stepCount += 1
        val screenshot = screenshotController.capture()
        val currentApp = DeviceController.getCurrentApp(context)

        if (isFirst) {
            contextMessages.add(MessageBuilder.createSystemMessage(buildSystemPrompt()))
            val screenInfo = MessageBuilder.buildScreenInfo(currentApp)
            val textContent =
                buildString {
                    append(userPrompt)
                    append("\n\n")
                    append(screenInfo)
                }
            contextMessages.add(
                MessageBuilder.createUserMessage(
                    text = textContent,
                    imageBase64 = screenshot.base64Data,
                ),
            )
        } else {
            val screenInfo = MessageBuilder.buildScreenInfo(currentApp)
            val extra = extraUserContextProvider()
            val textContent =
                buildString {
                    if (!extra.isNullOrBlank()) {
                        append("纠正的步骤：").append(extra).append("\n")
                    }
                    append("** Screen Info **\n\n")
                    append(screenInfo)
                }
            contextMessages.add(
                MessageBuilder.createUserMessage(
                    text = textContent,
                    imageBase64 = screenshot.base64Data,
                ),
            )
        }

        val response =
            try {
                modelClient.request(contextMessages)
            } catch (e: Exception) {
                return StepResult(false, true, null, "", "Model error: ${e.message}")
            }

        val action =
            try {
                ActionParser.parseAction(response.action)
            } catch (e: Exception) {
                ParsedAction("finish", mapOf("message" to response.action))
            }

        contextMessages[contextMessages.lastIndex] =
            MessageBuilder.removeImagesFromMessage(contextMessages.last())
        contextMessages.add(
            MessageBuilder.createAssistantMessage(
                "<think>${response.thinking}</think><answer>${response.action}</answer>",
            ),
        )

        val executor =
            ActionExecutor(
                appContext = context,
                confirmSensitiveAction = confirmSensitiveAction,
                onTakeover = takeoverCallback,
            )

        val actionResult =
            try {
                executor.execute(action, screenshot.width, screenshot.height)
            } catch (e: Exception) {
                ActionResult(false, true, "Action failed: ${e.message}")
            }

        val finished = action.metadata == "finish" || actionResult.shouldFinish
        return StepResult(
            success = actionResult.success,
            finished = finished,
            action = action,
            thinking = response.thinking,
            message = actionResult.message ?: action.fields["message"] as? String,
        )
    }

    private fun buildSystemPrompt(): String {
        val datePrefix = buildDatePrefix()
        val rules = systemRulesProvider()
        return "$datePrefix\n$systemPromptFixed\n$rules"
    }

    private fun buildDatePrefix(): String {
        val now = Date()
        val date = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(now)
        val weekdays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
        val weekday = weekdays[SimpleDateFormat("u", Locale.CHINA).format(now).toInt() - 1]
        return "今天的日期是: $date $weekday"
    }

    private fun reset() {
        contextMessages.clear()
        stepCount = 0
    }
}
