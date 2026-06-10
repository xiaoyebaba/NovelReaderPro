package com.zhujian.reader

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private var pendingImport: (((String, String) -> Unit) -> Unit)? = null
    private var importCallback: ((String, String) -> Unit)? = null

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = importCallback ?: return@registerForActivityResult
        if (uri != null) {
            val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: "本地导入.txt"
            val text = runCatching {
                contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                }.orEmpty()
            }.getOrElse {
                contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, charset("GBK"))).readText()
                }.orEmpty()
            }
            callback(name.substringBeforeLast('.'), text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovelReaderProApp(platformName = "Android") { callback ->
                importCallback = callback
                openDocument.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
            }
        }
        hideSystemBarsSafely()
    }

    private fun hideSystemBarsSafely() {
        // 在 Android 16KB/新版本模拟器上，onCreate 早期直接访问 window.insetsController
        // 可能因为 DecorView 尚未完全 attach 而空指针。延后到 DecorView 可用后再隐藏状态栏。
        window.decorView.post {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 先占位接入：后续把音量键事件传给 ReaderController 翻页。
        return if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) true else super.onKeyDown(keyCode, event)
    }
}
