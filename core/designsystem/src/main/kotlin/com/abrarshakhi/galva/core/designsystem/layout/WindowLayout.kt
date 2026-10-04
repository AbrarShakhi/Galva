package com.abrarshakhi.galva.core.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class WindowLayout {
    BottomBar, Rail,
}

fun windowLayoutFor(windowWidth: Dp, windowHeight: Dp): WindowLayout = when {
    windowWidth >= ExpandedWidth -> WindowLayout.Rail
    windowHeight < CompactHeight && windowWidth > windowHeight -> WindowLayout.Rail
    else -> WindowLayout.BottomBar
}

@Composable
fun rememberWindowLayout(): WindowLayout {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val width = with(density) { size.width.toDp() }
    val height = with(density) { size.height.toDp() }
    return remember(width, height) { windowLayoutFor(width, height) }
}

@Composable
fun rememberWindowSize(): DpSize {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return remember(size, density) { with(density) { DpSize(size.width.toDp(), size.height.toDp()) } }
}

@Composable
fun rememberIsCompactHeight(): Boolean = rememberWindowSize().height < CompactHeight

@Composable
fun ReadableWidth(content: @Composable () -> Unit) {
    Box(modifier = Modifier.widthIn(max = ReadableContentWidth)) { content() }
}

private val ReadableContentWidth = 640.dp
private val ExpandedWidth = 600.dp
private val CompactHeight = 480.dp
