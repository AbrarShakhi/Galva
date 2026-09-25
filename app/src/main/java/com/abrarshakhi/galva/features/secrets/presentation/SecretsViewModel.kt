package com.abrarshakhi.galva.features.secrets.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.common.mvi.MviViewModel
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.usecase.MediaSelectionActions
import com.abrarshakhi.galva.core.settings.domain.SettingsRepository
import com.abrarshakhi.galva.core.vault.domain.model.SecretItem
import com.abrarshakhi.galva.core.vault.domain.model.VaultState
import com.abrarshakhi.galva.core.vault.domain.repository.VaultRepository
import com.abrarshakhi.galva.core.vault.domain.usecase.RestoreSecretsUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Drives the Secrets tab.
 *
 * Its whole state follows the vault's: when the vault locks, everything that came out of it — the
 * item list, the selection, any dialog — is dropped here too.
 */
class SecretsViewModel(
    private val vault: VaultRepository,
    settingsRepository: SettingsRepository,
    private val restoreSecrets: RestoreSecretsUseCase,
    private val selectionActions: MediaSelectionActions,
) : MviViewModel<SecretsUiState, SecretsIntent, SecretsEffect>(SecretsUiState()) {

    init {
        vault.state
            .onEach { vaultState -> setState { following(vaultState) } }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .map { it.galleryColumns }
            .onEach { columns -> setState { copy(columns = columns) } }
            .launchIn(viewModelScope)
    }

    private fun SecretsUiState.following(vaultState: VaultState): SecretsUiState = when (vaultState) {
        VaultState.NotSetUp -> SecretsUiState(phase = SecretsPhase.NotSetUp, columns = columns)
        VaultState.Locked -> SecretsUiState(phase = SecretsPhase.Locked, columns = columns)
        is VaultState.Unlocked -> {
            val items = vaultState.items.map(SecretItem::toGridItem)
            val available = items.mapTo(HashSet(items.size), MediaItem::id)
            copy(
                phase = if (vaultState.needsNewPassphrase) SecretsPhase.NeedsNewPassphrase
                else SecretsPhase.Unlocked,
                items = items,
                pendingMoves = vaultState.items.filter { it.pendingOriginal != null },
                selection = selection.copy(selectedIds = selection.selectedIds intersect available),
            )
        }
    }

    override suspend fun reduce(intent: SecretsIntent) {
        when (intent) {
            is SecretsIntent.MediaTapped ->
                if (currentState.selection.isActive) {
                    setState { copy(selection = selection.toggle(intent.item.id)) }
                } else {
                    sendEffect(SecretsEffect.OpenViewer(intent.item.id))
                }

            is SecretsIntent.MediaLongPressed ->
                setState { copy(selection = selection.toggle(intent.item.id)) }

            SecretsIntent.SelectAll ->
                setState { copy(selection = selection.selectAll(items.map(MediaItem::id))) }

            SecretsIntent.ClearSelection -> setState { copy(selection = selection.cleared()) }

            SecretsIntent.RestoreSelection -> {
                val ids = currentState.selection.selectedIds.toList()
                if (ids.isEmpty()) return
                setState { copy(work = "Restoring ${ids.size} to the gallery…") }
                val outcome = restoreSecrets(ids)
                setState { copy(work = null, selection = selection.cleared()) }
                val restored = outcome.restoredMediaIds.size
                sendEffect(
                    SecretsEffect.ShowMessage(
                        outcome.failure?.let { "Restored $restored. $it" }
                            ?: "Restored $restored to the gallery",
                    )
                )
            }

            SecretsIntent.DeleteSelection ->
                if (currentState.selection.isActive) setState { copy(confirmingDelete = true) }

            SecretsIntent.DeleteDismissed -> setState { copy(confirmingDelete = false) }

            SecretsIntent.DeleteConfirmed -> {
                val ids = currentState.selection.selectedIds.toList()
                setState { copy(confirmingDelete = false, work = "Deleting…") }
                vault.delete(ids)
                setState { copy(work = null, selection = selection.cleared()) }
                sendEffect(SecretsEffect.ShowMessage("Deleted ${ids.size} for good"))
            }

            SecretsIntent.LockRequested -> vault.lock()

            SecretsIntent.ChangePassphraseRequested ->
                setState { copy(changingPassphrase = true, passphraseError = null) }

            SecretsIntent.ChangePassphraseDismissed ->
                setState { copy(changingPassphrase = false, passphraseError = null) }

            is SecretsIntent.ChangePassphraseSubmitted -> {
                val problem = PassphraseRules.problemWith(intent.new, intent.confirmation)
                if (problem != null) {
                    setState { copy(passphraseError = problem) }
                    return
                }
                setState { copy(isSavingPassphrase = true, passphraseError = null) }
                val changed = vault.changePassphrase(intent.current, intent.new)
                setState {
                    copy(
                        isSavingPassphrase = false,
                        changingPassphrase = !changed,
                        passphraseError = if (changed) null else "Your current passphrase is wrong",
                    )
                }
                if (changed) sendEffect(SecretsEffect.ShowMessage("Passphrase changed"))
            }

            is SecretsIntent.NewPassphraseSubmitted -> {
                val problem = PassphraseRules.problemWith(intent.new, intent.confirmation)
                if (problem != null) {
                    setState { copy(passphraseError = problem) }
                    return
                }
                setState { copy(isSavingPassphrase = true, passphraseError = null) }
                val changed = vault.changePassphrase(current = null, new = intent.new)
                setState {
                    copy(
                        isSavingPassphrase = false,
                        passphraseError = if (changed) null else "Couldn't save the new passphrase",
                    )
                }
                if (changed) sendEffect(SecretsEffect.ShowMessage("New passphrase saved"))
            }

            SecretsIntent.FinishPendingMoves -> {
                val originals = currentState.pendingMoves.mapNotNull(SecretItem::pendingOriginal)
                if (originals.isEmpty()) return
                sendEffect(
                    SecretsEffect.ConfirmOriginalsDeletion(
                        originalIds = originals.map { it.mediaId },
                        uris = originals.map { it.uri },
                    )
                )
            }

            SecretsIntent.UndoPendingMoves -> {
                val ids = currentState.pendingMoves.map(SecretItem::id)
                if (ids.isEmpty()) return
                setState { copy(work = "Undoing…") }
                vault.undoMove(ids)
                setState { copy(work = null) }
                sendEffect(SecretsEffect.ShowMessage("Left ${ids.size} in the gallery"))
            }

            is SecretsIntent.PendingMovesResolved -> {
                if (!intent.confirmed) return
                val resolved = intent.originalIds.toSet()
                val secretIds = currentState.pendingMoves
                    .filter { it.pendingOriginal?.mediaId in resolved }
                    .map(SecretItem::id)
                vault.confirmMove(secretIds)
                selectionActions.confirmDeleted(intent.originalIds)
                sendEffect(SecretsEffect.ShowMessage("Finished moving ${secretIds.size}"))
            }
        }
    }
}
