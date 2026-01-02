package com.example.mllm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

enum class PromptKind(val id: String) {
    PRIMARY("primary"),
    PLANNER("planner"),
    SUBTASK("subtask"),
    SCREEN_CHECK("screen_check"),
    ;

    companion object {
        fun fromId(id: String?): PromptKind {
            return when (id?.lowercase()) {
                "planner" -> PLANNER
                "subtask" -> SUBTASK
                "screen_check" -> SCREEN_CHECK
                "screen_check_input" -> SCREEN_CHECK
                else -> PRIMARY
            }
        }
    }
}

data class PromptEntry(
    val name: String,
    val content: String,
    val tags: String = "",
    val kind: PromptKind = PromptKind.PRIMARY,
)

data class LlmConfig(
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelNames: List<String>,
)

data class AppSettings(
    val llmConfigs: List<LlmConfig>,
    val selectedOperatorLlmName: String,
    val selectedPlannerLlmName: String,
    val selectedScreenCheckLlmName: String,
    val llmModel: String,
    val languageCode: String,
    val maxSteps: Int,
    val operatorMaxTokens: Int,
    val operatorTemperature: Double,
    val operatorTopP: Double,
    val operatorReasoningEnabled: Boolean,
    val operatorReasoningEffort: String,
    val plannerTextModel: String,
    val plannerTextMaxTokens: Int,
    val plannerTextTemperature: Double,
    val plannerTextTopP: Double,
    val plannerTextReasoningEnabled: Boolean,
    val plannerTextReasoningEffort: String,
    val plannerEnabled: Boolean,
    val screenCheckEnabled: Boolean,
    val screenCheckModel: String,
    val screenCheckMaxTokens: Int,
    val screenCheckTemperature: Double,
    val screenCheckTopP: Double,
    val screenCheckReasoningEnabled: Boolean,
    val screenCheckReasoningEffort: String,
    val selectedPrompt: String,
    val selectedPlannerPrompt: String,
    val selectedScreenCheckPrompt: String,
    val prompts: List<PromptEntry>,
    val appPackagesText: String,
    val bubbleSizeDp: Int,
    val baiduAppId: String,
    val baiduAppKey: String,
    val baiduSecretKey: String,
    val baiduWakeupEnabled: Boolean,
)

private val Context.dataStore by preferencesDataStore(name = "mllm_settings")

const val DEFAULT_LLM_CONFIG_NAME = "Operator LLM"

