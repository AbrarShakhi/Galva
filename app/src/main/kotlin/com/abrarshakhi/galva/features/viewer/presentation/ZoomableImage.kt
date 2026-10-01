package com.abrarshakhi.galva.features.viewer.presentation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlin.math.abs

/**
 * Pinch-to-zoom photo with panning bounded to the scaled image.
 *
 * The gesture handler only consumes horizontal drags while zoomed in; at rest it lets them through
 * so the enclosing pager keeps its swipe.
 */
@Composable
fun ZoomableImage(
    uri: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()

        var scale by remember(uri) { mutableFloatStateOf(1f) }
        var offset by remember(uri) { mutableStateOf(Offset.Zero) }

        fun clampOffset(candidate: Offset, atScale: Float): Offset {
            val maxX = (viewportWidth * (atScale - 1f) / 2f).coerceAtLeast(0f)
            val maxY = (viewportHeight * (atScale - 1f) / 2f).coerceAtLeast(0f)
            return Offset(
                x = candidate.x.coerceIn(-maxX, maxX),
                y = candidate.y.coerceIn(-maxY, maxY),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uri) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { tap ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = DOUBLE_TAP_SCALE
                                // Zoom toward the point that was tapped.
                                val focus = Offset(
                                    x = (viewportWidth / 2f - tap.x) * (DOUBLE_TAP_SCALE - 1f),
                                    y = (viewportHeight / 2f - tap.y) * (DOUBLE_TAP_SCALE - 1f),
                                )
                                offset = clampOffset(focus, DOUBLE_TAP_SCALE)
                            }
                        },
                    )
                }
                .pointerInput(uri) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        val nextOffset = if (nextScale > 1f) offset + pan else Offset.Zero
                        scale = nextScale
                        offset = clampOffset(nextOffset, nextScale)
                    }
                },
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
