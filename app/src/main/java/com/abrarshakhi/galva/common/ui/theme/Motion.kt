package com.abrarshakhi.galva.common.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Ente's navigation motion: `Curves.easeOutExpo` over 200ms — a fast start that settles without
 * overshoot, which keeps a tab change feeling immediate rather than springy.
 */
val EaseOutExpo: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

const val NavTransitionMillis = 200
