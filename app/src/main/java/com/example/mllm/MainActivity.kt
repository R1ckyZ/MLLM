@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.mllm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.mllm.agent.AgentRunner
import com.example.mllm.agent.ModelClient
import com.example.mllm.agent.ModelConfig
import com.example.mllm.baidu.BaiduConfig
import com.example.mllm.data.AppSettings
import com.example.mllm.data.DEFAULT_LLM_CONFIG_NAME
import com.example.mllm.data.LlmConfig
import com.example.mllm.data.PromptEntry
import com.example.mllm.data.PromptKind
import com.example.mllm.data.SettingsRepository
import com.example.mllm.device.AppPackages
import com.example.mllm.device.DeviceAccessibilityService
import com.example.mllm.device.DeviceController
import com.example.mllm.device.ScreenshotController
import com.example.mllm.overlay.OverlayController
import com.example.mllm.overlay.OverlayService
import com.example.mllm.ui.theme.MLLMTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

data class PromptConfig(
    val name: String,
    val content: String,
    val tags: String = "",
    val kind: PromptKind,
)

data class ChatMessage(
    val role: String,
    val content: String,
)

enum class HomeTab {
    PROMPT,
    LLM,
}

typealias SettingsSaveCallback = (
    String, // llmModel
    String, // languageCode
    String, // selectedOperatorLlmName
    String, // selectedPlannerLlmName
    String, // selectedScreenCheckLlmName
    Int, // maxSteps
    Int, // operatorMaxTokens
    Double, // operatorTemperature
    Double, // operatorTopP
    Boolean, // operatorReasoningEnabled
    String, // operatorReasoningEffort
    String, // plannerTextModel
    Int, // plannerTextMaxTokens
    Double, // plannerTextTemperature
    Double, // plannerTextTopP
    Boolean, // plannerTextReasoningEnabled
    String, // plannerTextReasoningEffort
    Boolean, // plannerEnabled
    Boolean, // screenCheckEnabled
    String, // screenCheckModel
    Int, // screenCheckMaxTokens
    Double, // screenCheckTemperature
    Double, // screenCheckTopP
    Boolean, // screenCheckReasoningEnabled
    String, // screenCheckReasoningEffort
    String, // plannerPromptName
    String, // screenCheckPromptName
    String, // promptName
    String, // appPackagesText
    Int, // bubbleSizeDp
    String, // baiduAppId
    String, // baiduAppKey
    String, // baiduSecretKey
    Boolean, // baiduWakeupEnabled
) -> Unit

data class SensitiveRequest(
    val message: String,
    val deferred: CompletableDeferred<Boolean>,
)

fun promptKindLabel(
    context: android.content.Context,
    kind: PromptKind,
): String {
    return when (kind) {
        PromptKind.PRIMARY -> context.getString(R.string.prompt_kind_operator)
        PromptKind.PLANNER -> context.getString(R.string.prompt_kind_planner)
        PromptKind.SUBTASK -> context.getString(R.string.prompt_kind_subtask)
        PromptKind.SCREEN_CHECK -> context.getString(R.string.prompt_kind_screen_check)
    }
}

private fun parsePlannerPromptName(raw: String): String? {
    val obj = parseJsonObject(raw) ?: return null
    return obj.optString("prompt_name", "").trim().ifEmpty { null }
}

private fun parseScreenCheckTask(raw: String): String? {
    val obj = parseJsonObject(raw) ?: return null
    return obj.optString("task", "").trim().ifEmpty { null }
}

private fun parseJsonObject(raw: String): JSONObject? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val direct = runCatching { JSONObject(trimmed) }.getOrNull()
    if (direct != null) {
        return direct
    }
    val start = trimmed.indexOf("{")
    val end = trimmed.lastIndexOf("}")
    if (start >= 0 && end > start) {
        val slice = trimmed.substring(start, end + 1)
        return runCatching { JSONObject(slice) }.getOrNull()
    }
    return null
}

private const val DEFAULT_PROMPT_NAME = "Default (Operator)"
private const val DEFAULT_LLM_MODEL = ""
private const val DEFAULT_MODEL_MANAGER_URL = ""
private const val DEFAULT_MODEL_MANAGER_API_KEY = ""
private const val DEFAULT_LANGUAGE_CODE = LANGUAGE_CODE_ZH
private const val DEFAULT_MAX_STEPS = 50
private const val DEFAULT_OPERATOR_MAX_TOKENS = 3000
private const val DEFAULT_OPERATOR_TEMPERATURE = 0.0
private const val DEFAULT_OPERATOR_TOP_P = 0.85
private const val DEFAULT_OPERATOR_REASONING_ENABLED = false
private const val DEFAULT_OPERATOR_REASONING_EFFORT = "low"
private const val DEFAULT_PLANNER_TEXT_MODEL = ""
private const val DEFAULT_PLANNER_TEXT_MAX_TOKENS = 3000
private const val DEFAULT_PLANNER_TEXT_TEMPERATURE = 0.0
private const val DEFAULT_PLANNER_TEXT_TOP_P = 0.85
private const val DEFAULT_PLANNER_TEXT_REASONING_ENABLED = false
private const val DEFAULT_PLANNER_TEXT_REASONING_EFFORT = "low"
private const val DEFAULT_PLANNER_ENABLED = true
private const val DEFAULT_SCREEN_CHECK_MODEL = ""
private const val DEFAULT_SCREEN_CHECK_MAX_TOKENS = 3000
private const val DEFAULT_SCREEN_CHECK_TEMPERATURE = 0.0
private const val DEFAULT_SCREEN_CHECK_TOP_P = 0.85
private const val DEFAULT_SCREEN_CHECK_ENABLED = true
private const val DEFAULT_SCREEN_CHECK_REASONING_ENABLED = false
private const val DEFAULT_SCREEN_CHECK_REASONING_EFFORT = "low"
private const val DEFAULT_BAIDU_APP_ID = ""
private const val DEFAULT_BAIDU_APP_KEY = ""
private const val DEFAULT_BAIDU_SECRET_KEY = ""
private const val DEFAULT_BAIDU_WAKEUP_ENABLED = false
private const val DEFAULT_BUBBLE_SIZE_DP = 48
private const val DEFAULT_PLANNER_PROMPT_NAME = "Default (Planner)"
private const val DEFAULT_SUBTASK_PROMPT_NAME = "其他"
private const val DEFAULT_SCREEN_CHECK_PROMPT_NAME = "Screen Check"
private val SYSTEM_PROMPT_FIXED = ""
private val DEFAULT_PLANNER_PROMPT =
    """
    你是“任务路由”专家：根据用户的一句话任务 TASK，在可用标签集合 TAGS 中选择最合适的 prompt_name。

    【输入格式】
    用户输入包含三段：
    - TASK: <task>                         # 用户一句话任务（可能为语音转写）
    - TAGS: ["PromptA","PromptB", ...]      # 可选 prompt_name 列表
    - TAGS_INFO: [
        {"name":"PromptA","tags":["tag1","tag2", ...]},
        {"name":"PromptB","tags":[...]},
        ...
      ]                                     # 每个 prompt 的关键词/别名/场景词（可为空）

    注意：最终只能从 TAGS 中挑选一个 prompt_name（若无匹配则输出“其他”）。

    【语音纠错】
    你可以在脑中对 TASK 做纠错、同音字替换、漏字补全与同义归一，仅用于理解；禁止在输出中体现纠错内容或推理过程。

    【选择目标】
    从 TAGS 中选出最贴近用户真实意图的一个 prompt_name。
    优先依据“明确动词 + 目标对象 + 场景词”判断（如：点/下单/外送；叫/打车；导航/路线；订/预订；支付/转账；提醒/闹钟；消息/电话；设置/权限 等）。

    【匹配规则（按优先级）】
    1) 强匹配优先（tag 命中）：
       - 若 TASK 命中某候选的 tags 中高置信关键词（包含同义/别名、常见口语表达），优先选择该候选。
    2) 语义优先于关键词堆叠：
       - 仅出现“打开/看看/帮我/怎么弄”等泛词不构成匹配；必须与某候选的“动作-对象-场景”语义一致。
    3) 多意图处理：
       - 若 TASK 同时包含多个意图，只选择最核心、最可执行、最直接的主任务对应的候选。
    4) 冲突消解（多个候选同时匹配时）：
       - 选择更具体的场景/对象对应的 prompt（细粒度 > 粗粒度）。
       - 若仍难区分，选择 tags 命中更明确、数量更高且更贴近核心任务的候选。
       - 若依然无法稳定判断，输出“其他”。
    5) 信息不足/无匹配：
       - 若 TASK 无法与任何候选形成稳定匹配，输出“其他”。
       - 即使 TAGS 中不包含“其他”，也仍输出“其他”。

    【输出约束】
    只能输出一个 JSON 对象且仅含字段 prompt_name；不得输出解释、Markdown、换行注释或任何多余字符。
    输出示例：{"prompt_name":"PromptA"} 或 {"prompt_name":"其他"}
    """.trimIndent()
private val DEFAULT_SCREEN_CHECK_PROMPT =
    """
    你是“动作-意图-结果”纠正助手。输入有五段：Plan(总子任务或目标流程)、Action(实际行为)、Thinking(意图与理由)、Result(执行结果)、ScreenInfo(截图信息/描述)。你必须严格只输出JSON：{"task":"xxx"}，不得有其它任何字符/字段。

    规则：
    1) 结合 Plan 和历史对话决策下一步行为
    - 先结合 Plan、历史对话与 ScreenInfo，判断当前实际所处步骤与系统状态，定位“现在应该在哪一步”。
    - 若 Plan 中存在未执行的前置步骤，但 ScreenInfo 显示其前置条件已满足或结果已达成（如已在目标页面/已登录/权限已开启等），则允许跳过该前置步骤，直接对齐到当前状态对应的步骤。
    - 若 Action 或 Thinking 明显偏离 Plan 主线且与当前状态不匹配，task 必须回到 Plan 主线并基于当前 ScreenInfo 进行动作纠正（输出纠正后的下一步动作）。
    - 输出约束：task 仅输出：当前屏幕状态与下一步动作，不得写多步流程，不得解释、总结或复述推理。

    2) 屏幕滑动决策
    触发判断：
    - 从 Action/Thinking 可判断“需要通过滑动继续查找应用/翻页找图标”（例如：继续找、翻页、滑动看看、下一个页面等），且 ScreenInfo 未出现目标应用；
      或 Action/Thinking/Result 明确反馈“找不到/没看到/无结果”，并且当前仍在“查找应用”的主线任务中。
    a) 先判断“是否应该滑屏”：
    - 若 ScreenInfo 显示当前界面不适合继续滑屏找应用（如弹窗遮挡、设置/详情页、搜索输入态等），
      则 task 必须纠正为：先处理阻塞操作（关闭弹窗/返回上一级/退出输入态/点击明确按钮等），回到可继续查找的界面。
    - 否则进入 b)。

    b) 若需要滑屏，则按历史纠错选择方向：
    1) 定义“屏幕无变化”判定：
       - 滑动后 ScreenInfo 的应用名称/图标集合/数量与布局基本一致（核心元素未变化）。

    2) 方向选择（优先级从高到低）：
       - 若历史显示“上一次右滑后无变化” → 本次改为左滑一次。
       - 若历史显示“上一次左滑后无变化” → 本次改为右滑一次。
       - 若左右都已尝试且仍无变化 → 改用纵向滑动：先上滑一次（下一轮如仍无变化再下滑）。
       - 若无历史/无倾向 → 默认左滑一次。

    3) 动作与意图不符：仅在“明确矛盾”时触发纠正
    - 触发条件（硬矛盾才触发）：
      * Thinking 明确要“点击/选择某控件”，但 Action 是 Swipe；
      * Thinking 明确要“输入/键入文本”，但 Action 不是输入类行为（Type/输入）。
    - 不触发：
      * Thinking 是滑动且 Action 也是 Swipe（视为一致，不算不符）。
    - 处理方式：
      * 一旦触发，不是简单“改成匹配 Thinking 的动作”，而是判定当前行为已偏离主线，必须回到规则 1) 基于 Plan + 历史对话 + ScreenInfo 重新分析并输出“下一步最应该执行的单一动作”（若重新分析后仍应滑动，也允许继续滑动）。

    输出task格式要求：
    - task 一句话，格式固定：当前屏幕状态：<...>；下一步动作：<单一动作>
    - 下一步动作只能一个，不得多步/解释/总结/推理
    - 仅输出JSON：{"task":"..."}

    示例输出：
    {"task":"当前屏幕状态：未看到“办公”应用图标；下一步动作：手指向左滑继续查找。"}
    {"task":"当前屏幕状态：弹窗遮挡无法继续查找；下一步动作：点击关闭弹窗。"}
    {"task":"当前屏幕状态：偏离Plan且不在目标页面；下一步动作：点击返回上一级。"}
    """.trimIndent()

