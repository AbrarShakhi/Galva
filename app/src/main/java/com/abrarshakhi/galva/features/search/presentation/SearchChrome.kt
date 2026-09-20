package com.abrarshakhi.galva.features.search.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.main.ScreenChrome
import com.abrarshakhi.galva.common.ui.appViewModel
import com.abrarshakhi.galva.common.ui.component.AppTabs
import com.abrarshakhi.galva.common.ui.component.SelectionActions
import com.abrarshakhi.galva.common.ui.component.SelectionTopBar
import com.abrarshakhi.galva.common.ui.selection.selectedBy

fun searchChrome(): ScreenChrome = ScreenChrome(
    topBar = {
        val viewModel: SearchViewModel = appViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.selection.isActive) {
            SelectionTopBar(
                count = state.selection.count,
                allSelected = state.allSelected,
                onClear = { viewModel.onIntent(SearchIntent.ClearSelection) },
                onSelectAll = { viewModel.onIntent(SearchIntent.SelectAll) },
            )
        } else {
            SearchField(
                query = state.query,
                onQueryChange = { viewModel.onIntent(SearchIntent.QueryChanged(it)) },
                onClear = { viewModel.onIntent(SearchIntent.QueryCleared) },
            )
        }
    },
    navigation = { scope ->
        val viewModel: SearchViewModel = appViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.selection.isActive) {
            val selected = state.results.selectedBy(state.selection)
            SelectionActions(
                layout = scope.layout,
                anySelected = selected.isNotEmpty(),
                allFavorite = selected.isNotEmpty() && selected.all { it.isFavorite },
                onShare = { viewModel.onIntent(SearchIntent.ShareSelection) },
                onFavorite = { viewModel.onIntent(SearchIntent.FavoriteSelection) },
                onAddToAlbum = { viewModel.onIntent(SearchIntent.AddToAlbumRequested) },
                onDelete = { viewModel.onIntent(SearchIntent.DeleteSelection) },
            )
        } else {
            AppTabs(
                layout = scope.layout,
                current = scope.currentRoute,
                onTabSelected = scope.switchTab,
            )
        }
    },
)

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Surface {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                )
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Search photos and videos") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        )
    }
}
