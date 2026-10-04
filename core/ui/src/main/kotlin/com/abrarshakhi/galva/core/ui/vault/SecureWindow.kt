package com.abrarshakhi.galva.core.ui.vault

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import java.util.WeakHashMap

@Composable
fun SecureWindow() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        SecureWindowRequests.acquire(window)
        onDispose { SecureWindowRequests.release(window) }
    }
}

private object SecureWindowRequests {

    private val holders = WeakHashMap<Window, Int>()

    fun acquire(window: Window) {
        val count = holders[window] ?: 0
        if (count == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        holders[window] = count + 1
    }

    fun release(window: Window) {
        val count = holders[window] ?: return
        if (count <= 1) {
            holders.remove(window)
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            holders[window] = count - 1
        }
    }
}
