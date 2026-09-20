package com.abrarshakhi.galva.features.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.BuildConfig
import com.abrarshakhi.galva.common.ui.appViewModel
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens
import com.abrarshakhi.galva.core.settings.domain.AlbumViewType
import com.abrarshakhi.galva.core.settings.domain.AppSettings
import com.abrarshakhi.galva.core.settings.domain.ThemeMode

/**
 * Settings live in the navigation drawer rather than on a route, the way Ente presents them: they
 * are reachable from the timeline without ever leaving it.
 */
@Composable
fun SettingsDrawerContent(modifier: Modifier = Modifier) {
    // A modal drawer is capped at 360dp, which on a 360dp-wide phone is the entire screen — it
    // stops reading as a drawer at all. Leaving a strip of scrim is what makes it legible as one.
    BoxWithConstraints(modifier = modifier) {
        val sheetWidth = minOf(DrawerDefaults.MaximumDrawerWidth, maxWidth - ScrimGap)
        ModalDrawerSheet(modifier = Modifier.width(sheetWidth)) {
            SettingsSections()
        }
    }
}

/** How much of the screen behind the drawer stays visible. */
private val ScrimGap = 56.dp

@Composable
private fun SettingsSections() {
    val viewModel: SettingsViewModel = appViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(GalvaDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(GalvaDimens.SectionSpacing),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        SettingsSection("Appearance") {
            SegmentedChoice(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                label = { it.label },
                onSelect = viewModel::setThemeMode,
            )
        }

        HorizontalDivider()

        SettingsSection("Grid size") {
            SegmentedChoice(
                options = AppSettings.COLUMN_RANGE.toList(),
                selected = settings.galleryColumns,
                label = { it.toString() },
                onSelect = viewModel::setGalleryColumns,
            )
        }

        HorizontalDivider()

        SettingsSection("Albums") {
            SegmentedChoice(
                options = AlbumViewType.entries,
                selected = settings.albumViewType,
                label = { it.label },
                onSelect = viewModel::setAlbumViewType,
            )
        }

        HorizontalDivider()

        SettingsSection("About") {
            Text(
                text = "Galva ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "All photos stay on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label(option)) },
            )
        }
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

private val AlbumViewType.label: String
    get() = when (this) {
        AlbumViewType.GRID -> "Grid"
        AlbumViewType.LIST -> "List"
    }
