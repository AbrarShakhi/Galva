package com.abrarshakhi.galva.features.viewer.presentation

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState

/**
 * Renders the shared player onto this page.
 *
 * The [Player] is owned by the viewer, not by the page: one instance is moved between video pages
 * so that swiping through a folder of clips does not allocate a decoder per page.
 *
 * A texture view is used rather than a surface view because this sits inside a pager — a surface
 * view punches through the window and shows a black rectangle mid-swipe.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPage(
    player: Player,
    controlsVisible: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentationState = rememberPresentationState(player)
    val playPauseState = rememberPlayPauseButtonState(player)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            // Without this the surface stretches to fill the page. `videoSizeDp` is null until the
            // first frame is decoded, at which point the surface is re-measured to the real ratio.
            modifier = Modifier.resizeWithContentScale(
                contentScale = ContentScale.Fit,
                sourceSizeDp = presentationState.videoSizeDp,
            ),
        )

        // Between media items the surface still holds the previous frame; hide it until the new
        // one is ready rather than flashing the wrong video.
        if (presentationState.coverSurface) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            Icon(
                imageVector = if (playPauseState.showPlay) Icons.Filled.PlayArrow
                else Icons.Filled.Pause,
                contentDescription = if (playPauseState.showPlay) "Play" else "Pause",
                tint = Color.White,
                modifier = Modifier
                    .size(64.dp)
                    .background(SCRIM, CircleShape)
                    .clickable(enabled = playPauseState.isEnabled) { playPauseState.onClick() },
            )
        }
    }
}

private val SCRIM = Color(0x66000000)
