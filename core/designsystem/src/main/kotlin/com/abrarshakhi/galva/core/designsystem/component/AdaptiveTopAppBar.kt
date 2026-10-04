package com.abrarshakhi.galva.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abrarshakhi.galva.core.designsystem.layout.rememberIsCompactHeight

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdaptiveTopAppBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    when {
        !rememberIsCompactHeight() -> LargeFlexibleTopAppBar(
            title = title,
            subtitle = subtitle,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )

        subtitle != null -> TopAppBar(
            title = title,
            subtitle = subtitle,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )

        else -> TopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )
    }
}
