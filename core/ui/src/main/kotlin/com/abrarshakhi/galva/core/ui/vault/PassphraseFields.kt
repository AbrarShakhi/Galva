package com.abrarshakhi.galva.core.ui.vault

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.abrarshakhi.galva.core.designsystem.layout.keepAboveKeyboard

object PassphraseRules {

    const val MIN_LENGTH = 6

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
    OutlinedTextField(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .keepAboveKeyboard()
            .semantics {
                password()
                contentType = ContentType.Password
                isSensitiveData = true
            },
        enabled = enabled,
        label = { Text(label) },
        isError = isError,
        shape = MaterialTheme.shapes.large,
        lineLimits = TextFieldLineLimits.SingleLine,
        outputTransformation = PassphraseMask,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        onKeyboardAction = onKeyboardAction,
    )
}

private object PassphraseMask : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        for (index in 0 until length) replace(index, index + 1, MASK)
    }
}

private const val MASK = "\u2022"
