package com.abrarshakhi.galva.common.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("UnusedBoxWithConstraintsScope")
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

    var active by remember { mutableStateOf(false) }
    LaunchedEffect(state.isScrollInProgress, dragging) {
        if (state.isScrollInProgress || dragging) {
            active = true
        } else {
            delay(IdleDelayMillis.milliseconds)
            active = false
        }
    }

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
                    .offset {
                        IntOffset(
                            0, labelOffset(thumbTravelPx(), currentFraction(), density)
                        )
                    }
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
                        dragFraction = scrolledFraction
                        dragging = true
                    },
                    onDragStopped = { dragging = false },
                )
                .semantics { contentDescription = "Scroll position" },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Surface(
                modifier = Modifier.size(width = HandleWidth, height = ThumbHeight),
                shape = RoundedCornerShape(
                    topStart = HandleWidth,
                    topEnd = 0.dp,
                    bottomEnd = 0.dp,
                    bottomStart = HandleWidth,
                ),
                color = handleColor,
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

private val ThumbHeight = 48.dp
private val HandleWidth = 24.dp
private val TouchWidth = 40.dp
private val LabelHeight = 32.dp
