package com.abrarshakhi.galva.feature.albums

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.abrarshakhi.galva.core.designsystem.component.AdaptiveTopAppBar
import com.abrarshakhi.galva.core.model.AlbumSort
import com.abrarshakhi.galva.core.model.AlbumViewType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AlbumsTopBar(
    albumCount: Int,
    viewType: AlbumViewType,
    sort: AlbumSort,
    onToggleViewType: () -> Unit,
    onSortSelected: (AlbumSort) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val motion = MaterialTheme.motionScheme
    AdaptiveTopAppBar(
        title = { Text("Albums") },
        subtitle = {
            if (albumCount > 0) Text(if (albumCount == 1) "1 album" else "$albumCount albums")
        },
        actions = {
            IconButton(onClick = onToggleViewType) {
                AnimatedContent(
                    targetState = viewType,
                    transitionSpec = {
                        (scaleIn(motion.fastSpatialSpec(), initialScale = 0.6f) +
                            fadeIn(motion.fastEffectsSpec())) togetherWith
                            fadeOut(motion.fastEffectsSpec())
                    },
                    label = "viewType",
                ) { type ->
                    Icon(
                        imageVector = if (type == AlbumViewType.GRID) Icons.Rounded.ViewAgenda
                        else Icons.Rounded.GridView,
                        contentDescription = if (type == AlbumViewType.GRID) "Show as list"
                        else "Show as grid",
                    )
                }
            }
            SortMenu(sort = sort, onSortSelected = onSortSelected)
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SortMenu(sort: AlbumSort, onSortSelected: (AlbumSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort albums")
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.large,
        ) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    trailingIcon = {
                        if (option.sort == sort) Icon(Icons.Rounded.Check, contentDescription = null)
                    },
                    onClick = {
                        onSortSelected(option.sort)
                        open = false
                    },
                )
            }
        }
    }
}

private enum class SortOption(val sort: AlbumSort, val label: String) {
    Recent(AlbumSort.RECENT_FIRST, "Last updated"),
    Oldest(AlbumSort.OLDEST_FIRST, "Oldest first"),
    NameAsc(AlbumSort.NAME_ASC, "Name (A–Z)"),
    NameDesc(AlbumSort.NAME_DESC, "Name (Z–A)"),
}
