package com.example.mllm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mllm.data.DEFAULT_LLM_CONFIG_NAME
import com.example.mllm.ui.theme.MLLMTheme

class ModelManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val initialName = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val nameEnabled = intent.getBooleanExtra(EXTRA_NAME_ENABLED, true)
        val initialUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        val initialApiKey = intent.getStringExtra(EXTRA_API_KEY).orEmpty()
        val initialNamesText = intent.getStringExtra(EXTRA_NAMES).orEmpty()
        val languageCode = intent.getStringExtra(EXTRA_LANGUAGE_CODE).orEmpty()
        val initialNames =
            initialNamesText.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toList()
        val localizedContext = createLocalizedContext(this, languageCode)
        val resolvedTitle =
            if (title.isBlank()) {
                localizedContext.getString(R.string.model_manager_title)
            } else {
                title
            }
        setContent {
            CompositionLocalProvider(LocalContext provides localizedContext) {
                MLLMTheme {
                    ModelManagerScreen(
                        title = resolvedTitle,
                        name = initialName,
                        nameEnabled = nameEnabled,
                        url = initialUrl,
                        apiKey = initialApiKey,
                        names = initialNames,
                        onCancel = { finish() },
                        onSave = { name, url, apiKey, names ->
                            val result =
                                Intent()
                                    .putExtra(EXTRA_NAME, name)
                                    .putExtra(EXTRA_URL, url)
                                    .putExtra(EXTRA_API_KEY, apiKey)
                                    .putExtra(EXTRA_NAMES, names)
                            setResult(Activity.RESULT_OK, result)
                            finish()
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "extra_model_manager_title"
        const val EXTRA_NAME = "extra_model_manager_name"
        const val EXTRA_NAME_ENABLED = "extra_model_manager_name_enabled"
        const val EXTRA_URL = "extra_model_manager_url"
        const val EXTRA_API_KEY = "extra_model_manager_api_key"
        const val EXTRA_NAMES = "extra_model_manager_names"
        const val EXTRA_LANGUAGE_CODE = "extra_language_code"

        fun createIntent(
            context: Context,
            title: String,
            name: String,
            nameEnabled: Boolean,
            url: String,
            apiKey: String,
            names: List<String>,
            languageCode: String,
        ): Intent {
            val namesText = names.joinToString("\n")
            return Intent(context, ModelManagerActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_NAME_ENABLED, nameEnabled)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_API_KEY, apiKey)
                .putExtra(EXTRA_NAMES, namesText)
                .putExtra(EXTRA_LANGUAGE_CODE, languageCode)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ModelManagerScreen(
    title: String,
    name: String,
    nameEnabled: Boolean,
    url: String,
    apiKey: String,
    names: List<String>,
    onCancel: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    val defaultName = DEFAULT_LLM_CONFIG_NAME
    var nameState by remember(name) { mutableStateOf(name) }
    var urlState by remember(url) { mutableStateOf(url) }
    var apiKeyState by remember(apiKey) { mutableStateOf(apiKey) }
    var namesState by remember(names) { mutableStateOf(names) }
    var newNameState by remember { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val namesText = namesState.joinToString("\n")
                            onSave(
                                nameState.trim(),
                                urlState.trim(),
                                apiKeyState.trim(),
                                namesText,
                            )
                        },
                    ) {
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
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = nameState,
                onValueChange = {
                    if (nameEnabled) {
                        nameState = it
                    }
                },
                readOnly = !nameEnabled,
                label = { Text(stringResource(R.string.model_manager_llm_name)) },
                placeholder = { Text(defaultName) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = urlState,
                onValueChange = { urlState = it },
                label = { Text(stringResource(R.string.model_manager_base_url)) },
                placeholder = { Text(stringResource(R.string.model_manager_base_url_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKeyState,
                onValueChange = { apiKeyState = it },
                label = { Text(stringResource(R.string.model_manager_api_key)) },
                placeholder = { Text(stringResource(R.string.model_manager_api_key_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.model_manager_model_names),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val addRowHeight = 56.dp
                OutlinedTextField(
                    value = newNameState,
                    onValueChange = { newNameState = it },
                    placeholder = { Text(stringResource(R.string.model_manager_add_model_placeholder)) },
                    singleLine = true,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(addRowHeight),
                )
                Button(
                    onClick = {
                        val trimmed = newNameState.trim()
                        if (trimmed.isNotEmpty() && !namesState.contains(trimmed)) {
                            namesState = namesState + trimmed
                            newNameState = ""
                        }
                    },
                    modifier =
                        Modifier
                            .width(96.dp)
                            .height(addRowHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }
            if (namesState.isEmpty()) {
                Text(
                    text = stringResource(R.string.model_manager_empty_models),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    namesState.forEach { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                namesState = namesState.filterNot { it == name }
                            }) {
                                Text(stringResource(R.string.action_remove))
                            }
                        }
                    }
                }
            }
        }
    }
}
