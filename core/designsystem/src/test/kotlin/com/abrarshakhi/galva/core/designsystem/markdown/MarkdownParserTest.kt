package com.abrarshakhi.galva.core.designsystem.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun `headings keep their level and text`() {
        assertEquals(
            listOf(MarkdownBlock.Heading(1, "Title"), MarkdownBlock.Heading(2, "Section")),
            parseMarkdown("# Title\n\n## Section"),
        )
    }

    @Test
    fun `wrapped lines join into one paragraph until a blank line`() {
        assertEquals(
            listOf(MarkdownBlock.Paragraph("one two"), MarkdownBlock.Paragraph("three")),
            parseMarkdown("one\ntwo\n\nthree"),
        )
    }

    @Test
    fun `bullets end the previous block and absorb their wrapped lines`() {
        assertEquals(
            listOf(
                MarkdownBlock.Paragraph("intro"),
                MarkdownBlock.Bullet("first item continues"),
                MarkdownBlock.Bullet("second"),
            ),
            parseMarkdown("intro\n- first item\n  continues\n* second"),
        )
    }

    @Test
    fun `a hash without a space is plain text`() {
        assertEquals(listOf(MarkdownBlock.Paragraph("#tag")), parseMarkdown("#tag"))
    }

    @Test
    fun `dividers split blocks`() {
        assertEquals(
            listOf(MarkdownBlock.Paragraph("a"), MarkdownBlock.Divider, MarkdownBlock.Paragraph("b")),
            parseMarkdown("a\n---\nb"),
        )
    }

    @Test
    fun `inline bold and links are split from plain text`() {
        assertEquals(
            listOf(
                MarkdownInline.Bold("Note:"),
                MarkdownInline.Plain(" see "),
                MarkdownInline.Link("GitHub", "https://github.com"),
                MarkdownInline.Plain("."),
            ),
            parseInline("**Note:** see [GitHub](https://github.com)."),
        )
    }

    @Test
    fun `unclosed markers stay as plain text`() {
        assertEquals(
            listOf(MarkdownInline.Plain("**open [link](no")),
            parseInline("**open [link](no"),
        )
    }
}
