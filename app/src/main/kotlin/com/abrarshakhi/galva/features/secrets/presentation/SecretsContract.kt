package com.abrarshakhi.galva.features.secrets.presentation

import com.abrarshakhi.galva.common.mvi.UiEffect
import com.abrarshakhi.galva.common.mvi.UiIntent
import com.abrarshakhi.galva.common.mvi.UiState
import com.abrarshakhi.galva.common.ui.selection.SelectionState
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.settings.domain.AppSettings
import com.abrarshakhi.galva.core.vault.domain.model.SecretItem

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
) : UiState {

    val isEmpty: Boolean get() = phase == SecretsPhase.Unlocked && items.isEmpty()

    val allSelected: Boolean get() = selection.isActive && selection.count == items.size
}

sealed interface SecretsIntent : UiIntent {

    data class MediaTapped(val item: MediaItem) : SecretsIntent

    data class MediaLongPressed(val item: MediaItem) : SecretsIntent

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

    data class PendingMovesResolved(
        val originalIds: List<Long>,
        val confirmed: Boolean,
    ) : SecretsIntent
}

sealed interface SecretsEffect : UiEffect {

    data class OpenViewer(val secretId: Long) : SecretsEffect

    data class ConfirmOriginalsDeletion(
        val originalIds: List<Long>,
        val uris: List<String>,
    ) : SecretsEffect

    data class ShowMessage(val text: String) : SecretsEffect
}
