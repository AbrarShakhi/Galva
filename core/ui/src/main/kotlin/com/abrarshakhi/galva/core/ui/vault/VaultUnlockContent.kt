package com.abrarshakhi.galva.core.ui.vault

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.ui.message.UserMessagesEffect
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultUnlockContent(modifier: Modifier = Modifier) {
    val viewModel: VaultUnlockViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val passphrase = rememberTextFieldState()
    val phrase = rememberTextFieldState()
    val focusManager = LocalFocusManager.current

    UserMessagesEffect(state.messages) { viewModel.onIntent(VaultUnlockIntent.MessageShown(it)) }

    if (state.unlocked) {
        LaunchedEffect(Unit) { viewModel.onIntent(VaultUnlockIntent.UnlockHandled) }
    }

    SecretsForm(modifier = modifier) {
        VaultEmblem(working = state.isWorking, modifier = Modifier.padding(top = 24.dp))
        Text(
            text = "Secrets is locked",
            style = MaterialTheme.typography.headlineMediumEmphasized,
            textAlign = TextAlign.Center,
        )

        val unavailable = state.unavailable
        val mode = when {
            unavailable != null -> null
            else -> state.mode
        }
        val motion = MaterialTheme.motionScheme
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (fadeIn(motion.defaultEffectsSpec()) + slideInVertically(motion.defaultSpatialSpec()) { it / 8 }) togetherWith
                    fadeOut(motion.fastEffectsSpec())
            },
            label = "unlockMode",
        ) { target ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (target) {
                    null -> {
                        Text(
                            text = unavailable.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        WorkingButton(
                            label = "Reset Secrets",
                            isWorking = false,
                            onClick = { viewModel.onIntent(VaultUnlockIntent.ResetRequested) },
                        )
                    }

                    UnlockMode.Passphrase -> {
                        Text(
                            text = "Enter your passphrase to open your encrypted photos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        val submit = {
                            focusManager.clearFocus()
                            viewModel.onIntent(VaultUnlockIntent.PassphraseSubmitted(passphrase.text))
                            passphrase.clearText()
                        }
                        PassphraseField(
                            state = passphrase,
                            label = "Passphrase",
                            enabled = !state.isWorking,
                            isError = state.error != null,
                            onKeyboardAction = { submit() },
                            modifier = Modifier.shakeOnError(state.error, state.isWorking),
                        )
                        ErrorText(state.error)
                        WorkingButton(
                            label = "Unlock",
                            isWorking = state.isWorking,
                            onClick = submit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(
                            onClick = { viewModel.onIntent(VaultUnlockIntent.UseRecoveryPhrase) },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text("Forgot it? Use your recovery phrase")
                        }
                    }

                    UnlockMode.RecoveryPhrase -> {
                        Text(
                            text = "Enter your 24 recovery words, in order.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        RecoveryPhraseField(
                            state = phrase,
                            enabled = !state.isWorking,
                            modifier = Modifier.shakeOnError(state.error, state.isWorking),
                        )
                        ErrorText(state.error)
                        WorkingButton(
                            label = "Unlock",
                            isWorking = state.isWorking,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.onIntent(VaultUnlockIntent.RecoveryPhraseSubmitted(phrase.text))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(
                            onClick = { viewModel.onIntent(VaultUnlockIntent.UsePassphrase) },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text("Use my passphrase instead")
                        }
                        TextButton(
                            onClick = { viewModel.onIntent(VaultUnlockIntent.ResetRequested) },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(
                                text = "Lost the recovery phrase too? Reset Secrets",
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultUnlockSheet(
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
) {
    val viewModel: VaultUnlockViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val passphrase = rememberTextFieldState()

    if (state.unlocked) {
        LaunchedEffect(Unit) {
            onUnlocked()
            viewModel.onIntent(VaultUnlockIntent.UnlockHandled)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VaultEmblem(working = state.isWorking, size = 88.dp)
            Text("Unlock Secrets", style = MaterialTheme.typography.headlineSmallEmphasized)
            val unavailable = state.unavailable
            if (unavailable != null) {
                Text(unavailable, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                return@Column
            }
            val focusManager = LocalFocusManager.current
            val submit = {
                focusManager.clearFocus()
                viewModel.onIntent(VaultUnlockIntent.PassphraseSubmitted(passphrase.text))
                passphrase.clearText()
            }
            PassphraseField(
                state = passphrase,
                label = "Passphrase",
                enabled = !state.isWorking,
                isError = state.error != null,
                onKeyboardAction = { submit() },
                modifier = Modifier.shakeOnError(state.error, state.isWorking),
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
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResetSecretsDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val confirmation = rememberTextFieldState()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("Reset Secrets?") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Everything in Secrets is destroyed for good, and nothing can bring it back. " +
                        "Type $RESET_WORD to confirm.",
                )
                OutlinedTextField(
                    state = confirmation,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    placeholder = { Text(RESET_WORD) },
                    shape = MaterialTheme.shapes.large,
                    isError = confirmation.text.isNotEmpty() && confirmation.text.toString() != RESET_WORD,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmation.text.toString() == RESET_WORD,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
        },
    )
}

private const val RESET_WORD = "DELETE"
