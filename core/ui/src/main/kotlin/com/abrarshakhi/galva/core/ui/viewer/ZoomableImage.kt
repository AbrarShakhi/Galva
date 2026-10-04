package com.abrarshakhi.galva.core.ui.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.abrarshakhi.galva.core.ui.transition.sharedMediaBounds
import kotlin.math.abs

@Composable
fun ZoomableImage(
    uri: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    sharedKey: String? = null,
    placeholderKey: String? = null,
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
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val multiTouch = event.changes.count { it.pressed } > 1
                            if (multiTouch || scale > 1f) {
                                val pan = event.calculatePan()
                                val nextScale = (scale * event.calculateZoom()).coerceIn(MIN_SCALE, MAX_SCALE)
                                val nextOffset = clampOffset(
                                    if (nextScale > 1f) offset + pan else Offset.Zero,
                                    nextScale,
                                )
                                val pushingPastEdge = !multiTouch &&
                                    abs(pan.x) > abs(pan.y) &&
                                    nextOffset.x == offset.x
                                if (!pushingPastEdge) {
                                    scale = nextScale
                                    offset = nextOffset
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .placeholderMemoryCacheKey(placeholderKey)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .sharedMediaBounds(sharedKey)
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
