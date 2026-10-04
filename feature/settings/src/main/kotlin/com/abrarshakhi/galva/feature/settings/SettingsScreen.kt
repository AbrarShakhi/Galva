package com.abrarshakhi.galva.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.designsystem.component.AdaptiveTopAppBar
import com.abrarshakhi.galva.core.designsystem.component.ConnectedChoiceGroup
import com.abrarshakhi.galva.core.designsystem.component.ListGroup
import com.abrarshakhi.galva.core.designsystem.component.ListGroupItem
import com.abrarshakhi.galva.core.designsystem.layout.ReadableWidth
import com.abrarshakhi.galva.core.designsystem.shape.MorphShape
import com.abrarshakhi.galva.core.designsystem.theme.supportsWallpaperColors
import com.abrarshakhi.galva.core.model.AlbumViewType
import com.abrarshakhi.galva.core.model.AppDocument
import com.abrarshakhi.galva.core.model.AppSettings
import com.abrarshakhi.galva.core.model.ThemeColor
import com.abrarshakhi.galva.core.model.ThemeMode
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(onBack: () -> Unit, onOpenDocument: (AppDocument) -> Unit) {
    SettingsScreen(viewModel = koinViewModel(), onBack = onBack, onOpenDocument = onOpenDocument)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenDocument: (AppDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AdaptiveTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { ReadableWidth { AppearanceGroup(settings, viewModel) } }
            item { ReadableWidth { LibraryGroup(settings, viewModel) } }
            item { ReadableWidth { AboutGroup(onOpenDocument) } }
            item { ReadableWidth { LegalGroup(onOpenDocument) } }
        }
    }
}

@Composable
private fun AppearanceGroup(settings: AppSettings, viewModel: SettingsViewModel) {
    ListGroup(title = "Appearance") {
        ListGroupItem {
            SettingTitle("Theme")
            ConnectedChoiceGroup(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                onSelect = viewModel::setThemeMode,
                label = { it.label },
            )
        }
        ListGroupItem {
            SettingTitle(
                title = "Color",
                supporting = if (settings.themeColor == ThemeColor.WALLPAPER) {
                    "Matches your wallpaper"
                } else {
                    "${settings.themeColor.label} accent"
                },
            )
            ColorPicker(selected = settings.themeColor, onSelect = viewModel::setThemeColor)
        }
        ListGroupItem(onClick = { viewModel.setPureBlack(!settings.pureBlack) }) {
            SwitchRow(
                title = "Pure black",
                supporting = "Use true black backgrounds in dark theme",
                checked = settings.pureBlack,
                onCheckedChange = viewModel::setPureBlack,
            )
        }
    }
}

@Composable
private fun LibraryGroup(settings: AppSettings, viewModel: SettingsViewModel) {
    ListGroup(title = "Library") {
        ListGroupItem {
            var columns by remember(settings.galleryColumns) {
                mutableFloatStateOf(settings.galleryColumns.toFloat())
            }
            SettingTitle(title = "Grid size", supporting = "${columns.roundToInt()} photos per row")
            Slider(
                value = columns,
                onValueChange = { columns = it },
                onValueChangeFinished = { viewModel.setGalleryColumns(columns.roundToInt()) },
                valueRange = AppSettings.COLUMN_RANGE.first.toFloat()..AppSettings.COLUMN_RANGE.last.toFloat(),
                steps = AppSettings.COLUMN_RANGE.last - AppSettings.COLUMN_RANGE.first - 1,
            )
        }
        ListGroupItem {
            SettingTitle("Album layout")
            ConnectedChoiceGroup(
                options = AlbumViewType.entries,
                selected = settings.albumViewType,
                onSelect = viewModel::setAlbumViewType,
                label = { it.label },
                icon = { it.icon },
            )
        }
    }
}

