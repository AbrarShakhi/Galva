package com.abrarshakhi.galva.common.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
inline fun <reified T : ViewModel> appViewModel(): T {
    val activity = checkNotNull(LocalActivity.current as? ComponentActivity) {
        "appViewModel() must be called from within a ComponentActivity"
    }
    return koinViewModel(viewModelStoreOwner = activity)
}
