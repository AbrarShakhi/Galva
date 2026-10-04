package com.abrarshakhi.galva.feature.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GalleryTopBar(
    itemCount: Int,
    isSyncing: Boolean,
    onOpenSettings: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val motion = MaterialTheme.motionScheme
    TopAppBar(
        title = { Text("Photos", style = MaterialTheme.typography.titleLargeEmphasized) },
        subtitle = {
            if (itemCount > 0) Text(if (itemCount == 1) "1 item" else "$itemCount items")
        },
        actions = {
            AnimatedVisibility(
                visible = isSyncing,
                enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
                exit = scaleOut(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
            ) {
                LoadingIndicator(modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
