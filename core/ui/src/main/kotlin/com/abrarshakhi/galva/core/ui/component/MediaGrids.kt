package com.abrarshakhi.galva.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abrarshakhi.galva.core.designsystem.theme.GalvaDimens
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.TimelineSection
import com.abrarshakhi.galva.core.ui.selection.SelectionState
import com.abrarshakhi.galva.core.ui.transition.sharedMediaKey
import com.abrarshakhi.galva.core.ui.util.adaptiveColumnCount
import com.abrarshakhi.galva.core.ui.util.rememberTimelineDateFormatter

@Composable
fun TimelineGrid(
    sections: List<TimelineSection>,
    selection: SelectionState,
    preferredColumns: Int,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onHeaderClick: (TimelineSection) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    state: LazyGridState = rememberLazyGridState(),
    sharedKeyNamespace: String = "media",
) {
    val dateFormatter = rememberTimelineDateFormatter()

    val labelForIndex: (Int) -> String = remember(sections, dateFormatter) {
        val sectionEnds = buildList {
            var cursor = 0
            sections.forEach { section ->
                cursor += 1 + section.items.size
                add(cursor)
            }
        }
        val resolve: (Int) -> String = { index ->
            val position = sectionEnds.indexOfFirst { end -> index < end }
            val section = if (position >= 0) sections.getOrNull(position) else sections.lastOrNull()
            section?.let { dateFormatter.format(it.date) }.orEmpty()
        }
        resolve
    }

    BoxWithConstraints(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(adaptiveColumnCount(preferredColumns, maxWidth)),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(GalvaDimens.GridSpacing),
            verticalArrangement = Arrangement.spacedBy(GalvaDimens.GridSpacing),
        ) {
            sections.forEach { section ->
                item(
                    key = "header-${section.date}",
                    contentType = CONTENT_TYPE_HEADER,
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    SectionHeader(
                        title = dateFormatter.format(section.date),
                        selectionMode = selection.isActive,
                        allSelected = section.items.all { selection.contains(it.id) },
                        onClick = { onHeaderClick(section) },
                    )
                }

                items(
                    items = section.items,
                    key = { it.id },
                    contentType = { CONTENT_TYPE_MEDIA },
                ) { item ->
                    MediaThumbnail(
                        item = item,
                        isSelected = selection.contains(item.id),
                        selectionMode = selection.isActive,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        sharedKey = sharedMediaKey(item.id, sharedKeyNamespace),
                    )
                }
            }
        }

        DraggableScrollbar(
            state = state,
            labelForIndex = labelForIndex,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(contentPadding),
        )
    }
}

@Composable
fun MediaGrid(
    items: List<MediaItem>,
    selection: SelectionState,
    preferredColumns: Int,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    state: LazyGridState = rememberLazyGridState(),
    sharedKeyNamespace: String = "media",
) {
    BoxWithConstraints(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(adaptiveColumnCount(preferredColumns, maxWidth)),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(GalvaDimens.GridSpacing),
            verticalArrangement = Arrangement.spacedBy(GalvaDimens.GridSpacing),
        ) {
            items(
                items = items,
                key = { it.id },
                contentType = { CONTENT_TYPE_MEDIA },
            ) { item ->
                MediaThumbnail(
                    item = item,
                    isSelected = selection.contains(item.id),
                    selectionMode = selection.isActive,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                    sharedKey = sharedMediaKey(item.id, sharedKeyNamespace),
                )
            }
        }

        DraggableScrollbar(
            state = state,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(contentPadding),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    selectionMode: Boolean,
    allSelected: Boolean,
    onClick: () -> Unit,
) {
    val motion = MaterialTheme.motionScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { if (selectionMode) role = Role.Checkbox }
            .padding(
                start = GalvaDimens.ScreenPadding,
                end = GalvaDimens.ScreenPadding,
                top = GalvaDimens.SectionSpacing,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AnimatedVisibility(
            visible = selectionMode,
            enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
            exit = scaleOut(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
        ) {
            Icon(
                imageVector = if (allSelected) Icons.Rounded.CheckCircle
                else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (allSelected) "Deselect day" else "Select day",
                tint = if (allSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val CONTENT_TYPE_HEADER = "header"
private const val CONTENT_TYPE_MEDIA = "media"
