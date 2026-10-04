package com.abrarshakhi.galva.core.designsystem.markdown

sealed interface MarkdownBlock {

    data class Heading(val level: Int, val text: String) : MarkdownBlock

    data class Paragraph(val text: String) : MarkdownBlock

    data class Bullet(val text: String) : MarkdownBlock

    data object Divider : MarkdownBlock
}

sealed interface MarkdownInline {

    data class Plain(val text: String) : MarkdownInline

    data class Bold(val text: String) : MarkdownInline

    data class Link(val text: String, val url: String) : MarkdownInline
}

fun parseMarkdown(source: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val pending = StringBuilder()
    var pendingIsBullet = false

    fun flush() {
        if (pending.isEmpty()) return
        val text = pending.toString()
        blocks += if (pendingIsBullet) MarkdownBlock.Bullet(text) else MarkdownBlock.Paragraph(text)
        pending.clear()
        pendingIsBullet = false
    }

    source.lineSequence().map(String::trim).forEach { line ->
        val heading = HEADING.matchEntire(line)
        when {
            line.isEmpty() -> flush()
            heading != null -> {
                flush()
                blocks += MarkdownBlock.Heading(heading.groupValues[1].length, heading.groupValues[2])
            }
            line == DIVIDER -> {
                flush()
                blocks += MarkdownBlock.Divider
            }
            BULLET.any(line::startsWith) -> {
                flush()
                pending.append(line.drop(BULLET_PREFIX_LENGTH).trim())
                pendingIsBullet = true
            }
            else -> {
                if (pending.isNotEmpty()) pending.append(' ')
                pending.append(line)
            }
        }
    }
    flush()
    return blocks
}

fun parseInline(text: String): List<MarkdownInline> {
    val spans = mutableListOf<MarkdownInline>()
    val plain = StringBuilder()
    var index = 0

    fun flushPlain() {
        if (plain.isNotEmpty()) spans += MarkdownInline.Plain(plain.toString())
        plain.clear()
    }

    while (index < text.length) {
        val bold = if (text.startsWith(BOLD, index)) text.indexOf(BOLD, index + BOLD.length) else -1
        val link = if (text[index] == '[') LINK.matchAt(text, index) else null
        when {
            bold > index -> {
                flushPlain()
                spans += MarkdownInline.Bold(text.substring(index + BOLD.length, bold))
                index = bold + BOLD.length
            }
            link != null -> {
                flushPlain()
                spans += MarkdownInline.Link(link.groupValues[1], link.groupValues[2])
                index = link.range.last + 1
            }
            else -> {
                plain.append(text[index])
                index++
            }
        }
    }
    flushPlain()
    return spans
}

private val HEADING = Regex("^(#{1,6})\\s+(.+)$")
private val LINK = Regex("\\[([^\\]]+)]\\(([^)\\s]+)\\)")
private val BULLET = listOf("- ", "* ")
private const val BULLET_PREFIX_LENGTH = 2
private const val DIVIDER = "---"
private const val BOLD = "**"
