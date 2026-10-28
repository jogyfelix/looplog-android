package com.jolabs.looplog.ai_engine.helpers

import android.app.ActivityManager
import android.content.Context
import kotlin.div

fun getTotalSystemRam(context: Context): Double {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    // Convert bytes to Gigabytes
    return memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
}