object SettingsRepository {
    private val KEY_LLM_URL = stringPreferencesKey("llm_url")
    private val KEY_LLM_MODEL = stringPreferencesKey("llm_model")
    private val KEY_LLM_API_KEY = stringPreferencesKey("llm_api_key")
    private val KEY_MODEL_MANAGER_URL = stringPreferencesKey("model_manager_url")
    private val KEY_MODEL_MANAGER_API_KEY = stringPreferencesKey("model_manager_api_key")
    private val KEY_MODEL_NAMES = stringPreferencesKey("model_manager_names")
    private val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
    private val KEY_LLM_CONFIGS_JSON = stringPreferencesKey("llm_configs_json")
    private val KEY_SELECTED_OPERATOR_LLM = stringPreferencesKey("selected_operator_llm")
    private val KEY_SELECTED_PLANNER_LLM = stringPreferencesKey("selected_planner_llm")
    private val KEY_SELECTED_SCREEN_CHECK_LLM = stringPreferencesKey("selected_screen_check_llm")
    private val KEY_MAX_STEPS = intPreferencesKey("max_steps")
    private val KEY_OPERATOR_MAX_TOKENS = intPreferencesKey("operator_max_tokens")
    private val KEY_OPERATOR_TEMPERATURE = stringPreferencesKey("operator_temperature")
    private val KEY_OPERATOR_TOP_P = stringPreferencesKey("operator_top_p")
    private val KEY_OPERATOR_REASONING_ENABLED = intPreferencesKey("operator_reasoning_enabled")
    private val KEY_OPERATOR_REASONING_EFFORT = stringPreferencesKey("operator_reasoning_effort")
    private val KEY_PLANNER_IMAGE_MODEL = stringPreferencesKey("planner_image_model")
    private val KEY_SCREEN_CHECK_MODEL = stringPreferencesKey("screen_check_model")
    private val KEY_PLANNER_TEXT_MODEL = stringPreferencesKey("planner_text_model")
    private val KEY_PLANNER_TEXT_MAX_TOKENS = intPreferencesKey("planner_text_max_tokens")
    private val KEY_PLANNER_TEXT_TEMPERATURE = stringPreferencesKey("planner_text_temperature")
    private val KEY_PLANNER_TEXT_TOP_P = stringPreferencesKey("planner_text_top_p")
    private val KEY_PLANNER_TEXT_REASONING_ENABLED =
        intPreferencesKey("planner_text_reasoning_enabled")
    private val KEY_PLANNER_TEXT_REASONING_EFFORT =
        stringPreferencesKey("planner_text_reasoning_effort")
    private val KEY_PLANNER_ENABLED = intPreferencesKey("planner_enabled")
    private val KEY_PLANNER_IMAGE_MAX_TOKENS = intPreferencesKey("planner_image_max_tokens")
    private val KEY_PLANNER_IMAGE_TEMPERATURE = stringPreferencesKey("planner_image_temperature")
    private val KEY_PLANNER_IMAGE_TOP_P = stringPreferencesKey("planner_image_top_p")
    private val KEY_PLANNER_IMAGE_REASONING_EFFORT =
        stringPreferencesKey("planner_image_reasoning_effort")
    private val KEY_SCREEN_CHECK_ENABLED = intPreferencesKey("screen_check_enabled")
    private val KEY_SCREEN_CHECK_MAX_TOKENS = intPreferencesKey("screen_check_max_tokens")
    private val KEY_SCREEN_CHECK_TEMPERATURE = stringPreferencesKey("screen_check_temperature")
    private val KEY_SCREEN_CHECK_TOP_P = stringPreferencesKey("screen_check_top_p")
    private val KEY_SCREEN_CHECK_REASONING_ENABLED =
        intPreferencesKey("screen_check_reasoning_enabled")
    private val KEY_SCREEN_CHECK_REASONING_EFFORT =
        stringPreferencesKey("screen_check_reasoning_effort")
    private val KEY_SELECTED_PROMPT = stringPreferencesKey("selected_prompt")
    private val KEY_SELECTED_PLANNER_PROMPT = stringPreferencesKey("selected_planner_prompt")
    private val KEY_SELECTED_SCREEN_CHECK_PROMPT = stringPreferencesKey("selected_screen_check_prompt")
    private val KEY_PROMPTS_JSON = stringPreferencesKey("prompts_json")
    private val KEY_APP_PACKAGES_TEXT = stringPreferencesKey("app_packages_text")
    private val KEY_BUBBLE_SIZE_DP = intPreferencesKey("bubble_size_dp")
    private val KEY_BAIDU_APP_ID = stringPreferencesKey("baidu_app_id")
    private val KEY_BAIDU_APP_KEY = stringPreferencesKey("baidu_app_key")
    private val KEY_BAIDU_SECRET_KEY = stringPreferencesKey("baidu_secret_key")
    private val KEY_BAIDU_WAKEUP_ENABLED = intPreferencesKey("baidu_wakeup_enabled")

