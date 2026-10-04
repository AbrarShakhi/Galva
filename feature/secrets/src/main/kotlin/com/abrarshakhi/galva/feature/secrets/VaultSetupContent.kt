package com.abrarshakhi.galva.feature.secrets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.designsystem.component.ListGroup
import com.abrarshakhi.galva.core.designsystem.component.ListGroupItem
import com.abrarshakhi.galva.core.ui.vault.ErrorText
import com.abrarshakhi.galva.core.ui.vault.PassphraseField
import com.abrarshakhi.galva.core.ui.vault.PassphraseRules
import com.abrarshakhi.galva.core.ui.vault.RecoveryPhraseField
import com.abrarshakhi.galva.core.ui.vault.SecretsForm
import com.abrarshakhi.galva.core.ui.vault.VaultEmblem
import com.abrarshakhi.galva.core.ui.vault.WorkingButton
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultSetupContent(modifier: Modifier = Modifier) {
    val viewModel: VaultSetupViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motion = MaterialTheme.motionScheme

    BackHandler(enabled = state.step != SetupStep.Intro && !state.isCreating) {
        viewModel.onIntent(VaultSetupIntent.Back)
    }

    Column(modifier = modifier.fillMaxSize()) {
        val progress by animateFloatAsState(
            targetValue = state.step.ordinal.toFloat() / (SetupStep.entries.size - 1),
            animationSpec = motion.slowSpatialSpec(),
            label = "setupProgress",
        )
        if (state.step != SetupStep.Intro) {
            LinearWavyProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val direction = if (forward) 1 else -1
                (slideInHorizontally(motion.defaultSpatialSpec()) { direction * it / 4 } +
                    fadeIn(motion.defaultEffectsSpec())) togetherWith
                    (slideOutHorizontally(motion.fastSpatialSpec()) { -direction * it / 4 } +
                        fadeOut(motion.fastEffectsSpec()))
            },
            label = "setupStep",
        ) { step ->
            SecretsForm {
                when (step) {
                    SetupStep.Intro -> Intro(onBegin = { viewModel.onIntent(VaultSetupIntent.Begin) })

                    SetupStep.Passphrase -> ChoosePassphrase(
                        error = state.error,
                        onSubmit = { passphrase, confirmation ->
                            viewModel.onIntent(
                                VaultSetupIntent.PassphraseChosen(passphrase, confirmation),
                            )
                        },
                    )

                    SetupStep.RecoveryPhrase -> ShowRecoveryPhrase(
                        words = state.recoveryWords,
                        onWrittenDown = { viewModel.onIntent(VaultSetupIntent.PhraseWrittenDown) },
                        onBack = { viewModel.onIntent(VaultSetupIntent.Back) },
                    )

                    SetupStep.ConfirmPhrase -> ConfirmRecoveryPhrase(
                        positions = state.checkPositions,
                        error = state.error,
                        isCreating = state.isCreating,
                        onSubmit = { answers ->
                            viewModel.onIntent(VaultSetupIntent.CheckSubmitted(answers))
                        },
                        onSkip = { viewModel.onIntent(VaultSetupIntent.SkipCheckRequested) },
                        onBack = { viewModel.onIntent(VaultSetupIntent.Back) },
                    )
                }
            }
        }
    }

    if (state.confirmingSkip) {
        SkipCheckDialog(
            onConfirm = { viewModel.onIntent(VaultSetupIntent.SkipCheckConfirmed) },
            onDismiss = { viewModel.onIntent(VaultSetupIntent.SkipCheckDismissed) },
        )
    }
}

