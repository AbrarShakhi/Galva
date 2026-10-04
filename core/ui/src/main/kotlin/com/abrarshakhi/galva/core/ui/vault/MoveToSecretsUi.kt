package com.abrarshakhi.galva.core.ui.vault

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BlockingProgress(message: String, progress: Float? = null) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 240.dp)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ContainedLoadingIndicator(modifier = Modifier.size(72.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                if (progress != null) {
                    val animated by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        label = "blockingProgress",
                    )
                    LinearWavyProgressIndicator(
                        progress = { animated.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

data class MoveProgress(val done: Int, val total: Int) {

    val label: String
        get() = if (total <= 1) "Encrypting…"
        else "Encrypting ${minOf(done + 1, total)} of $total…"

    val fraction: Float? get() = if (total <= 1) null else done.toFloat() / total
}

const val SET_UP_SECRETS_FIRST = "Set up Secrets first in the Secrets tab"

fun movedMessage(moved: Int?): String =
    if (moved == null) "Nothing was moved; the originals stay in your gallery"
    else "Moved $moved to Secrets"
