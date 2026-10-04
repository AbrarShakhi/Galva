package com.abrarshakhi.galva.core.ui.actions

import com.abrarshakhi.galva.core.domain.DeleteMediaUseCase
import com.abrarshakhi.galva.core.domain.MediaSelectionActions
import com.abrarshakhi.galva.core.domain.MoveToSecretsActions
import com.abrarshakhi.galva.core.domain.ToggleFavoriteUseCase
import com.abrarshakhi.galva.core.model.AddToVaultOutcome
import com.abrarshakhi.galva.core.model.DeleteOutcome
import com.abrarshakhi.galva.core.model.VaultState
import com.abrarshakhi.galva.core.ui.vault.SET_UP_SECRETS_FIRST
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaActionsStateHolderTest {

    private val log = mutableListOf<String>()
    private lateinit var media: FakeMediaRepository
    private lateinit var vault: FakeVaultRepository
    private lateinit var actions: MediaActionsStateHolder

    @Before
    fun setUp() {
        media = FakeMediaRepository(log)
        vault = FakeVaultRepository(log)
        val selection = MediaSelectionActions(
            toggleFavorite = ToggleFavoriteUseCase(media),
            deleteMedia = DeleteMediaUseCase(media),
        )
        actions = MediaActionsStateHolder(
            selectionActions = selection,
            moveToSecrets = MoveToSecretsActions(vault = vault, selectionActions = selection),
        )
    }

    @Test
    fun `delete asks for consent and prunes the index only once confirmed`() = runTest {
        val items = listOf(mediaItem(1), mediaItem(2))

        val requested = actions.perform(MediaAction.Delete, items)
        val consent = checkNotNull(actions.state.value.consent)

        assertEquals(MediaActionResult.None, requested)
        assertEquals(items.map { uriOf(it.id) }, consent.uris)
        assertTrue(media.forgotten.isEmpty())

        val resolved = actions.handle(MediaActionEvent.ConsentResolved(consent.id, confirmed = true))

        assertEquals(listOf(1L, 2L), media.forgotten)
        assertNull(actions.state.value.consent)
        assertTrue(resolved.completed)
        assertEquals("Deleted 2 items", resolved.message?.text)
    }

    @Test
    fun `a declined delete leaves the index alone`() = runTest {
        actions.perform(MediaAction.Delete, listOf(mediaItem(1)))
        val consent = checkNotNull(actions.state.value.consent)

        val resolved = actions.handle(MediaActionEvent.ConsentResolved(consent.id, confirmed = false))

        assertTrue(media.forgotten.isEmpty())
        assertNull(actions.state.value.consent)
        assertFalse(resolved.completed)
    }

    @Test
    fun `an answer for a different request is ignored`() = runTest {
        actions.perform(MediaAction.Delete, listOf(mediaItem(1)))
        val consent = checkNotNull(actions.state.value.consent)

        actions.handle(MediaActionEvent.ConsentResolved(consent.id + 1, confirmed = true))

        assertTrue(media.forgotten.isEmpty())
        assertEquals(consent, actions.state.value.consent)
    }

    @Test
    fun `a launched consent request is marked so it is not shown twice`() = runTest {
        actions.perform(MediaAction.Delete, listOf(mediaItem(1)))
        val consent = checkNotNull(actions.state.value.consent)

        actions.handle(MediaActionEvent.ConsentLaunched(consent.id))

        assertTrue(checkNotNull(actions.state.value.consent).launched)
    }

    @Test
    fun `removal callbacks run before the index is pruned`() = runTest {
        actions.perform(MediaAction.Delete, listOf(mediaItem(7)))
        val consent = checkNotNull(actions.state.value.consent)

        actions.handle(MediaActionEvent.ConsentResolved(consent.id, confirmed = true)) { ids ->
            log += "removing $ids"
        }

        assertEquals(listOf("removing [7]", "forget [7]"), log)
    }

    @Test
    fun `a delete the platform finished on its own completes immediately`() = runTest {
        media.deleteOutcome = { ids -> DeleteOutcome.Deleted(ids.size) }

        val result = actions.perform(MediaAction.Delete, listOf(mediaItem(3)))

        assertEquals(listOf(3L), media.forgotten)
        assertEquals("Deleted 1 item", result.message?.text)
        assertNull(actions.state.value.consent)
    }

    @Test
    fun `favorite favorites when anything is unfavorited`() = runTest {
        val result = actions.perform(
            MediaAction.Favorite,
            listOf(mediaItem(1, favorite = true), mediaItem(2)),
        )

        assertEquals(mapOf(1L to true, 2L to true), media.favorites)
        assertEquals("Added to Favorites", result.message?.text)
        assertTrue(result.completed)
    }

    @Test
    fun `favorite unfavorites when everything is already a favorite`() = runTest {
        val result = actions.perform(MediaAction.Favorite, listOf(mediaItem(1, favorite = true)))

        assertEquals(mapOf(1L to false), media.favorites)
        assertEquals("Removed from Favorites", result.message?.text)
    }

    @Test
    fun `share is exposed until the sheet has been launched`() = runTest {
        actions.perform(MediaAction.Share, listOf(mediaItem(1), mediaItem(2)))

        val share = checkNotNull(actions.state.value.share)
        assertEquals(listOf(uriOf(1), uriOf(2)), share.uris)

        actions.handle(MediaActionEvent.ShareLaunched)

        assertNull(actions.state.value.share)
    }

    @Test
    fun `adding to an album opens the sheet and completes once added`() = runTest {
        actions.perform(MediaAction.AddToAlbum, listOf(mediaItem(4), mediaItem(5)))

        assertEquals(listOf(4L, 5L), actions.state.value.addToAlbumIds)

        val result = actions.handle(MediaActionEvent.AddedToAlbum("Trips"))

        assertNull(actions.state.value.addToAlbumIds)
        assertEquals("Added to Trips", result.message?.text)
        assertTrue(result.completed)
    }

    @Test
    fun `nothing happens for an empty selection`() = runTest {
        MediaAction.entries.forEach { action ->
            assertEquals(MediaActionResult.None, actions.perform(action, emptyList()))
        }
        assertEquals(MediaActionsState(), actions.state.value)
    }

    @Test
    fun `moving before Secrets exists asks the user to set it up`() = runTest {
        vault.state.value = VaultState.NotSetUp

        val result = actions.perform(MediaAction.MoveToSecrets, listOf(mediaItem(1)))

        assertEquals(SET_UP_SECRETS_FIRST, result.message?.text)
        assertTrue(log.isEmpty())
    }

    @Test
    fun `moving while locked unlocks first and then encrypts`() = runTest {
        vault.state.value = VaultState.Locked

        actions.perform(MediaAction.MoveToSecrets, listOf(mediaItem(1)))

        assertTrue(actions.state.value.unlockingVault)
        assertTrue(log.isEmpty())

        vault.state.value = VaultState.Unlocked(items = emptyList())
        actions.handle(MediaActionEvent.VaultUnlocked)

        assertFalse(actions.state.value.unlockingVault)
        assertEquals(listOf("add [1]"), log)
        checkNotNull(actions.state.value.consent)
    }

    @Test
    fun `dismissing the unlock sheet abandons the move`() = runTest {
        vault.state.value = VaultState.Locked
        actions.perform(MediaAction.MoveToSecrets, listOf(mediaItem(1)))

        actions.handle(MediaActionEvent.VaultUnlockDismissed)
        vault.state.value = VaultState.Unlocked(items = emptyList())
        actions.handle(MediaActionEvent.VaultUnlocked)

        assertFalse(actions.state.value.unlockingVault)
        assertTrue(log.isEmpty())
    }

    @Test
    fun `a confirmed move settles the vault copies and prunes the originals`() = runTest {
        actions.perform(MediaAction.MoveToSecrets, listOf(mediaItem(1), mediaItem(2)))
        val consent = checkNotNull(actions.state.value.consent)
        assertNull(actions.state.value.progress)

        val result = actions.handle(MediaActionEvent.ConsentResolved(consent.id, confirmed = true))

        val secretIds = listOf(1L, 2L).map { it + FakeVaultRepository.SECRET_ID_OFFSET }
        assertEquals(listOf("add [1, 2]", "confirm $secretIds", "forget [1, 2]"), log)
        assertEquals("Moved 2 to Secrets", result.message?.text)
        assertTrue(result.completed)
    }

    @Test
    fun `a declined move destroys the vault copies and keeps the originals`() = runTest {
        actions.perform(MediaAction.MoveToSecrets, listOf(mediaItem(1)))
        val consent = checkNotNull(actions.state.value.consent)

        actions.handle(MediaActionEvent.ConsentResolved(consent.id, confirmed = false))

        val secretId = 1L + FakeVaultRepository.SECRET_ID_OFFSET
        assertEquals(listOf("add [1]", "undo [$secretId]"), log)
        assertTrue(media.forgotten.isEmpty())
    }

    @Test
    fun `a failed encryption is reported and asks for nothing`() = runTest {
        vault.addOutcome = { AddToVaultOutcome.Failed("There isn't enough free space to move these") }

        val result = actions.perform(MediaAction.MoveToSecrets, listOf(mediaItem(1)))

        assertEquals("There isn't enough free space to move these", result.message?.text)
        assertNull(actions.state.value.consent)
        assertNull(actions.state.value.progress)
    }
}
