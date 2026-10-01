package com.abrarshakhi.galva.features.secrets.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens
import org.koin.androidx.compose.koinViewModel

/** The Secrets tab before a vault exists. */
@Composable
fun VaultSetupContent(modifier: Modifier = Modifier) {
    val viewModel: VaultSetupViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.step != SetupStep.Intro && !state.isCreating) {
        viewModel.onIntent(VaultSetupIntent.Back)
    }

    SecretsForm(modifier = modifier) {
        when (state.step) {
            SetupStep.Intro -> Intro(onBegin = { viewModel.onIntent(VaultSetupIntent.Begin) })

            SetupStep.Passphrase -> ChoosePassphrase(
                error = state.error,
                onSubmit = { passphrase, confirmation ->
                    viewModel.onIntent(
                        VaultSetupIntent.PassphraseChosen(passphrase, confirmation),
                    )
                },
            )

            SetupStep.RecoveryPhrase -> ShowRecoveryPhrase(
                words = state.recoveryWords,
                onWrittenDown = { viewModel.onIntent(VaultSetupIntent.PhraseWrittenDown) },
                onBack = { viewModel.onIntent(VaultSetupIntent.Back) },
            )

            SetupStep.ConfirmPhrase -> ConfirmRecoveryPhrase(
                positions = state.checkPositions,
                error = state.error,
                isCreating = state.isCreating,
                onSubmit = { answers ->
                    viewModel.onIntent(VaultSetupIntent.CheckSubmitted(answers))
                },
                onBack = { viewModel.onIntent(VaultSetupIntent.Back) },
            )
        }
    }
}

@Composable
private fun Intro(onBegin: () -> Unit) {
    Icon(
        imageVector = Icons.Outlined.Lock,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp),
    )
    Text("Keep photos only you can open", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Photos and videos you move here are encrypted with a passphrase only you know. " +
            "Not even Galva can open them without it.",
    )
    Text("Before you start", style = MaterialTheme.typography.titleMedium)
    Warning(
        "Forget your passphrase and lose your recovery phrase, and everything in Secrets is " +
            "gone for good. Nobody can reset it.",
    )
    Warning(
        "Uninstalling Galva or clearing its storage permanently destroys Secrets. Restore " +
            "anything you want to keep first.",
    )
    Warning(
        "Moving a photo here deletes the original from your gallery. Copies elsewhere — cloud " +
            "backups, other apps — are not affected.",
    )
    Warning("Deleting from Secrets is final: not even your passphrase brings it back.")
    WorkingButton(
        label = "Set up Secrets",
        isWorking = false,
        onClick = onBegin,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Warning(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(GalvaDimens.ItemSpacing)) {
        Text("•", color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ChoosePassphrase(
    error: String?,
    onSubmit: (CharSequence, CharSequence) -> Unit,
) {
    val passphrase = rememberTextFieldState()
    val confirmation = rememberTextFieldState()
    val submit = { onSubmit(passphrase.text, confirmation.text) }

    Text("Choose a passphrase", style = MaterialTheme.typography.headlineSmall)
    Text(PassphraseRules.HINT, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PassphraseField(state = passphrase, label = "Passphrase", imeAction = ImeAction.Next)
    PassphraseField(
        state = confirmation,
        label = "Type it again",
        isError = error != null,
        onKeyboardAction = { submit() },
    )
    ErrorText(error)
    WorkingButton(
        label = "Continue",
        isWorking = false,
        onClick = submit,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.ShowRecoveryPhrase(
    words: List<String>,
    onWrittenDown: () -> Unit,
    onBack: () -> Unit,
) {
    Text("Your recovery phrase", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Write these 24 words down, in order, and keep them somewhere safe and offline. They " +
            "are the only way back in if you forget your passphrase, and Galva will never show " +
            "them again.",
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(GalvaDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            words.chunked(WORDS_PER_ROW).forEachIndexed { row, rowWords ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowWords.forEachIndexed { column, word ->
                        Text(
                            text = "${row * WORDS_PER_ROW + column + 1}. $word",
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
    Text(
        "Anyone who has these words can open your Secrets. Don't photograph them or store " +
            "them in a cloud note.",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
    WorkingButton(
        label = "I've written them down",
        isWorking = false,
        onClick = onWrittenDown,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text("Back")
    }
}

@Composable
private fun ColumnScope.ConfirmRecoveryPhrase(
    positions: List<Int>,
    error: String?,
    isCreating: Boolean,
    onSubmit: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val answers = positions.map { position -> key(position) { rememberTextFieldState() } }

    Text("Check your recovery phrase", style = MaterialTheme.typography.headlineSmall)
    Text("Type these words from what you wrote down.")
    positions.forEachIndexed { index, position ->
        RecoveryPhraseField(
            state = answers[index],
            enabled = !isCreating,
            label = "Word ${position + 1}",
            singleLine = true,
            imeAction = if (index == positions.lastIndex) ImeAction.Done else ImeAction.Next,
        )
    }
    ErrorText(error)
    WorkingButton(
        label = "Create Secrets",
        isWorking = isCreating,
        onClick = { onSubmit(answers.map { it.text.toString() }) },
        modifier = Modifier.fillMaxWidth(),
    )
    if (!isCreating) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Show the words again")
        }
    }
}

private const val WORDS_PER_ROW = 3
