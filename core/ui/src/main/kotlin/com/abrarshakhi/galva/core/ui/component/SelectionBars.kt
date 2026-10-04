package com.abrarshakhi.galva.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    count: Int,
    allSelected: Boolean,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val motion = MaterialTheme.motionScheme
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInVertically(motion.fastSpatialSpec()) { it * direction } +
                        fadeIn(motion.fastEffectsSpec())) togetherWith
                        (slideOutVertically(motion.fastSpatialSpec()) { -it * direction } +
                            fadeOut(motion.fastEffectsSpec()))
                },
                label = "selectionCount",
            ) { selected ->
                Text("$selected selected", style = MaterialTheme.typography.titleLargeEmphasized)
            }
        },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear selection")
            }
        },
        actions = {
            IconButton(onClick = if (allSelected) onClear else onSelectAll) {
                Icon(
                    imageVector = if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                    contentDescription = if (allSelected) "Select none" else "Select all",
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}
