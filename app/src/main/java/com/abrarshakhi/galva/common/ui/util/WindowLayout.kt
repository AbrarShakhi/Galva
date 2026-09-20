package com.abrarshakhi.galva.common.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where the tab navigation belongs for the current window.
 *
 * A bottom bar costs the same ~70dp whatever the window is. That is a fair price in portrait and a
 * bad one in landscape, where it plus the app bar can take over a third of the height and leave a
 * gallery showing barely a row of photos. On a short or wide window the navigation moves to the
 * leading edge instead, where the space it takes is the space there is most of.
 */
enum class ChromeLayout {
    BottomBar,
    Rail,
}

fun chromeLayoutFor(windowWidth: Dp, windowHeight: Dp): ChromeLayout = when {
    // Wide enough that a full-width bottom bar would strand its tabs in the middle of nowhere.
    windowWidth >= ExpandedWidth -> ChromeLayout.Rail
    // Short and landscape: vertical space is the scarce one.
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
