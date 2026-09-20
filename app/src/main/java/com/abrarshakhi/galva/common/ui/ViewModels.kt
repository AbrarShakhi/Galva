package com.abrarshakhi.galva.common.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Resolves a ViewModel against the Activity rather than the current navigation entry.
 *
 * Two things need this. Tabs are root swaps, so an entry-scoped ViewModel would be destroyed and
 * its scroll position and selection lost every time the user changes tab. And the top app bar is
 * rendered by the shared Scaffold, outside `NavDisplay`, so it can only reach a screen's state if
 * both resolve the same instance.
 */
@Composable
inline fun <reified T : ViewModel> appViewModel(): T {
    val activity = checkNotNull(LocalActivity.current as? ComponentActivity) {
        "appViewModel() must be called from within a ComponentActivity"
    }
    return koinViewModel(viewModelStoreOwner = activity)
}
