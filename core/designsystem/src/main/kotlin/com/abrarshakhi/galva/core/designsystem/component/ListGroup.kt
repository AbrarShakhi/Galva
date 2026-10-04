package com.abrarshakhi.galva.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun ListGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
            content = content,
        )
    }
}

@Composable
fun ListGroupItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val itemContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
    if (onClick == null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = InnerShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            content = itemContent,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = InnerShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            content = itemContent,
        )
    }
}

private val GroupGap = 2.dp
private val InnerShape = RoundedCornerShape(4.dp)
