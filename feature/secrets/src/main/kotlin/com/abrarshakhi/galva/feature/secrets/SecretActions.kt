package com.abrarshakhi.galva.feature.secrets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abrarshakhi.galva.core.designsystem.layout.WindowLayout
import com.abrarshakhi.galva.core.ui.component.ToolbarIconButton

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SecretActionToolbar(
    visible: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    layout: WindowLayout = WindowLayout.BottomBar,
) {
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec()),
        exit = slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut(motion.fastEffectsSpec()),
    ) {
        val fab: @Composable () -> Unit = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = onDelete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete for good")
            }
        }
        val colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
        if (layout == WindowLayout.Rail) {
            VerticalFloatingToolbar(expanded = true, floatingActionButton = fab, colors = colors) {
                ToolbarIconButton(Icons.Rounded.LockOpen, "Restore to gallery", onRestore)
            }
        } else {
            HorizontalFloatingToolbar(expanded = true, floatingActionButton = fab, colors = colors) {
                ToolbarIconButton(Icons.Rounded.LockOpen, "Restore to gallery", onRestore)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DeleteForGoodDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(title) },
        text = { Text(DELETE_FOR_GOOD_WARNING) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
        },
    )
}

internal const val DELETE_FOR_GOOD_WARNING =
    "It can't be recovered, not from Secrets and not with your passphrase."
