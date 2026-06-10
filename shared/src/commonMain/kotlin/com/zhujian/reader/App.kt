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
import androidx.compose.material.icons.filled.FormatSize
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
import kotlin.math.roundToInt

private data class Book(
    val id: String,
    val title: String,
    val author: String = "本地书籍",
    val format: BookFormat,
    val chapters: List<Chapter>,
    var progressChapter: Int = 0,
    var progressParagraph: Int = 0,
)

private enum class BookFormat { TXT, EPUB, PDF, MOBI, AZW3 }

private data class Chapter(
    val title: String,
    val paragraphs: List<String>,
)

private data class ReaderSettings(
    val fontSize: Float = 20f,
    val lineHeight: Float = 1.65f,
    val paragraphSpacing: Float = 10f,
    val margin: Float = 20f,
    val theme: ReaderTheme = ReaderTheme.EyeCare,
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
    }
}

@Composable
fun NovelReaderProApp(
    platformName: String,
    onImportTxt: ((title: String, content: String) -> Unit) -> Unit,
) {
    val books = remember { mutableStateListOf(sampleBook()) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var settings by remember { mutableStateOf(ReaderSettings()) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val book = selectedBook
            if (book == null) {
                BookshelfScreen(
                    platformName = platformName,
                    books = books,
                    onOpen = { selectedBook = it },
                    onImport = {
                        onImportTxt { title, content ->
                            val parsed = parseTxtBook(title, content)
                            books.add(0, parsed)
                            selectedBook = parsed
                        }
                    },
                )
            } else {
                ReaderScreen(
                    book = book,
                    settings = settings,
                    onSettingsChange = { settings = it },
                    onBack = { selectedBook = null },
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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("竹简阅读 Pro · $platformName") },
                actions = {
                    IconButton(onClick = onImport) {
                        Icon(Icons.Default.Add, contentDescription = "导入")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("纯本地阅读器首个可运行版本：TXT 导入、章节识别、阅读排版、主题、书签占位、搜索。", color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(books) { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(book) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(book.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text("${book.author} · ${book.format} · ${book.chapters.size} 章")
                            val percent = if (book.chapters.isEmpty()) 0 else ((book.progressChapter + 1) * 100 / book.chapters.size)
                            Text("阅读进度：$percent%", color = Color.Gray)
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
    onBack: () -> Unit,
) {
    var chapterIndex by remember(book.id) { mutableStateOf(book.progressChapter.coerceIn(0, book.chapters.lastIndex.coerceAtLeast(0))) }
    var showSettings by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val chapter = book.chapters.getOrNull(chapterIndex)
    val total = book.chapters.size.coerceAtLeast(1)
    book.progressChapter = chapterIndex

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .background(settings.theme.background)
                .padding(padding)
                .padding(settings.margin.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { if (chapterIndex > 0) chapterIndex-- }, enabled = chapterIndex > 0) { Text("上一章") }
                Spacer(Modifier.width(8.dp))
                Text("${chapterIndex + 1}/$total · ${((chapterIndex + 1) * 100f / total).roundToInt()}%", color = settings.theme.foreground)
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (chapterIndex < book.chapters.lastIndex) chapterIndex++ }, enabled = chapterIndex < book.chapters.lastIndex) { Text("下一章") }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = settings.theme.foreground)
            }

            if (showSettings) {
                ReaderSettingsPanel(settings = settings, onChange = onSettingsChange)
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
                            modifier = Modifier.clickable { chapterIndex = result.first }.padding(vertical = 3.dp),
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
            Text("行距：${settings.lineHeight}")
            Slider(value = settings.lineHeight, onValueChange = { onChange(settings.copy(lineHeight = it)) }, valueRange = 1.1f..2.4f)
            Text("页边距：${settings.margin.roundToInt()}")
            Slider(value = settings.margin, onValueChange = { onChange(settings.copy(margin = it)) }, valueRange = 8f..48f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReaderTheme.All.forEach { theme ->
                    Box(
                        modifier = Modifier.background(theme.background).clickable { onChange(settings.copy(theme = theme)) }.padding(8.dp),
                    ) {
                        Text(theme.title, color = theme.foreground, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun parseTxtBook(title: String, content: String): Book {
    val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
    val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
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
        if (chapterTitleRegex.matches(line) && paragraphs.isNotEmpty()) {
            flush()
            currentTitle = line.take(80)
        } else if (chapterTitleRegex.matches(line)) {
            currentTitle = line.take(80)
        } else {
            paragraphs.add(line)
        }
    }
    flush()
    if (chapters.isEmpty()) chapters.add(Chapter("正文", lines.ifEmpty { listOf("这本书暂无可读取文本。") }))
    return Book(id = title + content.length + currentTimestamp(), title = title.ifBlank { "未命名 TXT" }, format = BookFormat.TXT, chapters = chapters)
}

private fun searchBook(book: Book, query: String): List<Pair<Int, String>> {
    val key = query.trim()
    if (key.isEmpty()) return emptyList()
    val results = mutableListOf<Pair<Int, String>>()
    book.chapters.forEachIndexed { index, chapter ->
        chapter.paragraphs.firstOrNull { it.contains(key, ignoreCase = true) }?.let { paragraph ->
            results.add(index to paragraph.take(60))
        }
    }
    return results
}

private fun sampleBook(): Book = parseTxtBook(
    title = "欢迎使用竹简阅读 Pro",
    content = """
        第一章 本地阅读器
        这是一个从零开始的新阅读器项目。当前版本先实现 TXT 导入、自动章节识别、基础排版、主题切换和阅读进度。
        软件目标是纯净、离线、无广告、权限精简。

        第二章 后续路线
        下一步会继续加入 EPUB、PDF、书架分类、书签持久化、音量键翻页、听书和批注笔记。
        功能会按版本逐步做扎实，不做臃肿半成品。
    """.trimIndent(),
)

expect fun currentTimestamp(): Long
