package com.androidphonecheck.app.ui

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.androidphonecheck.app.domain.DiagnosticStatus
import java.io.File
import java.util.Locale

@Composable
fun UsbStorageTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val usbManager = remember { context.getSystemService(UsbManager::class.java) }
    val supportsHost = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
    }
    var storageResult by remember { mutableStateOf("尚未执行读写测试") }
    var storagePassed by remember { mutableStateOf(false) }
    var attachedCount by remember { mutableStateOf(usbManager.deviceList.size) }
    val storage = remember { StatFs(Environment.getDataDirectory().path) }

    fun runStorageTest() {
        val file = File(context.filesDir, "diagnostic-storage-test.bin")
        val expected = ByteArray(1024 * 256) { index -> (index % 251).toByte() }
        runCatching {
            file.outputStream().use { it.write(expected) }
            val actual = file.inputStream().use { it.readBytes() }
            check(actual.contentEquals(expected)) { "写入和读回内容不一致" }
        }.onSuccess {
            storagePassed = true
            storageResult = "256 KB 写入、读回和校验正常"
        }.onFailure {
            storagePassed = false
            storageResult = "存储读写失败：${it.message ?: "未知错误"}"
        }
        file.delete()
    }

    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("USB、OTG 与存储", style = MaterialTheme.typography.headlineSmall)
        Text("USB Host/OTG 能力：${if (supportsHost) "系统声明支持" else "系统未声明支持"}")
        Text("当前识别到的 USB 设备：$attachedCount 个")
        OutlinedButton(
            onClick = { attachedCount = usbManager.deviceList.size },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("插入 OTG 设备后重新扫描") }
        Text("内部存储总量：${(storage.blockCountLong * storage.blockSizeLong).readableSize()}")
        Text("内部存储可用：${(storage.availableBlocksLong * storage.blockSizeLong).readableSize()}")
        Text(storageResult)
        Button(onClick = ::runStorageTest, modifier = Modifier.fillMaxWidth()) { Text("执行存储读写校验") }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onResult(DiagnosticStatus.NORMAL) },
                modifier = Modifier.weight(1f),
                enabled = storagePassed,
            ) { Text("功能正常") }
            Button(onClick = { onResult(DiagnosticStatus.ABNORMAL) }, modifier = Modifier.weight(1f)) {
                Text("发现异常")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试") }
    }
}

@Composable
fun SecurityRiskTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val findings = remember { collectSecurityFindings() }
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("安全风险线索", style = MaterialTheme.typography.headlineSmall)
        Text("这些项目只能提示可疑线索，不能证明手机一定 Root、刷机或翻新。")
        if (findings.isEmpty()) {
            Text("未发现公开可见的常见风险线索。")
        } else {
            findings.forEach { Text("• $it") }
        }
        Text("Bootloader 解锁状态、完整维修史和系统级篡改无法由普通离线 App 全面确认。")
        Button(
            onClick = { onResult(if (findings.isEmpty()) DiagnosticStatus.NORMAL else DiagnosticStatus.RISK) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (findings.isEmpty()) "记录为未发现风险" else "记录以上风险提示")
        }
        Button(onClick = { onResult(DiagnosticStatus.ABNORMAL) }, modifier = Modifier.fillMaxWidth()) {
            Text("人工确认存在系统异常")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试") }
    }
}

private fun collectSecurityFindings(): List<String> = buildList {
    if (Build.TAGS?.contains("test-keys") == true) add("系统构建标签包含 test-keys")
    if (Debug.isDebuggerConnected()) add("当前有调试器连接")
    if (Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", ignoreCase = true)) {
        add("设备参数表现出模拟器特征")
    }
    val suspiciousPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
    )
    val visibleFiles = suspiciousPaths.filter { File(it).exists() }
    if (visibleFiles.isNotEmpty()) add("发现可见的 su 文件：${visibleFiles.joinToString()}")
}

private fun Long.readableSize(): String =
    String.format(Locale.getDefault(), "%.1f GB", this / 1024.0 / 1024.0 / 1024.0)
