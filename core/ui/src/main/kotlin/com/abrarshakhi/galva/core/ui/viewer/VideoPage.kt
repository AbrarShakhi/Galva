package com.abrarshakhi.galva.core.ui.viewer

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberMuteButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPlaybackSpeedState
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import androidx.media3.ui.compose.state.rememberRepeatButtonState
import androidx.media3.ui.compose.state.rememberSeekBackButtonState
import androidx.media3.ui.compose.state.rememberSeekForwardButtonState
import com.abrarshakhi.galva.core.designsystem.layout.WindowLayout
import com.abrarshakhi.galva.core.ui.util.formatDuration
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPage(
    player: Player,
    controlsVisible: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    controlsPadding: PaddingValues = PaddingValues(),
) {
    val presentationState = rememberPresentationState(player)
    val playPause = rememberPlayPauseButtonState(player)
    val seekBack = rememberSeekBackButtonState(player)
    val seekForward = rememberSeekForwardButtonState(player)
    val currentOnTap by rememberUpdatedState(onTap)

    var interactions by remember { mutableIntStateOf(0) }
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }

    val currentControlsVisible by rememberUpdatedState(controlsVisible)
    LaunchedEffect(playPause.showPlay) {
        if (playPause.showPlay && !currentControlsVisible) currentOnTap()
    }

    LaunchedEffect(controlsVisible, playPause.showPlay, interactions) {
        if (controlsVisible && !playPause.showPlay) {
            delay(CONTROLS_TIMEOUT_MS)
            currentOnTap()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(player) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onDoubleTap = { offset ->
                        val forward = offset.x > size.width / 2
                        if (forward && seekForward.isEnabled) seekForward.onClick()
                        if (!forward && seekBack.isEnabled) seekBack.onClick()
                        seekFeedback = SeekFeedback(forward, (seekFeedback?.serial ?: 0) + 1)
                        interactions++
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.resizeWithContentScale(
                contentScale = ContentScale.Fit,
                sourceSizeDp = presentationState.videoSizeDp,
            ),
        )

        if (presentationState.coverSurface) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            )
        }

        SeekFeedbackBubble(
            feedback = seekFeedback,
            onFinished = { seekFeedback = null },
            amountMs = if (seekFeedback?.forward == true) seekForward.seekForwardAmountMs
            else seekBack.seekBackAmountMs,
        )

        val motion = MaterialTheme.motionScheme
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.8f),
            exit = fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 0.8f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundControl(
                    icon = Icons.Rounded.Replay10,
                    label = "Back ${seekBack.seekBackAmountMs / 1_000} seconds",
                    enabled = seekBack.isEnabled,
                    onClick = {
                        seekBack.onClick()
                        interactions++
                    },
                )
                PlayPauseControl(
                    showPlay = playPause.showPlay,
                    enabled = playPause.isEnabled,
                    onClick = {
                        playPause.onClick()
                        interactions++
                    },
                )
                RoundControl(
                    icon = Icons.Rounded.Forward10,
                    label = "Forward ${seekForward.seekForwardAmountMs / 1_000} seconds",
                    enabled = seekForward.isEnabled,
                    onClick = {
                        seekForward.onClick()
                        interactions++
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(motion.defaultSpatialSpec()) { it / 2 } + fadeIn(motion.defaultEffectsSpec()),
            exit = slideOutVertically(motion.fastSpatialSpec()) { it / 2 } + fadeOut(motion.fastEffectsSpec()),
        ) {
            BottomControls(
                player = player,
                contentPadding = controlsPadding,
                onInteraction = { interactions++ },
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun BottomControls(
    player: Player,
    contentPadding: PaddingValues,
    onInteraction: () -> Unit,
) {
    val progress = rememberProgressStateWithTickInterval(player, PROGRESS_TICK_MS)
    val mute = rememberMuteButtonState(player)
    val repeat = rememberRepeatButtonState(player, listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ONE))
    val speed = rememberPlaybackSpeedState(player)

    val duration = progress.durationMs.coerceAtLeast(0L)
    val trackEnd = duration.toFloat().coerceAtLeast(1f)
    val slider = remember(trackEnd) { SliderState(trackRange = 0f..trackEnd) }
    var scrubbing by remember { mutableStateOf(false) }
    if (!scrubbing) slider.value = progress.currentPositionMs.toFloat().coerceIn(0f, trackEnd)
    val position = if (scrubbing) slider.value.toLong() else progress.currentPositionMs

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, BOTTOM_SCRIM)))
            .padding(contentPadding)
            .navigationBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = 16.dp),
    ) {
        Slider(
            state = slider,
            onValueChange = {
                scrubbing = true
                slider.value = it
                onInteraction()
            },
            onValueChangeFinished = {
                player.seekTo(slider.value.toLong())
                scrubbing = false
                onInteraction()
            },
            enabled = duration > 0,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${formatDuration(position)} / ${formatDuration(duration)}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            SpeedMenu(
                speed = speed.playbackSpeed,
                enabled = speed.isEnabled,
                onSpeedSelected = {
                    speed.updatePlaybackSpeed(it)
                    onInteraction()
                },
            )
            IconToggleButton(
                checked = repeat.repeatModeState == Player.REPEAT_MODE_ONE,
                onCheckedChange = {
                    repeat.onClick()
                    onInteraction()
                },
                enabled = repeat.isEnabled,
            ) {
                Icon(
                    imageVector = if (repeat.repeatModeState == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                    else Icons.Rounded.Repeat,
                    contentDescription = "Loop video",
                    tint = if (repeat.repeatModeState == Player.REPEAT_MODE_ONE) MaterialTheme.colorScheme.primary
                    else Color.White,
                )
            }
            IconButton(
                onClick = {
                    mute.onClick()
                    onInteraction()
                },
                enabled = mute.isEnabled,
            ) {
                Icon(
                    imageVector = if (mute.showMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (mute.showMuted) "Unmute" else "Mute",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SpeedMenu(speed: Float, enabled: Boolean, onSpeedSelected: (Float) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, enabled = enabled) {
            Text(speedLabel(speed), color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.large,
        ) {
            SPEEDS.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = speedLabel(option),
                            color = if (option == speed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        open = false
                        onSpeedSelected(option)
                    },
                )
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayPauseControl(showPlay: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.size(PLAY_BUTTON_SIZE),
    ) {
        AnimatedContent(
            targetState = showPlay,
            transitionSpec = {
                (scaleIn(motion.fastSpatialSpec(), initialScale = 0.5f) + fadeIn(motion.fastEffectsSpec())) togetherWith
                    (scaleOut(motion.fastSpatialSpec(), targetScale = 0.5f) + fadeOut(motion.fastEffectsSpec()))
            },
            label = "playPause",
        ) { play ->
            Icon(
                imageVector = if (play) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (play) "Play" else "Pause",
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RoundControl(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = CONTROL_SCRIM,
            contentColor = Color.White,
        ),
        modifier = Modifier.size(56.dp),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SeekFeedbackBubble(feedback: SeekFeedback?, onFinished: () -> Unit, amountMs: Long) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(feedback?.serial) {
        if (feedback != null) {
            delay(SEEK_FEEDBACK_MS)
            currentOnFinished()
        }
    }
    val motion = MaterialTheme.motionScheme
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = feedback != null,
            modifier = Modifier
                .align(if (feedback?.forward == true) Alignment.CenterEnd else Alignment.CenterStart)
                .offset(y = -SEEK_FEEDBACK_LIFT)
                .padding(horizontal = 32.dp),
            enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
            exit = fadeOut(motion.defaultEffectsSpec()),
        ) {
            Surface(shape = CircleShape, color = CONTROL_SCRIM, contentColor = Color.White) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val forward = feedback?.forward == true
                    Icon(
                        imageVector = if (forward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10,
                        contentDescription = null,
                    )
                    Text(
                        text = (if (forward) "+" else "−") + "${amountMs / 1_000}s",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

fun videoControlsPadding(layout: WindowLayout): PaddingValues =
    if (layout == WindowLayout.Rail) PaddingValues(end = TOOLBAR_CLEARANCE)
    else PaddingValues(bottom = TOOLBAR_CLEARANCE)

fun Modifier.viewerToolbarPadding(): Modifier = this
    .navigationBarsPadding()
    .displayCutoutPadding()
    .padding(16.dp)

private val TOOLBAR_CLEARANCE = 88.dp
const val VIDEO_SEEK_INCREMENT_MS = 10_000L

private data class SeekFeedback(val forward: Boolean, val serial: Int)

private fun speedLabel(speed: Float): String {
    val text = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "${text}×"
}

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private val PLAY_BUTTON_SIZE = 88.dp
private val SEEK_FEEDBACK_LIFT = 96.dp
private val CONTROL_SCRIM = Color(0x66000000)
private val BOTTOM_SCRIM = Color(0xB3000000)
private const val CONTROLS_TIMEOUT_MS = 3_500L
private const val PROGRESS_TICK_MS = 250L
private const val SEEK_FEEDBACK_MS = 700L
