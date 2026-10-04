package com.abrarshakhi.galva.feature.secrets

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.data.repository.SettingsRepository
import com.abrarshakhi.galva.core.domain.MediaSelectionActions
import com.abrarshakhi.galva.core.domain.RestoreSecretsUseCase
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.SecretItem
import com.abrarshakhi.galva.core.model.VaultState
import com.abrarshakhi.galva.core.ui.actions.ConsentRequest
import com.abrarshakhi.galva.core.ui.message.shown
import com.abrarshakhi.galva.core.ui.message.withMessage
import com.abrarshakhi.galva.core.ui.mvi.MviViewModel
import com.abrarshakhi.galva.core.ui.vault.PassphraseRules
import com.abrarshakhi.galva.core.vault.VaultRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class SecretsViewModel(
    private val vault: VaultRepository,
    settingsRepository: SettingsRepository,
    private val restoreSecrets: RestoreSecretsUseCase,
    private val selectionActions: MediaSelectionActions,
) : MviViewModel<SecretsUiState, SecretsIntent>(SecretsUiState()) {

    private var pendingFinish: PendingFinish? = null

    init {
        vault.state
            .onEach { vaultState -> setState { following(vaultState) } }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .map { it.galleryColumns }
            .onEach { columns -> setState { copy(columns = columns) } }
            .launchIn(viewModelScope)
    }

    private fun SecretsUiState.following(vaultState: VaultState): SecretsUiState =
        when (vaultState) {
            VaultState.NotSetUp -> SecretsUiState(
                phase = SecretsPhase.NotSetUp,
                columns = columns,
                messages = messages,
            )

            VaultState.Locked -> SecretsUiState(
                phase = SecretsPhase.Locked,
                columns = columns,
                messages = messages,
            )

            is VaultState.Unlocked -> {
                val items = vaultState.items.map(SecretItem::toGridItem)
                val available = items.mapTo(HashSet(items.size), MediaItem::id)
                copy(
                    phase = if (vaultState.needsNewPassphrase) SecretsPhase.NeedsNewPassphrase
                    else SecretsPhase.Unlocked,
                    items = items,
                    pendingMoves = vaultState.items.filter { it.pendingOriginal != null },
                    selection = selection.retainOnly(available),
                )
            }
        }

    override suspend fun reduce(intent: SecretsIntent) {
        when (intent) {
            is SecretsIntent.ToggleSelection ->
                setState { copy(selection = selection.toggle(intent.item.id)) }

            SecretsIntent.SelectAll ->
                setState { copy(selection = selection.selectAll(items.map(MediaItem::id))) }

            SecretsIntent.ClearSelection -> setState { copy(selection = selection.cleared()) }

            SecretsIntent.RestoreSelection -> {
                val ids = currentState.selection.selectedIds.toList()
                if (ids.isEmpty()) return
                setState { copy(work = "Restoring ${ids.size} to the gallery…") }
                val outcome = restoreSecrets(ids)
                val restored = outcome.restoredMediaIds.size
                val text = outcome.failure?.let { "Restored $restored. $it" }
                    ?: "Restored $restored to the gallery"
                setState {
                    copy(work = null, selection = selection.cleared(), messages = messages.withMessage(text))
                }
            }

            SecretsIntent.DeleteSelection ->
                if (currentState.selection.isActive) setState { copy(confirmingDelete = true) }

            SecretsIntent.DeleteDismissed -> setState { copy(confirmingDelete = false) }

            SecretsIntent.DeleteConfirmed -> {
                val ids = currentState.selection.selectedIds.toList()
                setState { copy(confirmingDelete = false, work = "Deleting…") }
                vault.delete(ids)
                setState {
                    copy(
                        work = null,
                        selection = selection.cleared(),
                        messages = messages.withMessage("Deleted ${ids.size} for good"),
                    )
                }
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
                        messages = if (changed) messages.withMessage("Passphrase changed") else messages,
                    )
                }
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
                        messages = if (changed) messages.withMessage("New passphrase saved") else messages,
                    )
                }
            }

            SecretsIntent.FinishPendingMoves -> {
                val originals = currentState.pendingMoves.mapNotNull(SecretItem::pendingOriginal)
                if (originals.isEmpty()) return
                val request = ConsentRequest(uris = originals.map { it.uri })
                pendingFinish = PendingFinish(request.id, originals.map { it.mediaId })
                setState { copy(originalsConsent = request) }
            }

            SecretsIntent.UndoPendingMoves -> {
                val ids = currentState.pendingMoves.map(SecretItem::id)
                if (ids.isEmpty()) return
                setState { copy(work = "Undoing…") }
                vault.undoMove(ids)
                setState {
                    copy(work = null, messages = messages.withMessage("Left ${ids.size} in the gallery"))
                }
            }

            is SecretsIntent.OriginalsConsentLaunched -> setState {
                val consent = originalsConsent?.takeIf { it.id == intent.requestId }
                copy(originalsConsent = consent?.copy(launched = true) ?: originalsConsent)
            }

            is SecretsIntent.OriginalsConsentResolved -> finishMoves(intent)

            is SecretsIntent.MessageShown ->
                setState { copy(messages = messages.shown(intent.id)) }
        }
    }

    private suspend fun finishMoves(intent: SecretsIntent.OriginalsConsentResolved) {
        val pending = pendingFinish?.takeIf { it.requestId == intent.requestId } ?: return
        pendingFinish = null
        setState { copy(originalsConsent = null) }
        if (!intent.confirmed) return
        val resolved = pending.originalIds.toSet()
        val secretIds = currentState.pendingMoves
            .filter { it.pendingOriginal?.mediaId in resolved }
            .map(SecretItem::id)
        vault.confirmMove(secretIds)
        selectionActions.confirmDeleted(pending.originalIds)
        setState { copy(messages = messages.withMessage("Finished moving ${secretIds.size}")) }
    }

    private data class PendingFinish(val requestId: Long, val originalIds: List<Long>)
}