@Composable
private fun ColumnScope.Intro(onBegin: () -> Unit) {
    VaultEmblem(working = false)
    StepHeading(
        title = "Keep photos only you can open",
        message = "Photos and videos you move here are encrypted with a passphrase only you " +
            "know. Not even Galva can open them without it.",
    )
    ListGroup(title = "Before you start", modifier = Modifier.fillMaxWidth()) {
        Warning(
            Icons.Rounded.Key,
            "Forget your passphrase and lose your recovery phrase, and everything in Secrets " +
                "is gone for good. Nobody can reset it.",
        )
        Warning(
            Icons.Rounded.PhoneAndroid,
            "Uninstalling Galva or clearing its storage permanently destroys Secrets. Restore " +
                "anything you want to keep first.",
        )
        Warning(
            Icons.Rounded.CloudOff,
            "Moving a photo here deletes the original from your gallery. Copies elsewhere, " +
                "like cloud backups or other apps, are not affected.",
        )
        Warning(
            Icons.Rounded.DeleteForever,
            "Deleting from Secrets is final: not even your passphrase brings it back.",
        )
    }
    WorkingButton(
        label = "Set up Secrets",
        isWorking = false,
        onClick = onBegin,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Warning(icon: ImageVector, text: String) {
    ListGroupItem {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepBadge(step: SetupStep, icon: ImageVector, shape: RoundedPolygon) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(shape.toShape())
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(36.dp),
        )
    }
    StepLabel(step)
}

@Composable
private fun StepLabel(step: SetupStep) {
    Text(
        text = "Step ${step.ordinal} of ${SetupStep.entries.size - 1}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepHeading(title: String, message: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMediumEmphasized,
        textAlign = TextAlign.Center,
    )
    Text(
        text = message,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChoosePassphrase(
    error: String?,
    onSubmit: (CharSequence, CharSequence) -> Unit,
) {
    val passphrase = rememberTextFieldState()
    val confirmation = rememberTextFieldState()
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSubmit(passphrase.text, confirmation.text)
    }

    StepBadge(
        step = SetupStep.Passphrase,
        icon = Icons.Rounded.Password,
        shape = MaterialShapes.Cookie9Sided,
    )
    StepHeading(title = "Choose a passphrase", message = PassphraseRules.HINT)
    PassphraseField(state = passphrase, label = "Passphrase", imeAction = ImeAction.Next)
    PassphraseField(
        state = confirmation,
        label = "Type it again",
        isError = error != null,
        onKeyboardAction = { submit() },
    )
    ErrorText(error)
    WorkingButton(
        label = "Continue",
        isWorking = false,
        onClick = submit,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShowRecoveryPhrase(
    words: List<String>,
    onWrittenDown: () -> Unit,
    onBack: () -> Unit,
) {
    StepBadge(
        step = SetupStep.RecoveryPhrase,
        icon = Icons.Rounded.Key,
        shape = MaterialShapes.Clover4Leaf,
    )
    StepHeading(
        title = "Your recovery phrase",
        message = "Write these 24 words down, in order, and keep them somewhere safe and " +
            "offline. They are the only way back in if you forget your passphrase, and Galva " +
            "will never show them again.",
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { isSensitiveData = true },
    ) {
        val perRow = if (maxWidth < WIDE_WORD_GRID) COMPACT_WORDS_PER_ROW else WIDE_WORDS_PER_ROW
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            words.chunked(perRow).forEachIndexed { row, rowWords ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowWords.forEachIndexed { column, word ->
                        RecoveryWord(
                            number = row * perRow + column + 1,
                            word = word,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null)
            Text(
                text = "Anyone who has these words can open your Secrets. Don't photograph " +
                    "them or store them in a cloud note.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    WorkingButton(
        label = "I've written them down",
        isWorking = false,
        onClick = onWrittenDown,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = onBack, shapes = ButtonDefaults.shapes()) {
        Text("Back")
    }
}

@Composable
private fun RecoveryWord(number: Int, word: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(28.dp)) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = word,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConfirmRecoveryPhrase(
    positions: List<Int>,
    error: String?,
    isCreating: Boolean,
    onSubmit: (List<String>) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    val answers = positions.map { position -> key(position) { rememberTextFieldState() } }
    val focusManager = LocalFocusManager.current

    VaultEmblem(working = isCreating, size = 96.dp)
    StepLabel(SetupStep.ConfirmPhrase)
    StepHeading(
        title = "Check your recovery phrase",
        message = "Type these words from what you wrote down.",
    )
    positions.forEachIndexed { index, position ->
        RecoveryPhraseField(
            state = answers[index],
            enabled = !isCreating,
            label = "Word ${position + 1}",
            singleLine = true,
            imeAction = if (index == positions.lastIndex) ImeAction.Done else ImeAction.Next,
        )
    }
    ErrorText(error)
    WorkingButton(
        label = "Create Secrets",
        isWorking = isCreating,
        onClick = {
            focusManager.clearFocus()
            onSubmit(answers.map { it.text.toString() })
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (!isCreating) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack, shapes = ButtonDefaults.shapes()) {
                Text("Show the words again")
            }
            TextButton(
                onClick = onSkip,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Skip check")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SkipCheckDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("Skip the check?") },
        text = {
            Text(
                "The check is the only proof that your copy of the recovery phrase is right. " +
                    "If a word is missing or misspelled and you forget your passphrase, " +
                    "everything in Secrets is lost for good, and nobody can recover it.",
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Skip anyway")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text("Check words")
            }
        },
    )
}

private const val COMPACT_WORDS_PER_ROW = 2
private const val WIDE_WORDS_PER_ROW = 3
private val WIDE_WORD_GRID = 420.dp
