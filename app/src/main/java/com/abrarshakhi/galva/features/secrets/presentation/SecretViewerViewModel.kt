package com.abrarshakhi.galva.features.secrets.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.common.mvi.MviViewModel
import com.abrarshakhi.galva.core.vault.domain.model.VaultState
import com.abrarshakhi.galva.core.vault.domain.repository.VaultRepository
import com.abrarshakhi.galva.core.vault.domain.usecase.RestoreSecretsUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SecretViewerViewModel(
    initialId: Long,
    private val vault: VaultRepository,
    private val restoreSecrets: RestoreSecretsUseCase,
) : MviViewModel<SecretViewerUiState, SecretViewerIntent, SecretViewerEffect>(
    SecretViewerUiState(),
) {

    private var anchorId: Long = initialId

    init {
        vault.state
            .onEach { vaultState ->
                val items = (vaultState as? VaultState.Unlocked)?.items.orEmpty()
                if (items.isEmpty()) {
                    setState { copy(items = emptyList(), isLoading = false) }
                    sendEffect(SecretViewerEffect.Close)
                    return@onEach
                }
                val index = items.indexOfFirst { it.id == anchorId }
                    .takeIf { it >= 0 }
                    ?: currentState.currentIndex.coerceIn(items.indices)
                anchorId = items[index].id
                setState { copy(items = items, currentIndex = index, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override suspend fun reduce(intent: SecretViewerIntent) {
        when (intent) {
            is SecretViewerIntent.PageSettled -> {
                val item = currentState.items.getOrNull(intent.index) ?: return
                anchorId = item.id
                setState { copy(currentIndex = intent.index) }
            }

            SecretViewerIntent.ChromeToggled -> setState { copy(chromeVisible = !chromeVisible) }

            SecretViewerIntent.RestoreRequested -> {
                val item = currentState.current ?: return
                setState { copy(work = "Restoring to the gallery…") }
                anchorPast(item.id)
                val outcome = restoreSecrets(listOf(item.id))
                setState { copy(work = null) }
                sendEffect(
                    SecretViewerEffect.ShowMessage(outcome.failure ?: "Restored to the gallery"),
                )
            }

            SecretViewerIntent.DeleteRequested -> setState { copy(confirmingDelete = true) }

            SecretViewerIntent.DeleteDismissed -> setState { copy(confirmingDelete = false) }

            SecretViewerIntent.DeleteConfirmed -> {
                val item = currentState.current ?: return
                setState { copy(confirmingDelete = false, work = "Deleting…") }
                anchorPast(item.id)
                vault.delete(listOf(item.id))
                setState { copy(work = null) }
                sendEffect(SecretViewerEffect.ShowMessage("Deleted for good"))
            }
        }
    }

    private fun anchorPast(removedId: Long) {
        val items = currentState.items
        val index = currentState.currentIndex
        val next = items.drop(index + 1).firstOrNull { it.id != removedId }
            ?: items.take(index).lastOrNull { it.id != removedId }
        anchorId = next?.id ?: NO_ANCHOR
    }

    private companion object {
        const val NO_ANCHOR = 0L
    }
}
