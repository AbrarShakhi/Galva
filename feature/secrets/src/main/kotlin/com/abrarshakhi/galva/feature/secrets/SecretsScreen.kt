package com.abrarshakhi.galva.feature.secrets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.designsystem.component.EmptyState
import com.abrarshakhi.galva.core.designsystem.component.Illustration
import com.abrarshakhi.galva.core.designsystem.layout.rememberWindowLayout
import com.abrarshakhi.galva.core.ui.appViewModel
import com.abrarshakhi.galva.core.ui.component.MediaGrid
import com.abrarshakhi.galva.core.ui.component.SelectionTopBar
import com.abrarshakhi.galva.core.ui.component.itemCountLabel
import com.abrarshakhi.galva.core.ui.component.rememberToolbarAwarePadding
import com.abrarshakhi.galva.core.ui.component.toolbarAlignment
import com.abrarshakhi.galva.core.ui.media.MediaConsentEffect
import com.abrarshakhi.galva.core.ui.message.UserMessagesEffect
import com.abrarshakhi.galva.core.ui.vault.BlockingProgress
import com.abrarshakhi.galva.core.ui.vault.ErrorText
import com.abrarshakhi.galva.core.ui.vault.PassphraseField
import com.abrarshakhi.galva.core.ui.vault.PassphraseRules
import com.abrarshakhi.galva.core.ui.vault.SecretsForm
import com.abrarshakhi.galva.core.ui.vault.SecureWindow
import com.abrarshakhi.galva.core.ui.vault.VaultEmblem
import com.abrarshakhi.galva.core.ui.vault.VaultUnlockContent
import com.abrarshakhi.galva.core.ui.vault.WorkingButton

@Composable
fun SecretsRoute(onOpenViewer: (secretId: Long) -> Unit) {
    SecretsScreen(
        viewModel = appViewModel(),
        onOpenViewer = onOpenViewer,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SecretsScreen(
    viewModel: SecretsViewModel,
    onOpenViewer: (secretId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureWindow()

    val state by viewModel.state.collectAsStateWithLifecycle()

    MediaConsentEffect(
        request = state.originalsConsent,
        onLaunched = { viewModel.onIntent(SecretsIntent.OriginalsConsentLaunched(it)) },
        onResult = { id, confirmed ->
            viewModel.onIntent(SecretsIntent.OriginalsConsentResolved(id, confirmed))
        },
    )
    UserMessagesEffect(state.messages) { viewModel.onIntent(SecretsIntent.MessageShown(it)) }

    BackHandler(enabled = state.selection.isActive) {
        viewModel.onIntent(SecretsIntent.ClearSelection)
    }

    val layout = rememberWindowLayout()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    LaunchedEffect(state.phase) {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (state.selection.isActive) {
                SelectionTopBar(
                    count = state.selection.count,
                    allSelected = state.allSelected,
                    onClear = { viewModel.onIntent(SecretsIntent.ClearSelection) },
                    onSelectAll = { viewModel.onIntent(SecretsIntent.SelectAll) },
                    scrollBehavior = scrollBehavior,
                )
            } else {
                SecretsTopBar(
                    unlocked = state.phase == SecretsPhase.Unlocked,
                    itemCount = state.items.size,
                    onLock = { viewModel.onIntent(SecretsIntent.LockRequested) },
                    onChangePassphrase = {
                        viewModel.onIntent(SecretsIntent.ChangePassphraseRequested)
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (state.phase) {
                    SecretsPhase.Loading -> ContainedLoadingIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp),
                    )

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
                                illustration = Illustration.Vault,
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
                                sharedKeyNamespace = SECRET_NAMESPACE,
                                onItemClick = { item ->
                                    if (state.selection.isActive) {
                                        viewModel.onIntent(SecretsIntent.ToggleSelection(item))
                                    } else {
                                        onOpenViewer(item.id)
                                    }
                                },
                                onItemLongClick = {
                                    viewModel.onIntent(SecretsIntent.ToggleSelection(it))
                                },
                                contentPadding = rememberToolbarAwarePadding(state.selection.isActive, layout),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            SecretActionToolbar(
                visible = state.selection.isActive,
                layout = layout,
                onRestore = { viewModel.onIntent(SecretsIntent.RestoreSelection) },
                onDelete = { viewModel.onIntent(SecretsIntent.DeleteSelection) },
                modifier = Modifier
                    .align(toolbarAlignment(layout))
                    .padding(16.dp),
            )
        }
    }

    if (state.confirmingDelete) {
        DeleteForGoodDialog(
            title = "Delete ${itemCountLabel(state.selection.count)} for good?",
            onConfirm = { viewModel.onIntent(SecretsIntent.DeleteConfirmed) },
            onDismiss = { viewModel.onIntent(SecretsIntent.DeleteDismissed) },
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PendingMovesBanner(count: Int, onFinish: () -> Unit, onUndo: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                Text(
                    text = if (count == 1) "1 item is still in your gallery too."
                    else "$count items are still in your gallery too.",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(
                    onClick = onUndo,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Text("Keep in gallery only")
                }
                Button(
                    onClick = onFinish,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                ) {
                    Text("Remove from gallery")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewPassphraseContent(
    isSaving: Boolean,
    error: String?,
    onSubmit: (CharSequence, CharSequence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val passphrase = rememberTextFieldState()
    val confirmation = rememberTextFieldState()
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSubmit(passphrase.text, confirmation.text)
    }

    SecretsForm(modifier = modifier) {
        VaultEmblem(working = isSaving, modifier = Modifier.padding(top = 16.dp))
        Text(
            text = "Choose a new passphrase",
            style = MaterialTheme.typography.headlineMediumEmphasized,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "You opened Secrets with your recovery phrase. Choose a new passphrase to " +
                "finish; the old one stops working. ${PassphraseRules.HINT}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        icon = { Icon(Icons.Rounded.Password, contentDescription = null) },
        title = { Text("Change passphrase") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
            Button(
                onClick = { onSubmit(current.text, new.text, confirmation.text) },
                enabled = !isSaving,
                shapes = ButtonDefaults.shapes(),
            ) {
                if (isSaving) {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving, shapes = ButtonDefaults.shapes()) {
                Text("Cancel")
            }
        },
    )
}
