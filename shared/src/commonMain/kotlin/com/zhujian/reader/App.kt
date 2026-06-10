package com.zhujian.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

private const val APP_NAME = "青简阅读"

private data class Book(
    val id: String,
    val title: String,
    val author: String = "本地书籍",
    val format: BookFormat,
    val sourceText: String,
    val chapters: List<Chapter>,
    var progressChapter: Int = 0,
    var isPinned: Boolean = false,
    val bookmarks: MutableList<Bookmark> = mutableListOf(),
)

private enum class BookFormat { TXT, EPUB, PDF, MOBI, AZW3 }

private data class Chapter(
    val title: String,
    val paragraphs: List<String>,
)

private data class Bookmark(
    val chapterIndex: Int,
    val chapterTitle: String,
    val createdAt: Long,
)

private data class ReaderSettings(
    val fontSize: Float = 20f,
    val lineHeight: Float = 1.65f,
    val paragraphSpacing: Float = 10f,
    val margin: Float = 20f,
    val theme: ReaderTheme = ReaderTheme.EyeCare,
    val volumeKeyTurnPage: Boolean = true,
)

private data class ReaderTheme(
    val id: String,
    val title: String,
    val background: Color,
    val foreground: Color,
) {
    companion object {
        val White = ReaderTheme("white", "纯白", Color(0xFFFAFAFA), Color(0xFF222222))
        val Kraft = ReaderTheme("kraft", "牛皮纸", Color(0xFFE7D3A4), Color(0xFF3E2E1D))
        val Parchment = ReaderTheme("parchment", "羊皮纸", Color(0xFFF1E2BE), Color(0xFF33281E))
        val EyeCare = ReaderTheme("eye", "护眼绿", Color(0xFFCFE8CF), Color(0xFF1F3321))
        val InkBlack = ReaderTheme("black", "水墨黑", Color(0xFF101214), Color(0xFFE6E6E6))
        val All = listOf(White, Kraft, Parchment, EyeCare, InkBlack)
        fun byId(id: String): ReaderTheme = All.firstOrNull { it.id == id } ?: EyeCare
    }
}

object ReaderKeyBridge {
    var onPrevious: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var volumeKeyEnabled: Boolean = true
}

