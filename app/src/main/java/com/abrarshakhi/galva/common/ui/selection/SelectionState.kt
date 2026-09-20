package com.abrarshakhi.galva.common.ui.selection

import androidx.compose.runtime.Immutable

@Immutable
data class SelectionState(
    val selectedIds: Set<Long> = emptySet(),
) {
    val isActive: Boolean get() = selectedIds.isNotEmpty()
    val count: Int get() = selectedIds.size

    fun contains(id: Long): Boolean = id in selectedIds

    fun toggle(id: Long): SelectionState =
        copy(selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id)

    fun selectAll(ids: Collection<Long>): SelectionState = copy(selectedIds = ids.toSet())

    fun cleared(): SelectionState = SelectionState()
}
