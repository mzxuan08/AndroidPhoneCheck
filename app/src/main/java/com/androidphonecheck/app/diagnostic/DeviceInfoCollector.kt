package com.androidphonecheck.app.diagnostic

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import com.androidphonecheck.app.domain.DiagnosticCategory
import com.androidphonecheck.app.domain.DiagnosticResult
import com.androidphonecheck.app.domain.DiagnosticStatus
import java.util.Locale

class DeviceInfoCollector(private val context: Context) {
    fun collect(): DiagnosticResult {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val storage = StatFs(Environment.getDataDirectory().path)
        val metrics = context.resources.displayMetrics
        val display = context.getSystemService(WindowManager::class.java).defaultDisplay

        return DiagnosticResult(
            id = "device_info",
            category = DiagnosticCategory.DEVICE_INFO,
            title = "设备基本信息",
            status = DiagnosticStatus.NORMAL,
            summary = "${Build.MANUFACTURER} ${Build.MODEL}",
            details = linkedMapOf(
                "品牌" to Build.BRAND,
                "厂商" to Build.MANUFACTURER,
                "型号" to Build.MODEL,
                "Android" to "${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）",
                "CPU ABI" to Build.SUPPORTED_ABIS.joinToString(),
                "总内存" to memoryInfo.totalMem.toReadableSize(),
                "可用内存" to memoryInfo.availMem.toReadableSize(),
                "存储容量" to (storage.blockCountLong * storage.blockSizeLong).toReadableSize(),
                "存储可用" to (storage.availableBlocksLong * storage.blockSizeLong).toReadableSize(),
                "逻辑分辨率" to "${metrics.widthPixels} × ${metrics.heightPixels}",
                "像素密度" to "${metrics.densityDpi} dpi",
                "当前刷新率" to String.format(Locale.getDefault(), "%.1f Hz", display.refreshRate),
                "可用显示模式" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    display.supportedModes.joinToString { "${it.physicalWidth}×${it.physicalHeight}@${String.format(Locale.getDefault(), "%.0f", it.refreshRate)}Hz" }
                } else {
                    "系统未提供"
                },
            ),
        )
    }
}

private fun Long.toReadableSize(): String {
    val gib = this / 1024.0 / 1024.0 / 1024.0
    return String.format(Locale.getDefault(), "%.1f GB", gib)
}
