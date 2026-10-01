package com.abrarshakhi.galva.features.secrets.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.mvi.CollectEffects
import com.abrarshakhi.galva.common.ui.appViewModel
import com.abrarshakhi.galva.common.ui.component.EmptyState
import com.abrarshakhi.galva.common.ui.component.MediaGrid
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens
import com.abrarshakhi.galva.core.media.ui.rememberMediaDeleteLauncher
import com.abrarshakhi.galva.core.vault.ui.SecureWindow
import org.koin.compose.koinInject

@Composable
fun SecretsScreen(
    onOpenViewer: (secretId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureWindow()

    val viewModel: SecretsViewModel = appViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar: SnackbarDispatcher = koinInject()

    val originalsLauncher = rememberMediaDeleteLauncher { confirmed, ids ->
        viewModel.onIntent(SecretsIntent.PendingMovesResolved(ids, confirmed))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is SecretsEffect.OpenViewer -> onOpenViewer(effect.secretId)
            is SecretsEffect.ConfirmOriginalsDeletion ->
                originalsLauncher.request(effect.originalIds, effect.uris)
            is SecretsEffect.ShowMessage -> snackbar.show(effect.text)
        }
    }

    BackHandler(enabled = state.selection.isActive) {
        viewModel.onIntent(SecretsIntent.ClearSelection)
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (state.phase) {
            SecretsPhase.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            SecretsPhase.NotSetUp -> VaultSetupContent(Modifier.fillMaxSize())

            SecretsPhase.Locked -> VaultUnlockContent(Modifier.fillMaxSize())

            SecretsPhase.NeedsNewPassphrase -> NewPassphraseContent(
                isSaving = state.isSavingPassphrase,
                error = state.passphraseError,
                onSubmit = { new, confirmation ->
                    viewModel.onIntent(SecretsIntent.NewPassphraseSubmitted(new, confirmation))
                },
                modifier = Modifier.fillMaxSize(),
            )

            SecretsPhase.Unlocked -> Column(modifier = Modifier.fillMaxSize()) {
                if (state.pendingMoves.isNotEmpty()) {
                    PendingMovesBanner(
                        count = state.pendingMoves.size,
                        onFinish = { viewModel.onIntent(SecretsIntent.FinishPendingMoves) },
                        onUndo = { viewModel.onIntent(SecretsIntent.UndoPendingMoves) },
                    )
                }
                if (state.isEmpty) {
                    EmptyState(
                        icon = Icons.Outlined.Lock,
                        title = "Nothing here yet",
                        message = "Select photos or videos anywhere in Galva and choose " +
                            "Move to Secrets.",
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    MediaGrid(
                        items = state.items,
                        selection = state.selection,
                        preferredColumns = state.columns,
                        onItemClick = { viewModel.onIntent(SecretsIntent.MediaTapped(it)) },
                        onItemLongClick = {
                            viewModel.onIntent(SecretsIntent.MediaLongPressed(it))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(SecretsIntent.DeleteDismissed) },
            title = { Text("Delete ${state.selection.count} for good?") },
            text = { Text(DELETE_FOR_GOOD_WARNING) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(SecretsIntent.DeleteConfirmed) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(SecretsIntent.DeleteDismissed) }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (state.changingPassphrase) {
        ChangePassphraseDialog(
            isSaving = state.isSavingPassphrase,
            error = state.passphraseError,
            onSubmit = { current, new, confirmation ->
                viewModel.onIntent(
                    SecretsIntent.ChangePassphraseSubmitted(current, new, confirmation),
                )
            },
            onDismiss = { viewModel.onIntent(SecretsIntent.ChangePassphraseDismissed) },
        )
    }

    state.work?.let { BlockingProgress(it) }
}

/** Moves whose originals were never confirmed deleted: an interruption, or a declined dialog. */
@Composable
private fun PendingMovesBanner(count: Int, onFinish: () -> Unit, onUndo: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = GalvaDimens.ScreenPadding, vertical = 8.dp),
        ) {
            Text(
                text = if (count == 1) "1 item is still in your gallery too."
                else "$count items are still in your gallery too.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onFinish) { Text("Remove from gallery") }
                TextButton(onClick = onUndo) { Text("Keep in gallery only") }
            }
        }
    }
}

@Composable
private fun NewPassphraseContent(
    isSaving: Boolean,
    error: String?,
    onSubmit: (CharSequence, CharSequence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val passphrase = rememberTextFieldState()
    val confirmation = rememberTextFieldState()
    val submit = { onSubmit(passphrase.text, confirmation.text) }

    SecretsForm(modifier = modifier) {
        Text("Choose a new passphrase", style = MaterialTheme.typography.headlineSmall)
        Text(
            "You opened Secrets with your recovery phrase. Choose a new passphrase to finish; " +
                "the old one stops working. ${PassphraseRules.HINT}",
        )
        PassphraseField(
            state = passphrase,
            label = "New passphrase",
            enabled = !isSaving,
            imeAction = ImeAction.Next,
        )
        PassphraseField(
            state = confirmation,
            label = "Type it again",
            enabled = !isSaving,
            isError = error != null,
            onKeyboardAction = { submit() },
        )
        ErrorText(error)
        WorkingButton(
            label = "Save",
            isWorking = isSaving,
            onClick = submit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChangePassphraseDialog(
    isSaving: Boolean,
    error: String?,
    onSubmit: (CharSequence, CharSequence, CharSequence) -> Unit,
    onDismiss: () -> Unit,
) {
    val current = rememberTextFieldState()
    val new = rememberTextFieldState()
    val confirmation = rememberTextFieldState()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Change passphrase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GalvaDimens.ItemSpacing)) {
                PassphraseField(
                    state = current,
                    label = "Current passphrase",
                    enabled = !isSaving,
                    imeAction = ImeAction.Next,
                )
                PassphraseField(
                    state = new,
                    label = "New passphrase",
                    enabled = !isSaving,
                    imeAction = ImeAction.Next,
                )
                PassphraseField(
                    state = confirmation,
                    label = "Type it again",
                    enabled = !isSaving,
                    isError = error != null,
                )
                Text(
                    PassphraseRules.HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(current.text, new.text, confirmation.text) },
                enabled = !isSaving,
            ) {
                Text(if (isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        },
    )
}

const val DELETE_FOR_GOOD_WARNING =
    "It can't be recovered — not from Secrets, not with your passphrase."