private val DEFAULT_OPERATOR_PROMPT_CN =
    """
    你是一个智能体分析专家，通过分析操作历史、当前状态图和下一步引导动作来执行一系列操作来完成任务。
    你必须严格按照要求输出以下格式：
    <think>{think}</think>
    <answer>{action}</answer>

    其中：
    - {think} 是对你为什么选择这个操作的简短推理说明。
    - {action} 是本次执行的具体操作指令，必须严格遵循下方定义的指令格式。

    操作指令及其作用如下：
    - do(action="Launch", app="xxx")  
        Launch是启动目标app的操作，这比通过主屏幕导航更快。此操作完成后，您将自动收到结果状态的截图。
    - do(action="Tap", element=[x,y])  
        Tap是点击操作，点击屏幕上的特定点。可用此操作点击按钮、选择项目、从主屏幕打开应用程序，或与任何可点击的用户界面元素进行交互。坐标系统从左上角 (0,0) 开始到右下角（999,999)结束。此操作完成后，您将自动收到结果状态的截图。
    - do(action="Tap", element=[x,y], message="重要操作")  
        基本功能同Tap，点击涉及财产、支付、隐私等敏感按钮时触发。
    - do(action="Type", text="xxx")  
        Type是输入操作，在当前聚焦的输入框中输入文本。使用此操作前，请确保输入框已被聚焦（先点击它）。输入的文本将像使用键盘输入一样输入。重要提示：手机可能正在使用 ADB 键盘，该键盘不会像普通键盘那样占用屏幕空间。要确认键盘已激活，请查看屏幕底部是否显示 'ADB Keyboard {ON}' 类似的文本，或者检查输入框是否处于激活/高亮状态。不要仅仅依赖视觉上的键盘显示。自动清除文本：当你使用输入操作时，输入框中现有的任何文本（包括占位符文本和实际输入）都会在输入新文本前自动清除。你无需在输入前手动清除文本——直接使用输入操作输入所需文本即可。操作完成后，你将自动收到结果状态的截图。
    - do(action="Type_Name", text="xxx")  
        Type_Name是输入人名的操作，基本功能同Type。
    - do(action="Interact")  
        Interact是当有多个满足条件的选项时而触发的交互操作，询问用户如何选择。
    - do(action="Swipe", start=[x1,y1], end=[x2,y2])  
        Swipe是滑动操作，通过从起始坐标拖动到结束坐标来执行滑动手势。可用于滚动内容、在屏幕之间导航、下拉通知栏以及项目栏或进行基于手势的导航。坐标系统从左上角 (0,0) 开始到右下角（999,999)结束。滑动持续时间会自动调整以实现自然的移动。此操作完成后，您将自动收到结果状态的截图。
    - do(action="Note", message="True")  
        记录当前页面内容以便后续总结。
    - do(action="Call_API", instruction="xxx")  
        总结或评论当前页面或已记录的内容。
    - do(action="Long Press", element=[x,y])  
        Long Pres是长按操作，在屏幕上的特定点长按指定时间。可用于触发上下文菜单、选择文本或激活长按交互。坐标系统从左上角 (0,0) 开始到右下角（999,999)结束。此操作完成后，您将自动收到结果状态的屏幕截图。
    - do(action="Double Tap", element=[x,y])  
        Double Tap在屏幕上的特定点快速连续点按两次。使用此操作可以激活双击交互，如缩放、选择文本或打开项目。坐标系统从左上角 (0,0) 开始到右下角（999,999)结束。此操作完成后，您将自动收到结果状态的截图。
    - do(action="Take_over", message="xxx")  
        Take_over是接管操作，表示在登录和验证阶段需要用户协助。
    - do(action="Back")  
        导航返回到上一个屏幕或关闭当前对话框。相当于按下 Android 的返回按钮。使用此操作可以从更深的屏幕返回、关闭弹出窗口或退出当前上下文。此操作完成后，您将自动收到结果状态的截图。
    - do(action="Home") 
        Home是回到系统桌面的操作，相当于按下 Android 主屏幕按钮。使用此操作可退出当前应用并返回启动器，或从已知状态启动新任务。此操作完成后，您将自动收到结果状态的截图。
    - do(action="Wait", duration="x seconds")  
        等待页面加载，x为需要等待多少秒。
    - finish(message="xxx")  
        finish是结束任务的操作，表示准确完整完成任务，message是终止信息。 

    必须遵循的规则：
    1. 在执行任何操作前，先检查当前app是否是目标app，如果不是，先执行 Launch。
    2. 如果进入到了无关页面，先执行 Back。如果执行Back后页面没有变化，请点击页面左上角的返回键进行返回，或者右上角的X号关闭。
    3. 如果页面未加载出内容，最多连续 Wait 三次，否则执行 Back重新进入。
    4. 如果页面显示网络问题，需要重新加载，请点击重新加载。
    5. 如果当前页面找不到目标联系人、商品、店铺等信息，可以尝试 Swipe 滑动查找。
    6. 遇到价格区间、时间区间等筛选条件，如果没有完全符合的，可以放宽要求。
    7. 在做小红书总结类任务时一定要筛选图文笔记。
    8. 购物车全选后再点击全选可以把状态设为全不选，在做购物车任务时，如果购物车里已经有商品被选中时，你需要点击全选后再点击取消全选，再去找需要购买或者删除的商品。
    9. 在做外卖任务时，如果相应店铺购物车里已经有其他商品你需要先把购物车清空再去购买用户指定的外卖。
    10. 在做点外卖任务时，如果用户需要点多个外卖，请尽量在同一店铺进行购买，如果无法找到可以下单，并说明某个商品未找到。
    11. 请严格遵循用户意图执行任务，用户的特殊要求可以执行多次搜索，滑动查找。比如（i）用户要求点一杯咖啡，要咸的，你可以直接搜索咸咖啡，或者搜索咖啡后滑动查找咸的咖啡，比如海盐咖啡。（ii）用户要找到XX群，发一条消息，你可以先搜索XX群，找不到结果后，将"群"字去掉，搜索XX重试。（iii）用户要找到宠物友好的餐厅，你可以搜索餐厅，找到筛选，找到设施，选择可带宠物，或者直接搜索可带宠物，必要时可以使用AI搜索。
    12. 在选择日期时，如果原滑动方向与预期日期越来越远，请向反方向滑动查找。
    13. 执行任务过程中如果有多个可选择的项目栏，请逐个查找每个项目栏，直到完成任务，一定不要在同一项目栏多次查找，从而陷入死循环。
    14. 在执行下一步操作前请一定要检查上一步的操作是否生效，如果点击没生效，可能因为app反应较慢，请先稍微等待一下，如果还是不生效请调整一下点击位置重试，如果仍然不生效请跳过这一步继续任务，并在finish message说明点击不生效。
    15. 在执行任务中如果遇到滑动不生效的情况，请调整一下起始点位置，增大滑动距离重试，如果还是不生效，有可能是已经滑到底了，请继续向反方向滑动，直到顶部或底部，如果仍然没有符合要求的结果，请跳过这一步继续任务，并在finish message说明但没找到要求的项目。
    16. 在做游戏任务时如果在战斗页面如果有自动战斗一定要开启自动战斗，如果多轮历史状态相似要检查自动战斗是否开启。
    17. 如果没有合适的搜索结果，可能是因为搜索页面不对，请返回到搜索页面的上一级尝试重新搜索，如果尝试三次返回上一级搜索后仍然没有符合要求的结果，执行 finish(message="原因")。
    18. 在结束任务前请一定要仔细检查任务是否完整准确的完成，如果出现错选、漏选、多选的情况，请返回之前的步骤进行纠正。
    """.trimIndent()

private val DEFAULT_OPERATOR_PROMPT_EN =
    """
    # Setup
    You are a professional Android operation agent assistant, performing a series of operations to complete a task by analyzing operation history, current state diagrams, and next guided actions.

    # More details about the code
    Your response format must be structured as follows:

    Think first: Use <think>...</think> to analyze the current screen, identify key elements, and determine the most efficient action.
    Provide the action: Use <answer>...</answer> to return a single line of pseudo-code representing the operation.

    Your output should STRICTLY follow the format:
    <think>
    [Your thought]
    </think>
    <answer>
    [Your operation code]
    </answer>

    - **Tap**
      Perform a tap action on a specified screen area. The element is a list of 2 integers, representing the coordinates of the tap point.
      **Example**:
      <answer>
      do(action="Tap", element=[x,y])
      </answer>
    - **Type**
      Enter text into the currently focused input field.
      **Example**:
      <answer>
      do(action="Type", text="Hello World")
      </answer>
    - **Swipe**
      Perform a swipe action with start point and end point.
      **Examples**:
      <answer>
      do(action="Swipe", start=[x1,y1], end=[x2,y2])
      </answer>
    - **Long Press**
      Perform a long press action on a specified screen area.
      You can add the element to the action to specify the long press area. The element is a list of 2 integers, representing the coordinates of the long press point.
      **Example**:
      <answer>
      do(action="Long Press", element=[x,y])
      </answer>
    - **Launch**
      Launch an app. Try to use launch action when you need to launch an app. Check the instruction to choose the right app before you use this action.
      **Example**:
      <answer>
      do(action="Launch", app="Settings")
      </answer>
    - **Back**
      Press the Back button to navigate to the previous screen.
      **Example**:
      <answer>
      do(action="Back")
      </answer>
    - **Finish**
      Terminate the program and optionally print a message.
      **Example**:
      <answer>
      finish(message="Task completed.")
      </answer>


    REMEMBER:
    - Think before you act: Always analyze the current UI and the best course of action before executing any step, and output in <think> part.
    - Only ONE LINE of action in <answer> part per response: Each step must contain exactly one line of executable code.
    - Generate execution code strictly according to format requirements.
    """.trimIndent()

private val DEFAULT_PROMPT_RULES = DEFAULT_OPERATOR_PROMPT_CN

class MainActivity : ComponentActivity() {
    // Requires API 23+ (overlay permission APIs).
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MLLMTheme {
                MainScreen()
            }
        }
    }
}

// Requires API 23+ (overlay permission APIs).
@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val baseContext = LocalContext.current
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
    val baseConfiguration = LocalConfiguration.current
    var languageCode by remember { mutableStateOf(DEFAULT_LANGUAGE_CODE) }
    val localizedContext =
        remember(baseContext, languageCode) {
            createLocalizedContext(baseContext, languageCode)
        }
    val localizedConfiguration =
        remember(baseConfiguration, languageCode) {
            Configuration(baseConfiguration).apply {
                setLocale(resolveLocale(languageCode))
            }
        }
    LaunchedEffect(languageCode) {
        OverlayController.updateLanguage(languageCode)
    }
    if (activityResultRegistryOwner != null) {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration,
            LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
        ) {
            MainScreenContent(
                languageCode = languageCode,
                onLanguageCodeChange = { languageCode = it },
                activity = baseContext as? Activity,
                appContext = baseContext.applicationContext,
            )
        }
    } else {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration,
        ) {
            MainScreenContent(
                languageCode = languageCode,
                onLanguageCodeChange = { languageCode = it },
                activity = baseContext as? Activity,
                appContext = baseContext.applicationContext,
            )
        }
    }
}

private fun applyAppLanguage(languageCode: String) {
    val tag = resolveLanguageTag(languageCode)
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}

private fun startActivitySafely(
    activity: Activity?,
    appContext: Context,
    intent: Intent,
) {
    if (activity != null) {
        activity.startActivity(intent)
    } else {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }
}

