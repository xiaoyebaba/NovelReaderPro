package com.zhujian.reader

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private var importCallback: ((String, ByteArray) -> Unit)? = null

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = importCallback ?: return@registerForActivityResult
        if (uri != null) {
            val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: "本地导入.txt"
            val bytes = contentResolver.openInputStream(uri)?.use { input -> input.readBytes() } ?: ByteArray(0)
            callback(name, bytes)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializePlatform(this)
        setContent {
            NovelReaderProApp(platformName = "Android") { callback ->
                importCallback = callback
                openDocument.launch(arrayOf("text/plain", "application/epub+zip", "application/pdf", "application/octet-stream", "*/*"))
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