    fun settingsFlow(
        context: Context,
        defaultSettings: AppSettings,
    ): Flow<AppSettings> {
        return context.dataStore.data.map { prefs ->
            val prompts = parsePrompts(prefs[KEY_PROMPTS_JSON]) ?: defaultSettings.prompts
            val legacyModelNames = parseModelNames(prefs[KEY_MODEL_NAMES])
            val legacyBaseUrl =
                prefs[KEY_MODEL_MANAGER_URL]
                    ?: prefs[KEY_LLM_URL]
                    ?: defaultSettings.llmConfigs.firstOrNull()?.baseUrl.orEmpty()
            val legacyApiKey =
                prefs[KEY_MODEL_MANAGER_API_KEY]
                    ?: prefs[KEY_LLM_API_KEY]
                    ?: defaultSettings.llmConfigs.firstOrNull()?.apiKey.orEmpty()
            val legacyNames =
                legacyModelNames
                    ?: defaultSettings.llmConfigs.firstOrNull()?.modelNames
                    ?: listOf(defaultSettings.llmModel)
            val defaultConfig =
                LlmConfig(
                    name = DEFAULT_LLM_CONFIG_NAME,
                    baseUrl = legacyBaseUrl,
                    apiKey = legacyApiKey,
                    modelNames = legacyNames,
                )
            val parsedConfigs = parseLlmConfigs(prefs[KEY_LLM_CONFIGS_JSON])
            val llmConfigs =
                if (parsedConfigs.isNullOrEmpty()) {
                    listOf(defaultConfig)
                } else if (parsedConfigs.any { it.name == DEFAULT_LLM_CONFIG_NAME }) {
                    parsedConfigs
                } else {
                    listOf(defaultConfig) + parsedConfigs
                }
            AppSettings(
                llmConfigs = llmConfigs,
                selectedOperatorLlmName =
                    prefs[KEY_SELECTED_OPERATOR_LLM]
                        ?: defaultSettings.selectedOperatorLlmName,
                selectedPlannerLlmName =
                    prefs[KEY_SELECTED_PLANNER_LLM]
                        ?: defaultSettings.selectedPlannerLlmName,
                selectedScreenCheckLlmName =
                    prefs[KEY_SELECTED_SCREEN_CHECK_LLM]
                        ?: defaultSettings.selectedScreenCheckLlmName,
                llmModel = prefs[KEY_LLM_MODEL] ?: defaultSettings.llmModel,
                languageCode = prefs[KEY_LANGUAGE_CODE] ?: defaultSettings.languageCode,
                maxSteps = prefs[KEY_MAX_STEPS] ?: defaultSettings.maxSteps,
                operatorMaxTokens =
                    prefs[KEY_OPERATOR_MAX_TOKENS]
                        ?: defaultSettings.operatorMaxTokens,
                operatorTemperature =
                    prefs[KEY_OPERATOR_TEMPERATURE]
                        ?.toDoubleOrNull()
                        ?: defaultSettings.operatorTemperature,
                operatorTopP =
                    prefs[KEY_OPERATOR_TOP_P]
                        ?.toDoubleOrNull()
                        ?: defaultSettings.operatorTopP,
                operatorReasoningEnabled =
                    (
                        prefs[KEY_OPERATOR_REASONING_ENABLED]
                            ?: if (defaultSettings.operatorReasoningEnabled) 1 else 0
                    ) == 1,
                operatorReasoningEffort =
                    prefs[KEY_OPERATOR_REASONING_EFFORT]
                        ?: defaultSettings.operatorReasoningEffort,
                plannerTextModel = prefs[KEY_PLANNER_TEXT_MODEL] ?: defaultSettings.plannerTextModel,
                plannerTextMaxTokens =
                    prefs[KEY_PLANNER_TEXT_MAX_TOKENS]
                        ?: defaultSettings.plannerTextMaxTokens,
                plannerTextTemperature =
                    prefs[KEY_PLANNER_TEXT_TEMPERATURE]
                        ?.toDoubleOrNull()
                        ?: defaultSettings.plannerTextTemperature,
                plannerTextTopP =
                    prefs[KEY_PLANNER_TEXT_TOP_P]
                        ?.toDoubleOrNull()
                        ?: defaultSettings.plannerTextTopP,
                plannerTextReasoningEnabled =
                    (
                        prefs[KEY_PLANNER_TEXT_REASONING_ENABLED]
                            ?: if (defaultSettings.plannerTextReasoningEnabled) 1 else 0
                    ) == 1,
                plannerTextReasoningEffort =
                    prefs[KEY_PLANNER_TEXT_REASONING_EFFORT]
                        ?: defaultSettings.plannerTextReasoningEffort,
                plannerEnabled =
                    (
                        prefs[KEY_PLANNER_ENABLED]
                            ?: if (defaultSettings.plannerEnabled) 1 else 0
                    ) == 1,
                screenCheckEnabled =
                    (
                        prefs[KEY_SCREEN_CHECK_ENABLED]
                            ?: if (defaultSettings.screenCheckEnabled) 1 else 0
                    ) == 1,
                screenCheckModel =
                    prefs[KEY_SCREEN_CHECK_MODEL]
                        ?: prefs[KEY_PLANNER_IMAGE_MODEL]
                        ?: defaultSettings.screenCheckModel,
                screenCheckMaxTokens =
                    prefs[KEY_SCREEN_CHECK_MAX_TOKENS]
                        ?: prefs[KEY_PLANNER_IMAGE_MAX_TOKENS]
                        ?: defaultSettings.screenCheckMaxTokens,
                screenCheckTemperature =
                    prefs[KEY_SCREEN_CHECK_TEMPERATURE]
                        ?.toDoubleOrNull()
                        ?: prefs[KEY_PLANNER_IMAGE_TEMPERATURE]?.toDoubleOrNull()
                        ?: defaultSettings.screenCheckTemperature,
                screenCheckTopP =
                    prefs[KEY_SCREEN_CHECK_TOP_P]
                        ?.toDoubleOrNull()
                        ?: prefs[KEY_PLANNER_IMAGE_TOP_P]?.toDoubleOrNull()
                        ?: defaultSettings.screenCheckTopP,
                screenCheckReasoningEnabled =
                    (
                        prefs[KEY_SCREEN_CHECK_REASONING_ENABLED]
                            ?: if (defaultSettings.screenCheckReasoningEnabled) 1 else 0
                    ) == 1,
                screenCheckReasoningEffort =
                    prefs[KEY_SCREEN_CHECK_REASONING_EFFORT]
                        ?: prefs[KEY_PLANNER_IMAGE_REASONING_EFFORT]
                        ?: defaultSettings.screenCheckReasoningEffort,
                selectedPrompt = prefs[KEY_SELECTED_PROMPT] ?: defaultSettings.selectedPrompt,
                selectedPlannerPrompt =
                    prefs[KEY_SELECTED_PLANNER_PROMPT]
                        ?: defaultSettings.selectedPlannerPrompt,
                selectedScreenCheckPrompt =
                    prefs[KEY_SELECTED_SCREEN_CHECK_PROMPT]
                        ?: defaultSettings.selectedScreenCheckPrompt,
                prompts = prompts,
                appPackagesText =
                    prefs[KEY_APP_PACKAGES_TEXT]
                        ?: defaultSettings.appPackagesText,
                bubbleSizeDp =
                    prefs[KEY_BUBBLE_SIZE_DP]
                        ?: defaultSettings.bubbleSizeDp,
                baiduAppId = prefs[KEY_BAIDU_APP_ID] ?: defaultSettings.baiduAppId,
                baiduAppKey = prefs[KEY_BAIDU_APP_KEY] ?: defaultSettings.baiduAppKey,
                baiduSecretKey = prefs[KEY_BAIDU_SECRET_KEY] ?: defaultSettings.baiduSecretKey,
                baiduWakeupEnabled =
                    (
                        prefs[KEY_BAIDU_WAKEUP_ENABLED]
                            ?: if (defaultSettings.baiduWakeupEnabled) 1 else 0
                    ) == 1,
            )
        }
    }