@Composable
private fun AboutGroup(onOpenDocument: (AppDocument) -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    ListGroup(title = "About") {
        ListGroupItem {
            SettingTitle(title = "Galva $versionName", supporting = "Your photos never leave this device")
        }
        LinkRow(
            icon = Icons.Rounded.Info,
            title = "About Galva",
            onClick = { onOpenDocument(AppDocument.ABOUT) },
        )
        LinkRow(
            icon = Icons.Rounded.Code,
            title = "Source code",
            supporting = "Open source on GitHub",
            trailing = Icons.AutoMirrored.Rounded.OpenInNew,
            onClick = { runCatching { uriHandler.openUri(SOURCE_CODE_URL) } },
        )
    }
}

@Composable
private fun LegalGroup(onOpenDocument: (AppDocument) -> Unit) {
    ListGroup(title = "Legal", modifier = Modifier.navigationBarsPadding()) {
        LinkRow(
            icon = Icons.Rounded.Gavel,
            title = "Terms of service",
            onClick = { onOpenDocument(AppDocument.TERMS) },
        )
        LinkRow(
            icon = Icons.Rounded.PrivacyTip,
            title = "Privacy policy",
            onClick = { onOpenDocument(AppDocument.PRIVACY) },
        )
        LinkRow(
            icon = Icons.Rounded.VolunteerActivism,
            title = "Credits",
            supporting = "Open-source libraries and licenses",
            onClick = { onOpenDocument(AppDocument.CREDITS) },
        )
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    supporting: String? = null,
    trailing: ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
) {
    ListGroupItem(onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.weight(1f)) {
                SettingTitle(title = title, supporting = supporting)
            }
            Icon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingTitle(title: String, supporting: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) { SettingTitle(title, supporting) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(selected: ThemeColor, onSelect: (ThemeColor) -> Unit) {
    val context = LocalContext.current
    val options = remember {
        ThemeColor.entries.filter { it != ThemeColor.WALLPAPER || supportsWallpaperColors() }
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        options.forEach { option ->
            val color = if (option == ThemeColor.WALLPAPER && supportsWallpaperColors()) {
                Color(context.getColor(android.R.color.system_accent1_500))
            } else {
                Color(option.seedArgb)
            }
            ColorSwatch(
                color = color,
                label = option.label,
                selected = option == selected,
                showWallpaperIcon = option == ThemeColor.WALLPAPER,
                onClick = { onSelect(option) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    showWallpaperIcon: Boolean,
    onClick: () -> Unit,
) {
    val motion = MaterialTheme.motionScheme
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "swatchMorph",
    )
    val rotation by animateFloatAsState(
        targetValue = if (selected) SELECTED_ROTATION else 0f,
        animationSpec = motion.slowSpatialSpec(),
        label = "swatchRotation",
    )
    val contentColor = if (color.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(SWATCH_SIZE.dp)
            .graphicsLayer { rotationZ = rotation }
            .clip(MorphShape(morph, progress.coerceIn(0f, 1f)))
            .background(color)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.graphicsLayer { rotationZ = -rotation }) {
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
                exit = scaleOut(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = contentColor)
            }
            if (showWallpaperIcon && !selected) {
                Icon(
                    imageVector = Icons.Rounded.Wallpaper,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.padding(2.dp),
                )
            }
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

private val AlbumViewType.icon
    get() = when (this) {
        AlbumViewType.GRID -> Icons.Rounded.GridView
        AlbumViewType.LIST -> Icons.Rounded.ViewAgenda
    }

private val ThemeColor.label: String
    get() = when (this) {
        ThemeColor.WALLPAPER -> "Wallpaper"
        ThemeColor.EMERALD -> "Emerald"
        ThemeColor.OCEAN -> "Ocean"
        ThemeColor.VIOLET -> "Violet"
        ThemeColor.ROSE -> "Rose"
        ThemeColor.SUNSET -> "Sunset"
        ThemeColor.SAND -> "Sand"
    }

private const val SWATCH_SIZE = 52
private const val SELECTED_ROTATION = 90f

private const val SOURCE_CODE_URL = "https://github.com/AbrarShakhi/Galva"
