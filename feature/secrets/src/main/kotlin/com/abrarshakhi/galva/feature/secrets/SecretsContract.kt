package com.abrarshakhi.galva.feature.secrets

import com.abrarshakhi.galva.core.model.AppSettings
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.SecretItem
import com.abrarshakhi.galva.core.ui.actions.ConsentRequest
import com.abrarshakhi.galva.core.ui.message.UserMessage
import com.abrarshakhi.galva.core.ui.mvi.UiIntent
import com.abrarshakhi.galva.core.ui.mvi.UiState
import com.abrarshakhi.galva.core.ui.selection.SelectionState

enum class SecretsPhase {
    Loading,
    NotSetUp,
    Locked,
    NeedsNewPassphrase,
    Unlocked,
}

data class SecretsUiState(
    val phase: SecretsPhase = SecretsPhase.Loading,
    val items: List<MediaItem> = emptyList(),
    val pendingMoves: List<SecretItem> = emptyList(),
    val selection: SelectionState = SelectionState(),
    val columns: Int = AppSettings.DEFAULT_COLUMNS,
    val work: String? = null,
    val confirmingDelete: Boolean = false,
    val changingPassphrase: Boolean = false,
    val passphraseError: String? = null,
    val isSavingPassphrase: Boolean = false,
    val originalsConsent: ConsentRequest? = null,
    val messages: List<UserMessage> = emptyList(),
) : UiState {

    val isEmpty: Boolean get() = phase == SecretsPhase.Unlocked && items.isEmpty()

    val allSelected: Boolean get() = selection.isActive && selection.count == items.size
}

sealed interface SecretsIntent : UiIntent {

    data class ToggleSelection(val item: MediaItem) : SecretsIntent

    data object SelectAll : SecretsIntent

    data object ClearSelection : SecretsIntent

    data object RestoreSelection : SecretsIntent

    data object DeleteSelection : SecretsIntent

    data object DeleteConfirmed : SecretsIntent

    data object DeleteDismissed : SecretsIntent

    data object LockRequested : SecretsIntent

    data object ChangePassphraseRequested : SecretsIntent

    data object ChangePassphraseDismissed : SecretsIntent

    data class ChangePassphraseSubmitted(
        val current: CharSequence,
        val new: CharSequence,
        val confirmation: CharSequence,
    ) : SecretsIntent

    data class NewPassphraseSubmitted(
        val new: CharSequence,
        val confirmation: CharSequence,
    ) : SecretsIntent

    data object FinishPendingMoves : SecretsIntent

    data object UndoPendingMoves : SecretsIntent

    data class OriginalsConsentLaunched(val requestId: Long) : SecretsIntent

    data class OriginalsConsentResolved(val requestId: Long, val confirmed: Boolean) : SecretsIntent

    data class MessageShown(val id: Long) : SecretsIntent
}
