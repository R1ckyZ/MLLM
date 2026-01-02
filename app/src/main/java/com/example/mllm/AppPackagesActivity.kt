package com.example.mllm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mllm.ui.theme.MLLMTheme

class AppPackagesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialText = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val languageCode = intent.getStringExtra(EXTRA_LANGUAGE_CODE).orEmpty()
        val localizedContext = createLocalizedContext(this, languageCode)
        setContent {
            CompositionLocalProvider(LocalContext provides localizedContext) {
                MLLMTheme {
                    AppPackagesScreen(
                        text = initialText,
                        onCancel = { finish() },
                        onSave = { updated ->
                            val result = Intent().putExtra(EXTRA_TEXT, updated)
                            setResult(Activity.RESULT_OK, result)
                            finish()
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TEXT = "extra_app_packages_text"
        const val EXTRA_LANGUAGE_CODE = "extra_language_code"

        fun createIntent(
            context: Context,
            text: String,
            languageCode: String,
        ): Intent {
            return Intent(context, AppPackagesActivity::class.java)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_LANGUAGE_CODE, languageCode)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppPackagesScreen(
    text: String,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
) {
    var editorText by remember(text) { mutableStateOf(text) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_packages_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { onSave(editorText.trim()) }) {
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
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.app_packages_hint),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = editorText,
                onValueChange = { editorText = it },
                label = { Text(stringResource(R.string.app_packages_label)) },
                placeholder = { Text(stringResource(R.string.app_packages_placeholder)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp),
            )
        }
    }
}
