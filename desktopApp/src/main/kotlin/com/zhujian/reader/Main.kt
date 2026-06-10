package com.zhujian.reader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "竹简阅读 Pro") {
        NovelReaderProApp(platformName = "Windows") { callback ->
            val dialog = FileDialog(null as Frame?, "选择 TXT 小说", FileDialog.LOAD)
            dialog.file = "*.txt"
            dialog.isVisible = true
            val fileName = dialog.file
            val dir = dialog.directory
            if (fileName != null && dir != null) {
                val file = File(dir, fileName)
                val text = runCatching { file.readText(Charsets.UTF_8) }
                    .getOrElse { file.readText(charset("GBK")) }
                callback(file.nameWithoutExtension, text)
            }
        }
    }
}