@Composable
fun NovelReaderProApp(
    platformName: String,
    onImportTxt: ((title: String, content: String) -> Unit) -> Unit,
) {
    val books = remember { mutableStateListOf<Book>().apply { addAll(loadBooks()) } }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var settings by remember { mutableStateOf(loadSettings()) }

    fun persistAll() {
        saveBooks(books)
        saveSettings(settings)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val book = selectedBook
            if (book == null) {
                ReaderKeyBridge.onPrevious = null
                ReaderKeyBridge.onNext = null
                BookshelfScreen(
                    platformName = platformName,
                    books = books.sortedWith(compareByDescending<Book> { it.isPinned }.thenBy { it.title }),
                    onOpen = { selectedBook = it },
                    onImport = {
                        onImportTxt { title, content ->
                            val parsed = parseTxtBook(title, content)
                            books.add(0, parsed)
                            selectedBook = parsed
                            persistAll()
                        }
                    },
                    onTogglePin = { target ->
                        target.isPinned = !target.isPinned
                        saveBooks(books)
                    },
                    onDelete = { target ->
                        books.removeAll { it.id == target.id }
                        if (selectedBook?.id == target.id) selectedBook = null
                        saveBooks(books)
                    },
                )
            } else {
                ReaderScreen(
                    book = book,
                    settings = settings,
                    onSettingsChange = {
                        settings = it
                        ReaderKeyBridge.volumeKeyEnabled = it.volumeKeyTurnPage
                        saveSettings(it)
                    },
                    onBookChanged = { saveBooks(books) },
                    onBack = {
                        saveBooks(books)
                        selectedBook = null
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookshelfScreen(
    platformName: String,
    books: List<Book>,
    onOpen: (Book) -> Unit,
    onImport: () -> Unit,
    onTogglePin: (Book) -> Unit,
    onDelete: (Book) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$APP_NAME · $platformName") },
                actions = {
                    IconButton(onClick = onImport) { Icon(Icons.Default.Add, contentDescription = "导入") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("v0.2.0：本地书架、阅读进度、书签、排版设置已可保存；音量键可翻章。", color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            if (books.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("书架还是空的")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onImport) { Text("导入 TXT 小说") }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(books, key = { it.id }) { book ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(book) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(book.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onTogglePin(book) }) {
                                        Icon(Icons.Default.PushPin, contentDescription = "置顶", tint = if (book.isPinned) Color(0xFF2E7D32) else Color.Gray)
                                    }
                                    IconButton(onClick = { onDelete(book) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Gray)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("${book.author} · ${book.format} · ${book.chapters.size} 章 · ${book.bookmarks.size} 个书签")
                                val percent = if (book.chapters.isEmpty()) 0 else ((book.progressChapter + 1) * 100 / book.chapters.size)
                                Text("阅读进度：第 ${book.progressChapter + 1} 章 / ${book.chapters.size} 章 · $percent%", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(
    book: Book,
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onBookChanged: () -> Unit,
    onBack: () -> Unit,
) {
    var chapterIndex by remember(book.id) { mutableStateOf(book.progressChapter.coerceIn(0, book.chapters.lastIndex.coerceAtLeast(0))) }
    var showSettings by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val chapter = book.chapters.getOrNull(chapterIndex)
    val total = book.chapters.size.coerceAtLeast(1)

    fun goPrevious() {
        if (chapterIndex > 0) {
            chapterIndex--
            book.progressChapter = chapterIndex
            onBookChanged()
        }
    }

    fun goNext() {
        if (chapterIndex < book.chapters.lastIndex) {
            chapterIndex++
            book.progressChapter = chapterIndex
            onBookChanged()
        }
    }

    ReaderKeyBridge.volumeKeyEnabled = settings.volumeKeyTurnPage
    ReaderKeyBridge.onPrevious = { goPrevious() }
    ReaderKeyBridge.onNext = { goNext() }
    book.progressChapter = chapterIndex

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book.title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } },
                actions = {
                    IconButton(onClick = { showBookmarks = !showBookmarks }) { Icon(Icons.Default.Bookmark, contentDescription = "书签") }
                    IconButton(onClick = { showSettings = !showSettings }) { Icon(Icons.Default.Settings, contentDescription = "设置") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(settings.theme.background).padding(padding).padding(settings.margin.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { goPrevious() }, enabled = chapterIndex > 0) { Text("上一章") }
                Spacer(Modifier.width(8.dp))
                Text("${chapterIndex + 1}/$total · ${((chapterIndex + 1) * 100f / total).roundToInt()}%", color = settings.theme.foreground)
                Spacer(Modifier.width(8.dp))
                Button(onClick = { goNext() }, enabled = chapterIndex < book.chapters.lastIndex) { Text("下一章") }
                Spacer(Modifier.weight(1f))
                val bookmarked = book.bookmarks.any { it.chapterIndex == chapterIndex }
                IconButton(onClick = {
                    if (bookmarked) book.bookmarks.removeAll { it.chapterIndex == chapterIndex }
                    else book.bookmarks.add(Bookmark(chapterIndex, chapter?.title ?: "第${chapterIndex + 1}章", currentTimestamp()))
                    onBookChanged()
                }) {
                    Icon(if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "添加/取消书签", tint = settings.theme.foreground)
                }
            }

            if (showSettings) ReaderSettingsPanel(settings = settings, onChange = onSettingsChange)

            if (showBookmarks) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("书签", style = MaterialTheme.typography.titleMedium)
                        if (book.bookmarks.isEmpty()) Text("当前没有书签", color = Color.Gray)
                        book.bookmarks.sortedBy { it.chapterIndex }.forEach { mark ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { chapterIndex = mark.chapterIndex; book.progressChapter = chapterIndex; onBookChanged() }.padding(vertical = 6.dp)) {
                                Text("第${mark.chapterIndex + 1}章：${mark.chapterTitle}", modifier = Modifier.weight(1f))
                                Text("跳转", color = Color(0xFF1E88E5))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("全书搜索关键词") },
                singleLine = true,
            )
            if (query.isNotBlank()) {
                val results = searchBook(book, query).take(5)
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    results.forEach { result ->
                        Text(
                            text = "第${result.first + 1}章：${result.second}",
                            modifier = Modifier.clickable { chapterIndex = result.first; book.progressChapter = chapterIndex; onBookChanged() }.padding(vertical = 3.dp),
                            color = Color(0xFF1E88E5),
                            fontSize = 14.sp,
                        )
                    }
                    if (results.isEmpty()) Text("没有找到匹配内容", color = Color.Gray)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = chapter?.title ?: "无章节",
                        color = settings.theme.foreground,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                items(chapter?.paragraphs ?: emptyList()) { paragraph ->
                    Text(
                        text = paragraph,
                        color = settings.theme.foreground,
                        fontSize = settings.fontSize.sp,
                        lineHeight = (settings.fontSize * settings.lineHeight).sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = settings.paragraphSpacing.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderSettingsPanel(settings: ReaderSettings, onChange: (ReaderSettings) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FormatSize, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("字号：${settings.fontSize.roundToInt()}")
            }
            Slider(value = settings.fontSize, onValueChange = { onChange(settings.copy(fontSize = it)) }, valueRange = 14f..34f)
            Text("行距：${settings.lineHeight.roundToOneDecimal()}")
            Slider(value = settings.lineHeight, onValueChange = { onChange(settings.copy(lineHeight = it)) }, valueRange = 1.1f..2.4f)
            Text("页边距：${settings.margin.roundToInt()}")
            Slider(value = settings.margin, onValueChange = { onChange(settings.copy(margin = it)) }, valueRange = 8f..48f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReaderTheme.All.forEach { theme ->
                    Box(modifier = Modifier.background(theme.background).clickable { onChange(settings.copy(theme = theme)) }.padding(8.dp)) {
                        Text(theme.title, color = theme.foreground, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onChange(settings.copy(volumeKeyTurnPage = !settings.volumeKeyTurnPage)) }) {
                Text(if (settings.volumeKeyTurnPage) "音量键翻章：开" else "音量键翻章：关")
            }
        }
    }
}

private fun parseTxtBook(title: String, content: String): Book {
    val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
    val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() && !it.contains("最新网址", ignoreCase = true) }
    val chapterTitleRegex = Regex("^(第[零一二三四五六七八九十百千万0-9]+[章节回卷集部].*|Chapter\\s+\\d+.*|[0-9]+[、.].*)$", setOf(RegexOption.IGNORE_CASE))
    val chapters = mutableListOf<Chapter>()
    var currentTitle = "开始阅读"
    val paragraphs = mutableListOf<String>()

    fun flush() {
        if (paragraphs.isNotEmpty()) {
            chapters.add(Chapter(currentTitle, paragraphs.toList()))
            paragraphs.clear()
        }
    }

    for (line in lines) {
        val isTitle = line.length <= 80 && chapterTitleRegex.matches(line)
        if (isTitle && paragraphs.isNotEmpty()) {
            flush()
            currentTitle = line.take(80)
        } else if (isTitle) {
            currentTitle = line.take(80)
        } else {
            paragraphs.add(line)
        }
    }
    flush()
    if (chapters.isEmpty()) chapters.add(Chapter("正文", lines.ifEmpty { listOf("这本书暂无可读取文本。") }))
    val stableId = "txt-${abs((title + content.length.toString()).hashCode())}-${currentTimestamp()}"
    return Book(id = stableId, title = title.ifBlank { "未命名 TXT" }, format = BookFormat.TXT, sourceText = content, chapters = chapters)
}

private fun searchBook(book: Book, query: String): List<Pair<Int, String>> {
    val key = query.trim()
    if (key.isEmpty()) return emptyList()
    val results = mutableListOf<Pair<Int, String>>()
    book.chapters.forEachIndexed { index, chapter ->
        chapter.paragraphs.firstOrNull { it.contains(key, ignoreCase = true) }?.let { paragraph -> results.add(index to paragraph.take(60)) }
    }
    return results
}

private fun sampleBook(): Book = parseTxtBook(
    title = "欢迎使用$APP_NAME",
    content = """
        第一章 本地阅读器
        这是一个从零开始的新阅读器项目。当前版本实现 TXT 导入、自动章节识别、基础排版、主题切换和阅读进度保存。
        软件目标是纯净、离线、无广告、权限精简。

        第二章 后续路线
        下一步会继续加入 EPUB、PDF、书架分类、听书和批注笔记。
        功能会按版本逐步做扎实，不做臃肿半成品。
    """.trimIndent(),
)

private fun loadBooks(): List<Book> {
    val raw = persistentLoad("books_v2").orEmpty()
    if (raw.isBlank()) return listOf(sampleBook())
    return raw.split("\n---BOOK---\n").mapNotNull { block -> decodeBook(block) }.ifEmpty { listOf(sampleBook()) }
}

private fun saveBooks(books: List<Book>) {
    persistentSave("books_v2", books.joinToString("\n---BOOK---\n") { encodeBook(it) })
}

private fun loadSettings(): ReaderSettings {
    val raw = persistentLoad("settings_v2") ?: return ReaderSettings()
    val map = raw.lines().mapNotNull { line -> line.substringBefore('=', "").takeIf { it.isNotEmpty() }?.let { it to line.substringAfter('=') } }.toMap()
    return ReaderSettings(
        fontSize = map["fontSize"]?.toFloatOrNull() ?: 20f,
        lineHeight = map["lineHeight"]?.toFloatOrNull() ?: 1.65f,
        paragraphSpacing = map["paragraphSpacing"]?.toFloatOrNull() ?: 10f,
        margin = map["margin"]?.toFloatOrNull() ?: 20f,
        theme = ReaderTheme.byId(map["theme"].orEmpty()),
        volumeKeyTurnPage = map["volumeKeyTurnPage"]?.toBooleanStrictOrNull() ?: true,
    )
}

private fun saveSettings(settings: ReaderSettings) {
    persistentSave("settings_v2", listOf(
        "fontSize=${settings.fontSize}",
        "lineHeight=${settings.lineHeight}",
        "paragraphSpacing=${settings.paragraphSpacing}",
        "margin=${settings.margin}",
        "theme=${settings.theme.id}",
        "volumeKeyTurnPage=${settings.volumeKeyTurnPage}",
    ).joinToString("\n"))
}

private fun encodeBook(book: Book): String = listOf(
    "id=${esc(book.id)}",
    "title=${esc(book.title)}",
    "author=${esc(book.author)}",
    "format=${book.format.name}",
    "progressChapter=${book.progressChapter}",
    "isPinned=${book.isPinned}",
    "bookmarks=${esc(book.bookmarks.joinToString(";") { "${it.chapterIndex},${esc(it.chapterTitle)},${it.createdAt}" })}",
    "sourceText=${esc(book.sourceText)}",
).joinToString("\n")

private fun decodeBook(block: String): Book? {
    val map = block.lines().mapNotNull { line -> line.substringBefore('=', "").takeIf { it.isNotEmpty() }?.let { it to line.substringAfter('=') } }.toMap()
    val title = unesc(map["title"] ?: return null)
    val source = unesc(map["sourceText"] ?: return null)
    val book = parseTxtBook(title, source).copy(
        id = unesc(map["id"] ?: title),
        author = unesc(map["author"] ?: "本地书籍"),
        progressChapter = map["progressChapter"]?.toIntOrNull() ?: 0,
        isPinned = map["isPinned"]?.toBooleanStrictOrNull() ?: false,
    )
    val marks = unesc(map["bookmarks"].orEmpty())
    if (marks.isNotBlank()) {
        marks.split(';').forEach { item ->
            val parts = item.split(',', limit = 3)
            if (parts.size == 3) book.bookmarks.add(Bookmark(parts[0].toIntOrNull() ?: 0, unesc(parts[1]), parts[2].toLongOrNull() ?: currentTimestamp()))
        }
    }
    return book
}

private fun esc(value: String): String = value
    .replace("%", "%25")
    .replace("\n", "%0A")
    .replace("\r", "%0D")
    .replace("=", "%3D")
    .replace(";", "%3B")
    .replace(",", "%2C")

private fun unesc(value: String): String = value
    .replace("%2C", ",")
    .replace("%3B", ";")
    .replace("%3D", "=")
    .replace("%0D", "\r")
    .replace("%0A", "\n")
    .replace("%25", "%")

private fun Float.roundToOneDecimal(): String = (this * 10).roundToInt().let { "${it / 10}.${it % 10}" }

expect fun currentTimestamp(): Long
expect fun persistentLoad(key: String): String?
expect fun persistentSave(key: String, value: String)
