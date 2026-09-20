package com.abrarshakhi.galva.common.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abrarshakhi.galva.common.ui.selection.SelectionState
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens
import com.abrarshakhi.galva.common.ui.util.adaptiveColumnCount
import com.abrarshakhi.galva.common.ui.util.rememberTimelineDateFormatter
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.TimelineSection

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
                    Text(
                        text = dateFormatter.format(section.date),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHeaderClick(section) }
                            .padding(
                                start = GalvaDimens.ScreenPadding,
                                end = GalvaDimens.ScreenPadding,
                                top = GalvaDimens.SectionSpacing,
                                bottom = 8.dp,
                            ),
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
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                    )
                }
            }
        }

        DraggableScrollbar(
            state = state,
            labelForIndex = labelForIndex,
            modifier = Modifier.align(Alignment.TopEnd),
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
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                )
            }
        }

        DraggableScrollbar(
            state = state,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

private const val CONTENT_TYPE_HEADER = "header"
private const val CONTENT_TYPE_MEDIA = "media"
