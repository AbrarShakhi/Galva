package com.abrarshakhi.galva.core.ui.transition

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

fun sharedMediaKey(id: Long, namespace: String = "media"): String = "$namespace-$id"

fun thumbnailCacheKey(uri: String): String = "$uri#thumbnail"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedMediaBounds(key: String?): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    if (key == null || sharedScope == null) return this
    val animatedScope = LocalNavAnimatedContentScope.current
    val motion = MaterialTheme.motionScheme
    return with(sharedScope) {
        this@sharedMediaBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = { _, _ -> motion.defaultSpatialSpec() },
            resizeMode = RemeasureToBounds,
        )
    }
}
