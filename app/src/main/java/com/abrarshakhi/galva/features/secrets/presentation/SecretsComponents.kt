package com.abrarshakhi.galva.features.secrets.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens

/**
 * The page the vault's full-screen steps are laid out on: setup, unlock and choosing a new
 * passphrase. It scrolls, because the keyboard takes half of a phone screen, and stays at a
 * readable width on tablets and in landscape.
 */
@Composable
fun SecretsForm(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = FORM_MAX_WIDTH)
                .padding(GalvaDimens.ScreenPadding * 2),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(GalvaDimens.ItemSpacing),
            content = content,
        )
    }
}

/**
 * Words typed in the clear so they can be checked, but with a password keyboard, so the keyboard
 * does not learn them into its suggestions.
 */
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
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
    )
}

@Composable
fun ErrorText(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** A button that shows progress in place of its label while the vault works. */
@Composable
fun WorkingButton(
    label: String,
    isWorking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, enabled = !isWorking, modifier = modifier) {
        if (isWorking) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(label)
        }
    }
}

private val FORM_MAX_WIDTH = 520.dp
