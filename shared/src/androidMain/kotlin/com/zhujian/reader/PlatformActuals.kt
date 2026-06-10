package com.zhujian.reader

import android.content.Context

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
