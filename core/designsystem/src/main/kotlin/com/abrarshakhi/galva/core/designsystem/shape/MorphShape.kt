package com.abrarshakhi.galva.core.designsystem.shape

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

class MorphShape(private val morph: Morph, private val progress: Float) : Shape {

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress)
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}
