package com.abrarshakhi.galva.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import coil3.SingletonImageLoader
import com.abrarshakhi.galva.core.designsystem.layout.WindowLayout
import com.abrarshakhi.galva.core.designsystem.layout.rememberWindowLayout
import com.abrarshakhi.galva.core.model.VaultState
import com.abrarshakhi.galva.core.ui.message.LocalUserMessageHost
import com.abrarshakhi.galva.core.ui.message.rememberUserMessageHost
import com.abrarshakhi.galva.core.ui.transition.LocalSharedTransitionScope
import com.abrarshakhi.galva.core.vault.VaultRepository
import com.abrarshakhi.galva.core.vault.media.purgeVaultImages
import com.abrarshakhi.galva.navigation.NavTransitions
import com.abrarshakhi.galva.navigation.Navigator
import com.abrarshakhi.galva.navigation.TOP_LEVEL_ROUTES
import com.abrarshakhi.galva.navigation.appEntryProvider
import com.abrarshakhi.galva.navigation.isTopLevel
import com.abrarshakhi.galva.navigation.rememberNavigationState
import org.koin.compose.koinInject

@Composable
fun AppRoot(startRoute: NavKey, mainAppViewModel: MainAppViewModel) {
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AppShell(startRoute: NavKey) {
    val navigationState = rememberNavigationState(
        startRoute = startRoute,
        topLevelRoutes = TOP_LEVEL_ROUTES,
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val motion = MaterialTheme.motionScheme
    val transitions = remember(motion) { NavTransitions(motion) }
    val entryProvider = remember(navigator, transitions) { appEntryProvider(navigator, transitions) }

    val layout = rememberWindowLayout()
    val userMessageHost = rememberUserMessageHost()
    val showNavigation = navigationState.currentRoute.isTopLevel
    val showBottomBar = showNavigation && layout == WindowLayout.BottomBar
    val showRail = showNavigation && layout == WindowLayout.Rail

    CompositionLocalProvider(LocalUserMessageHost provides userMessageHost) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = {
                SnackbarHost(
                    hostState = userMessageHost.snackbarHostState,
                    modifier = if (showBottomBar) Modifier else Modifier.navigationBarsPadding(),
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = expandVertically(motion.defaultSpatialSpec()) +
                        fadeIn(motion.defaultEffectsSpec()),
                    exit = shrinkVertically(motion.fastSpatialSpec()) +
                        fadeOut(motion.fastEffectsSpec()),
                ) {
                    AppNavigationBar(
                        selectedRoute = navigationState.topLevelRoute,
                        onSelect = { navigator.navigate(it.route) },
                    )
                }
            },
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                AnimatedVisibility(
                    visible = showRail,
                    enter = expandHorizontally(motion.defaultSpatialSpec()) +
                        fadeIn(motion.defaultEffectsSpec()),
                    exit = shrinkHorizontally(motion.fastSpatialSpec()) +
                        fadeOut(motion.fastEffectsSpec()),
                ) {
                    AppNavigationRail(
                        selectedRoute = navigationState.topLevelRoute,
                        onSelect = { navigator.navigate(it.route) },
                    )
                }
                SharedTransitionLayout(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (showRail) {
                                Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavDisplay(
                            entries = navigationState.toDecoratedEntries(entryProvider),
                            onBack = navigator::goBack,
                            transitionSpec = transitions.forward(),
                            popTransitionSpec = transitions.pop(),
                            predictivePopTransitionSpec = transitions.predictivePop(),
                        )
                    }
                }
            }
        }
    }
}
