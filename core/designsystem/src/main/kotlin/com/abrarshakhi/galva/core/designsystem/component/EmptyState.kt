package com.abrarshakhi.galva.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abrarshakhi.galva.core.designsystem.theme.GalvaDimens

@Composable
fun EmptyState(
    illustration: Illustration,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val sideBySide = maxHeight < COMPACT_HEIGHT && maxWidth > maxHeight
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.85f),
        ) {
            if (sideBySide) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = GalvaDimens.ScreenPadding * 2, vertical = GalvaDimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(GalvaDimens.ScreenPadding * 2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GalvaIllustration(illustration = illustration, size = 160.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(GalvaDimens.ItemSpacing),
                    ) {
                        EmptyStateText(title, message, TextAlign.Start)
                        if (action != null) {
                            Box(modifier = Modifier.padding(top = GalvaDimens.ItemSpacing)) { action() }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(GalvaDimens.ScreenPadding * 2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GalvaDimens.ItemSpacing),
                ) {
                    GalvaIllustration(illustration = illustration)
                    EmptyStateText(title, message, TextAlign.Center)
                    if (action != null) {
                        Box(modifier = Modifier.padding(top = GalvaDimens.ItemSpacing)) { action() }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyStateText(title: String, message: String?, textAlign: TextAlign) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmallEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = textAlign,
    )
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
        )
    }
}

private val COMPACT_HEIGHT = 480.dp
