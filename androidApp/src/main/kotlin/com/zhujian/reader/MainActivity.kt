package com.zhujian.reader

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
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
        initializePlatform(this)
        setContent {
            NovelReaderProApp(platformName = "Android") { callback ->
                importCallback = callback
                openDocument.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (ReaderKeyBridge.volumeKeyEnabled) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    ReaderKeyBridge.onPrevious?.invoke()
                    return ReaderKeyBridge.onPrevious != null
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    ReaderKeyBridge.onNext?.invoke()
                    return ReaderKeyBridge.onNext != null
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