    suspend fun saveSettings(
        context: Context,
        settings: AppSettings,
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LLM_CONFIGS_JSON] = encodeLlmConfigs(settings.llmConfigs)
            prefs[KEY_SELECTED_OPERATOR_LLM] = settings.selectedOperatorLlmName
            prefs[KEY_SELECTED_PLANNER_LLM] = settings.selectedPlannerLlmName
            prefs[KEY_SELECTED_SCREEN_CHECK_LLM] = settings.selectedScreenCheckLlmName
            prefs[KEY_LLM_MODEL] = settings.llmModel
            prefs[KEY_LANGUAGE_CODE] = settings.languageCode
            prefs[KEY_MAX_STEPS] = settings.maxSteps
            prefs[KEY_OPERATOR_MAX_TOKENS] = settings.operatorMaxTokens
            prefs[KEY_OPERATOR_TEMPERATURE] = settings.operatorTemperature.toString()
            prefs[KEY_OPERATOR_TOP_P] = settings.operatorTopP.toString()
            prefs[KEY_OPERATOR_REASONING_ENABLED] = if (settings.operatorReasoningEnabled) 1 else 0
            prefs[KEY_OPERATOR_REASONING_EFFORT] = settings.operatorReasoningEffort
            prefs[KEY_PLANNER_TEXT_MODEL] = settings.plannerTextModel
            prefs[KEY_PLANNER_TEXT_MAX_TOKENS] = settings.plannerTextMaxTokens
            prefs[KEY_PLANNER_TEXT_TEMPERATURE] = settings.plannerTextTemperature.toString()
            prefs[KEY_PLANNER_TEXT_TOP_P] = settings.plannerTextTopP.toString()
            prefs[KEY_PLANNER_TEXT_REASONING_ENABLED] =
                if (settings.plannerTextReasoningEnabled) 1 else 0
            prefs[KEY_PLANNER_TEXT_REASONING_EFFORT] = settings.plannerTextReasoningEffort
            prefs[KEY_PLANNER_ENABLED] = if (settings.plannerEnabled) 1 else 0
            prefs[KEY_SCREEN_CHECK_ENABLED] = if (settings.screenCheckEnabled) 1 else 0
            prefs[KEY_SCREEN_CHECK_MODEL] = settings.screenCheckModel
            prefs[KEY_SCREEN_CHECK_MAX_TOKENS] = settings.screenCheckMaxTokens
            prefs[KEY_SCREEN_CHECK_TEMPERATURE] = settings.screenCheckTemperature.toString()
            prefs[KEY_SCREEN_CHECK_TOP_P] = settings.screenCheckTopP.toString()
            prefs[KEY_SCREEN_CHECK_REASONING_ENABLED] =
                if (settings.screenCheckReasoningEnabled) 1 else 0
            prefs[KEY_SCREEN_CHECK_REASONING_EFFORT] = settings.screenCheckReasoningEffort
            prefs[KEY_SELECTED_PROMPT] = settings.selectedPrompt
            prefs[KEY_SELECTED_PLANNER_PROMPT] = settings.selectedPlannerPrompt
            prefs[KEY_SELECTED_SCREEN_CHECK_PROMPT] = settings.selectedScreenCheckPrompt
            prefs[KEY_PROMPTS_JSON] = encodePrompts(settings.prompts)
            prefs[KEY_APP_PACKAGES_TEXT] = settings.appPackagesText
            prefs[KEY_BUBBLE_SIZE_DP] = settings.bubbleSizeDp
            prefs[KEY_BAIDU_APP_ID] = settings.baiduAppId
            prefs[KEY_BAIDU_APP_KEY] = settings.baiduAppKey
            prefs[KEY_BAIDU_SECRET_KEY] = settings.baiduSecretKey
            prefs[KEY_BAIDU_WAKEUP_ENABLED] = if (settings.baiduWakeupEnabled) 1 else 0
        }
    }

    suspend fun savePrompts(
        context: Context,
        prompts: List<PromptEntry>,
        selectedPrompt: String,
        selectedPlannerPrompt: String,
        selectedScreenCheckPrompt: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROMPTS_JSON] = encodePrompts(prompts)
            prefs[KEY_SELECTED_PROMPT] = selectedPrompt
            prefs[KEY_SELECTED_PLANNER_PROMPT] = selectedPlannerPrompt
            prefs[KEY_SELECTED_SCREEN_CHECK_PROMPT] = selectedScreenCheckPrompt
        }
    }

    private fun encodePrompts(prompts: List<PromptEntry>): String {
        val array = JSONArray()
        prompts.forEach { prompt ->
            val obj = JSONObject()
            obj.put("name", prompt.name)
            obj.put("content", prompt.content)
            obj.put("tags", prompt.tags)
            obj.put("kind", prompt.kind.id)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parsePrompts(json: String?): List<PromptEntry>? {
        if (json.isNullOrBlank()) return null
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<PromptEntry>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name", "")
                val content = obj.optString("content", "")
                val tags = obj.optString("tags", "")
                val kind = PromptKind.fromId(obj.optString("kind", "primary"))
                if (name.isNotBlank()) {
                    list.add(PromptEntry(name, content, tags, kind))
                }
            }
            if (list.isEmpty()) null else list
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeLlmConfigs(configs: List<LlmConfig>): String {
        val array = JSONArray()
        configs.forEach { config ->
            if (config.name.isBlank()) return@forEach
            val obj = JSONObject()
            obj.put("name", config.name)
            obj.put("baseUrl", config.baseUrl)
            obj.put("apiKey", config.apiKey)
            obj.put("modelNames", encodeModelNames(config.modelNames))
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseLlmConfigs(json: String?): List<LlmConfig>? {
        if (json.isNullOrBlank()) return null
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<LlmConfig>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                if (name.isBlank()) continue
                val baseUrl = obj.optString("baseUrl", "").trim()
                val apiKey = obj.optString("apiKey", "").trim()
                val modelNames =
                    parseModelNames(obj.optString("modelNames", ""))
                        ?: emptyList()
                list.add(
                    LlmConfig(
                        name = name,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        modelNames = modelNames,
                    ),
                )
            }
            if (list.isEmpty()) null else list
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeModelNames(modelNames: List<String>): String {
        val array = JSONArray()
        modelNames.forEach { name ->
            if (name.isNotBlank()) {
                array.put(name)
            }
        }
        return array.toString()
    }

    private fun parseModelNames(json: String?): List<String>? {
        if (json.isNullOrBlank()) return null
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val name = array.optString(i, "").trim()
                if (name.isNotBlank()) {
                    list.add(name)
                }
            }
            if (list.isEmpty()) null else list
        } catch (_: Exception) {
            null
        }
    }
}
