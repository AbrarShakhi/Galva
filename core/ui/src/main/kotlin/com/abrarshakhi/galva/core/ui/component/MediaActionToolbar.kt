package com.abrarshakhi.galva.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abrarshakhi.galva.core.designsystem.layout.WindowLayout
import com.abrarshakhi.galva.core.ui.actions.MediaAction

data class ToolbarAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaActionToolbar(
    visible: Boolean,
    isFavorite: Boolean,
    onAction: (MediaAction) -> Unit,
    modifier: Modifier = Modifier,
    layout: WindowLayout = WindowLayout.BottomBar,
    extraActions: List<ToolbarAction> = emptyList(),
) {
    val motion = MaterialTheme.motionScheme
    val vertical = layout == WindowLayout.Rail
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (vertical) {
            slideInHorizontally(motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec())
        } else {
            slideInVertically(motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec())
        },
        exit = if (vertical) {
            slideOutHorizontally(motion.fastSpatialSpec()) { it } + fadeOut(motion.fastEffectsSpec())
        } else {
            slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut(motion.fastEffectsSpec())
        },
    ) {
        val colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
        val fab: @Composable () -> Unit = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = { onAction(MediaAction.Delete) },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete")
            }
        }
        val actions: @Composable () -> Unit = {
            ToolbarIconButton(Icons.Rounded.Share, "Share") { onAction(MediaAction.Share) }
            FavoriteButton(isFavorite) { onAction(MediaAction.Favorite) }
            ToolbarIconButton(Icons.Rounded.LibraryAdd, "Add to album") {
                onAction(MediaAction.AddToAlbum)
            }
            ToolbarIconButton(Icons.Rounded.Lock, "Move to Secrets") {
                onAction(MediaAction.MoveToSecrets)
            }
            extraActions.forEach { ToolbarIconButton(it.icon, it.label, it.onClick) }
        }
        if (vertical) {
            VerticalFloatingToolbar(expanded = true, floatingActionButton = fab, colors = colors) {
                actions()
            }
        } else {
            HorizontalFloatingToolbar(expanded = true, floatingActionButton = fab, colors = colors) {
                actions()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    val label = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
    ToolbarTooltip(label) {
        IconButton(onClick = onClick) {
            AnimatedContent(
                targetState = isFavorite,
                transitionSpec = {
                    (scaleIn(motion.fastSpatialSpec(), initialScale = 0.4f) +
                        fadeIn(motion.fastEffectsSpec())) togetherWith
                        fadeOut(motion.fastEffectsSpec())
                },
                label = "favorite",
            ) { favorite ->
                Icon(
                    imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = label,
                )
            }
        }
    }
}

@Composable
fun ToolbarIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    ToolbarTooltip(label) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarTooltip(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content,
    )
}

@Composable
fun rememberToolbarAwarePadding(
    toolbarVisible: Boolean,
    layout: WindowLayout,
    extraBottom: Dp = 0.dp,
): PaddingValues {
    val motion = MaterialTheme.motionScheme
    val bottom by animateDpAsState(
        targetValue = if (toolbarVisible && layout == WindowLayout.BottomBar) ToolbarClearance else 0.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "toolbarBottomClearance",
    )
    val end by animateDpAsState(
        targetValue = if (toolbarVisible && layout == WindowLayout.Rail) ToolbarClearance else 0.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "toolbarEndClearance",
    )
    return PaddingValues(
        end = end.coerceAtLeast(0.dp),
        bottom = extraBottom + bottom.coerceAtLeast(0.dp),
    )
}

fun toolbarAlignment(layout: WindowLayout): Alignment =
    if (layout == WindowLayout.Rail) Alignment.CenterEnd else Alignment.BottomCenter

private val ToolbarClearance = 104.dp
