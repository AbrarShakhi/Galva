package com.abrarshakhi.galva.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abrarshakhi.galva.core.model.Album
import com.abrarshakhi.galva.core.model.AlbumRef

@Composable
fun AlbumTile(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.largeIncreased)
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AlbumCover(
            album = album,
            shape = MaterialTheme.shapes.largeIncreased,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(bottom = 6.dp),
        )
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = itemCountLabel(album.itemCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
fun AlbumListItem(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumCover(album = album, shape = MaterialTheme.shapes.large, modifier = Modifier.size(64.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = itemCountLabel(album.itemCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlbumCover(album: Album, shape: Shape, modifier: Modifier = Modifier) {
    val coverUri = album.coverUri
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (album.ref == AlbumRef.Favorites) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            coverUri != null -> AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            album.ref == AlbumRef.Favorites -> Box(
                modifier = Modifier
                    .fillMaxSize(HEART_FRACTION)
                    .clip(MaterialShapes.Heart.toShape())
                    .background(MaterialTheme.colorScheme.tertiary),
            )

            else -> Icon(
                imageVector = if (album.ref is AlbumRef.User) Icons.Rounded.PhotoAlbum
                else Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

fun itemCountLabel(count: Int): String = if (count == 1) "1 item" else "$count items"

private const val HEART_FRACTION = 0.42f
