package com.abrarshakhi.galva.common.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.abrarshakhi.galva.common.navigation.AppNavigation
import com.abrarshakhi.galva.common.navigation.AppRouteKey
import com.abrarshakhi.galva.common.navigation.currentRoute
import com.abrarshakhi.galva.common.navigation.isTopLevel
import com.abrarshakhi.galva.common.navigation.rememberAppBackStack
import com.abrarshakhi.galva.common.navigation.switchTabTo
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import com.abrarshakhi.galva.common.ui.component.MediaPermissionGate
import com.abrarshakhi.galva.common.ui.util.ChromeLayout
import com.abrarshakhi.galva.common.ui.util.rememberChromeLayout
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
import com.abrarshakhi.galva.features.settings.presentation.SettingsDrawerContent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil3.SingletonImageLoader
import com.abrarshakhi.galva.core.vault.domain.model.VaultState
import com.abrarshakhi.galva.core.vault.domain.repository.VaultRepository
import com.abrarshakhi.galva.core.vault.ui.purgeVaultImages
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppRoot(startRoute: AppRouteKey, mainAppViewModel: MainAppViewModel) {
    VaultLifecycle()
    MediaPermissionGate(onAccessChanged = mainAppViewModel::onAccessChanged) {
        AppShell(startRoute = startRoute)
    }
}

@Composable
private fun VaultLifecycle() {
    val vault: VaultRepository = koinInject()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vault.lock() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vault.cancelPendingLock() }

    LaunchedEffect(vault) {
        vault.state.collect { state ->
            if (state !is VaultState.Unlocked) SingletonImageLoader.get(context).purgeVaultImages()
        }
    }
}

@Composable
private fun AppShell(startRoute: AppRouteKey) {
    val backStack = rememberAppBackStack(startRoute)
    val current = backStack.currentRoute()
    val chrome = current?.chrome()

    val layout = rememberChromeLayout()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarDispatcher: SnackbarDispatcher = koinInject()
    LaunchedEffect(snackbarDispatcher) {
        snackbarDispatcher.messages.collect { message ->
            snackbarHostState.showSnackbar(
                message = message.text,
                withDismissAction = message.duration != SnackbarDuration.Short,
                duration = message.duration,
            )
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    // From a secondary tab, back returns to the timeline instead of leaving the app.
    BackHandler(enabled = !drawerState.isOpen && current != null && current.isTopLevel && current != AppRouteKey.Gallery) {
        backStack.switchTabTo(AppRouteKey.Gallery)
    }

    LaunchedEffect(current) {
        scrollBehavior.state.contentOffset = 0f
        scrollBehavior.state.heightOffset = 0f
    }

    val chromeScope = ChromeScope(
        backStack = backStack,
        currentRoute = current,
        scrollBehavior = scrollBehavior,
        openDrawer = { coroutineScope.launch { drawerState.open() } },
        switchTab = backStack::switchTabTo,
        layout = layout,
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = current == AppRouteKey.Gallery || drawerState.isOpen,
        drawerContent = { SettingsDrawerContent() },
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { chrome?.topBar?.invoke(chromeScope) },
            bottomBar = {
                if (layout == ChromeLayout.BottomBar) chrome?.navigation?.invoke(chromeScope)
            },
            floatingActionButton = { chrome?.fab?.invoke(chromeScope) },
        ) { innerPadding ->
            val immersive = chrome?.immersive == true
            val contentModifier =
                if (immersive) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(innerPadding)

            if (layout == ChromeLayout.Rail && !immersive) {
                // The rail sits beside the content rather than above it, so the top app bar keeps
                // the full width and only the navigation moves.
                Row(modifier = contentModifier) {
                    chrome?.navigation?.invoke(chromeScope)
                    AppNavigation(
                        backStack = backStack,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                AppNavigation(backStack = backStack, modifier = contentModifier)
            }
        }
    }
}
