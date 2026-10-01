package com.abrarshakhi.galva.common.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A draggable fast-scroller for a lazy grid.
 *
 * Position is tracked in **item indices**, not pixels. A timeline grid mixes full-width date
 * headers with square cells, so it has no uniform row height and no meaningful total pixel extent
 * to measure against; index-space is the only quantity that is both known and monotonic.
 *
 * The thumb's travel is applied in a layout-phase `offset` lambda rather than as a composed value,
 * so dragging re-lays-out the handle without recomposing it or the grid behind it.
 *
 * @param labelForIndex optional bubble shown while dragging — for the timeline, the date the
 * handle is currently over.
 */
@Composable
fun DraggableScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    labelForIndex: ((Int) -> String)? = null,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var trackHeightPx by remember { mutableIntStateOf(0) }

    // Derived so the grid's per-frame layout churn only wakes this handle when the values it
    // actually draws from change.
    val scrollableItems by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            (info.totalItemsCount - info.visibleItemsInfo.size).coerceAtLeast(0)
        }
    }
    val scrolledFraction by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val span = (info.totalItemsCount - info.visibleItemsInfo.size).coerceAtLeast(0)
            if (span == 0) 0f else (state.firstVisibleItemIndex.toFloat() / span).coerceIn(0f, 1f)
        }
    }

    // The handle stays put rather than fading out entirely. A handle that vanishes a second after
    // scrolling stops cannot be grabbed deliberately — you have to catch it — so it only dims.
    var active by remember { mutableStateOf(false) }
    LaunchedEffect(state.isScrollInProgress, dragging) {
        if (state.isScrollInProgress || dragging) {
            active = true
        } else {
            delay(IdleDelayMillis)
            active = false
        }
    }

    // The idle/active distinction is carried by colour now, not opacity: a surface-container fill
    // already recedes against photos, and dimming it further made it hard to find.
    val handleColor by animateColorAsState(
        targetValue = if (dragging) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(durationMillis = FadeMillis),
        label = "scrollbarColor",
    )

    val thumbTravelPx: () -> Float = {
        (trackHeightPx - with(density) { ThumbHeight.toPx() }).coerceAtLeast(0f)
    }
    val currentFraction: () -> Float = { if (dragging) dragFraction else scrolledFraction }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .onSizeChanged { trackHeightPx = it.height },
    ) {
        if (scrollableItems == 0) return@BoxWithConstraints

        if (labelForIndex != null && dragging) {
            val index = (currentFraction() * scrollableItems).roundToInt()
            ScrollLabel(
                text = labelForIndex(index),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, labelOffset(thumbTravelPx(), currentFraction(), density)) }
                    .padding(end = TouchWidth),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, (thumbTravelPx() * currentFraction()).roundToInt()) }
                .size(width = TouchWidth, height = ThumbHeight)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val travel = thumbTravelPx()
                        if (travel <= 0f) return@rememberDraggableState
                        dragFraction = (dragFraction + delta / travel).coerceIn(0f, 1f)
                        scope.launch {
                            state.scrollToItem((dragFraction * scrollableItems).roundToInt())
                        }
                    },
                    onDragStarted = {
                        // Adopt the current position so the handle does not jump under the finger.
                        dragFraction = scrolledFraction
                        dragging = true
                    },
                    onDragStopped = { dragging = false },
                )
                .semantics { contentDescription = "Scroll position" },
            // The handle sits flush against the edge; the grab strip extends inward past it.
            contentAlignment = Alignment.CenterEnd,
        ) {
            // A true semicircle: both start corners carry a radius equal to the width, so together
            // they span the full height. Rounding the *start* side rather than a fixed left keeps
            // it hugging the correct edge under RTL.
            Surface(
                modifier = Modifier.size(width = HandleWidth, height = ThumbHeight),
                shape = RoundedCornerShape(
                    topStart = HandleWidth,
                    topEnd = 0.dp,
                    bottomEnd = 0.dp,
                    bottomStart = HandleWidth,
                ),
                color = handleColor,
                // Lifts a pale chrome colour off an equally pale photo.
                shadowElevation = 6.dp,
                content = {},
            )
        }
    }
}

@Composable
private fun ScrollLabel(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Centres the bubble on the thumb rather than aligning their top edges. */
private fun labelOffset(
    travelPx: Float,
    fraction: Float,
    density: Density,
): Int {
    val thumbCentre = travelPx * fraction + with(density) { ThumbHeight.toPx() } / 2f
    val labelHalf = with(density) { LabelHeight.toPx() } / 2f
    return (thumbCentre - labelHalf).roundToInt()
}

private const val IdleDelayMillis = 1_200L
private const val FadeMillis = 180

/** Height is twice the width so the clipped start corners form an exact half circle. */
private val ThumbHeight = 48.dp
private val HandleWidth = 24.dp
private val TouchWidth = 40.dp
private val LabelHeight = 32.dp
