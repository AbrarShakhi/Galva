package com.abrarshakhi.galva.features.secrets.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.main.ScreenChrome
import com.abrarshakhi.galva.common.ui.appViewModel
import com.abrarshakhi.galva.common.ui.component.AppTabs
import com.abrarshakhi.galva.common.ui.component.SelectionAction
import com.abrarshakhi.galva.common.ui.component.SelectionActionsContainer
import com.abrarshakhi.galva.common.ui.component.SelectionTopBar

fun secretsChrome(): ScreenChrome = ScreenChrome(
    topBar = { scope ->
        val viewModel: SecretsViewModel = appViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.selection.isActive) {
            SelectionTopBar(
                count = state.selection.count,
                allSelected = state.allSelected,
                onClear = { viewModel.onIntent(SecretsIntent.ClearSelection) },
                onSelectAll = { viewModel.onIntent(SecretsIntent.SelectAll) },
                scrollBehavior = scope.scrollBehavior,
            )
        } else {
            SecretsTopBar(
                unlocked = state.phase == SecretsPhase.Unlocked,
                onLock = { viewModel.onIntent(SecretsIntent.LockRequested) },
                onChangePassphrase = {
                    viewModel.onIntent(SecretsIntent.ChangePassphraseRequested)
                },
                scrollBehavior = scope.scrollBehavior,
            )
        }
    },
    navigation = { scope ->
        val viewModel: SecretsViewModel = appViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.selection.isActive) {
            SelectionActionsContainer(layout = scope.layout) {
                SelectionAction(
                    icon = Icons.Filled.LockOpen,
                    label = "Restore to gallery",
                    enabled = state.selection.isActive,
                    onClick = { viewModel.onIntent(SecretsIntent.RestoreSelection) },
                )
                SelectionAction(
                    icon = Icons.Filled.DeleteForever,
                    label = "Delete for good",
                    enabled = state.selection.isActive,
                    onClick = { viewModel.onIntent(SecretsIntent.DeleteSelection) },
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            AppTabs(
                layout = scope.layout,
                current = scope.currentRoute,
                onTabSelected = scope.switchTab,
            )
        }
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecretsTopBar(
    unlocked: Boolean,
    onLock: () -> Unit,
    onChangePassphrase: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Secrets") },
        actions = {
            if (unlocked) {
                IconButton(onClick = onLock) {
                    Icon(Icons.Filled.Lock, contentDescription = "Lock Secrets")
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Secrets options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Change passphrase") },
                        onClick = {
                            menuOpen = false
                            onChangePassphrase()
                        },
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
