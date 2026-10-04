package com.abrarshakhi.galva.core.ui.selection

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

    fun toggleAll(ids: Collection<Long>): SelectionState {
        val allSelected = ids.isNotEmpty() && ids.all(selectedIds::contains)
        return copy(selectedIds = if (allSelected) selectedIds - ids.toSet() else selectedIds + ids)
    }

    fun selectAll(ids: Collection<Long>): SelectionState = copy(selectedIds = ids.toSet())

    fun retainOnly(available: Set<Long>): SelectionState =
        if (available.containsAll(selectedIds)) this
        else copy(selectedIds = selectedIds intersect available)

    fun cleared(): SelectionState = SelectionState()
}
