package com.abrarshakhi.galva.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent

object TopLevelMetadataKey : NavMetadataKey<Boolean>

fun topLevelMetadata(): Map<String, Any> = metadata { put(TopLevelMetadataKey, true) }

private val Scene<*>.isTopLevel: Boolean get() = metadata[TopLevelMetadataKey] == true

class NavTransitions(private val motion: MotionScheme) {

    fun <T : Any> forward(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        if (initialState.isTopLevel && targetState.isTopLevel) fadeThrough() else sharedAxisX(true)
    }

    fun <T : Any> pop(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        if (initialState.isTopLevel && targetState.isTopLevel) fadeThrough() else sharedAxisX(false)
    }

    fun <T : Any> predictivePop():
        AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform =
        { edge ->
            val towardEdge = if (edge == NavigationEvent.EDGE_RIGHT) -1 else 1
            (fadeIn(motion.defaultEffectsSpec()) +
                scaleIn(motion.defaultSpatialSpec(), initialScale = PEEK_SCALE)) togetherWith
                (scaleOut(motion.defaultSpatialSpec(), targetScale = PREDICTIVE_SCALE) +
                    slideOutHorizontally(motion.defaultSpatialSpec()) { it / 10 * towardEdge } +
                    fadeOut(motion.defaultEffectsSpec()))
        }

    fun immersiveMetadata(): Map<String, Any> =
        NavDisplay.transitionSpec { crossFade() } +
            NavDisplay.popTransitionSpec { crossFade() } +
            NavDisplay.predictivePopTransitionSpec { crossFade() }

    private fun crossFade(): ContentTransform =
        fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.defaultEffectsSpec())

    private fun fadeThrough(): ContentTransform =
        (fadeIn(motion.defaultEffectsSpec()) +
            scaleIn(motion.defaultSpatialSpec(), initialScale = PEEK_SCALE)) togetherWith
            fadeOut(motion.fastEffectsSpec())

    private fun sharedAxisX(forward: Boolean): ContentTransform {
        val direction = if (forward) 1 else -1
        val slide = motion.defaultSpatialSpec<IntOffset>()
        return (slideInHorizontally(slide) { it / SLIDE_FRACTION * direction } +
            fadeIn(motion.defaultEffectsSpec())) togetherWith
            (slideOutHorizontally(slide) { -it / SLIDE_FRACTION * direction } +
                fadeOut(motion.fastEffectsSpec()))
    }

    private companion object {
        const val SLIDE_FRACTION = 4
        const val PEEK_SCALE = 0.94f
        const val PREDICTIVE_SCALE = 0.9f
    }
}
