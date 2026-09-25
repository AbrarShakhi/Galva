package com.abrarshakhi.galva.features.secrets.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

object PassphraseRules {

    const val MIN_LENGTH = 10

    const val HINT = "At least $MIN_LENGTH characters. A few unrelated words are easy to " +
        "remember and hard to guess."

    fun problemWith(passphrase: CharSequence, confirmation: CharSequence): String? = when {
        passphrase.length < MIN_LENGTH -> "Use at least $MIN_LENGTH characters"
        passphrase.all(Char::isDigit) -> "Use letters too, not only numbers"
        !passphrase.contentEquals(confirmation) -> "The two passphrases don't match"
        else -> null
    }
}

@Composable
fun PassphraseField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    OutlinedSecureTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        onKeyboardAction = onKeyboardAction,
    )
}
