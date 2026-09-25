package com.abrarshakhi.galva.features.secrets.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun BlockingProgress(message: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

data class MoveProgress(val done: Int, val total: Int) {

    val label: String
        get() = if (total <= 1) "Encrypting…"
        else "Encrypting ${minOf(done + 1, total)} of $total…"
}

const val SET_UP_SECRETS_FIRST = "Set up Secrets first — open the Secrets tab"

fun movedMessage(moved: Int?): String =
    if (moved == null) "Nothing was moved; the originals stay in your gallery"
    else "Moved $moved to Secrets"
