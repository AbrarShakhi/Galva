package com.abrarshakhi.galva.common.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ChromeLayout { BottomBar, Rail }

fun chromeLayoutFor(windowWidth: Dp, windowHeight: Dp): ChromeLayout = when {
    windowWidth >= ExpandedWidth -> ChromeLayout.Rail
    windowHeight < CompactHeight && windowWidth > windowHeight -> ChromeLayout.Rail
    else -> ChromeLayout.BottomBar
}

@Composable
fun rememberChromeLayout(): ChromeLayout {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val width = with(density) { size.width.toDp() }
    val height = with(density) { size.height.toDp() }
    return remember(width, height) { chromeLayoutFor(width, height) }
}

private val ExpandedWidth = 600.dp
private val CompactHeight = 480.dp
