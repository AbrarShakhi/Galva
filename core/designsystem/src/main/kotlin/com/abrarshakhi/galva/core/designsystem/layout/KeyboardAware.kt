package com.abrarshakhi.galva.core.designsystem.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun Modifier.keepAboveKeyboard(spaceBelow: Dp = KEYBOARD_CLEARANCE): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget

    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        snapshotFlow { ime.getBottom(density) to imeTarget.getBottom(density) }
            .filter { (current, target) -> target > 0 && current == target }
            .collect {
                val clearance = with(density) { spaceBelow.toPx() }
                requester.bringIntoView(
                    Rect(0f, 0f, size.width.toFloat(), size.height + clearance),
                )
            }
    }

    return this
        .bringIntoViewRequester(requester)
        .onSizeChanged { size = it }
        .onFocusChanged { focused = it.hasFocus }
}

private val KEYBOARD_CLEARANCE = 96.dp
