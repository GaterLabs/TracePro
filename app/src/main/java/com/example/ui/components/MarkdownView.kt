package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Full-syntax Markdown renderer for Compose.
 * Supports Headers, Bullet & Numbered Lists, Task Checkboxes, Blockquotes,
 * Code Blocks with Copy button, Tables, Dividers, and rich inline formatting
 * (Bold, Italic, Strikethrough, Inline Code, Links).
 */
@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    isDarkBubble: Boolean = false,
    baseTextColor: Color = if (isDarkBubble) Color.White else Slate900
) {
    val blocks = parseMarkdownBlocks(markdown)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Header -> {
                    val style = when (block.level) {
                        1 -> MarkdownHeaderStyle(17.sp, FontWeight.ExtraBold, if (isDarkBubble) Color.White else Slate950, 6.dp)
                        2 -> MarkdownHeaderStyle(15.sp, FontWeight.Bold, if (isDarkBubble) Color.White else Slate900, 4.dp)
                        3 -> MarkdownHeaderStyle(13.5.sp, FontWeight.Bold, if (isDarkBubble) EmeraldBorder else Color(0xFF047857), 2.dp)
                        else -> MarkdownHeaderStyle(12.5.sp, FontWeight.SemiBold, if (isDarkBubble) Color.White else Slate800, 0.dp)
                    }
                    Spacer(modifier = Modifier.height(style.topSpace))
                    Text(
                        text = buildAnnotatedMarkdown(block.text, isDarkBubble, baseTextColor),
                        fontSize = style.fontSize,
                        fontWeight = style.fontWeight,
                        color = style.color,
                        lineHeight = (style.fontSize.value * 1.3).sp
                    )
                    if (block.level == 1) {
                        HorizontalDivider(
                            color = if (isDarkBubble) Color.White.copy(alpha = 0.2f) else Slate200,
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildAnnotatedMarkdown(block.text, isDarkBubble, baseTextColor),
                        fontSize = 12.5.sp,
                        color = baseTextColor,
                        lineHeight = 18.sp
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.depth * 12).dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isDarkBubble) EmeraldBorder else EmeraldPrimary)
                        )
                        Text(
                            text = buildAnnotatedMarkdown(block.text, isDarkBubble, baseTextColor),
                            fontSize = 12.5.sp,
                            color = baseTextColor,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.depth * 12).dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (isDarkBubble) Color.White.copy(alpha = 0.15f) else Color(0xFFECFDF5),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = block.number,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkBubble) Color.White else EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = buildAnnotatedMarkdown(block.text, isDarkBubble, baseTextColor),
                            fontSize = 12.5.sp,
                            color = baseTextColor,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.TaskItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (block.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (block.isChecked) (if (isDarkBubble) Color.White else EmeraldPrimary) else (if (isDarkBubble) Slate400 else Slate400),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Text(
                            text = buildAnnotatedMarkdown(block.text, isDarkBubble, baseTextColor),
                            fontSize = 12.5.sp,
                            color = baseTextColor,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.BlockQuote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isDarkBubble) Color.White.copy(alpha = 0.08f)
                                else Color(0xFFF1F5F9)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isDarkBubble) EmeraldBorder else EmeraldPrimary)
                        )
                        Text(
                            text = buildAnnotatedMarkdown(block.text, isDarkBubble, baseTextColor),
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = if (isDarkBubble) Color(0xFFE2E8F0) else Slate700,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    val context = LocalContext.current
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = block.language.ifBlank { "code" }.uppercase(),
                                    color = EmeraldBorder,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", block.code))
                                        Toast.makeText(context, "Kode disalin!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Salin Kode",
                                        tint = Slate400,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = Slate800, thickness = 1.dp, modifier = Modifier.padding(bottom = 6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = block.code,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.Table -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkBubble) Color.White.copy(alpha = 0.2f) else Slate200),
                        color = if (isDarkBubble) Color.White.copy(alpha = 0.05f) else Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            // Headers
                            if (block.headers.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDarkBubble) Slate800 else Slate100, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    block.headers.forEach { header ->
                                        Text(
                                            text = buildAnnotatedMarkdown(header, isDarkBubble, baseTextColor),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = if (isDarkBubble) Color.White else Slate900,
                                            modifier = Modifier
                                                .widthIn(min = 90.dp)
                                                .padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                            // Rows
                            block.rows.forEachIndexed { idx, row ->
                                val rowBg = if (idx % 2 == 1) {
                                    if (isDarkBubble) Color.White.copy(alpha = 0.04f) else Slate50
                                } else Color.Transparent
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(horizontal = 6.dp, vertical = 5.dp)
                                ) {
                                    row.forEach { cell ->
                                        Text(
                                            text = buildAnnotatedMarkdown(cell, isDarkBubble, baseTextColor),
                                            fontSize = 11.sp,
                                            color = baseTextColor,
                                            modifier = Modifier
                                                .widthIn(min = 90.dp)
                                                .padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = if (isDarkBubble) Color.White.copy(alpha = 0.25f) else Slate200,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private data class MarkdownHeaderStyle(
    val fontSize: androidx.compose.ui.unit.TextUnit,
    val fontWeight: FontWeight,
    val color: Color,
    val topSpace: androidx.compose.ui.unit.Dp
)

/**
 * Builds rich inline AnnotatedString from Markdown text.
 * Handles bold (**text**), italic (*text*), strikethrough (~~text~~),
 * inline code (`code`), and links ([label](url)).
 */
fun buildAnnotatedMarkdown(
    raw: String,
    isDarkBubble: Boolean = false,
    baseTextColor: Color = if (isDarkBubble) Color.White else Slate900
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    val len = raw.length

    val inlineCodeBg = if (isDarkBubble) Color.White.copy(alpha = 0.18f) else Color(0xFFF1F5F9)
    val inlineCodeColor = if (isDarkBubble) Color(0xFFA7F3D0) else Color(0xFF047857)

    while (i < len) {
        // Inline code: `code`
        if (raw[i] == '`') {
            val endIdx = raw.indexOf('`', i + 1)
            if (endIdx != -1) {
                val codeText = raw.substring(i + 1, endIdx)
                val start = builder.length
                builder.append(" $codeText ")
                builder.addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = inlineCodeBg,
                        color = inlineCodeColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    ),
                    start,
                    builder.length
                )
                i = endIdx + 1
                continue
            }
        }

        // Bold + Italic: ***text***
        if (raw.startsWith("***", i) || raw.startsWith("___", i)) {
            val tag = raw.substring(i, i + 3)
            val endIdx = raw.indexOf(tag, i + 3)
            if (endIdx != -1) {
                val inner = raw.substring(i + 3, endIdx)
                val start = builder.length
                builder.append(inner)
                builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic),
                    start,
                    builder.length
                )
                i = endIdx + 3
                continue
            }
        }

        // Bold: **text** or __text__
        if (raw.startsWith("**", i) || raw.startsWith("__", i)) {
            val tag = raw.substring(i, i + 2)
            val endIdx = raw.indexOf(tag, i + 2)
            if (endIdx != -1) {
                val inner = raw.substring(i + 2, endIdx)
                val start = builder.length
                builder.append(inner)
                builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    start,
                    builder.length
                )
                i = endIdx + 2
                continue
            }
        }

        // Strikethrough: ~~text~~
        if (raw.startsWith("~~", i)) {
            val endIdx = raw.indexOf("~~", i + 2)
            if (endIdx != -1) {
                val inner = raw.substring(i + 2, endIdx)
                val start = builder.length
                builder.append(inner)
                builder.addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    start,
                    builder.length
                )
                i = endIdx + 2
                continue
            }
        }

        // Italic: *text* or _text_
        if ((raw[i] == '*' || raw[i] == '_') && i + 1 < len && raw[i + 1] != ' ') {
            val tag = raw[i]
            val endIdx = raw.indexOf(tag, i + 1)
            if (endIdx != -1 && endIdx > i + 1) {
                val inner = raw.substring(i + 1, endIdx)
                val start = builder.length
                builder.append(inner)
                builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    start,
                    builder.length
                )
                i = endIdx + 1
                continue
            }
        }

        // Link: [text](url)
        if (raw[i] == '[') {
            val closeBracket = raw.indexOf(']', i + 1)
            if (closeBracket != -1 && closeBracket + 1 < len && raw[closeBracket + 1] == '(') {
                val closeParen = raw.indexOf(')', closeBracket + 2)
                if (closeParen != -1) {
                    val linkText = raw.substring(i + 1, closeBracket)
                    val start = builder.length
                    builder.append(linkText)
                    builder.addStyle(
                        SpanStyle(
                            color = if (isDarkBubble) EmeraldBorder else Color(0xFF0284C7),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.SemiBold
                        ),
                        start,
                        builder.length
                    )
                    i = closeParen + 1
                    continue
                }
            }
        }

        // Default character
        builder.append(raw[i])
        i++
    }

    return builder.toAnnotatedString()
}

