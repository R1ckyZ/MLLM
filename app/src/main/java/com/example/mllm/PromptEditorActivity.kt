package com.example.mllm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mllm.data.PromptKind
import com.example.mllm.ui.theme.MLLMTheme

class PromptEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val rawTitle = intent.getStringExtra(EXTRA_TITLE)
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
        val tags = intent.getStringExtra(EXTRA_TAGS).orEmpty()
        val kindId = intent.getStringExtra(EXTRA_KIND).orEmpty()
        val nameEnabled = intent.getBooleanExtra(EXTRA_NAME_ENABLED, true)
        val kindEnabled = intent.getBooleanExtra(EXTRA_KIND_ENABLED, true)
        val fixedContent = intent.getStringExtra(EXTRA_FIXED_CONTENT).orEmpty()
        val helperText = intent.getStringExtra(EXTRA_HELPER_TEXT)
        val rawConfirmLabel = intent.getStringExtra(EXTRA_CONFIRM_LABEL)
        val originalName = intent.getStringExtra(EXTRA_ORIGINAL_NAME)
        val languageCode = intent.getStringExtra(EXTRA_LANGUAGE_CODE).orEmpty()
        val operatorTemplateEnabled =
            intent.getBooleanExtra(EXTRA_OPERATOR_TEMPLATE_ENABLED, false)
        val operatorTemplateCn = intent.getStringExtra(EXTRA_OPERATOR_TEMPLATE_CN).orEmpty()
        val operatorTemplateEn = intent.getStringExtra(EXTRA_OPERATOR_TEMPLATE_EN).orEmpty()
        val localizedContext = createLocalizedContext(this, languageCode)
        val title = rawTitle ?: localizedContext.getString(R.string.edit_system_prompt_title)
        val confirmLabel = rawConfirmLabel ?: localizedContext.getString(R.string.action_save)
        setContent {
            CompositionLocalProvider(LocalContext provides localizedContext) {
                MLLMTheme {
                    PromptEditorScreen(
                        title = title,
                        name = name,
                        content = content,
                        tags = tags,
                        kind = PromptKind.fromId(kindId),
                        nameEnabled = nameEnabled,
                        kindEnabled = kindEnabled,
                        fixedContent = fixedContent,
                        helperText = helperText,
                        confirmLabel = confirmLabel,
                        operatorTemplateEnabled = operatorTemplateEnabled,
                        operatorTemplateCn = operatorTemplateCn,
                        operatorTemplateEn = operatorTemplateEn,
                        onBack = { finish() },
                        onConfirm = { updatedName, updatedContent, updatedTags, updatedKind ->
                            val data =
                                Intent().apply {
                                    putExtra(EXTRA_NAME, updatedName)
                                    putExtra(EXTRA_CONTENT, updatedContent)
                                    putExtra(EXTRA_TAGS, updatedTags)
                                    putExtra(EXTRA_KIND, updatedKind.id)
                                    putExtra(EXTRA_ORIGINAL_NAME, originalName)
                                }
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "prompt_editor_title"
        const val EXTRA_NAME = "prompt_editor_name"
        const val EXTRA_CONTENT = "prompt_editor_content"
        const val EXTRA_TAGS = "prompt_editor_tags"
        const val EXTRA_KIND = "prompt_editor_kind"
        const val EXTRA_NAME_ENABLED = "prompt_editor_name_enabled"
        const val EXTRA_KIND_ENABLED = "prompt_editor_kind_enabled"
        const val EXTRA_FIXED_CONTENT = "prompt_editor_fixed_content"
        const val EXTRA_HELPER_TEXT = "prompt_editor_helper_text"
        const val EXTRA_CONFIRM_LABEL = "prompt_editor_confirm_label"
        const val EXTRA_ORIGINAL_NAME = "prompt_editor_original_name"
        const val EXTRA_LANGUAGE_CODE = "prompt_editor_language_code"
        const val EXTRA_OPERATOR_TEMPLATE_ENABLED = "prompt_editor_operator_template_enabled"
        const val EXTRA_OPERATOR_TEMPLATE_CN = "prompt_editor_operator_template_cn"
        const val EXTRA_OPERATOR_TEMPLATE_EN = "prompt_editor_operator_template_en"

        fun createIntent(
            context: Context,
            title: String,
            name: String,
            content: String,
            tags: String,
            kindId: String,
            nameEnabled: Boolean,
            kindEnabled: Boolean,
            fixedContent: String,
            helperText: String?,
            confirmLabel: String,
            originalName: String?,
            languageCode: String,
            operatorTemplateEnabled: Boolean,
            operatorTemplateCn: String,
            operatorTemplateEn: String,
        ): Intent {
            return Intent(context, PromptEditorActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_CONTENT, content)
                putExtra(EXTRA_TAGS, tags)
                putExtra(EXTRA_KIND, kindId)
                putExtra(EXTRA_NAME_ENABLED, nameEnabled)
                putExtra(EXTRA_KIND_ENABLED, kindEnabled)
                putExtra(EXTRA_FIXED_CONTENT, fixedContent)
                putExtra(EXTRA_HELPER_TEXT, helperText)
                putExtra(EXTRA_CONFIRM_LABEL, confirmLabel)
                putExtra(EXTRA_ORIGINAL_NAME, originalName)
                putExtra(EXTRA_LANGUAGE_CODE, languageCode)
                putExtra(EXTRA_OPERATOR_TEMPLATE_ENABLED, operatorTemplateEnabled)
                putExtra(EXTRA_OPERATOR_TEMPLATE_CN, operatorTemplateCn)
                putExtra(EXTRA_OPERATOR_TEMPLATE_EN, operatorTemplateEn)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PromptEditorScreen(
    title: String,
    name: String,
    content: String,
    tags: String,
    kind: PromptKind,
    nameEnabled: Boolean,
    kindEnabled: Boolean,
    fixedContent: String,
    helperText: String?,
    confirmLabel: String,
    operatorTemplateEnabled: Boolean,
    operatorTemplateCn: String,
    operatorTemplateEn: String,
    onBack: () -> Unit,
    onConfirm: (String, String, String, PromptKind) -> Unit,
) {
    val context = LocalContext.current
    var kindMenuExpanded by remember { mutableStateOf(false) }
    var nameState by remember(name) { mutableStateOf(name) }
    var contentState by remember(content) { mutableStateOf(content) }
    var contentDirty by remember { mutableStateOf(false) }
    var tagsState by remember(tags) { mutableStateOf(tags) }
    var kindState by remember(kind) { mutableStateOf(kind) }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf(LANGUAGE_CODE_ZH) }
    val templateOptions =
        remember {
            listOf(
                LANGUAGE_CODE_ZH to R.string.language_chinese,
                LANGUAGE_CODE_EN to R.string.language_english,
            )
        }
    val selectedTemplateLabel =
        stringResource(
            templateOptions.firstOrNull { it.first == selectedTemplate }?.second
                ?: R.string.language_chinese,
        )
    val templateText =
        if (selectedTemplate == LANGUAGE_CODE_EN) {
            operatorTemplateEn
        } else {
            operatorTemplateCn
        }
    LaunchedEffect(selectedTemplate) {
        if (!contentDirty && templateText.isNotBlank()) {
            contentState = templateText
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val resolvedTags =
                                if (kindState == PromptKind.SUBTASK) {
                                    tagsState.trim()
                                } else {
                                    ""
                                }
                            onConfirm(
                                nameState.trim(),
                                contentState.trim(),
                                resolvedTags,
                                kindState,
                            )
                        },
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
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
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = nameState,
                onValueChange = { nameState = it },
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
                    value = promptKindLabel(context, kindState),
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
                                kindState = option
                                kindMenuExpanded = false
                            },
                        )
                    }
                }
            }
            if (!helperText.isNullOrBlank()) {
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (operatorTemplateEnabled && kindState == PromptKind.PRIMARY) {
                ExposedDropdownMenuBox(
                    expanded = templateMenuExpanded,
                    onExpandedChange = { templateMenuExpanded = !templateMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedTemplateLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_operator_template)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = templateMenuExpanded,
                            )
                        },
                        modifier =
                            Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = templateMenuExpanded,
                        onDismissRequest = { templateMenuExpanded = false },
                    ) {
                        templateOptions.forEach { (code, labelRes) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelRes)) },
                                onClick = {
                                    selectedTemplate = code
                                    templateMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            if (kindState == PromptKind.SUBTASK) {
                OutlinedTextField(
                    value = tagsState,
                    onValueChange = { tagsState = it },
                    label = { Text(stringResource(R.string.tags_comma)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (kindState == PromptKind.PRIMARY) {
                if (fixedContent.isNotBlank()) {
                    OutlinedTextField(
                        value = fixedContent,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.system_prompt_fixed)) },
                        readOnly = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                    )
                }
                OutlinedTextField(
                    value = contentState,
                    onValueChange = {
                        contentState = it
                        contentDirty = true
                    },
                    label = { Text(stringResource(R.string.system_prompt)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
            } else {
                OutlinedTextField(
                    value = contentState,
                    onValueChange = {
                        contentState = it
                        contentDirty = true
                    },
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
