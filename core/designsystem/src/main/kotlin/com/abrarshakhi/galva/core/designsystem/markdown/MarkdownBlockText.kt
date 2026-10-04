package com.abrarshakhi.galva.core.designsystem.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarkdownBlockText(block: MarkdownBlock, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography
    when (block) {
        is MarkdownBlock.Heading -> Text(
            text = rememberInline(block.text),
            style = when (block.level) {
                1 -> typography.headlineSmallEmphasized
                2 -> typography.titleLargeEmphasized
                else -> typography.titleMedium
            },
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.padding(top = 8.dp),
        )

        is MarkdownBlock.Paragraph -> Text(
            text = rememberInline(block.text),
            style = typography.bodyLarge,
            modifier = modifier,
        )

        is MarkdownBlock.Bullet -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.padding(top = BULLET_TOP),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
            Text(text = rememberInline(block.text), style = typography.bodyLarge)
        }

        MarkdownBlock.Divider -> HorizontalDivider(modifier = modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun rememberInline(text: String): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    return remember(text, linkColor) {
        val linkStyles = TextLinkStyles(
            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
        )
        buildAnnotatedString {
            parseInline(text).forEach { span ->
                when (span) {
                    is MarkdownInline.Plain -> append(span.text)
                    is MarkdownInline.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(span.text)
                    }
                    is MarkdownInline.Link -> withLink(LinkAnnotation.Url(span.url, linkStyles)) {
                        append(span.text)
                    }
                }
            }
        }
    }
}

private val BULLET_TOP = 10.dp
