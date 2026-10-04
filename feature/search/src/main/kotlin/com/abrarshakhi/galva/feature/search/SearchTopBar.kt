package com.abrarshakhi.galva.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.abrarshakhi.galva.core.model.MediaFilter

@Composable
internal fun SearchHeader(
    query: String,
    filter: MediaFilter,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onFilterSelected: (MediaFilter) -> Unit,
) {
    Column(
        modifier = Modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
            .padding(top = 8.dp),
    ) {
        SearchField(query = query, onQueryChange = onQueryChange, onClear = onClear)
        FilterRow(selected = filter, onSelect = onFilterSelected)
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val motion = MaterialTheme.motionScheme
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text("Search photos and videos") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
                exit = scaleOut(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
            ) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

@Composable
private fun FilterRow(selected: MediaFilter, onSelect: (MediaFilter) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterOption.entries.forEach { option ->
            ToggleButton(
                checked = option.filter == selected,
                onCheckedChange = { onSelect(option.filter) },
                icon = { Icon(option.icon, contentDescription = null) },
            ) {
                Text(option.label)
            }
        }
    }
}

private enum class FilterOption(val filter: MediaFilter, val label: String, val icon: ImageVector) {
    All(MediaFilter.ALL, "All", Icons.Rounded.Apps),
    Photos(MediaFilter.IMAGES, "Photos", Icons.Rounded.Image),
    Videos(MediaFilter.VIDEOS, "Videos", Icons.Rounded.Videocam),
    Favorites(MediaFilter.FAVORITES, "Favorites", Icons.Rounded.Favorite),
}
