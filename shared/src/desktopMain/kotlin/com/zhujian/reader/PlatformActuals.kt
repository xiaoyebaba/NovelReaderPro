package com.zhujian.reader

import java.io.File

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
