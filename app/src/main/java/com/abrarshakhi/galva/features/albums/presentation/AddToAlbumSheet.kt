package com.abrarshakhi.galva.features.albums.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToAlbumSheet(
    mediaIds: List<Long>,
    onDismiss: () -> Unit,
    onAdded: (albumName: String) -> Unit,
) {
}