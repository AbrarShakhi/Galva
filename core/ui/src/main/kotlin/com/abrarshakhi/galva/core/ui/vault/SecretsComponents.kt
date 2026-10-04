package com.abrarshakhi.galva.core.ui.vault

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.abrarshakhi.galva.core.designsystem.layout.keepAboveKeyboard
import com.abrarshakhi.galva.core.designsystem.shape.MorphShape
import kotlin.math.roundToInt

@Composable
fun SecretsForm(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = FORM_MAX_WIDTH)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultEmblem(
    working: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
) {
    val motion = MaterialTheme.motionScheme
    val morph = remember { Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Circle) }
    val progress by animateFloatAsState(
        targetValue = if (working) 1f else 0f,
        animationSpec = motion.slowSpatialSpec(),
        label = "emblemMorph",
    )
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(working) {
        if (working) {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(EMBLEM_TURN_MILLIS, easing = LinearEasing),
                )
            }
        } else {
            val rest = (rotation.value / EMBLEM_SYMMETRY_DEGREES).roundToInt() * EMBLEM_SYMMETRY_DEGREES
            rotation.animateTo(rest, motion.slowSpatialSpec())
            rotation.snapTo(rest % 360f)
        }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.value }
                .clip(MorphShape(morph, progress.coerceIn(0f, 1f)))
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        AnimatedContent(
            targetState = working,
            transitionSpec = {
                (scaleIn(motion.defaultSpatialSpec(), initialScale = 0.6f) +
                    fadeIn(motion.defaultEffectsSpec())) togetherWith fadeOut(motion.fastEffectsSpec())
            },
            label = "emblemContent",
        ) { isWorking ->
            if (isWorking) {
                LoadingIndicator(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(size * 0.5f),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(size * 0.38f),
                )
            }
        }
    }
}

@Composable
fun Modifier.shakeOnError(error: String?, isWorking: Boolean): Modifier {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(error, isWorking) {
        if (error != null && !isWorking) {
            offset.snapTo(0f)
            offset.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = SHAKE_DAMPING, stiffness = Spring.StiffnessMedium),
                initialVelocity = SHAKE_VELOCITY,
            )
        }
    }
    return graphicsLayer { translationX = offset.value }
}

@Composable
fun RecoveryPhraseField(
    state: TextFieldState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Recovery phrase",
    singleLine: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
) {
    OutlinedTextField(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .keepAboveKeyboard()
            .semantics { isSensitiveData = true },
        enabled = enabled,
        label = { Text(label) },
        shape = MaterialTheme.shapes.large,
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine
        else TextFieldLineLimits.MultiLine(minHeightInLines = RECOVERY_FIELD_LINES),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
    )
}

@Composable
fun ErrorText(error: String?) {
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = error != null,
        enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
        exit = shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkingButton(
    label: String,
    isWorking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = ButtonDefaults.MediumContainerHeight
    Button(
        onClick = onClick,
        enabled = !isWorking,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(height),
        modifier = modifier.heightIn(min = height),
    ) {
        if (isWorking) {
            LoadingIndicator(
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(height) * 1.4f),
            )
        } else {
            Text(label, style = ButtonDefaults.textStyleFor(height))
        }
    }
}

private val FORM_MAX_WIDTH = 520.dp
private const val EMBLEM_TURN_MILLIS = 2_400
private const val EMBLEM_SYMMETRY_DEGREES = 30f
private const val SHAKE_DAMPING = 0.2f
private const val RECOVERY_FIELD_LINES = 3
private const val SHAKE_VELOCITY = 2_400f
