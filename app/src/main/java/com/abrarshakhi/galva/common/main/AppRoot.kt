package com.abrarshakhi.galva.common.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.abrarshakhi.galva.common.navigation.AppNavigation
import com.abrarshakhi.galva.common.navigation.AppRouteKey
import com.abrarshakhi.galva.common.navigation.currentRoute
import com.abrarshakhi.galva.common.navigation.rememberAppBackStack
import com.abrarshakhi.galva.common.navigation.switchTabTo
import kotlinx.coroutines.launch

@Composable
fun AppRoot(startRoute: AppRouteKey, mainAppViewModel: MainAppViewModel) {
    AppShell(startRoute = startRoute)
}

@Composable
private fun AppShell(startRoute: AppRouteKey) {
    val backStack = rememberAppBackStack(startRoute)
    val current = backStack.currentRoute()
    val chrome = current?.chrome()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    val chromeScope = ChromeScope(
        backStack = backStack,
        currentRoute = current,
        scrollBehavior = scrollBehavior,
        openDrawer = { coroutineScope.launch { drawerState.open() } },
        switchTab = backStack::switchTabTo,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { chrome?.topBar?.invoke(chromeScope) },
        floatingActionButton = { chrome?.fab?.invoke(chromeScope) },
    ) { innerPadding ->
        AppNavigation(backStack = backStack, modifier = Modifier.padding(innerPadding))
    }
}
