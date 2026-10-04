package com.abrarshakhi.galva.core.designsystem.component

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abrarshakhi.galva.core.designsystem.R
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.LottieDynamicProperty
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.model.KeyPath

enum class Illustration(@param:RawRes val resId: Int) {
    EmptyGallery(R.raw.galva_empty_gallery),
    Search(R.raw.galva_empty_search),
    EmptyAlbum(R.raw.galva_empty_album),
    Vault(R.raw.galva_vault),
}

@Composable
fun GalvaIllustration(
    illustration: Illustration,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    loop: Boolean = true,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(illustration.resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (loop) LottieConstants.IterateForever else 1,
    )
    val colorScheme = MaterialTheme.colorScheme
    val dynamicProperties = remember(colorScheme) { themedProperties(colorScheme) }

    LottieAnimation(
        composition = composition,
        progress = { progress },
        dynamicProperties = dynamicProperties,
        modifier = modifier.size(size),
    )
}

private fun themedProperties(colors: ColorScheme): LottieDynamicProperties {
    val roles = mapOf(
        "primary" to colors.primary,
        "onPrimary" to colors.onPrimary,
        "primaryContainer" to colors.primaryContainer,
        "secondary" to colors.secondary,
        "secondaryContainer" to colors.secondaryContainer,
        "tertiary" to colors.tertiary,
        "tertiaryContainer" to colors.tertiaryContainer,
    )
    return LottieDynamicProperties(
        roles.flatMap { (role, color) -> rolePaints(role, color) },
    )
}

private fun rolePaints(role: String, color: Color): List<LottieDynamicProperty<*>> {
    val keyPath = KeyPath("**", role, "**")
    val argb = color.toArgb()
    return listOf(
        LottieDynamicProperty(LottieProperty.COLOR, keyPath, argb),
        LottieDynamicProperty(LottieProperty.STROKE_COLOR, keyPath, argb),
    )
}
