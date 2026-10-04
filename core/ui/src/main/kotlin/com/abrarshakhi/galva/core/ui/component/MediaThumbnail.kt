package com.abrarshakhi.galva.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.ui.transition.sharedMediaBounds
import com.abrarshakhi.galva.core.ui.transition.thumbnailCacheKey
import com.abrarshakhi.galva.core.ui.util.formatDuration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnail(
    item: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    sharedKey: String? = null,
) {
    val motion = MaterialTheme.motionScheme
    val scale by animateFloatAsState(
        targetValue = if (isSelected) SELECTED_SCALE else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "thumbnailScale",
    )
    val corner by animateDpAsState(
        targetValue = when {
            isSelected -> 20.dp
            selectionMode -> 6.dp
            else -> 0.dp
        },
        animationSpec = motion.fastSpatialSpec(),
        label = "thumbnailCorner",
    )
    val background by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = motion.defaultEffectsSpec(),
        label = "thumbnailBackground",
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics {
                contentDescription = item.displayName
                if (selectionMode) selected = isSelected
            },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .memoryCacheKey(thumbnailCacheKey(item.uri))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .sharedMediaBounds(sharedKey)
                .scale(scale)
                .clip(RoundedCornerShape(corner.coerceAtLeast(0.dp))),
        )

        if (item.isVideo) {
            VideoDurationBadge(
                durationMs = item.durationMs,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
            )
        }

        if (item.isFavorite) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(16.dp),
            )
        }

        AnimatedVisibility(
            visible = selectionMode,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
            exit = scaleOut(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
        ) {
            SelectionMark(selected = isSelected)
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    if (selected) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .background(SCRIM, CircleShape),
        )
    }
}

@Composable
private fun VideoDurationBadge(durationMs: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(SCRIM, CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

private const val SELECTED_SCALE = 0.84f
private val SCRIM = Color(0x66000000)
