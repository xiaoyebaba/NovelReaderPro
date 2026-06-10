package com.zhujian.reader

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

private lateinit var appContext: Context

fun initializePlatform(context: Context) {
    appContext = context.applicationContext
}

actual fun currentTimestamp(): Long = System.currentTimeMillis()

actual fun persistentLoad(key: String): String? {
    if (!::appContext.isInitialized) return null
    return appContext.getSharedPreferences("qingjian_reader", Context.MODE_PRIVATE).getString(key, null)
}

actual fun persistentSave(key: String, value: String) {
    if (!::appContext.isInitialized) return
    appContext.getSharedPreferences("qingjian_reader", Context.MODE_PRIVATE).edit().putString(key, value).apply()
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
    if (!::appContext.isInitialized) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xiaoyebaba/NovelReaderPro/releases/latest"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    appContext.startActivity(intent)
}

private fun parseEpubBook(fileName: String, bytes: ByteArray): Book {
    val entries = mutableListOf<Pair<String, String>>()
    var metadataTitle: String? = null
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name
            val lower = name.lowercase()
            if (!entry.isDirectory) {
                val data = zip.readBytes()
                if (lower.endsWith(".opf")) {
                    val opf = data.toString(Charsets.UTF_8)
                    metadataTitle = Regex("<dc:title[^>]*>([\\s\\S]*?)</dc:title>", RegexOption.IGNORE_CASE)
                        .find(opf)?.groupValues?.getOrNull(1)?.let { htmlToText(it).trim() }?.takeIf { it.isNotBlank() }
                }
                if (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) {
                    entries.add(name to data.toString(Charsets.UTF_8))
                }
            }
            zip.closeEntry()
        }
    }
    val chapters = entries.sortedBy { it.first }.mapNotNull { (entryName, html) ->
        val heading = Regex("<h[1-6][^>]*>([\\s\\S]*?)</h[1-6]>", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.let { htmlToText(it).trim() }
        val text = htmlToText(html)
        val paragraphs = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) null else {
            val title = heading?.takeIf { it.isNotBlank() }?.take(80)
                ?: paragraphs.firstOrNull()?.take(60)?.takeIf { it.isNotBlank() }
                ?: entryName.substringAfterLast('/').substringBeforeLast('.')
            Chapter(title, paragraphs.drop(if (paragraphs.size > 1 && paragraphs.first() == title) 1 else 0).ifEmpty { paragraphs })
        }
    }
    val finalChapters = chapters.ifEmpty {
        listOf(Chapter("EPUB", listOf("EPUB 已导入，但暂未解析到可阅读正文。")))
    }
    return Book(
        id = "epub-${kotlin.math.abs((fileName + bytes.size).hashCode())}-${currentTimestamp()}",
        title = metadataTitle ?: fileName.substringBeforeLast('.').ifBlank { "EPUB 电子书" },
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