// Requires API 23+ (overlay permission APIs).
@RequiresApi(Build.VERSION_CODES.M)
@Composable
private fun MainScreenContent(
    languageCode: String,
    onLanguageCodeChange: (String) -> Unit,
    activity: Activity?,
    appContext: Context,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val screenshotController = remember { ScreenshotController(context) }

    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showGearMenu by remember { mutableStateOf(false) }
    var promptActionMenuFor by remember { mutableStateOf<String?>(null) }
    var llmActionMenuFor by remember { mutableStateOf<String?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var homeTab by remember { mutableStateOf(HomeTab.PROMPT) }
    var overlayPermissionGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var overlayEnabled by remember { mutableStateOf(overlayPermissionGranted) }
    var overlayEnableRequested by remember { mutableStateOf(false) }
    var micPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var notificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            },
        )
    }
    val requestMicPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            micPermissionGranted = granted
        }
    val requestNotificationPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            notificationPermissionGranted = granted
        }

    var llmConfigs by remember {
        mutableStateOf(
            listOf(
                LlmConfig(
                    name = DEFAULT_LLM_CONFIG_NAME,
                    baseUrl = DEFAULT_MODEL_MANAGER_URL,
                    apiKey = DEFAULT_MODEL_MANAGER_API_KEY,
                    modelNames =
                        listOf(
                            DEFAULT_LLM_MODEL,
                            DEFAULT_PLANNER_TEXT_MODEL,
                            DEFAULT_SCREEN_CHECK_MODEL,
                        ),
                ),
            ),
        )
    }
    var selectedOperatorLlmName by remember { mutableStateOf(DEFAULT_LLM_CONFIG_NAME) }
    var selectedPlannerLlmName by remember { mutableStateOf(DEFAULT_LLM_CONFIG_NAME) }
    var selectedScreenCheckLlmName by remember { mutableStateOf(DEFAULT_LLM_CONFIG_NAME) }
    var llmModel by remember { mutableStateOf(DEFAULT_LLM_MODEL) }
    var maxSteps by remember { mutableStateOf(DEFAULT_MAX_STEPS) }
    var operatorMaxTokens by remember { mutableStateOf(DEFAULT_OPERATOR_MAX_TOKENS) }
    var operatorTemperature by remember { mutableStateOf(DEFAULT_OPERATOR_TEMPERATURE) }
    var operatorTopP by remember { mutableStateOf(DEFAULT_OPERATOR_TOP_P) }
    var operatorReasoningEnabled by remember { mutableStateOf(DEFAULT_OPERATOR_REASONING_ENABLED) }
    var operatorReasoningEffort by remember { mutableStateOf(DEFAULT_OPERATOR_REASONING_EFFORT) }
    var plannerTextModel by remember { mutableStateOf(DEFAULT_PLANNER_TEXT_MODEL) }
    var plannerTextMaxTokens by remember { mutableStateOf(DEFAULT_PLANNER_TEXT_MAX_TOKENS) }
    var plannerTextTemperature by remember { mutableStateOf(DEFAULT_PLANNER_TEXT_TEMPERATURE) }
    var plannerTextTopP by remember { mutableStateOf(DEFAULT_PLANNER_TEXT_TOP_P) }
    var plannerTextReasoningEnabled by remember {
        mutableStateOf(DEFAULT_PLANNER_TEXT_REASONING_ENABLED)
    }
    var plannerTextReasoningEffort by remember {
        mutableStateOf(DEFAULT_PLANNER_TEXT_REASONING_EFFORT)
    }
    var plannerEnabled by remember { mutableStateOf(DEFAULT_PLANNER_ENABLED) }
    var screenCheckModel by remember { mutableStateOf(DEFAULT_SCREEN_CHECK_MODEL) }
    var screenCheckMaxTokens by remember { mutableStateOf(DEFAULT_SCREEN_CHECK_MAX_TOKENS) }
    var screenCheckTemperature by remember { mutableStateOf(DEFAULT_SCREEN_CHECK_TEMPERATURE) }
    var screenCheckTopP by remember { mutableStateOf(DEFAULT_SCREEN_CHECK_TOP_P) }
    var screenCheckEnabled by remember { mutableStateOf(DEFAULT_SCREEN_CHECK_ENABLED) }
    var screenCheckReasoningEnabled by remember {
        mutableStateOf(DEFAULT_SCREEN_CHECK_REASONING_ENABLED)
    }
    var screenCheckReasoningEffort by remember {
        mutableStateOf(DEFAULT_SCREEN_CHECK_REASONING_EFFORT)
    }
    var plannerSelectedPromptName by remember { mutableStateOf(DEFAULT_PLANNER_PROMPT_NAME) }
    var screenCheckSelectedPromptName by remember { mutableStateOf(DEFAULT_SCREEN_CHECK_PROMPT_NAME) }
    var baiduAppId by remember { mutableStateOf(DEFAULT_BAIDU_APP_ID) }
    var baiduAppKey by remember { mutableStateOf(DEFAULT_BAIDU_APP_KEY) }
    var baiduSecretKey by remember { mutableStateOf(DEFAULT_BAIDU_SECRET_KEY) }
    var baiduWakeupEnabled by remember { mutableStateOf(DEFAULT_BAIDU_WAKEUP_ENABLED) }
    var appPackagesText by remember { mutableStateOf(AppPackages.toText()) }
    var bubbleSizeDp by remember { mutableStateOf(DEFAULT_BUBBLE_SIZE_DP) }

    val defaultPrompts =
        remember {
            listOf(
                PromptConfig(
                    name = DEFAULT_PROMPT_NAME,
                    content = DEFAULT_PROMPT_RULES,
                    kind = PromptKind.PRIMARY,
                ),
                PromptConfig(
                    name = DEFAULT_PLANNER_PROMPT_NAME,
                    content = DEFAULT_PLANNER_PROMPT,
                    kind = PromptKind.PLANNER,
                ),
                PromptConfig(
                    name = DEFAULT_SUBTASK_PROMPT_NAME,
                    content =
                        """
                        你是一个任务拆解专家。你的目标是把用户的实际需求拆解成多个可执行的子任务，并按数字编号逐步输出。

                        规则：
                        1) 只有当某一步需要“打开某个应用程序”时，该步骤使用如下固定格式（不限于第1步）：
                           使用Launch命令打开【AppName】：Launch(AppName)（若失败则：返回主屏幕找到“AppName”再打开）
                        2) 用户可能是语音输入，存在错别字/同音字/漏字；请先基于语境纠错后再拆解，但不要额外解释纠错过程。
                        3) 每一步都用“进入/点击/搜索/选择/添加/提交/支付/返回”等动词开头，句式尽量统一，动作原子化且可执行。
                        4) 不要加入“询问/确认/请提供信息”等提问步骤；信息缺失时按最常见默认路径继续拆解。
                        5) 输出只包含步骤清单（阿拉伯数字编号），不输出任何解释、标题或额外文本。

                        输出示例（仅示例）：
                        1. 使用Launch命令打开美团：Launch(美团)（若失败则：返回主屏幕找到“美团”再打开）
                        2. 选中搜索框
                        3. 搜索老乡鸡并进入匹配的店铺
                        4. 选择商品并添加到购物车
                        5. 进入结算页，选择地址，并填写相关信息
                        6. 提交订单，等待用户支付
                        """.trimIndent(),
                    kind = PromptKind.SUBTASK,
                ),
                PromptConfig(
                    name = DEFAULT_SCREEN_CHECK_PROMPT_NAME,
                    content = DEFAULT_SCREEN_CHECK_PROMPT,
                    kind = PromptKind.SCREEN_CHECK,
                ),
            )
        }
    var prompts by remember { mutableStateOf(defaultPrompts) }
    var selectedPromptName by remember { mutableStateOf(defaultPrompts.first().name) }
    val editPromptLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
            val data = result.data ?: return@rememberLauncherForActivityResult
            val name = data.getStringExtra(PromptEditorActivity.EXTRA_NAME)?.trim().orEmpty()
            val content = data.getStringExtra(PromptEditorActivity.EXTRA_CONTENT)?.trim().orEmpty()
            val tags = data.getStringExtra(PromptEditorActivity.EXTRA_TAGS)?.trim().orEmpty()
            val kindId = data.getStringExtra(PromptEditorActivity.EXTRA_KIND)?.trim().orEmpty()
            val originalName = data.getStringExtra(PromptEditorActivity.EXTRA_ORIGINAL_NAME)
            val kind = PromptKind.fromId(kindId)
            if (name.isBlank()) return@rememberLauncherForActivityResult
            if (originalName.isNullOrBlank()) {
                prompts = prompts + PromptConfig(name = name, content = content, tags = tags, kind = kind)
            } else {
                prompts =
                    prompts.map { existing ->
                        if (existing.name == originalName) {
                            PromptConfig(name = name, content = content, tags = tags, kind = kind)
                        } else {
                            existing
                        }
                    }
                if (selectedPromptName == originalName) {
                    selectedPromptName = if (kind == PromptKind.PRIMARY) name else DEFAULT_PROMPT_NAME
                }
                if (plannerSelectedPromptName == originalName) {
                    plannerSelectedPromptName =
                        if (kind == PromptKind.PLANNER) name else DEFAULT_PLANNER_PROMPT_NAME
                }
                if (screenCheckSelectedPromptName == originalName) {
                    screenCheckSelectedPromptName =
                        if (kind == PromptKind.SCREEN_CHECK) name else DEFAULT_SCREEN_CHECK_PROMPT_NAME
                }
            }
            scope.launch {
                SettingsRepository.savePrompts(
                    context,
                    prompts.map { PromptEntry(it.name, it.content, it.tags, it.kind) },
                    selectedPromptName,
                    plannerSelectedPromptName,
                    screenCheckSelectedPromptName,
                )
            }
        }
    val editAppPackagesLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
            val data = result.data ?: return@rememberLauncherForActivityResult
            val updatedText =
                data.getStringExtra(AppPackagesActivity.EXTRA_TEXT)
                    ?.trim()
                    .orEmpty()
            appPackagesText = updatedText
            AppPackages.updateFromText(updatedText)
        }
    var editingLlmOriginalName by remember { mutableStateOf<String?>(null) }
    val editLlmLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
            val data = result.data ?: return@rememberLauncherForActivityResult
            val updatedName = data.getStringExtra(ModelManagerActivity.EXTRA_NAME)?.trim().orEmpty()
            val updatedUrl = data.getStringExtra(ModelManagerActivity.EXTRA_URL)?.trim().orEmpty()
            val updatedApiKey = data.getStringExtra(ModelManagerActivity.EXTRA_API_KEY)?.trim().orEmpty()
            val updatedNamesText = data.getStringExtra(ModelManagerActivity.EXTRA_NAMES)?.trim().orEmpty()
            val updatedNames =
                updatedNamesText.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toList()
            if (updatedName.isBlank()) {
                editingLlmOriginalName = null
                return@rememberLauncherForActivityResult
            }
            if (editingLlmOriginalName.isNullOrBlank() &&
                updatedName == DEFAULT_LLM_CONFIG_NAME
            ) {
                editingLlmOriginalName = null
                return@rememberLauncherForActivityResult
            }
            val originalName = editingLlmOriginalName
            val duplicateExists = llmConfigs.any { it.name == updatedName }
            if (!originalName.isNullOrBlank() && originalName != updatedName && duplicateExists) {
                editingLlmOriginalName = null
                return@rememberLauncherForActivityResult
            }
            if (originalName.isNullOrBlank() && duplicateExists) {
                editingLlmOriginalName = null
                return@rememberLauncherForActivityResult
            }
            val updatedConfig =
                LlmConfig(
                    name = updatedName,
                    baseUrl = updatedUrl,
                    apiKey = updatedApiKey,
                    modelNames = updatedNames,
                )
            llmConfigs =
                if (originalName.isNullOrBlank()) {
                    llmConfigs + updatedConfig
                } else {
                    llmConfigs.map { config ->
                        if (config.name == originalName) {
                            updatedConfig
                        } else {
                            config
                        }
                    }
                }
            if (!originalName.isNullOrBlank() && originalName != updatedName) {
                if (selectedOperatorLlmName == originalName) {
                    selectedOperatorLlmName = updatedName
                }
                if (selectedPlannerLlmName == originalName) {
                    selectedPlannerLlmName = updatedName
                }
                if (selectedScreenCheckLlmName == originalName) {
                    selectedScreenCheckLlmName = updatedName
                }
            }
            editingLlmOriginalName = null
            scope.launch {
                SettingsRepository.saveSettings(
                    context,
                    AppSettings(
                        llmConfigs = llmConfigs,
                        selectedOperatorLlmName = selectedOperatorLlmName,
                        selectedPlannerLlmName = selectedPlannerLlmName,
                        selectedScreenCheckLlmName = selectedScreenCheckLlmName,
                        llmModel = llmModel,
                        languageCode = languageCode,
                        maxSteps = maxSteps,
                        operatorMaxTokens = operatorMaxTokens,
                        operatorTemperature = operatorTemperature,
                        operatorTopP = operatorTopP,
                        operatorReasoningEnabled = operatorReasoningEnabled,
                        operatorReasoningEffort = operatorReasoningEffort,
                        plannerTextModel = plannerTextModel,
                        plannerTextMaxTokens = plannerTextMaxTokens,
                        plannerTextTemperature = plannerTextTemperature,
                        plannerTextTopP = plannerTextTopP,
                        plannerTextReasoningEnabled = plannerTextReasoningEnabled,
                        plannerTextReasoningEffort = plannerTextReasoningEffort,
                        plannerEnabled = plannerEnabled,
                        screenCheckEnabled = screenCheckEnabled,
                        screenCheckModel = screenCheckModel,
                        screenCheckMaxTokens = screenCheckMaxTokens,
                        screenCheckTemperature = screenCheckTemperature,
                        screenCheckTopP = screenCheckTopP,
                        screenCheckReasoningEnabled = screenCheckReasoningEnabled,
                        screenCheckReasoningEffort = screenCheckReasoningEffort,
                        selectedPrompt = selectedPromptName,
                        selectedPlannerPrompt = plannerSelectedPromptName,
                        selectedScreenCheckPrompt = screenCheckSelectedPromptName,
                        prompts = prompts.map { PromptEntry(it.name, it.content, it.tags, it.kind) },
                        appPackagesText = appPackagesText,
                        bubbleSizeDp = bubbleSizeDp,
                        baiduAppId = baiduAppId,
                        baiduAppKey = baiduAppKey,
                        baiduSecretKey = baiduSecretKey,
                        baiduWakeupEnabled = baiduWakeupEnabled,
                    ),
                )
            }
        }
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var chatInput by remember { mutableStateOf("") }
    var chatRunning by remember { mutableStateOf(false) }
    var pendingConfirmation by remember { mutableStateOf<SensitiveRequest?>(null) }
    var taskJob by remember { mutableStateOf<Job?>(null) }
    val llmConfigNames =
        remember(llmConfigs) {
            val names = llmConfigs.map { it.name }.filter { it.isNotBlank() }.distinct()
            if (names.contains(DEFAULT_LLM_CONFIG_NAME)) {
                names
            } else {
                listOf(DEFAULT_LLM_CONFIG_NAME) + names
            }
        }
    val fallbackLlmConfig =
        remember(llmConfigs) {
            llmConfigs.firstOrNull { it.name == DEFAULT_LLM_CONFIG_NAME }
                ?: llmConfigs.firstOrNull()
                ?: LlmConfig(
                    name = DEFAULT_LLM_CONFIG_NAME,
                    baseUrl = DEFAULT_MODEL_MANAGER_URL,
                    apiKey = DEFAULT_MODEL_MANAGER_API_KEY,
                    modelNames = listOf(DEFAULT_LLM_MODEL),
                )
        }

    val defaultSettings =
        remember {
            AppSettings(
                llmConfigs =
                    listOf(
                        LlmConfig(
                            name = DEFAULT_LLM_CONFIG_NAME,
                            baseUrl = DEFAULT_MODEL_MANAGER_URL,
                            apiKey = DEFAULT_MODEL_MANAGER_API_KEY,
                            modelNames =
                                listOf(
                                    DEFAULT_LLM_MODEL,
                                    DEFAULT_PLANNER_TEXT_MODEL,
                                    DEFAULT_SCREEN_CHECK_MODEL,
                                ),
                        ),
                    ),
                selectedOperatorLlmName = DEFAULT_LLM_CONFIG_NAME,
                selectedPlannerLlmName = DEFAULT_LLM_CONFIG_NAME,
                selectedScreenCheckLlmName = DEFAULT_LLM_CONFIG_NAME,
                llmModel = DEFAULT_LLM_MODEL,
                languageCode = DEFAULT_LANGUAGE_CODE,
                maxSteps = DEFAULT_MAX_STEPS,
                operatorMaxTokens = DEFAULT_OPERATOR_MAX_TOKENS,
                operatorTemperature = DEFAULT_OPERATOR_TEMPERATURE,
                operatorTopP = DEFAULT_OPERATOR_TOP_P,
                operatorReasoningEnabled = DEFAULT_OPERATOR_REASONING_ENABLED,
                operatorReasoningEffort = DEFAULT_OPERATOR_REASONING_EFFORT,
                plannerTextModel = DEFAULT_PLANNER_TEXT_MODEL,
                plannerTextMaxTokens = DEFAULT_PLANNER_TEXT_MAX_TOKENS,
            plannerTextTemperature = DEFAULT_PLANNER_TEXT_TEMPERATURE,
            plannerTextTopP = DEFAULT_PLANNER_TEXT_TOP_P,
            plannerTextReasoningEnabled = DEFAULT_PLANNER_TEXT_REASONING_ENABLED,
            plannerTextReasoningEffort = DEFAULT_PLANNER_TEXT_REASONING_EFFORT,
            plannerEnabled = DEFAULT_PLANNER_ENABLED,
            screenCheckModel = DEFAULT_SCREEN_CHECK_MODEL,
                screenCheckMaxTokens = DEFAULT_SCREEN_CHECK_MAX_TOKENS,
                screenCheckTemperature = DEFAULT_SCREEN_CHECK_TEMPERATURE,
                screenCheckTopP = DEFAULT_SCREEN_CHECK_TOP_P,
                screenCheckEnabled = DEFAULT_SCREEN_CHECK_ENABLED,
                screenCheckReasoningEnabled = DEFAULT_SCREEN_CHECK_REASONING_ENABLED,
                screenCheckReasoningEffort = DEFAULT_SCREEN_CHECK_REASONING_EFFORT,
                selectedPrompt = DEFAULT_PROMPT_NAME,
                selectedPlannerPrompt = DEFAULT_PLANNER_PROMPT_NAME,
                selectedScreenCheckPrompt = DEFAULT_SCREEN_CHECK_PROMPT_NAME,
                prompts = defaultPrompts.map { PromptEntry(it.name, it.content, it.tags, it.kind) },
                appPackagesText = AppPackages.toText(),
                bubbleSizeDp = DEFAULT_BUBBLE_SIZE_DP,
                baiduAppId = DEFAULT_BAIDU_APP_ID,
                baiduAppKey = DEFAULT_BAIDU_APP_KEY,
                baiduSecretKey = DEFAULT_BAIDU_SECRET_KEY,
                baiduWakeupEnabled = DEFAULT_BAIDU_WAKEUP_ENABLED,
            )
        }

    val startTask: suspend (String) -> Unit = startTask@{ task ->
        if (chatRunning) return@startTask
        if (task.isBlank()) return@startTask
        chatMessages.clear()
        OverlayController.clearMessages()
        if (DeviceAccessibilityService.instance == null) {
            val isEnglish = resolveLocale(languageCode).language == Locale.ENGLISH.language
            val accessibilityMessage =
                if (isEnglish) {
                    "Enable accessibility service to control the device."
                } else {
                    "请开启无障碍服务以控制设备。"
                }
            chatMessages.add(
                ChatMessage(
                    role = "system",
                    content = accessibilityMessage,
                ),
            )
            OverlayController.appendMessage(
                role = "system",
                text = accessibilityMessage,
            )
            return@startTask
        }
        val plannerPrompts = prompts.filter { it.kind == PromptKind.PLANNER }
        val subtaskPrompts = prompts.filter { it.kind == PromptKind.SUBTASK }
        val screenCheckPrompts = prompts.filter { it.kind == PromptKind.SCREEN_CHECK }
        val resumeWakeupAfterTask = baiduWakeupEnabled
        if (resumeWakeupAfterTask) {
            OverlayController.updateWakeupEnabled(false)
        }
        chatMessages.add(ChatMessage(role = "user", content = task))
        OverlayController.appendMessage(role = "user", text = task)
        chatInput = ""
        chatRunning = true
        OverlayController.setCancelVisible(true)
        OverlayController.showPanel("Task: $task")

        val operatorLlmConfig =
            llmConfigs.firstOrNull { it.name == selectedOperatorLlmName }
                ?: fallbackLlmConfig
        val plannerLlmConfig =
            llmConfigs.firstOrNull { it.name == selectedPlannerLlmName }
                ?: fallbackLlmConfig
        val screenCheckLlmConfig =
            llmConfigs.firstOrNull { it.name == selectedScreenCheckLlmName }
                ?: fallbackLlmConfig

        val primaryPrompts = prompts.filter { it.kind == PromptKind.PRIMARY }
        val prompt =
            primaryPrompts.firstOrNull { it.name == selectedPromptName }
                ?: primaryPrompts.firstOrNull()
                ?: prompts.first()
        var taskForAgent = task
        if (plannerEnabled && subtaskPrompts.isNotEmpty() && plannerPrompts.isNotEmpty()) {
            val plannerPrompt =
                plannerPrompts.firstOrNull { it.name == plannerSelectedPromptName }
                    ?: plannerPrompts.first()
            val plannerConfig =
                ModelConfig(
                    baseUrl = plannerLlmConfig.baseUrl.ifBlank { DEFAULT_MODEL_MANAGER_URL },
                    apiKey = plannerLlmConfig.apiKey.ifBlank { DEFAULT_MODEL_MANAGER_API_KEY },
                    modelName = plannerTextModel.ifBlank { DEFAULT_PLANNER_TEXT_MODEL },
                    maxTokens = plannerTextMaxTokens,
                    temperature = plannerTextTemperature,
                    topP = plannerTextTopP,
                    reasoningEffort =
                        if (plannerTextReasoningEnabled) {
                            com.openai.models.ReasoningEffort.of(plannerTextReasoningEffort)
                        } else {
                            null
                        },
                )
            val plannerClient = ModelClient(plannerConfig)
            val tagsArray = org.json.JSONArray()
            val tagsInfoArray = org.json.JSONArray()
            subtaskPrompts.forEach { promptItem ->
                tagsArray.put(promptItem.name)
                val tagObj = org.json.JSONObject()
                tagObj.put("name", promptItem.name)
                val tagList =
                    promptItem.tags.split(",", "，")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                val tagJson = org.json.JSONArray()
                tagList.forEach { tagJson.put(it) }
                tagObj.put("tags", tagJson)
                tagsInfoArray.put(tagObj)
            }
            val plannerMessages =
                listOf(
                    com.example.mllm.agent.MessageBuilder.createSystemMessage(plannerPrompt.content),
                    com.example.mllm.agent.MessageBuilder.createUserMessage(
                        text = "TASK: $task\nTAGS: $tagsArray\nTAGS_INFO: $tagsInfoArray",
                        imageBase64 = null,
                    ),
                )
            val selectedName =
                try {
                    val response = plannerClient.request(plannerMessages)
                    parsePlannerPromptName(response.rawContent)
                } catch (_: Exception) {
                    null
                }
            val matched =
                selectedName?.let { name ->
                    subtaskPrompts.firstOrNull { it.name == name }
                }
            val fallback = subtaskPrompts.firstOrNull { it.name == DEFAULT_SUBTASK_PROMPT_NAME }
            val chosen = matched ?: fallback
            if (chosen != null) {
                chatMessages.add(ChatMessage(role = "system", content = "Planner: ${chosen.name}"))
                OverlayController.appendMessage(role = "system", text = "Planner: ${chosen.name}")
                val subtaskMessages =
                    listOf(
                        com.example.mllm.agent.MessageBuilder.createSystemMessage(chosen.content),
                        com.example.mllm.agent.MessageBuilder.createUserMessage(
                            text = task,
                            imageBase64 = null,
                        ),
                    )
                val subtaskText =
                    try {
                        plannerClient.request(subtaskMessages).rawContent.trim()
                    } catch (_: Exception) {
                        ""
                    }
                if (subtaskText.isNotBlank()) {
                    taskForAgent = subtaskText
                    chatMessages.add(ChatMessage(role = "assistant", content = subtaskText))
                    OverlayController.appendMessage(role = "assistant", text = subtaskText)
                }
            }
        }
        if (OverlayController.consumeHidePanelAfterSend()) {
            scope.launch {
                delay(3000)
                OverlayController.hidePanel()
            }
        }
        val config =
            ModelConfig(
                baseUrl = operatorLlmConfig.baseUrl.ifBlank { DEFAULT_MODEL_MANAGER_URL },
                apiKey = operatorLlmConfig.apiKey.ifBlank { DEFAULT_MODEL_MANAGER_API_KEY },
                modelName = llmModel.ifBlank { DEFAULT_LLM_MODEL },
                maxTokens = operatorMaxTokens,
                temperature = operatorTemperature,
                topP = operatorTopP,
                reasoningEffort =
                    if (operatorReasoningEnabled) {
                        com.openai.models.ReasoningEffort.of(operatorReasoningEffort)
                    } else {
                        null
                    },
            )

        var pendingScreenCorrection: String? = null
        val runner =
            AgentRunner(
                context = context,
                screenshotController = screenshotController,
                modelClient = ModelClient(config),
                systemPromptFixed = SYSTEM_PROMPT_FIXED,
                systemRulesProvider = { prompt.content },
                extraUserContextProvider = {
                    val correction = pendingScreenCorrection
                    pendingScreenCorrection = null
                    correction
                },
                confirmSensitiveAction = { message ->
                    val deferred = CompletableDeferred<Boolean>()
                    pendingConfirmation = SensitiveRequest(message, deferred)
                    deferred.await()
                },
                takeoverCallback = { message ->
                    chatMessages.add(
                        ChatMessage(role = "system", content = "Takeover: $message"),
                    )
                    OverlayController.appendMessage(
                        role = "system",
                        text = "Takeover: $message",
                    )
                },
            )

        var stepIndex = 0
        val planSummary = taskForAgent
        var lastScreenCheckStep = 0
        var cancelled = false
        val screenCheckHistory = mutableListOf<com.example.mllm.agent.ModelMessage>()
        try {
            runner.runTask(taskForAgent, maxSteps = maxSteps) { step ->
                stepIndex += 1
                val actionName = step.action?.actionName ?: "finish"
                val actionFields =
                    step.action?.fields?.entries
                        ?.joinToString(", ") { "${it.key}=${it.value}" }
                val resultLine = step.message ?: if (step.success) "OK" else "Failed"
                val content =
                    buildString {
                        append("Step ").append(stepIndex)
                        append("\nAction: ").append(actionName)
                        if (!actionFields.isNullOrBlank()) {
                            append(" (").append(actionFields).append(")")
                        }
                        if (step.thinking.isNotBlank()) {
                            append("\nThinking: ").append(step.thinking)
                        }
                        append("\nResult: ").append(resultLine)
                        if (step.finished) {
                            append("\nStatus: Finished")
                        }
                    }
                chatMessages.add(ChatMessage(role = "assistant", content = content))
                OverlayController.appendMessage(role = "assistant", text = content)
                OverlayController.updatePanel("Step $stepIndex: $actionName")
                if (!step.finished && stepIndex > lastScreenCheckStep && screenCheckEnabled) {
                    lastScreenCheckStep = stepIndex
                    val screenCheckPrompt =
                        screenCheckPrompts.firstOrNull {
                            it.name == screenCheckSelectedPromptName
                        } ?: screenCheckPrompts.firstOrNull()
                    if (screenCheckPrompt != null) {
                        val screenCheckConfig =
                            ModelConfig(
                                baseUrl = screenCheckLlmConfig.baseUrl.ifBlank { DEFAULT_MODEL_MANAGER_URL },
                                apiKey = screenCheckLlmConfig.apiKey.ifBlank { DEFAULT_MODEL_MANAGER_API_KEY },
                                modelName = screenCheckModel.ifBlank { DEFAULT_SCREEN_CHECK_MODEL },
                                maxTokens = screenCheckMaxTokens,
                                temperature = screenCheckTemperature,
                                topP = screenCheckTopP,
                                reasoningEffort =
                                    if (screenCheckReasoningEnabled) {
                                        com.openai.models.ReasoningEffort.of(screenCheckReasoningEffort)
                                    } else {
                                        null
                                    },
                            )
                        val screenCheckClient = ModelClient(screenCheckConfig)
                        val screenshot = screenshotController.capture()
                        val currentApp = DeviceController.getCurrentApp(context)
                        val screenInfo = com.example.mllm.agent.MessageBuilder.buildScreenInfo(currentApp)
                        val userText =
                            buildString {
                                append("Plan: ").append(planSummary)
                                append("\nAction: ").append(actionName)
                                if (!actionFields.isNullOrBlank()) {
                                    append(" (").append(actionFields).append(")")
                                }
                                append("\nThinking: ").append(step.thinking)
                                append("\nResult: ").append(resultLine)
                                append("\nScreenInfo: ").append(screenInfo)
                            }
                        if (screenCheckHistory.isEmpty()) {
                            screenCheckHistory.add(
                                com.example.mllm.agent.MessageBuilder.createSystemMessage(
                                    screenCheckPrompt.content,
                                ),
                            )
                        }
                        val userMessage =
                            com.example.mllm.agent.MessageBuilder.createUserMessage(
                                text = userText,
                                imageBase64 = screenshot.base64Data,
                            )
                        screenCheckHistory.add(userMessage)
                        val response =
                            try {
                                screenCheckClient.request(screenCheckHistory)
                            } catch (_: Exception) {
                                null
                            }
                        screenCheckHistory[screenCheckHistory.lastIndex] =
                            com.example.mllm.agent.MessageBuilder.removeImagesFromMessage(
                                screenCheckHistory.last(),
                            )
                        val correctedTask = response?.rawContent?.let { parseScreenCheckTask(it) }
                        if (!response?.rawContent.isNullOrBlank()) {
                            screenCheckHistory.add(
                                com.example.mllm.agent.MessageBuilder.createAssistantMessage(
                                    response.rawContent,
                                ),
                            )
                        }
                        if (!correctedTask.isNullOrBlank()) {
                            pendingScreenCorrection = correctedTask
                            chatMessages.add(
                                ChatMessage(role = "system", content = "Screen Check: $correctedTask"),
                            )
                            OverlayController.appendMessage(
                                role = "system",
                                text = "Screen Check: $correctedTask",
                            )
                        }
                    }
                }
            }
        } catch (_: CancellationException) {
            cancelled = true
            chatMessages.add(ChatMessage(role = "system", content = "Task cancelled"))
            OverlayController.updatePanel(
                context.getString(R.string.overlay_cancelled),
            )
        } finally {
            chatRunning = false
            OverlayController.setCancelVisible(false)
            if (OverlayController.consumeRestorePanelOnFinish()) {
                OverlayController.setPanelVisible(true)
                val finishedText =
                    if (cancelled) {
                        context.getString(R.string.overlay_cancelled)
                    } else {
                        context.getString(R.string.overlay_finished)
                    }
                OverlayController.updatePanel(finishedText)
            }
            if (resumeWakeupAfterTask) {
                OverlayController.updateWakeupEnabled(true)
            }
        }
    }

    val launchTask: (String) -> Unit = launchTask@{ task ->
        if (taskJob?.isActive == true) return@launchTask
        val job = scope.launch { startTask(task) }
        taskJob = job
        job.invokeOnCompletion {
            scope.launch {
                if (taskJob == job) {
                    taskJob = null
                }
            }
        }
    }

    val cancelCurrentTask: () -> Unit = {
        taskJob?.cancel()
        taskJob = null
        chatRunning = false
        OverlayController.setCancelVisible(false)
        OverlayController.updatePanel(context.getString(R.string.overlay_cancelled))
    }

    val latestLaunchTask by rememberUpdatedState(launchTask)
    val latestCancelTask by rememberUpdatedState(cancelCurrentTask)

    LaunchedEffect(Unit) {
        OverlayController.onTaskRequested = { task ->
            if (task.isNotBlank()) {
                latestLaunchTask(task)
            }
        }
        OverlayController.onTaskCancelRequested = {
            latestCancelTask()
        }
        SettingsRepository.settingsFlow(context, defaultSettings).collect { settings ->
            llmModel = settings.llmModel
            onLanguageCodeChange(settings.languageCode)
            applyAppLanguage(settings.languageCode)
            OverlayController.updateLanguage(settings.languageCode)
            llmConfigs = settings.llmConfigs
            selectedOperatorLlmName = settings.selectedOperatorLlmName
            selectedPlannerLlmName = settings.selectedPlannerLlmName
            selectedScreenCheckLlmName = settings.selectedScreenCheckLlmName
            maxSteps = settings.maxSteps
            operatorMaxTokens = settings.operatorMaxTokens
            operatorTemperature = settings.operatorTemperature
            operatorTopP = settings.operatorTopP
            operatorReasoningEnabled = settings.operatorReasoningEnabled
            operatorReasoningEffort = settings.operatorReasoningEffort
            plannerTextModel = settings.plannerTextModel
            plannerTextMaxTokens = settings.plannerTextMaxTokens
            plannerTextTemperature = settings.plannerTextTemperature
            plannerTextTopP = settings.plannerTextTopP
            plannerTextReasoningEnabled = settings.plannerTextReasoningEnabled
            plannerTextReasoningEffort = settings.plannerTextReasoningEffort
            plannerEnabled = settings.plannerEnabled
            screenCheckModel = settings.screenCheckModel
            screenCheckMaxTokens = settings.screenCheckMaxTokens
            screenCheckTemperature = settings.screenCheckTemperature
            screenCheckTopP = settings.screenCheckTopP
            screenCheckEnabled = settings.screenCheckEnabled
            screenCheckReasoningEnabled = settings.screenCheckReasoningEnabled
            screenCheckReasoningEffort = settings.screenCheckReasoningEffort
            plannerSelectedPromptName = settings.selectedPlannerPrompt
            screenCheckSelectedPromptName = settings.selectedScreenCheckPrompt
            appPackagesText = settings.appPackagesText
            AppPackages.updateFromText(settings.appPackagesText)
            bubbleSizeDp = settings.bubbleSizeDp
            OverlayController.updateBubbleSize(settings.bubbleSizeDp)
            baiduAppId = settings.baiduAppId
            baiduAppKey = settings.baiduAppKey
            baiduSecretKey = settings.baiduSecretKey
            baiduWakeupEnabled = settings.baiduWakeupEnabled
            OverlayController.baiduConfig =
                BaiduConfig(
                    settings.baiduAppId,
                    settings.baiduAppKey,
                    settings.baiduSecretKey,
                )
            OverlayController.updateWakeupEnabled(settings.baiduWakeupEnabled)
            val loadedPrompts =
                settings.prompts.map {
                    PromptConfig(it.name, it.content, it.tags, it.kind)
                }.toMutableList()
            val hasPlanner = loadedPrompts.any { it.kind == PromptKind.PLANNER }
            if (!hasPlanner) {
                loadedPrompts.add(
                    PromptConfig(
                        name = DEFAULT_PLANNER_PROMPT_NAME,
                        content = DEFAULT_PLANNER_PROMPT,
                        kind = PromptKind.PLANNER,
                    ),
                )
            }
            val hasSubtask = loadedPrompts.any { it.kind == PromptKind.SUBTASK }
            if (!hasSubtask) {
                loadedPrompts.add(
                    PromptConfig(
                        name = DEFAULT_SUBTASK_PROMPT_NAME,
                        content =
                            """
                            你是一个任务拆解专家。你的目标是把用户的实际需求拆解成多个可执行的子任务，并按数字编号逐步输出。

                            规则：
                            1) 只有当某一步需要“打开某个应用程序”时，该步骤使用如下固定格式（不限于第1步）：
                               使用Launch命令打开【AppName】：Launch(AppName)（若失败则：返回主屏幕找到“AppName”再打开）
                            2) 用户可能是语音输入，存在错别字/同音字/漏字；请先基于语境纠错后再拆解，但不要额外解释纠错过程。
                            3) 每一步都用“进入/点击/搜索/选择/添加/提交/支付/返回”等动词开头，句式尽量统一，动作原子化且可执行。
                            4) 不要加入“询问/确认/请提供信息”等提问步骤；信息缺失时按最常见默认路径继续拆解。
                            5) 输出只包含步骤清单（阿拉伯数字编号），不输出任何解释、标题或额外文本。

                            输出示例（仅示例）：
                            1. 使用Launch命令打开美团：Launch(美团)（若失败则：返回主屏幕找到“美团”再打开）
                            2. 选中搜索框
                            3. 搜索老乡鸡并进入匹配的店铺
                            4. 选择商品并添加到购物车
                            5. 进入结算页，选择地址，并填写相关信息
                            6. 提交订单，等待用户支付
                            """.trimIndent(),
                        kind = PromptKind.SUBTASK,
                    ),
                )
            }
            val hasScreenCheck = loadedPrompts.any { it.kind == PromptKind.SCREEN_CHECK }
            if (!hasScreenCheck) {
                loadedPrompts.add(
                    PromptConfig(
                        name = DEFAULT_SCREEN_CHECK_PROMPT_NAME,
                        content = DEFAULT_SCREEN_CHECK_PROMPT,
                        kind = PromptKind.SCREEN_CHECK,
                    ),
                )
            }
            prompts = loadedPrompts
            val primaryPrompts = loadedPrompts.filter { it.kind == PromptKind.PRIMARY }
            selectedPromptName = primaryPrompts.firstOrNull { it.name == settings.selectedPrompt }?.name
                ?: primaryPrompts.firstOrNull()?.name
                ?: DEFAULT_PROMPT_NAME
        }
    }

    LaunchedEffect(overlayPermissionGranted, overlayEnabled) {
        if (overlayPermissionGranted && overlayEnabled) {
            OverlayController.ensureService(context.applicationContext)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    overlayPermissionGranted = Settings.canDrawOverlays(context)
                    if (!overlayPermissionGranted) {
                        overlayEnabled = false
                    } else if (overlayEnableRequested) {
                        overlayEnabled = true
                        overlayEnableRequested = false
                    } else {
                        overlayEnabled = true
                    }
                    micPermissionGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    notificationPermissionGranted =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    if (overlayPermissionGranted && overlayEnabled) {
                        OverlayController.ensureService(context.applicationContext)
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showSettings) {
        SettingsScreen(
            llmModel = llmModel,
            languageCode = languageCode,
            llmConfigs = llmConfigs,
            selectedOperatorLlmName = selectedOperatorLlmName,
            selectedPlannerLlmName = selectedPlannerLlmName,
            selectedScreenCheckLlmName = selectedScreenCheckLlmName,
            maxSteps = maxSteps,
            operatorMaxTokens = operatorMaxTokens,
            operatorTemperature = operatorTemperature,
            operatorTopP = operatorTopP,
            operatorReasoningEnabled = operatorReasoningEnabled,
            operatorReasoningEffort = operatorReasoningEffort,
            plannerTextModel = plannerTextModel,
            plannerTextMaxTokens = plannerTextMaxTokens,
            plannerTextTemperature = plannerTextTemperature,
            plannerTextTopP = plannerTextTopP,
            plannerTextReasoningEnabled = plannerTextReasoningEnabled,
            plannerTextReasoningEffort = plannerTextReasoningEffort,
            plannerEnabled = plannerEnabled,
            screenCheckModel = screenCheckModel,
            screenCheckMaxTokens = screenCheckMaxTokens,
            screenCheckTemperature = screenCheckTemperature,
            screenCheckTopP = screenCheckTopP,
            screenCheckEnabled = screenCheckEnabled,
            screenCheckReasoningEnabled = screenCheckReasoningEnabled,
            screenCheckReasoningEffort = screenCheckReasoningEffort,
            selectedPlannerPromptName = plannerSelectedPromptName,
            selectedScreenCheckPromptName = screenCheckSelectedPromptName,
            appPackagesText = appPackagesText,
            bubbleSizeDp = bubbleSizeDp,
            baiduAppId = baiduAppId,
            baiduAppKey = baiduAppKey,
            baiduSecretKey = baiduSecretKey,
            baiduWakeupEnabled = baiduWakeupEnabled,
            prompts = prompts,
            selectedPromptName = selectedPromptName,
            onBack = { showSettings = false },
            overlayPermissionGranted = overlayPermissionGranted,
            onEnableOverlay = {
                if (Settings.canDrawOverlays(context)) {
                    overlayEnabled = true
                    OverlayController.ensureService(appContext)
                } else {
                    overlayEnableRequested = true
                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        )
                    startActivitySafely(activity, appContext, intent)
                }
            },
            onDisableOverlay = {
                appContext.stopService(Intent(appContext, OverlayService::class.java))
                overlayEnableRequested = false
                val intent =
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                startActivitySafely(activity, appContext, intent)
            },
            micPermissionGranted = micPermissionGranted,
            onRequestMicPermission = {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            },
            onOpenAppSettings = {
                val intent =
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}"),
                    )
                startActivitySafely(activity, appContext, intent)
            },
            onOpenAppPackages = {
                val intent =
                    AppPackagesActivity.createIntent(
                        context,
                        appPackagesText,
                        languageCode,
                    )
                editAppPackagesLauncher.launch(intent)
            },
            notificationPermissionGranted = notificationPermissionGranted,
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onSave = {
                    model: String,
                    languageCodeValue: String,
                    operatorLlmName: String,
                    plannerLlmName: String,
                    screenCheckLlmName: String,
                    steps: Int,
                    operatorMaxTokensValue: Int,
                    operatorTemperatureValue: Double,
                    operatorTopPValue: Double,
                    operatorReasoningEnabledValue: Boolean,
                    operatorReasoningEffortValue: String,
                    plannerTextModelValue: String,
                    plannerTextMaxTokensValue: Int,
                    plannerTextTemperatureValue: Double,
                    plannerTextTopPValue: Double,
                    plannerTextReasoningEnabledValue: Boolean,
                    plannerTextReasoningEffortValue: String,
                    plannerEnabledValue: Boolean,
                    screenCheckEnabledValue: Boolean,
                    screenCheckModelValue: String,
                    screenCheckMaxTokensValue: Int,
                    screenCheckTemperatureValue: Double,
                    screenCheckTopPValue: Double,
                    screenCheckReasoningEnabledValue: Boolean,
                    screenCheckReasoningEffortValue: String,
                    plannerPromptName: String,
                    screenCheckPromptName: String,
                    promptName: String,
                    appPackagesTextValue: String,
                    bubbleSizeDpValue: Int,
                    appId: String,
                    appKey: String,
                    secretKey: String,
                    wakeupEnabled: Boolean,
                ->
                llmModel = model
                val languageChanged = languageCodeValue != languageCode
                onLanguageCodeChange(languageCodeValue)
                OverlayController.updateLanguage(languageCodeValue)
                selectedOperatorLlmName = operatorLlmName
                selectedPlannerLlmName = plannerLlmName
                selectedScreenCheckLlmName = screenCheckLlmName
                operatorMaxTokens = operatorMaxTokensValue
                operatorTemperature = operatorTemperatureValue
                operatorTopP = operatorTopPValue
                operatorReasoningEnabled = operatorReasoningEnabledValue
                operatorReasoningEffort = operatorReasoningEffortValue
                plannerTextModel = plannerTextModelValue
                plannerTextMaxTokens = plannerTextMaxTokensValue
                plannerTextTemperature = plannerTextTemperatureValue
                plannerTextTopP = plannerTextTopPValue
                plannerTextReasoningEnabled = plannerTextReasoningEnabledValue
                plannerTextReasoningEffort = plannerTextReasoningEffortValue
                plannerEnabled = plannerEnabledValue
                screenCheckEnabled = screenCheckEnabledValue
                screenCheckModel = screenCheckModelValue
                screenCheckMaxTokens = screenCheckMaxTokensValue
                screenCheckTemperature = screenCheckTemperatureValue
                screenCheckTopP = screenCheckTopPValue
                screenCheckReasoningEnabled = screenCheckReasoningEnabledValue
                screenCheckReasoningEffort = screenCheckReasoningEffortValue
                plannerSelectedPromptName = plannerPromptName
                screenCheckSelectedPromptName = screenCheckPromptName
                selectedPromptName = promptName
                maxSteps = steps
                appPackagesText = appPackagesTextValue
                AppPackages.updateFromText(appPackagesTextValue)
                bubbleSizeDp = bubbleSizeDpValue
                OverlayController.updateBubbleSize(bubbleSizeDpValue)
                baiduAppId = appId
                baiduAppKey = appKey
                baiduSecretKey = secretKey
                baiduWakeupEnabled = wakeupEnabled
                showSettings = false
                OverlayController.baiduConfig = BaiduConfig(appId, appKey, secretKey)
                OverlayController.updateWakeupEnabled(wakeupEnabled)
                if (wakeupEnabled && Settings.canDrawOverlays(context)) {
                    OverlayController.ensureService(context.applicationContext)
                }
                scope.launch {
                    SettingsRepository.saveSettings(
                        context,
                        AppSettings(
                            llmConfigs = llmConfigs,
                            selectedOperatorLlmName = operatorLlmName,
                            selectedPlannerLlmName = plannerLlmName,
                            selectedScreenCheckLlmName = screenCheckLlmName,
                            llmModel = model,
                            languageCode = languageCodeValue,
                            maxSteps = steps,
                            operatorMaxTokens = operatorMaxTokensValue,
                            operatorTemperature = operatorTemperatureValue,
                            operatorTopP = operatorTopPValue,
                            operatorReasoningEnabled = operatorReasoningEnabledValue,
                            operatorReasoningEffort = operatorReasoningEffortValue,
                            plannerTextModel = plannerTextModelValue,
                            plannerTextMaxTokens = plannerTextMaxTokensValue,
                            plannerTextTemperature = plannerTextTemperatureValue,
                            plannerTextTopP = plannerTextTopPValue,
                            plannerTextReasoningEnabled = plannerTextReasoningEnabledValue,
                            plannerTextReasoningEffort = plannerTextReasoningEffortValue,
                            plannerEnabled = plannerEnabledValue,
                            screenCheckEnabled = screenCheckEnabledValue,
                            screenCheckModel = screenCheckModelValue,
                            screenCheckMaxTokens = screenCheckMaxTokensValue,
                            screenCheckTemperature = screenCheckTemperatureValue,
                            screenCheckTopP = screenCheckTopPValue,
                            screenCheckReasoningEnabled = screenCheckReasoningEnabledValue,
                            screenCheckReasoningEffort = screenCheckReasoningEffortValue,
                            selectedPrompt = promptName,
                            selectedPlannerPrompt = plannerPromptName,
                            selectedScreenCheckPrompt = screenCheckPromptName,
                            prompts = prompts.map { PromptEntry(it.name, it.content, it.tags, it.kind) },
                            appPackagesText = appPackagesTextValue,
                            bubbleSizeDp = bubbleSizeDpValue,
                            baiduAppId = appId,
                            baiduAppKey = appKey,
                            baiduSecretKey = secretKey,
                            baiduWakeupEnabled = wakeupEnabled,
                        ),
                    )
                    if (languageChanged) {
                        applyAppLanguage(languageCodeValue)
                    }
                    if (languageChanged && context is Activity) {
                        context.recreate()
                    }
                }
            },
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.main_title)) },
                    actions = {
                        IconButton(onClick = {
                            if (homeTab == HomeTab.PROMPT) {
                                val intent =
                                    PromptEditorActivity.createIntent(
                                        context = context,
                                        title = context.getString(R.string.new_system_prompt_title),
                                        name = "",
                                        content = "",
                                        tags = "",
                                        kindId = PromptKind.PRIMARY.id,
                                        nameEnabled = true,
                                        kindEnabled = true,
                                        fixedContent = SYSTEM_PROMPT_FIXED,
                                        helperText = null,
                                        confirmLabel = context.getString(R.string.action_add),
                                        originalName = null,
                                        languageCode = languageCode,
                                        operatorTemplateEnabled = true,
                                        operatorTemplateCn = DEFAULT_OPERATOR_PROMPT_CN,
                                        operatorTemplateEn = DEFAULT_OPERATOR_PROMPT_EN,
                                    )
                                editPromptLauncher.launch(intent)
                            } else {
                                editingLlmOriginalName = null
                                val intent =
                                    ModelManagerActivity.createIntent(
                                        context = context,
                                        title = context.getString(R.string.new_llm_title),
                                        name = "",
                                        nameEnabled = true,
                                        url = "",
                                        apiKey = "",
                                        names = emptyList(),
                                        languageCode = languageCode,
                                    )
                                editLlmLauncher.launch(intent)
                            }
                        }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.cd_new_prompt),
                            )
                        }
                        IconButton(onClick = { showGearMenu = true }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.cd_settings),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .fillMaxSize()
                        .imePadding()
                        .windowInsetsPadding(WindowInsets.ime),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val primaryPrompts = prompts.filter { it.kind == PromptKind.PRIMARY }
                val promptsForList =
                    prompts.sortedWith(
                        compareBy { prompt ->
                            when (prompt.kind) {
                                PromptKind.PRIMARY -> 0
                                PromptKind.PLANNER -> 1
                                PromptKind.SCREEN_CHECK -> 2
                                PromptKind.SUBTASK -> 3
                            }
                        },
                    )
                var promptMenuExpanded by remember { mutableStateOf(false) }
                var llmMenuExpanded by remember { mutableStateOf(false) }
                var selectedLlmConfigName by remember { mutableStateOf(fallbackLlmConfig.name) }
                LaunchedEffect(llmConfigs) {
                    if (llmConfigs.none { it.name == selectedLlmConfigName }) {
                        selectedLlmConfigName = fallbackLlmConfig.name
                    }
                }
                val selectedLlmConfig =
                    llmConfigs.firstOrNull { it.name == selectedLlmConfigName }
                        ?: fallbackLlmConfig
                val llmConfigsForList =
                    remember(llmConfigs) {
                        val defaultConfig = llmConfigs.firstOrNull { it.name == DEFAULT_LLM_CONFIG_NAME }
                        val rest = llmConfigs.filter { it.name != DEFAULT_LLM_CONFIG_NAME }
                        listOfNotNull(defaultConfig) + rest
                    }
                if (homeTab == HomeTab.PROMPT) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(promptsForList) { prompt ->
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                when (prompt.kind) {
                                                    PromptKind.PRIMARY -> selectedPromptName = prompt.name
                                                    PromptKind.PLANNER -> plannerSelectedPromptName = prompt.name
                                                    PromptKind.SCREEN_CHECK ->
                                                        screenCheckSelectedPromptName = prompt.name
                                                    PromptKind.SUBTASK -> Unit
                                                }
                                                scope.launch {
                                                    SettingsRepository.savePrompts(
                                                        context,
                                                        prompts.map {
                                                            PromptEntry(
                                                                it.name,
                                                                it.content,
                                                                it.tags,
                                                                it.kind,
                                                            )
                                                        },
                                                        selectedPromptName,
                                                        plannerSelectedPromptName,
                                                        screenCheckSelectedPromptName,
                                                    )
                                                }
                                                promptActionMenuFor = prompt.name
                                            },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(
                                            modifier =
                                                Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp),
                                        ) {
                                            Text(
                                                text = prompt.name,
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text =
                                                    buildString {
                                                        append(promptKindLabel(context, prompt.kind))
                                                    },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = prompt.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (prompt.kind == PromptKind.SUBTASK && prompt.tags.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text =
                                                        stringResource(
                                                            R.string.prompt_tags,
                                                            prompt.tags,
                                                        ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (prompt.kind == PromptKind.PRIMARY &&
                                            prompt.name == selectedPromptName
                                        ) {
                                            Text(
                                                text = stringResource(R.string.selected_operator),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        if (prompt.kind == PromptKind.PLANNER &&
                                            prompt.name == plannerSelectedPromptName
                                        ) {
                                            Text(
                                                text = stringResource(R.string.selected_planner),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        if (prompt.kind == PromptKind.SCREEN_CHECK &&
                                            prompt.name == screenCheckSelectedPromptName
                                        ) {
                                            Text(
                                                text = stringResource(R.string.selected_screen_check),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                                PromptActionsMenu(
                                    expanded = promptActionMenuFor == prompt.name,
                                    canDelete =
                                        !(
                                            (
                                                prompt.kind == PromptKind.PRIMARY &&
                                                    prompt.name == DEFAULT_PROMPT_NAME
                                            ) ||
                                                (
                                                    prompt.kind == PromptKind.PLANNER &&
                                                        prompt.name == DEFAULT_PLANNER_PROMPT_NAME
                                                ) ||
                                                (
                                                    prompt.kind == PromptKind.SUBTASK &&
                                                        prompt.name == DEFAULT_SUBTASK_PROMPT_NAME
                                                ) ||
                                                (
                                                    prompt.kind == PromptKind.SCREEN_CHECK &&
                                                        prompt.name == DEFAULT_SCREEN_CHECK_PROMPT_NAME
                                                )
                                        ),
                                    onDismiss = { promptActionMenuFor = null },
                                    onEdit = {
                                        promptActionMenuFor = null
                                        val intent =
                                            PromptEditorActivity.createIntent(
                                                context = context,
                                                title = context.getString(R.string.edit_system_prompt_title),
                                                name = prompt.name,
                                                content = prompt.content,
                                                tags = prompt.tags,
                                                kindId = prompt.kind.id,
                                                nameEnabled = true,
                                                kindEnabled = true,
                                                fixedContent = SYSTEM_PROMPT_FIXED,
                                                helperText = null,
                                                confirmLabel = context.getString(R.string.action_save),
                                                originalName = prompt.name,
                                                languageCode = languageCode,
                                                operatorTemplateEnabled = false,
                                                operatorTemplateCn = "",
                                                operatorTemplateEn = "",
                                            )
                                        editPromptLauncher.launch(intent)
                                    },
                                    onDelete = {
                                        prompts = prompts.filterNot { it.name == prompt.name }
                                        if (prompt.kind == PromptKind.PRIMARY &&
                                            selectedPromptName == prompt.name
                                        ) {
                                            selectedPromptName = DEFAULT_PROMPT_NAME
                                        }
                                        if (prompt.kind == PromptKind.PLANNER &&
                                            plannerSelectedPromptName == prompt.name
                                        ) {
                                            plannerSelectedPromptName = DEFAULT_PLANNER_PROMPT_NAME
                                        }
                                        if (prompt.kind == PromptKind.SCREEN_CHECK &&
                                            screenCheckSelectedPromptName == prompt.name
                                        ) {
                                            screenCheckSelectedPromptName =
                                                DEFAULT_SCREEN_CHECK_PROMPT_NAME
                                        }
                                        promptActionMenuFor = null
                                        scope.launch {
                                            SettingsRepository.savePrompts(
                                                context,
                                                prompts.map { PromptEntry(it.name, it.content, it.tags, it.kind) },
                                                selectedPromptName,
                                                plannerSelectedPromptName,
                                                screenCheckSelectedPromptName,
                                            )
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd),
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(llmConfigsForList) { config ->
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                ) {
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedLlmConfigName = config.name
                                                    llmActionMenuFor = config.name
                                                },
                                    ) {
                                        Text(
                                            text = config.name,
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        val isOperator = config.name == selectedOperatorLlmName
                                        val isPlanner = config.name == selectedPlannerLlmName
                                        val isScreenCheck = config.name == selectedScreenCheckLlmName
                                        if (isOperator || isPlanner || isScreenCheck) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (isOperator) {
                                                    Text(
                                                        text = stringResource(R.string.selected_operator),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                                if (isPlanner) {
                                                    Text(
                                                        text = stringResource(R.string.selected_planner),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                                if (isScreenCheck) {
                                                    Text(
                                                        text = stringResource(R.string.selected_screen_check),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    LlmActionsMenu(
                                        expanded = llmActionMenuFor == config.name,
                                        canDelete = llmConfigs.firstOrNull() != config,
                                        onDismiss = { llmActionMenuFor = null },
                                        onEdit = {
                                            llmActionMenuFor = null
                                            editingLlmOriginalName = config.name
                                            val intent =
                                                ModelManagerActivity.createIntent(
                                                    context = context,
                                                    title = context.getString(R.string.edit_llm_title),
                                                    name = config.name,
                                                    nameEnabled = true,
                                                    url = config.baseUrl,
                                                    apiKey = config.apiKey,
                                                    names = config.modelNames,
                                                    languageCode = languageCode,
                                                )
                                            editLlmLauncher.launch(intent)
                                        },
                                        onDelete = {
                                            val toDelete = config.name
                                            if (llmConfigs.firstOrNull() == config) return@LlmActionsMenu
                                            llmConfigs = llmConfigs.filterNot { it.name == toDelete }
                                            if (selectedOperatorLlmName == toDelete) {
                                                selectedOperatorLlmName = DEFAULT_LLM_CONFIG_NAME
                                            }
                                            if (selectedPlannerLlmName == toDelete) {
                                                selectedPlannerLlmName = DEFAULT_LLM_CONFIG_NAME
                                            }
                                            if (selectedScreenCheckLlmName == toDelete) {
                                                selectedScreenCheckLlmName = DEFAULT_LLM_CONFIG_NAME
                                            }
                                            llmActionMenuFor = null
                                            scope.launch {
                                                SettingsRepository.saveSettings(
                                                    context,
                                                    AppSettings(
                                                        llmConfigs = llmConfigs,
                                                        selectedOperatorLlmName = selectedOperatorLlmName,
                                                        selectedPlannerLlmName = selectedPlannerLlmName,
                                                        selectedScreenCheckLlmName = selectedScreenCheckLlmName,
                                                        llmModel = llmModel,
                                                        languageCode = languageCode,
                                                        maxSteps = maxSteps,
                                                        operatorMaxTokens = operatorMaxTokens,
                                                        operatorTemperature = operatorTemperature,
                                                        operatorTopP = operatorTopP,
                                                        operatorReasoningEnabled = operatorReasoningEnabled,
                                                        operatorReasoningEffort = operatorReasoningEffort,
                                                        plannerTextModel = plannerTextModel,
                                                        plannerTextMaxTokens = plannerTextMaxTokens,
                                                        plannerTextTemperature = plannerTextTemperature,
                                                        plannerTextTopP = plannerTextTopP,
                                                        plannerTextReasoningEnabled = plannerTextReasoningEnabled,
                                                        plannerTextReasoningEffort = plannerTextReasoningEffort,
                                                        plannerEnabled = plannerEnabled,
                                                        screenCheckModel = screenCheckModel,
                                                        screenCheckMaxTokens = screenCheckMaxTokens,
                                                        screenCheckTemperature = screenCheckTemperature,
                                                        screenCheckTopP = screenCheckTopP,
                                                        screenCheckEnabled = screenCheckEnabled,
                                                        screenCheckReasoningEnabled = screenCheckReasoningEnabled,
                                                        screenCheckReasoningEffort = screenCheckReasoningEffort,
                                                        selectedPrompt = selectedPromptName,
                                                        selectedPlannerPrompt = plannerSelectedPromptName,
                                                        selectedScreenCheckPrompt = screenCheckSelectedPromptName,
                                                        prompts =
                                                            prompts.map {
                                                                PromptEntry(it.name, it.content, it.tags, it.kind)
                                                            },
                                                        appPackagesText = appPackagesText,
                                                        bubbleSizeDp = bubbleSizeDp,
                                                        baiduAppId = baiduAppId,
                                                        baiduAppKey = baiduAppKey,
                                                        baiduSecretKey = baiduSecretKey,
                                                        baiduWakeupEnabled = baiduWakeupEnabled,
                                                    ),
                                                )
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val promptSelected = homeTab == HomeTab.PROMPT
                    val llmSelected = homeTab == HomeTab.LLM
                    if (promptSelected) {
                        Button(
                            onClick = { homeTab = HomeTab.PROMPT },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.tab_prompt))
                        }
                    } else {
                        OutlinedButton(
                            onClick = { homeTab = HomeTab.PROMPT },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.tab_prompt))
                        }
                    }
                    if (llmSelected) {
                        Button(
                            onClick = { homeTab = HomeTab.LLM },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.tab_llm))
                        }
                    } else {
                        OutlinedButton(
                            onClick = { homeTab = HomeTab.LLM },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.tab_llm))
                        }
                    }
                }
            }
        }

        if (showGearMenu) {
            val menuTitle = context.getString(R.string.menu_title)
            val menuChat = context.getString(R.string.menu_chat)
            val menuSettings = context.getString(R.string.menu_settings)
            val menuAbout = context.getString(R.string.menu_about)
            val menuClose = context.getString(R.string.menu_close)
            AlertDialog(
                onDismissRequest = { showGearMenu = false },
                title = { Text(text = menuTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                showGearMenu = false
                                showChat = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(menuChat)
                        }
                        Button(
                            onClick = {
                                showGearMenu = false
                                showSettings = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(menuSettings)
                        }
                        Button(
                            onClick = {
                                showGearMenu = false
                                showAbout = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(menuAbout)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showGearMenu = false }) {
                        Text(menuClose)
                    }
                },
            )
        }

        if (showAbout) {
            AboutDialog(onDismiss = { showAbout = false })
        }

        if (showChat) {
            ChatDialog(
                messages = chatMessages,
                input = chatInput,
                running = chatRunning,
                onInputChange = { chatInput = it },
                onDismiss = { showChat = false },
                onSend = { task -> launchTask(task) },
            )
        }

        if (pendingConfirmation != null) {
            ConfirmationDialog(
                message = pendingConfirmation!!.message,
                onConfirm = {
                    pendingConfirmation?.deferred?.complete(true)
                    pendingConfirmation = null
                },
                onCancel = {
                    pendingConfirmation?.deferred?.complete(false)
                    pendingConfirmation = null
                },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    llmConfigs: List<LlmConfig>,
    llmModel: String,
    languageCode: String,
    selectedOperatorLlmName: String,
    selectedPlannerLlmName: String,
    selectedScreenCheckLlmName: String,
    maxSteps: Int,
    operatorMaxTokens: Int,
    operatorTemperature: Double,
    operatorTopP: Double,
    operatorReasoningEnabled: Boolean,
    operatorReasoningEffort: String,
    plannerTextModel: String,
    plannerTextMaxTokens: Int,
    plannerTextTemperature: Double,
    plannerTextTopP: Double,
    plannerTextReasoningEnabled: Boolean,
    plannerTextReasoningEffort: String,
    plannerEnabled: Boolean,
    screenCheckEnabled: Boolean,
    screenCheckModel: String,
    screenCheckMaxTokens: Int,
    screenCheckTemperature: Double,
    screenCheckTopP: Double,
    screenCheckReasoningEnabled: Boolean,
    screenCheckReasoningEffort: String,
    selectedPlannerPromptName: String,
    selectedScreenCheckPromptName: String,
    appPackagesText: String,
    bubbleSizeDp: Int,
    baiduAppId: String,
    baiduAppKey: String,
    baiduSecretKey: String,
    baiduWakeupEnabled: Boolean,
    prompts: List<PromptConfig>,
    selectedPromptName: String,
    onBack: () -> Unit,
    overlayPermissionGranted: Boolean,
    onEnableOverlay: () -> Unit,
    onDisableOverlay: () -> Unit,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenAppPackages: () -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSave: SettingsSaveCallback,
) {
    var model by remember { mutableStateOf(llmModel) }
    var languageCodeState by remember(languageCode) { mutableStateOf(languageCode) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var operatorLlmNameState by remember { mutableStateOf(selectedOperatorLlmName) }
    var plannerLlmNameState by remember { mutableStateOf(selectedPlannerLlmName) }
    var screenCheckLlmNameState by remember { mutableStateOf(selectedScreenCheckLlmName) }
    var maxStepsText by remember { mutableStateOf(maxSteps.toString()) }
    var operatorMaxTokensText by remember { mutableStateOf(operatorMaxTokens.toString()) }
    var operatorTemperatureText by remember { mutableStateOf(operatorTemperature.toString()) }
    var operatorTopPText by remember { mutableStateOf(operatorTopP.toString()) }
    var operatorReasoningEnabledState by remember { mutableStateOf(operatorReasoningEnabled) }
    var operatorReasoningEffortState by remember { mutableStateOf(operatorReasoningEffort) }
    var operatorReasoningMenuExpanded by remember { mutableStateOf(false) }
    var plannerTextModelState by remember { mutableStateOf(plannerTextModel) }
    var plannerTextMaxTokensText by remember { mutableStateOf(plannerTextMaxTokens.toString()) }
    var plannerTextTemperatureText by remember {
        mutableStateOf(plannerTextTemperature.toString())
    }
    var plannerTextTopPText by remember { mutableStateOf(plannerTextTopP.toString()) }
    var plannerTextReasoningEnabledState by remember {
        mutableStateOf(plannerTextReasoningEnabled)
    }
    var plannerTextReasoningEffortState by remember {
        mutableStateOf(plannerTextReasoningEffort)
    }
    var plannerEnabledState by remember { mutableStateOf(plannerEnabled) }
    var plannerTextReasoningMenuExpanded by remember { mutableStateOf(false) }
    var screenCheckModelState by remember { mutableStateOf(screenCheckModel) }
    var screenCheckMaxTokensText by remember { mutableStateOf(screenCheckMaxTokens.toString()) }
    var screenCheckTemperatureText by remember {
        mutableStateOf(screenCheckTemperature.toString())
    }
    var screenCheckTopPText by remember { mutableStateOf(screenCheckTopP.toString()) }
    var screenCheckEnabledState by remember { mutableStateOf(screenCheckEnabled) }
    var screenCheckReasoningEnabledState by remember {
        mutableStateOf(screenCheckReasoningEnabled)
    }
    var screenCheckReasoningEffortState by remember {
        mutableStateOf(screenCheckReasoningEffort)
    }
    var screenCheckReasoningMenuExpanded by remember { mutableStateOf(false) }
    val plannerPrompts = prompts.filter { it.kind == PromptKind.PLANNER }
    val resolvedPlannerPromptName =
        plannerPrompts.firstOrNull { it.name == selectedPlannerPromptName }?.name
            ?: plannerPrompts.firstOrNull()?.name
            ?: DEFAULT_PLANNER_PROMPT_NAME
    var plannerPromptName by remember(resolvedPlannerPromptName) {
        mutableStateOf(resolvedPlannerPromptName)
    }
    val screenCheckPrompts = prompts.filter { it.kind == PromptKind.SCREEN_CHECK }
    val resolvedScreenCheckPromptName =
        screenCheckPrompts.firstOrNull { it.name == selectedScreenCheckPromptName }?.name
            ?: screenCheckPrompts.firstOrNull()?.name
            ?: DEFAULT_SCREEN_CHECK_PROMPT_NAME
    var screenCheckPromptName by remember(resolvedScreenCheckPromptName) {
        mutableStateOf(resolvedScreenCheckPromptName)
    }
    var baiduId by remember { mutableStateOf(baiduAppId) }
    var baiduKey by remember { mutableStateOf(baiduAppKey) }
    var baiduSecret by remember { mutableStateOf(baiduSecretKey) }
    var wakeupEnabled by remember { mutableStateOf(baiduWakeupEnabled) }
    var bubbleSizeText by remember { mutableStateOf(bubbleSizeDp.toString()) }
    val primaryPrompts = prompts.filter { it.kind == PromptKind.PRIMARY }
    val resolvedPromptName =
        primaryPrompts.firstOrNull { it.name == selectedPromptName }?.name
            ?: primaryPrompts.firstOrNull()?.name
            ?: DEFAULT_PROMPT_NAME
    var promptName by remember(resolvedPromptName) { mutableStateOf(resolvedPromptName) }
    var promptMenuExpanded by remember { mutableStateOf(false) }
    var plannerPromptMenuExpanded by remember { mutableStateOf(false) }
    var screenCheckPromptMenuExpanded by remember { mutableStateOf(false) }
    val appPackageCount =
        appPackagesText.lineSequence()
            .map { it.trim() }
            .count { it.isNotEmpty() && !it.startsWith("#") }

    val llmConfigNames =
        remember(llmConfigs) {
            val names = llmConfigs.map { it.name }.filter { it.isNotBlank() }.distinct()
            if (names.contains(DEFAULT_LLM_CONFIG_NAME)) {
                names
            } else {
                listOf(DEFAULT_LLM_CONFIG_NAME) + names
            }
        }
    val fallbackConfig =
        llmConfigs.firstOrNull { it.name == DEFAULT_LLM_CONFIG_NAME }
            ?: llmConfigs.firstOrNull()
    LaunchedEffect(llmConfigs) {
        if (llmConfigNames.isNotEmpty()) {
            if (!llmConfigNames.contains(operatorLlmNameState)) {
                operatorLlmNameState = llmConfigNames.first()
            }
            if (!llmConfigNames.contains(plannerLlmNameState)) {
                plannerLlmNameState = llmConfigNames.first()
            }
            if (!llmConfigNames.contains(screenCheckLlmNameState)) {
                screenCheckLlmNameState = llmConfigNames.first()
            }
        }
    }
    val operatorAvailableModels =
        remember(llmConfigs, operatorLlmNameState, llmModel) {
            val config = llmConfigs.firstOrNull { it.name == operatorLlmNameState } ?: fallbackConfig
            val base = config?.modelNames?.ifEmpty { listOf(llmModel) } ?: listOf(llmModel)
            val normalized = base.map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            if (!normalized.contains(llmModel) && llmModel.isNotBlank()) {
                normalized.add(llmModel)
            }
            normalized.distinct()
        }
    val plannerAvailableModels =
        remember(llmConfigs, plannerLlmNameState, plannerTextModel) {
            val config = llmConfigs.firstOrNull { it.name == plannerLlmNameState } ?: fallbackConfig
            val base =
                config?.modelNames?.ifEmpty { listOf(plannerTextModel) }
                    ?: listOf(plannerTextModel)
            val normalized = base.map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            if (!normalized.contains(plannerTextModel) && plannerTextModel.isNotBlank()) {
                normalized.add(plannerTextModel)
            }
            normalized.distinct()
        }
    val screenCheckAvailableModels =
        remember(llmConfigs, screenCheckLlmNameState, screenCheckModel) {
            val config = llmConfigs.firstOrNull { it.name == screenCheckLlmNameState } ?: fallbackConfig
            val base =
                config?.modelNames?.ifEmpty { listOf(screenCheckModel) }
                    ?: listOf(screenCheckModel)
            val normalized = base.map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            if (!normalized.contains(screenCheckModel) && screenCheckModel.isNotBlank()) {
                normalized.add(screenCheckModel)
            }
            normalized.distinct()
        }
    var operatorModelMenuExpanded by remember { mutableStateOf(false) }
    var plannerModelMenuExpanded by remember { mutableStateOf(false) }
    var screenCheckModelMenuExpanded by remember { mutableStateOf(false) }
    var operatorLlmMenuExpanded by remember { mutableStateOf(false) }
    var plannerLlmMenuExpanded by remember { mutableStateOf(false) }
    var screenCheckLlmMenuExpanded by remember { mutableStateOf(false) }

    val handleSave = {
        val steps =
            maxStepsText.toIntOrNull()?.coerceAtLeast(1)
                ?: DEFAULT_MAX_STEPS
        val operatorMaxTokensValue =
            operatorMaxTokensText.toIntOrNull()
                ?.coerceAtLeast(1)
                ?: DEFAULT_OPERATOR_MAX_TOKENS
        val operatorTemperatureValue =
            operatorTemperatureText.toDoubleOrNull()
                ?: DEFAULT_OPERATOR_TEMPERATURE
        val operatorTopPValue =
            operatorTopPText.toDoubleOrNull()
                ?: DEFAULT_OPERATOR_TOP_P
        val plannerTextMaxTokensValue =
            plannerTextMaxTokensText.toIntOrNull()
                ?.coerceAtLeast(1)
                ?: DEFAULT_PLANNER_TEXT_MAX_TOKENS
        val plannerTextTemperatureValue =
            plannerTextTemperatureText.toDoubleOrNull()
                ?: DEFAULT_PLANNER_TEXT_TEMPERATURE
        val plannerTextTopPValue =
            plannerTextTopPText.toDoubleOrNull()
                ?: DEFAULT_PLANNER_TEXT_TOP_P
        val screenCheckMaxTokensValue =
            screenCheckMaxTokensText.toIntOrNull()
                ?.coerceAtLeast(1)
                ?: DEFAULT_SCREEN_CHECK_MAX_TOKENS
        val screenCheckTemperatureValue =
            screenCheckTemperatureText.toDoubleOrNull()
                ?: DEFAULT_SCREEN_CHECK_TEMPERATURE
        val screenCheckTopPValue =
            screenCheckTopPText.toDoubleOrNull()
                ?: DEFAULT_SCREEN_CHECK_TOP_P
        val bubbleSizeValue =
            bubbleSizeText.toIntOrNull()
                ?.coerceIn(32, 96)
                ?: DEFAULT_BUBBLE_SIZE_DP
        onSave(
            model,
            languageCodeState,
            operatorLlmNameState,
            plannerLlmNameState,
            screenCheckLlmNameState,
            steps,
            operatorMaxTokensValue,
            operatorTemperatureValue,
            operatorTopPValue,
            operatorReasoningEnabledState,
            operatorReasoningEffortState,
            plannerTextModelState,
            plannerTextMaxTokensValue,
            plannerTextTemperatureValue,
            plannerTextTopPValue,
            plannerTextReasoningEnabledState,
            plannerTextReasoningEffortState,
            plannerEnabledState,
            screenCheckEnabledState,
            screenCheckModelState,
            screenCheckMaxTokensValue,
            screenCheckTemperatureValue,
            screenCheckTopPValue,
            screenCheckReasoningEnabledState,
            screenCheckReasoningEffortState,
            plannerPromptName,
            screenCheckPromptName,
            promptName,
            appPackagesText,
            bubbleSizeValue,
            baiduId,
            baiduKey,
            baiduSecret,
            wakeupEnabled,
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = handleSave) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.label_language),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            val languageOptions =
                listOf(
                    LANGUAGE_CODE_ZH to stringResource(R.string.language_chinese),
                    LANGUAGE_CODE_EN to stringResource(R.string.language_english),
                )
            ExposedDropdownMenuBox(
                expanded = languageMenuExpanded,
                onExpandedChange = { languageMenuExpanded = !languageMenuExpanded },
            ) {
                OutlinedTextField(
                    value =
                        languageOptions.firstOrNull { it.first == languageCodeState }?.second
                            ?: stringResource(R.string.language_chinese),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_language)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = languageMenuExpanded,
                        )
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = languageMenuExpanded,
                    onDismissRequest = { languageMenuExpanded = false },
                ) {
                    languageOptions.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                languageCodeState = code
                                languageMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.label_operator),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            ExposedDropdownMenuBox(
                expanded = operatorLlmMenuExpanded,
                onExpandedChange = { operatorLlmMenuExpanded = !operatorLlmMenuExpanded },
            ) {
                OutlinedTextField(
                    value = operatorLlmNameState,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_llm)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = operatorLlmMenuExpanded,
                        )
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = operatorLlmMenuExpanded,
                    onDismissRequest = { operatorLlmMenuExpanded = false },
                ) {
                    llmConfigNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                operatorLlmNameState = name
                                operatorLlmMenuExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = operatorModelMenuExpanded,
                onExpandedChange = { operatorModelMenuExpanded = !operatorModelMenuExpanded },
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_model)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = operatorModelMenuExpanded,
                        )
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = operatorModelMenuExpanded,
                    onDismissRequest = { operatorModelMenuExpanded = false },
                ) {
                    operatorAvailableModels.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                model = name
                                operatorModelMenuExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = maxStepsText,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } || value.isEmpty()) {
                        maxStepsText = value
                    }
                },
                label = { Text(stringResource(R.string.label_max_steps)) },
                placeholder = { Text(DEFAULT_MAX_STEPS.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = operatorMaxTokensText,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } || value.isEmpty()) {
                        operatorMaxTokensText = value
                    }
                },
                label = { Text(stringResource(R.string.label_max_tokens)) },
                placeholder = { Text(DEFAULT_OPERATOR_MAX_TOKENS.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = operatorTemperatureText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
                        operatorTemperatureText = value
                    }
                },
                label = { Text(stringResource(R.string.label_temperature)) },
                placeholder = { Text(DEFAULT_OPERATOR_TEMPERATURE.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = operatorTopPText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
                        operatorTopPText = value
                    }
                },
                label = { Text(stringResource(R.string.label_top_p)) },
                placeholder = { Text(DEFAULT_OPERATOR_TOP_P.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.label_reasoning_effort))
                androidx.compose.material3.Switch(
                    checked = operatorReasoningEnabledState,
                    onCheckedChange = { operatorReasoningEnabledState = it },
                )
            }
            if (operatorReasoningEnabledState) {
                ExposedDropdownMenuBox(
                    expanded = operatorReasoningMenuExpanded,
                    onExpandedChange = {
                        operatorReasoningMenuExpanded = !operatorReasoningMenuExpanded
                    },
                ) {
                    OutlinedTextField(
                        value = operatorReasoningEffortState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_effort)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = operatorReasoningMenuExpanded,
                            )
                        },
                        modifier =
                            Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = operatorReasoningMenuExpanded,
                        onDismissRequest = { operatorReasoningMenuExpanded = false },
                    ) {
                        listOf("minimal", "low", "medium", "high").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    operatorReasoningEffortState = option
                                    operatorReasoningMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            Text(text = stringResource(R.string.label_operator_prompt))
            ExposedDropdownMenuBox(
                expanded = promptMenuExpanded,
                onExpandedChange = { promptMenuExpanded = !promptMenuExpanded },
            ) {
                OutlinedTextField(
                    value = promptName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = promptMenuExpanded)
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = promptMenuExpanded,
                    onDismissRequest = { promptMenuExpanded = false },
                ) {
                    primaryPrompts.forEach { prompt ->
                        DropdownMenuItem(
                            text = { Text(prompt.name) },
                            onClick = {
                                promptName = prompt.name
                                promptMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_planner),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.label_enable_planner))
                androidx.compose.material3.Switch(
                    checked = plannerEnabledState,
                    onCheckedChange = { plannerEnabledState = it },
                )
            }
            ExposedDropdownMenuBox(
                expanded = plannerLlmMenuExpanded,
                onExpandedChange = { plannerLlmMenuExpanded = !plannerLlmMenuExpanded },
            ) {
                OutlinedTextField(
                    value = plannerLlmNameState,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_llm)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = plannerLlmMenuExpanded,
                        )
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = plannerLlmMenuExpanded,
                    onDismissRequest = { plannerLlmMenuExpanded = false },
                ) {
                    llmConfigNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                plannerLlmNameState = name
                                plannerLlmMenuExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = plannerModelMenuExpanded,
                onExpandedChange = { plannerModelMenuExpanded = !plannerModelMenuExpanded },
            ) {
                OutlinedTextField(
                    value = plannerTextModelState,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_model)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = plannerModelMenuExpanded,
                        )
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = plannerModelMenuExpanded,
                    onDismissRequest = { plannerModelMenuExpanded = false },
                ) {
                    plannerAvailableModels.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                plannerTextModelState = name
                                plannerModelMenuExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = plannerTextMaxTokensText,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } || value.isEmpty()) {
                        plannerTextMaxTokensText = value
                    }
                },
                label = { Text(stringResource(R.string.label_max_tokens)) },
                placeholder = { Text(DEFAULT_PLANNER_TEXT_MAX_TOKENS.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = plannerTextTemperatureText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
                        plannerTextTemperatureText = value
                    }
                },
                label = { Text(stringResource(R.string.label_temperature)) },
                placeholder = { Text(DEFAULT_PLANNER_TEXT_TEMPERATURE.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = plannerTextTopPText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
                        plannerTextTopPText = value
                    }
                },
                label = { Text(stringResource(R.string.label_top_p)) },
                placeholder = { Text(DEFAULT_PLANNER_TEXT_TOP_P.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.label_reasoning_effort))
                androidx.compose.material3.Switch(
                    checked = plannerTextReasoningEnabledState,
                    onCheckedChange = { plannerTextReasoningEnabledState = it },
                )
            }
            if (plannerTextReasoningEnabledState) {
                ExposedDropdownMenuBox(
                    expanded = plannerTextReasoningMenuExpanded,
                    onExpandedChange = {
                        plannerTextReasoningMenuExpanded = !plannerTextReasoningMenuExpanded
                    },
                ) {
                    OutlinedTextField(
                        value = plannerTextReasoningEffortState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_reasoning_effort)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = plannerTextReasoningMenuExpanded,
                            )
                        },
                        modifier =
                            Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = plannerTextReasoningMenuExpanded,
                        onDismissRequest = { plannerTextReasoningMenuExpanded = false },
                    ) {
                        listOf("minimal", "low", "medium", "high").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    plannerTextReasoningEffortState = option
                                    plannerTextReasoningMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            Text(text = stringResource(R.string.label_planner_prompt))
            ExposedDropdownMenuBox(
                expanded = plannerPromptMenuExpanded,
                onExpandedChange = { plannerPromptMenuExpanded = !plannerPromptMenuExpanded },
            ) {
                OutlinedTextField(
                    value = plannerPromptName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = plannerPromptMenuExpanded)
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = plannerPromptMenuExpanded,
                    onDismissRequest = { plannerPromptMenuExpanded = false },
                ) {
                    plannerPrompts.forEach { prompt ->
                        DropdownMenuItem(
                            text = { Text(prompt.name) },
                            onClick = {
                                plannerPromptName = prompt.name
                                plannerPromptMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_screen_check),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.label_enable_screen_check))
                androidx.compose.material3.Switch(
                    checked = screenCheckEnabledState,
                    onCheckedChange = { screenCheckEnabledState = it },
                )
            }
            ExposedDropdownMenuBox(
                expanded = screenCheckLlmMenuExpanded,
                onExpandedChange = { screenCheckLlmMenuExpanded = !screenCheckLlmMenuExpanded },
            ) {
                OutlinedTextField(
                    value = screenCheckLlmNameState,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_llm)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = screenCheckLlmMenuExpanded,
                        )
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = screenCheckLlmMenuExpanded,
                    onDismissRequest = { screenCheckLlmMenuExpanded = false },
                ) {
                    llmConfigNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                screenCheckLlmNameState = name
                                screenCheckLlmMenuExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = screenCheckModelMenuExpanded,
                onExpandedChange = {
                    screenCheckModelMenuExpanded = !screenCheckModelMenuExpanded
                },
            ) {
                OutlinedTextField(
                    value = screenCheckModelState,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_model)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = screenCheckModelMenuExpanded,
                        )
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = screenCheckModelMenuExpanded,
                    onDismissRequest = { screenCheckModelMenuExpanded = false },
                ) {
                    screenCheckAvailableModels.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                screenCheckModelState = name
                                screenCheckModelMenuExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = screenCheckMaxTokensText,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } || value.isEmpty()) {
                        screenCheckMaxTokensText = value
                    }
                },
                label = { Text(stringResource(R.string.label_max_tokens)) },
                placeholder = { Text(DEFAULT_SCREEN_CHECK_MAX_TOKENS.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = screenCheckTemperatureText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
                        screenCheckTemperatureText = value
                    }
                },
                label = { Text(stringResource(R.string.label_temperature)) },
                placeholder = { Text(DEFAULT_SCREEN_CHECK_TEMPERATURE.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = screenCheckTopPText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("\\d*\\.?\\d*"))) {
                        screenCheckTopPText = value
                    }
                },
                label = { Text(stringResource(R.string.label_top_p)) },
                placeholder = { Text(DEFAULT_SCREEN_CHECK_TOP_P.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.label_reasoning_effort))
                androidx.compose.material3.Switch(
                    checked = screenCheckReasoningEnabledState,
                    onCheckedChange = { screenCheckReasoningEnabledState = it },
                )
            }
            if (screenCheckReasoningEnabledState) {
                ExposedDropdownMenuBox(
                    expanded = screenCheckReasoningMenuExpanded,
                    onExpandedChange = {
                        screenCheckReasoningMenuExpanded = !screenCheckReasoningMenuExpanded
                    },
                ) {
                    OutlinedTextField(
                        value = screenCheckReasoningEffortState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_effort)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = screenCheckReasoningMenuExpanded,
                            )
                        },
                        modifier =
                            Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = screenCheckReasoningMenuExpanded,
                        onDismissRequest = { screenCheckReasoningMenuExpanded = false },
                    ) {
                        listOf("minimal", "low", "medium", "high").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    screenCheckReasoningEffortState = option
                                    screenCheckReasoningMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            Text(text = stringResource(R.string.label_screen_check_prompt))
            ExposedDropdownMenuBox(
                expanded = screenCheckPromptMenuExpanded,
                onExpandedChange = { screenCheckPromptMenuExpanded = !screenCheckPromptMenuExpanded },
            ) {
                OutlinedTextField(
                    value = screenCheckPromptName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = screenCheckPromptMenuExpanded,
                        )
                    },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = screenCheckPromptMenuExpanded,
                    onDismissRequest = { screenCheckPromptMenuExpanded = false },
                ) {
                    screenCheckPrompts.forEach { prompt ->
                        DropdownMenuItem(
                            text = { Text(prompt.name) },
                            onClick = {
                                screenCheckPromptName = prompt.name
                                screenCheckPromptMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_baidu_speech),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = baiduId,
                onValueChange = { baiduId = it },
                label = { Text(stringResource(R.string.label_baidu_app_id)) },
                placeholder = { Text(stringResource(R.string.placeholder_baidu_app_id)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = baiduKey,
                onValueChange = { baiduKey = it },
                label = { Text(stringResource(R.string.label_baidu_app_key)) },
                placeholder = { Text(stringResource(R.string.placeholder_baidu_app_key)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = baiduSecret,
                onValueChange = { baiduSecret = it },
                label = { Text(stringResource(R.string.label_baidu_secret_key)) },
                placeholder = { Text(stringResource(R.string.placeholder_baidu_secret_key)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_packages_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.configured_apps, appPackageCount),
                    style = MaterialTheme.typography.titleSmall,
                )
                Button(onClick = onOpenAppPackages) {
                    Text(stringResource(R.string.action_edit))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_capabilities),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = bubbleSizeText,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } || value.isEmpty()) {
                        bubbleSizeText = value
                    }
                },
                label = { Text(stringResource(R.string.label_bubble_size)) },
                placeholder = { Text(DEFAULT_BUBBLE_SIZE_DP.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            PermissionRow(
                label = stringResource(R.string.permission_floating_ball),
                enabled = overlayPermissionGranted,
                onEnable = onEnableOverlay,
                onDisable = onDisableOverlay,
            )
            PermissionRow(
                label = stringResource(R.string.permission_microphone),
                enabled = micPermissionGranted,
                onEnable = onRequestMicPermission,
                onDisable = onOpenAppSettings,
            )
            PermissionRow(
                label = stringResource(R.string.permission_notifications),
                enabled = notificationPermissionGranted,
                onEnable = onRequestNotificationPermission,
                onDisable = onOpenAppSettings,
            )
            PermissionRow(
                label = stringResource(R.string.permission_wakeup),
                enabled = wakeupEnabled,
                onEnable = { wakeupEnabled = true },
                onDisable = { wakeupEnabled = false },
            )
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    enabled: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    val buttonText =
        if (enabled) {
            stringResource(R.string.action_disable)
        } else {
            stringResource(R.string.action_enable)
        }
    val buttonColor = if (enabled) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Button(
            onClick = if (enabled) onDisable else onEnable,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        ) {
            Text(buttonText)
        }
    }
}

@Composable
fun NewPromptDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, PromptKind) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(PromptKind.PRIMARY) }

    FullScreenPromptEditor(
        title = stringResource(R.string.new_system_prompt_title),
        name = name,
        content = content,
        nameEnabled = true,
        fixedContent = SYSTEM_PROMPT_FIXED,
        kind = kind,
        kindEnabled = true,
        onNameChange = { name = it },
        onContentChange = { content = it },
        onKindChange = { kind = it },
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isNotBlank()) {
                onCreate(name.trim(), content.trim(), kind)
            }
        },
        confirmLabel = stringResource(R.string.action_add),
    )
}

