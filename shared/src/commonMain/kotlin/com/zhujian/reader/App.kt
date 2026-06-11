package com.zhujian.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val APP_NAME = "青简阅读"
private const val APP_VERSION = "0.6.1"

data class Book(
    val id: String,
    val title: String,
    val author: String = "本地书籍",
    val format: BookFormat,
    val sourceText: String,
    val fileName: String = "",
    val chapters: List<Chapter>,
    var progressChapter: Int = 0,
    var isPinned: Boolean = false,
    val bookmarks: MutableList<Bookmark> = mutableListOf(),
)

enum class BookFormat { TXT, EPUB, PDF, MOBI, AZW3 }

enum class PageTurnMode(val label: String) {
    None("无动画"),
    Slide("滑动"),
    Simulation("仿真翻页")
}

enum class ReaderFont(val label: String, val family: FontFamily) {
    Serif("宋体/衬线", FontFamily.Serif),
    Sans("黑体/无衬线", FontFamily.SansSerif),
    Mono("等宽", FontFamily.Monospace),
    Cursive("手写", FontFamily.Cursive)
}

data class Chapter(
    val title: String,
    val paragraphs: List<String>,
)

data class Bookmark(
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
    val font: ReaderFont = ReaderFont.Serif,
    val pageTurnMode: PageTurnMode = PageTurnMode.None,
    val brightness: Float = 1.0f,
    val colorTemperature: Float = 0.5f,
    val eyeCareSoft: Boolean = true,
    val fullscreen: Boolean = false,
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

object ReaderDisplayBridge {
    var onFullscreenChanged: ((Boolean) -> Unit)? = null
}

object ReaderBackBridge {
    var onBack: (() -> Boolean)? = null
}

@Composable
fun NovelReaderProApp(
    platformName: String,
    onImportFile: ((fileName: String, bytes: ByteArray) -> Unit) -> Unit,
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
                        onImportFile { fileName, bytes ->
                            val parsed = parseImportedBook(fileName, bytes)
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
                        ReaderDisplayBridge.onFullscreenChanged?.invoke(it.fullscreen)
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
                    TextButton(onClick = { openLatestReleasePage() }) { Text("更新") }
                    IconButton(onClick = onImport) { Icon(Icons.Default.Add, contentDescription = "导入") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("v$APP_VERSION：修正菜单、设置弹层、返回键和翻页模式交互。", color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            if (books.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("书架还是空的")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onImport) { Text("导入 TXT / EPUB / PDF") }
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
    var chapterIndex by remember(book.id) { mutableIntStateOf(book.progressChapter.coerceIn(0, book.chapters.lastIndex.coerceAtLeast(0))) }
    var currentPage by remember(chapterIndex) { mutableIntStateOf(0) }
    var needLastPage by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val chapter = book.chapters.getOrNull(chapterIndex)
    val total = book.chapters.size.coerceAtLeast(1)
    val remainingChapters = (total - chapterIndex - 1).coerceAtLeast(0)
    val estimatedMinutesLeft = (remainingChapters * 4).coerceAtLeast(0)

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var pages by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var totalPages by remember { mutableIntStateOf(1) }

    fun goPrevious() {
        if (currentPage > 0) currentPage--
        else if (chapterIndex > 0) {
            needLastPage = true; chapterIndex--; currentPage = 999
            book.progressChapter = chapterIndex; onBookChanged()
        }
    }
    fun goNext() {
        if (currentPage < totalPages - 1) currentPage++
        else if (chapterIndex < book.chapters.lastIndex) {
            chapterIndex++; currentPage = 0
            book.progressChapter = chapterIndex; onBookChanged()
        }
    }

    ReaderKeyBridge.volumeKeyEnabled = settings.volumeKeyTurnPage
    ReaderKeyBridge.onPrevious = { goPrevious() }
    ReaderKeyBridge.onNext = { goNext() }
    book.progressChapter = chapterIndex
    ReaderBackBridge.onBack = {
        when {
            showSettings -> { showSettings = false; true }
            showSearch -> { showSearch = false; query = ""; true }
            showBookmarks -> { showBookmarks = false; true }
            showCatalog -> { showCatalog = false; true }
            showOverlay -> { showOverlay = false; true }
            else -> { onBack(); true }
        }
    }

    val warmth = settings.colorTemperature
    val displayBackground = blendColor(settings.theme.background, if (warmth >= 0.5f) Color(0xFFFFE0B2) else Color(0xFFE3F2FD), kotlin.math.abs(warmth - 0.5f) * 0.45f)

    // Auto-hide overlay after 4 seconds
    LaunchedEffect(showOverlay) {
        if (showOverlay) { delay(4000); showOverlay = false }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(displayBackground)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    when {
                        offset.x < w * 0.15f -> goPrevious()
                        offset.x > w * 0.85f -> goNext()
                        else -> showOverlay = !showOverlay
                    }
                }
            }
            .pointerInput(settings.pageTurnMode) {
                if (settings.pageTurnMode != PageTurnMode.None) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount ->
                            if (amount > 40f) goPrevious()
                            else if (amount < -40f) goNext()
                        },
                    )
                }
            }
    ) {
        val statusBarH = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        // Compute available text area using REAL screen dimensions
        val topPad = if (currentPage == 0) statusBarH + 28.dp else statusBarH + 8.dp
        val topPadPx = with(density) { topPad.toPx() }
        val marginPx = settings.margin * density.density
        val footerPx = with(density) { 28.dp.toPx() }
        val pageHeight = constraints.maxHeight.toFloat() - topPadPx - marginPx * 2 - footerPx
        val pageWidth = constraints.maxWidth.toFloat() - marginPx * 2

        // Paginate
        SideEffect {
            val paras = chapter?.paragraphs ?: emptyList()
            pages = paginateChapter(paras, settings, textMeasurer, density, pageHeight, pageWidth)
            totalPages = pages.size.coerceAtLeast(1)
            if (needLastPage) {
                currentPage = (pages.size - 1).coerceAtLeast(0)
                needLastPage = false
            }
        }

        val pageParagraphs = pages.getOrElse(currentPage) { emptyList() }

        // Page content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPad)
                .padding(settings.margin.dp)
                .padding(bottom = 32.dp)
        ) {
            // Panels (search, catalog, bookmarks, settings)
            if (showSearch) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("全书搜索关键词") }, singleLine = true,
                )
                if (query.isNotBlank()) {
                    val results = searchBook(book, query).take(6)
                    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            results.forEach { r ->
                                Text("第${r.first + 1}章：${r.second}",
                                    modifier = Modifier.clickable { chapterIndex = r.first; currentPage = 0; book.progressChapter = chapterIndex; onBookChanged(); showSearch = false }.padding(vertical = 4.dp),
                                    color = Color(0xFF1E88E5), fontSize = 14.sp)
                            }
                            if (results.isEmpty()) Text("没有找到匹配内容", color = Color.Gray)
                        }
                    }
                }
            }
            if (showCatalog) {
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("目录", style = MaterialTheme.typography.titleMedium)
                        LazyColumn(Modifier.fillMaxWidth().height(220.dp)) {
                            items(book.chapters.size) { index ->
                                Text(
                                    if (index == chapterIndex) "▶ ${index + 1}. ${book.chapters[index].title}" else "${index + 1}. ${book.chapters[index].title}",
                                    modifier = Modifier.fillMaxWidth().clickable { chapterIndex = index; currentPage = 0; book.progressChapter = index; onBookChanged(); showCatalog = false }.padding(vertical = 6.dp),
                                    color = if (index == chapterIndex) Color(0xFF1E88E5) else settings.theme.foreground, fontSize = 15.sp,
                                )
                            }
                        }
                    }
                }
            }
            if (showBookmarks) {
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("书签", style = MaterialTheme.typography.titleMedium)
                        if (book.bookmarks.isEmpty()) Text("当前没有书签", color = Color.Gray)
                        book.bookmarks.sortedBy { it.chapterIndex }.forEach { mark ->
                            Row(Modifier.fillMaxWidth().clickable { chapterIndex = mark.chapterIndex; currentPage = 0; book.progressChapter = chapterIndex; onBookChanged(); showBookmarks = false }.padding(vertical = 6.dp)) {
                                Text("第${mark.chapterIndex + 1}章：${mark.chapterTitle}", Modifier.weight(1f))
                                Text("跳转", color = Color(0xFF1E88E5))
                            }
                        }
                    }
                }
            }
            if (showSettings) {
                ReaderSettingsPanel(settings = settings, onChange = onSettingsChange, onDismiss = { showSettings = false })
            }

            // Chapter title — only on first page
            if (currentPage == 0) {
                Text(
                    chapter?.title ?: "无章节",
                    color = settings.theme.foreground,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 18.dp),
                )
            }

            // Current page paragraphs — EXACT fit for one screen
            Column(modifier = Modifier.weight(1f)) {
                pageParagraphs.forEach { paragraph ->
                    Text(
                        paragraph,
                        color = settings.theme.foreground,
                        fontSize = settings.fontSize.sp,
                        lineHeight = (settings.fontSize * settings.lineHeight).sp,
                        fontFamily = settings.font.family,
                        modifier = Modifier.padding(bottom = settings.paragraphSpacing.dp),
                    )
                }
            }

            // Page footer — page number + hint
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    "${currentPage + 1} / $totalPages 页  ·  ${chapterIndex + 1} / $total 章",
                    color = Color.Gray, fontSize = 12.sp,
                )
            }
        }

        // Overlay UI (semi-transparent, with status bar padding)
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250)),
        ) {
            Column(Modifier.fillMaxSize()) {
                // Top bar — padded for status bar
                Surface(color = Color.Black.copy(alpha = 0.65f), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回", tint = Color.White) }
                        Text(book.title, color = Color.White, fontSize = 17.sp,
                            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), softWrap = false)
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "设置", tint = Color.White) }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Bottom bar
                Surface(color = Color.Black.copy(alpha = 0.65f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { (currentPage + 1f) / totalPages },
                            modifier = Modifier.fillMaxWidth().height(3.dp).padding(bottom = 4.dp),
                            color = Color.White, trackColor = Color.White.copy(alpha = 0.2f),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { goPrevious() }, enabled = currentPage > 0 || chapterIndex > 0) { Text("◀", color = Color.White) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${currentPage + 1}/$totalPages 页", color = Color.White, fontSize = 13.sp)
                                Text("${chapterIndex + 1}/$total 章 · 约剩$estimatedMinutesLeft 分钟", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            TextButton(onClick = { goNext() }, enabled = currentPage < totalPages - 1 || chapterIndex < book.chapters.lastIndex) { Text("▶", color = Color.White) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            val bookmarked = book.bookmarks.any { it.chapterIndex == chapterIndex }
                            IconButton(onClick = {
                                if (bookmarked) book.bookmarks.removeAll { it.chapterIndex == chapterIndex }
                                else book.bookmarks.add(Bookmark(chapterIndex, chapter?.title ?: "第${chapterIndex + 1}章", currentTimestamp()))
                                onBookChanged()
                            }) {
                                Icon(if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "书签",
                                    tint = if (bookmarked) Color(0xFFFF9800) else Color.White.copy(alpha = 0.7f))
                            }
                            TextButton(onClick = { showCatalog = !showCatalog }) { Text("目录", color = Color.White) }
                            IconButton(onClick = { showSearch = true }) { Icon(Icons.Default.Search, "搜索", tint = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Split chapter paragraphs into pages that fit one screen height.
 * Uses TextMeasurer to accurately calculate paragraph heights.
 */
private fun paginateChapter(
    paragraphs: List<String>,
    settings: ReaderSettings,
    textMeasurer: TextMeasurer,
    density: Density,
    pageHeightPx: Float,
    maxWidthPx: Float,
): List<List<String>> {
    val style = TextStyle(
        fontSize = settings.fontSize.sp,
        lineHeight = (settings.fontSize * settings.lineHeight).sp,
        fontFamily = settings.font.family,
    )

    val pages = mutableListOf<List<String>>()
    val currentPage = mutableListOf<String>()
    var currentHeightPx = 0f

    for (paragraph in paragraphs) {
        val layout = textMeasurer.measure(
            text = paragraph,
            style = style,
            constraints = Constraints(maxWidth = maxWidthPx.roundToInt()),
        )
        val paraHeightPx = layout.size.height.toFloat() + settings.paragraphSpacing * density.density

        if (currentHeightPx + paraHeightPx > pageHeightPx && currentPage.isNotEmpty()) {
            pages.add(currentPage.toList())
            currentPage.clear()
            currentHeightPx = 0f
        }

        currentPage.add(paragraph)
        currentHeightPx += paraHeightPx
    }

    if (currentPage.isNotEmpty()) {
        pages.add(currentPage.toList())
    }

    return pages.ifEmpty { listOf(emptyList()) }
}

@Composable
private fun ReaderSettingsPanel(settings: ReaderSettings, onChange: (ReaderSettings) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("阅读设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("字体")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReaderFont.values().forEach { font ->
                        TextButton(onClick = { onChange(settings.copy(font = font)) }) { Text(if (settings.font == font) "✓ ${font.label}" else font.label) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatSize, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("字号：${settings.fontSize.roundToInt()}")
                }
                Slider(value = settings.fontSize, onValueChange = { onChange(settings.copy(fontSize = it)) }, valueRange = 14f..36f)
                Text("行距：${settings.lineHeight.roundToOneDecimal()}")
                Slider(value = settings.lineHeight, onValueChange = { onChange(settings.copy(lineHeight = it)) }, valueRange = 1.1f..2.6f)
                Text("段距：${settings.paragraphSpacing.roundToInt()}")
                Slider(value = settings.paragraphSpacing, onValueChange = { onChange(settings.copy(paragraphSpacing = it)) }, valueRange = 2f..28f)
                Text("页边距：${settings.margin.roundToInt()}")
                Slider(value = settings.margin, onValueChange = { onChange(settings.copy(margin = it)) }, valueRange = 4f..56f)
                Text("亮度：${(settings.brightness * 100).roundToInt()}%")
                Slider(value = settings.brightness, onValueChange = { onChange(settings.copy(brightness = it)) }, valueRange = 0.35f..1.0f)
                Text("色温：${(settings.colorTemperature * 100).roundToInt()}%（冷 ← → 暖）")
                Slider(value = settings.colorTemperature, onValueChange = { onChange(settings.copy(colorTemperature = it)) }, valueRange = 0f..1f)
                Text("翻页模式")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PageTurnMode.values().forEach { mode ->
                        TextButton(onClick = { onChange(settings.copy(pageTurnMode = mode)) }) { Text(if (settings.pageTurnMode == mode) "✓ ${mode.label}" else mode.label) }
                    }
                }
                Text("主题背景")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReaderTheme.All.forEach { theme ->
                        Box(modifier = Modifier.background(theme.background).clickable { onChange(settings.copy(theme = theme)) }.padding(8.dp)) {
                            Text(theme.title, color = theme.foreground, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onChange(settings.copy(eyeCareSoft = !settings.eyeCareSoft)) }) { Text(if (settings.eyeCareSoft) "护眼柔光：开" else "护眼柔光：关") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onChange(settings.copy(fullscreen = !settings.fullscreen)) }) { Text(if (settings.fullscreen) "全屏：开" else "全屏：关") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onChange(settings.copy(volumeKeyTurnPage = !settings.volumeKeyTurnPage)) }) { Text(if (settings.volumeKeyTurnPage) "音量键翻章：开" else "音量键翻章：关") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

private fun blendColor(base: Color, overlay: Color, amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = base.red * (1f - a) + overlay.red * a,
        green = base.green * (1f - a) + overlay.green * a,
        blue = base.blue * (1f - a) + overlay.blue * a,
        alpha = 1f,
    )
}

fun parseTxtBook(title: String, content: String, fileName: String = ""): Book {
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
    return Book(id = stableId, title = title.ifBlank { "未命名 TXT" }, format = BookFormat.TXT, sourceText = content, fileName = fileName, chapters = chapters)
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
        v0.6.1 修正菜单、设置弹层、返回键和翻页模式交互。
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
        font = map["font"]?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() } ?: ReaderFont.Serif,
        pageTurnMode = map["pageTurnMode"]?.let { runCatching { PageTurnMode.valueOf(it) }.getOrNull() } ?: PageTurnMode.None,
        brightness = map["brightness"]?.toFloatOrNull() ?: 1.0f,
        colorTemperature = map["colorTemperature"]?.toFloatOrNull() ?: 0.5f,
        eyeCareSoft = map["eyeCareSoft"]?.toBooleanStrictOrNull() ?: true,
        fullscreen = map["fullscreen"]?.toBooleanStrictOrNull() ?: false,
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
        "font=${settings.font.name}",
        "pageTurnMode=${settings.pageTurnMode.name}",
        "brightness=${settings.brightness}",
        "colorTemperature=${settings.colorTemperature}",
        "eyeCareSoft=${settings.eyeCareSoft}",
        "fullscreen=${settings.fullscreen}",
        "volumeKeyTurnPage=${settings.volumeKeyTurnPage}",
    ).joinToString("\n"))
}

private fun encodeBook(book: Book): String = listOf(
    "id=${esc(book.id)}",
    "title=${esc(book.title)}",
    "author=${esc(book.author)}",
    "format=${book.format.name}",
    "fileName=${esc(book.fileName)}",
    "progressChapter=${book.progressChapter}",
    "isPinned=${book.isPinned}",
    "bookmarks=${esc(book.bookmarks.joinToString(";") { "${it.chapterIndex},${esc(it.chapterTitle)},${it.createdAt}" })}",
    "sourceText=${esc(book.sourceText)}",
    "chapters=${esc(encodeChapters(book.chapters))}",
).joinToString("\n")

private fun decodeBook(block: String): Book? {
    val map = block.lines().mapNotNull { line -> line.substringBefore('=', "").takeIf { it.isNotEmpty() }?.let { it to line.substringAfter('=') } }.toMap()
    val title = unesc(map["title"] ?: return null)
    val source = unesc(map["sourceText"] ?: "")
    val format = map["format"]?.let { runCatching { BookFormat.valueOf(it) }.getOrNull() } ?: BookFormat.TXT
    val savedChapters = decodeChapters(unesc(map["chapters"].orEmpty()))
    val fallback = if (format == BookFormat.TXT) parseTxtBook(title, source) else Book(
        id = unesc(map["id"] ?: title),
        title = title,
        author = unesc(map["author"] ?: "本地书籍"),
        format = format,
        sourceText = source,
        fileName = unesc(map["fileName"].orEmpty()),
        chapters = savedChapters.ifEmpty { listOf(Chapter(title, listOf("这本书的章节缓存为空，请重新导入文件。"))) },
    )
    val book = fallback.copy(
        id = unesc(map["id"] ?: title),
        title = title,
        author = unesc(map["author"] ?: "本地书籍"),
        format = format,
        fileName = unesc(map["fileName"].orEmpty()),
        chapters = savedChapters.ifEmpty { fallback.chapters },
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

private fun encodeChapters(chapters: List<Chapter>): String = chapters.joinToString("\n---CHAPTER---\n") { chapter ->
    "title=${esc(chapter.title)}\nparagraphs=${esc(chapter.paragraphs.joinToString("\n---PARA---\n"))}"
}

private fun decodeChapters(raw: String): List<Chapter> {
    if (raw.isBlank()) return emptyList()
    return raw.split("\n---CHAPTER---\n").mapNotNull { block ->
        val map = block.lines().mapNotNull { line -> line.substringBefore('=', "").takeIf { it.isNotEmpty() }?.let { it to line.substringAfter('=') } }.toMap()
        val title = unesc(map["title"].orEmpty()).ifBlank { "未命名章节" }
        val paragraphs = unesc(map["paragraphs"].orEmpty()).split("\n---PARA---\n").map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) null else Chapter(title, paragraphs)
    }
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

fun parsePdfPlaceholder(fileName: String): Book = Book(
    id = "pdf-${abs(fileName.hashCode())}-${currentTimestamp()}",
    title = fileName.substringBeforeLast('.').ifBlank { "PDF 文档" },
    format = BookFormat.PDF,
    sourceText = "",
    fileName = fileName,
    chapters = listOf(
        Chapter(
            "PDF 阅读",
            listOf(
                "已成功导入 PDF：$fileName",
                "当前 v0.6.1 先完成 PDF 文件识别、入库、进度/书签框架接入。",
                "下一步会接入 PDF 页面渲染器，实现真正翻页、缩放和页码记忆。"
            )
        )
    )
)

expect fun parseImportedBook(fileName: String, bytes: ByteArray): Book
expect fun openLatestReleasePage()
expect fun currentTimestamp(): Long
expect fun persistentLoad(key: String): String?
expect fun persistentSave(key: String, value: String)
