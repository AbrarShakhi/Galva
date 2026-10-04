package com.abrarshakhi.galva.feature.secrets

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.abrarshakhi.galva.core.designsystem.component.AdaptiveTopAppBar
import com.abrarshakhi.galva.core.ui.component.itemCountLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SecretsTopBar(
    unlocked: Boolean,
    itemCount: Int,
    onLock: () -> Unit,
    onChangePassphrase: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    AdaptiveTopAppBar(
        title = { Text("Secrets") },
        subtitle = {
            Text(
                when {
                    !unlocked -> "Encrypted on this device"
                    itemCount == 0 -> "Unlocked"
                    else -> itemCountLabel(itemCount)
                },
            )
        },
        actions = {
            if (unlocked) {
                FilledTonalIconButton(
                    onClick = onLock,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Lock Secrets")
                }
                OptionsMenu(onChangePassphrase = onChangePassphrase)
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun OptionsMenu(onChangePassphrase: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Secrets options")
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.large,
        ) {
            DropdownMenuItem(
                text = { Text("Change passphrase") },
                leadingIcon = { Icon(Icons.Rounded.Password, contentDescription = null) },
                onClick = {
                    open = false
                    onChangePassphrase()
                },
            )
        }
    }
}