@Composable
fun EditPromptDialog(
    prompt: PromptConfig,
    onDismiss: () -> Unit,
    onSave: (String, PromptConfig) -> Unit,
) {
    val originalName = prompt.name
    val isDefault =
        (prompt.kind == PromptKind.PRIMARY && prompt.name == DEFAULT_PROMPT_NAME) ||
            (prompt.kind == PromptKind.PLANNER && prompt.name == DEFAULT_PLANNER_PROMPT_NAME)
    var name by remember { mutableStateOf(prompt.name) }
    var content by remember { mutableStateOf(prompt.content) }
    var kind by remember { mutableStateOf(prompt.kind) }

    FullScreenPromptEditor(
        title = stringResource(R.string.edit_system_prompt_title),
        name = name,
        content = content,
        nameEnabled = true,
        fixedContent = SYSTEM_PROMPT_FIXED,
        kind = kind,
        kindEnabled = !isDefault,
        onNameChange = { name = it },
        onContentChange = { content = it },
        onKindChange = { if (!isDefault) kind = it },
        helperText = null,
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isNotBlank()) {
                onSave(
                    originalName,
                    PromptConfig(
                        name = name.trim(),
                        content = content.trim(),
                        kind = kind,
                    ),
                )
            }
        },
        confirmLabel = stringResource(R.string.action_save),
    )
}