/**
 * Sealed class representing top-level Markdown structure blocks.
 */
sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class BulletItem(val text: String, val depth: Int = 0) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String, val depth: Int = 0) : MarkdownBlock()
    data class TaskItem(val isChecked: Boolean, val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

/**
 * Parser converts Markdown raw text into ordered MarkdownBlock elements.
 */
fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val rawLine = lines[i]
        val trimmed = rawLine.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // 1. Code Block: ```lang
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        // 2. Horizontal Divider: ---, ***, ___
        if (trimmed == "---" || trimmed == "***" || trimmed == "___" || trimmed == "--------------------------------------------------") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // 3. Table: | col1 | col2 |
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size && lines[i + 1].trim().contains("---")) {
            val headers = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            i += 2 // skip header and separator row
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                val rowCells = lines[i].trim().split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (rowCells.isNotEmpty()) {
                    rows.add(rowCells)
                }
                i++
            }
            blocks.add(MarkdownBlock.Table(headers, rows))
            continue
        }

        // 4. Headers: #, ##, ###, ####
        if (trimmed.startsWith("#")) {
            var level = 0
            while (level < trimmed.length && trimmed[level] == '#') {
                level++
            }
            if (level in 1..6 && level < trimmed.length && trimmed[level] == ' ') {
                val headerText = trimmed.substring(level + 1).trim()
                blocks.add(MarkdownBlock.Header(level, headerText))
                i++
                continue
            }
        }

        // 5. Task List: - [ ] or - [x]
        if (trimmed.startsWith("- [ ]") || trimmed.startsWith("* [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]") || trimmed.startsWith("* [x]")) {
            val isChecked = trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]") || trimmed.startsWith("* [x]")
            val text = trimmed.substring(5).trim()
            blocks.add(MarkdownBlock.TaskItem(isChecked, text))
            i++
            continue
        }

        // 6. BlockQuote: > Quote
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.BlockQuote(quoteLines.joinToString("\n")))
            continue
        }

        // 7. Numbered List: 1. or 2)
        val numMatch = Regex("""^(\d+[\.\)])\s+(.*)""").find(trimmed)
        if (numMatch != null) {
            val number = numMatch.groupValues[1]
            val text = numMatch.groupValues[2]
            val leadingSpaces = rawLine.takeWhile { it == ' ' }.length
            val depth = leadingSpaces / 2
            blocks.add(MarkdownBlock.NumberedItem(number, text, depth))
            i++
            continue
        }

        // 8. Bullet List: * , - , + , •
        if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("+ ") || trimmed.startsWith("• ")) {
            val bulletText = trimmed.substring(2).trim()
            val leadingSpaces = rawLine.takeWhile { it == ' ' }.length
            val depth = leadingSpaces / 2
            blocks.add(MarkdownBlock.BulletItem(bulletText, depth))
            i++
            continue
        }

        // 9. Standard Paragraph (or multi-line paragraph)
        val paraLines = mutableListOf<String>()
        while (i < lines.size) {
            val curTrimmed = lines[i].trim()
            if (curTrimmed.isEmpty() ||
                curTrimmed.startsWith("#") ||
                curTrimmed.startsWith("```") ||
                curTrimmed.startsWith("- ") ||
                curTrimmed.startsWith("* ") ||
                curTrimmed.startsWith("+ ") ||
                curTrimmed.startsWith("• ") ||
                curTrimmed.startsWith(">") ||
                curTrimmed == "---" ||
                curTrimmed.startsWith("|") ||
                Regex("""^(\d+[\.\)])\s+""").containsMatchIn(curTrimmed)
            ) {
                break
            }
            paraLines.add(curTrimmed)
            i++
        }
        if (paraLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString(" ")))
        }
    }

    return blocks
}
