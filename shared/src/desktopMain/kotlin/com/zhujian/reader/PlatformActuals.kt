package com.zhujian.reader

import java.awt.Desktop
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

actual fun currentTimestamp(): Long = System.currentTimeMillis()

private val dataDir: File by lazy {
    File(System.getProperty("user.home"), ".qingjian-reader").apply { mkdirs() }
}

actual fun persistentLoad(key: String): String? {
    val file = File(dataDir, "$key.txt")
    return if (file.exists()) file.readText(Charsets.UTF_8) else null
}

actual fun persistentSave(key: String, value: String) {
    File(dataDir, "$key.txt").writeText(value, Charsets.UTF_8)
}

actual fun parseImportedBook(fileName: String, bytes: ByteArray): Book {
    val lower = fileName.lowercase()
    return when {
        lower.endsWith(".epub") -> parseEpubBook(fileName, bytes)
        lower.endsWith(".pdf") -> parsePdfPlaceholder(fileName)
        else -> {
            val text = runCatching { bytes.toString(Charsets.UTF_8) }
                .getOrElse { bytes.toString(charset("GBK")) }
            parseTxtBook(fileName.substringBeforeLast('.'), text, fileName)
        }
    }
}

actual fun openLatestReleasePage() {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI("https://github.com/xiaoyebaba/NovelReaderPro/releases/latest"))
        }
    }
}

private fun parseEpubBook(fileName: String, bytes: ByteArray): Book {
    val chapters = mutableListOf<Chapter>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name.lowercase()
            if (!entry.isDirectory && (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm"))) {
                val html = zip.readBytes().toString(Charsets.UTF_8)
                val text = htmlToText(html)
                val paragraphs = text.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (paragraphs.isNotEmpty()) {
                    val title = paragraphs.firstOrNull()?.take(60)?.ifBlank { entry.name.substringAfterLast('/') }
                        ?: entry.name.substringAfterLast('/')
                    chapters.add(Chapter(title, paragraphs.drop(if (paragraphs.size > 1) 1 else 0).ifEmpty { paragraphs }))
                }
            }
            zip.closeEntry()
        }
    }
    val finalChapters = chapters.ifEmpty {
        listOf(Chapter("EPUB", listOf("EPUB 已导入，但暂未解析到可阅读正文。")))
    }
    return Book(
        id = "epub-${kotlin.math.abs((fileName + bytes.size).hashCode())}-${currentTimestamp()}",
        title = fileName.substringBeforeLast('.').ifBlank { "EPUB 电子书" },
        format = BookFormat.EPUB,
        sourceText = "EPUB:${fileName}:${bytes.size}",
        fileName = fileName,
        chapters = finalChapters,
    )
}

private fun htmlToText(html: String): String {
    return html
        .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("</p>|<br\\s*/?>|</h[1-6]>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace(Regex("[ \\t]+"), " ")
        .lines().joinToString("\n") { it.trim() }
}