@Composable
fun FullScreenPromptEditor(
    title: String,
    name: String,
    content: String,
    nameEnabled: Boolean,
    fixedContent: String,
    kind: PromptKind,
    kindEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onKindChange: (PromptKind) -> Unit,
    helperText: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
) {
    val context = LocalContext.current
    var kindMenuExpanded by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                    actions = {
                        TextButton(onClick = onConfirm) {
                            Text(confirmLabel)
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.prompt_name)) },
                    enabled = nameEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = kindMenuExpanded,
                    onExpandedChange = {
                        if (kindEnabled) {
                            kindMenuExpanded = !kindMenuExpanded
                        }
                    },
                ) {
                    OutlinedTextField(
                        value = promptKindLabel(context, kind),
                        onValueChange = {},
                        readOnly = true,
                        enabled = kindEnabled,
                        label = { Text(stringResource(R.string.prompt_type)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindMenuExpanded)
                        },
                        modifier =
                            Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = kindMenuExpanded,
                        onDismissRequest = { kindMenuExpanded = false },
                    ) {
                        listOf(
                            PromptKind.PRIMARY,
                            PromptKind.PLANNER,
                            PromptKind.SCREEN_CHECK,
                            PromptKind.SUBTASK,
                        ).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(promptKindLabel(context, option)) },
                                onClick = {
                                    onKindChange(option)
                                    kindMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                if (helperText != null) {
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (kind == PromptKind.PRIMARY) {
                    if (fixedContent.isNotBlank()) {
                        OutlinedTextField(
                            value = fixedContent,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.system_prompt_fixed)) },
                            readOnly = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                        )
                    }
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        label = { Text(stringResource(R.string.system_prompt)) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    )
                } else {
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        label = { Text(stringResource(R.string.system_prompt)) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun ChatDialog(
    messages: List<ChatMessage>,
    input: String,
    running: Boolean,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    val context = LocalContext.current
    val chatTitle = context.getString(R.string.chat_title)
    val chatClose = context.getString(R.string.menu_close)
    val chatRunningText = context.getString(R.string.chat_running)
    val chatPlaceholder = context.getString(R.string.chat_placeholder)
    val chatSend = context.getString(R.string.chat_send)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(chatTitle) },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text(chatClose)
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LazyColumn(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages) { message ->
                        val isUser = message.role == "user"
                        val bubbleColor =
                            if (isUser) {
                                Color(0xFF95EC69)
                            } else {
                                Color(0xFFF2F3F5)
                            }
                        val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = align,
                        ) {
                            SelectionContainer {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(0.7f)
                                            .background(bubbleColor, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF111111),
                                    )
                                }
                            }
                        }
                    }
                }
                if (running) {
                    Text(
                        text = chatRunningText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(16.dp),
                            )
                            .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val inputHeight = 52.dp
                    TextField(
                        value = input,
                        onValueChange = onInputChange,
                        placeholder = { Text(chatPlaceholder) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions =
                            KeyboardActions(
                                onSend = {
                                    if (!running) {
                                        onSend(input)
                                    }
                                },
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(inputHeight),
                    )
                    Button(
                        onClick = { onSend(input) },
                        enabled = !running,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(inputHeight),
                    ) {
                        Text(chatSend)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.confirm_action_title)) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val aboutTitle = context.getString(R.string.about_title)
    val aboutAuthor = context.getString(R.string.about_author)
    val aboutGitHub = context.getString(R.string.about_github)
    val versionName =
        remember {
            val packageName = context.packageName
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        }
    val aboutVersion = context.getString(R.string.about_version, versionName)
    val aboutDesc = context.getString(R.string.about_desc)
    val aboutClose = context.getString(R.string.menu_close)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = aboutTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = aboutAuthor)
                Text(text = aboutGitHub)
                Text(text = aboutVersion)
                Text(text = aboutDesc)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(aboutClose)
            }
        },
    )
}

@Composable
fun PromptActionsMenu(
    expanded: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_edit)) },
            onClick = onEdit,
        )
        if (canDelete) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                onClick = onDelete,
            )
        }
    }
}

@Composable
fun LlmActionsMenu(
    expanded: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_edit)) },
            onClick = onEdit,
        )
        if (canDelete) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                onClick = onDelete,
            )
        }
    }
}

// Requires API 23+ (overlay permission APIs).
@RequiresApi(Build.VERSION_CODES.M)
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MLLMTheme {
        MainScreen()
    }
}
