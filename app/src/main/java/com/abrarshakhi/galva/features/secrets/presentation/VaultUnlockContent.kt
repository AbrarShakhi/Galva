package com.abrarshakhi.galva.features.secrets.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.mvi.CollectEffects
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** The Secrets tab while the vault is sealed. */
@Composable
fun VaultUnlockContent(modifier: Modifier = Modifier) {
    val viewModel: VaultUnlockViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar: SnackbarDispatcher = koinInject()
    val passphrase = rememberTextFieldState()
    val phrase = rememberTextFieldState()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            // The tab follows the vault's state; nothing else to do here.
            VaultUnlockEffect.Unlocked -> Unit
            is VaultUnlockEffect.ShowMessage -> snackbar.show(effect.text)
        }
    }

    SecretsForm(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text("Secrets is locked", style = MaterialTheme.typography.titleLarge)

        val unavailable = state.unavailable
        when {
            unavailable != null -> {
                Text(
                    text = unavailable,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { viewModel.onIntent(VaultUnlockIntent.ResetRequested) }) {
                    Text("Reset Secrets")
                }
            }

            state.mode == UnlockMode.Passphrase -> {
                val submit = {
                    viewModel.onIntent(VaultUnlockIntent.PassphraseSubmitted(passphrase.text))
                    passphrase.clearText()
                }
                PassphraseField(
                    state = passphrase,
                    label = "Passphrase",
                    enabled = !state.isWorking,
                    isError = state.error != null,
                    onKeyboardAction = { submit() },
                )
                ErrorText(state.error)
                WorkingButton(label = "Unlock", isWorking = state.isWorking, onClick = submit)
                TextButton(onClick = { viewModel.onIntent(VaultUnlockIntent.UseRecoveryPhrase) }) {
                    Text("Forgot it? Use your recovery phrase")
                }
            }

            else -> {
                Text(
                    text = "Enter your 24 recovery words, in order.",
                    textAlign = TextAlign.Center,
                )
                RecoveryPhraseField(state = phrase, enabled = !state.isWorking)
                ErrorText(state.error)
                WorkingButton(
                    label = "Unlock",
                    isWorking = state.isWorking,
                    onClick = {
                        viewModel.onIntent(VaultUnlockIntent.RecoveryPhraseSubmitted(phrase.text))
                    },
                )
                TextButton(onClick = { viewModel.onIntent(VaultUnlockIntent.UsePassphrase) }) {
                    Text("Use my passphrase instead")
                }
                TextButton(onClick = { viewModel.onIntent(VaultUnlockIntent.ResetRequested) }) {
                    Text(
                        text = "Lost the recovery phrase too? Reset Secrets",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (state.confirmingReset) {
        ResetSecretsDialog(
            onDismiss = { viewModel.onIntent(VaultUnlockIntent.ResetDismissed) },
            onConfirm = { viewModel.onIntent(VaultUnlockIntent.ResetConfirmed) },
        )
    }
}

/**
 * Unlocks the vault from another screen, so "Move to Secrets" can carry on where it was.
 *
 * Recovery is left to the Secrets tab, because recovering ends in choosing a new passphrase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnlockSheet(
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
) {
    val viewModel: VaultUnlockViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val passphrase = rememberTextFieldState()

    CollectEffects(viewModel.effects) { effect ->
        if (effect == VaultUnlockEffect.Unlocked) onUnlocked()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = GalvaDimens.ScreenPadding)
                .padding(bottom = GalvaDimens.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(GalvaDimens.ItemSpacing),
        ) {
            Text("Unlock Secrets", style = MaterialTheme.typography.titleMedium)
            val unavailable = state.unavailable
            if (unavailable != null) {
                Text(unavailable, color = MaterialTheme.colorScheme.error)
                return@Column
            }
            val submit = {
                viewModel.onIntent(VaultUnlockIntent.PassphraseSubmitted(passphrase.text))
                passphrase.clearText()
            }
            PassphraseField(
                state = passphrase,
                label = "Passphrase",
                enabled = !state.isWorking,
                isError = state.error != null,
                onKeyboardAction = { submit() },
            )
            ErrorText(state.error)
            WorkingButton(
                label = "Unlock",
                isWorking = state.isWorking,
                onClick = submit,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Forgot it? Open the Secrets tab to use your recovery phrase.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Typing the word is the point: a reset cannot be undone, so it must not be one tap away. */
@Composable
private fun ResetSecretsDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val confirmation = rememberTextFieldState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Secrets?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GalvaDimens.ItemSpacing)) {
                Text(
                    "Everything in Secrets is destroyed for good, and nothing can bring it back. " +
                        "Type $RESET_WORD to confirm.",
                )
                OutlinedTextField(
                    state = confirmation,
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmation.text.toString() == RESET_WORD,
            ) {
                Text("Reset", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val RESET_WORD = "DELETE"
