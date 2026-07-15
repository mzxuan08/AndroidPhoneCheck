package com.androidphonecheck.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.androidphonecheck.app.domain.DiagnosticStatus

@Composable
fun PhysicalTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var volumeUp by remember { mutableStateOf(false) }
    var volumeDown by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("请按音量加键和音量减键") }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it
        if (!it) message = "未获得相机权限，手电筒项目无法测试"
    }
    val cameraManager = remember { context.getSystemService(CameraManager::class.java) }
    val torchCameraId = remember {
        runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                val properties = cameraManager.getCameraCharacteristics(id)
                properties.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    properties.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        }.getOrNull()
    }

    fun setTorch(enabled: Boolean) {
        if (!cameraGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        if (torchCameraId == null) {
            message = "此设备未发现可用闪光灯"
            return
        }
        runCatching { cameraManager.setTorchMode(torchCameraId, enabled) }
            .onSuccess {
                torchEnabled = enabled
                message = if (enabled) "手电筒已开启，请观察闪光灯" else "手电筒已关闭"
            }
            .onFailure { message = "手电筒启动失败：${it.message ?: "未知错误"}" }
    }

    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    DisposableEffect(Unit) {
        onDispose { if (torchEnabled && torchCameraId != null) runCatching { cameraManager.setTorchMode(torchCameraId, false) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.VolumeUp -> volumeUp = true
                    Key.VolumeDown -> volumeDown = true
                    else -> return@onPreviewKeyEvent false
                }
                message = "音量加：${if (volumeUp) "已检测" else "未检测"}；音量减：${if (volumeDown) "已检测" else "未检测"}"
                true
            }
            .focusable()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("按键、震动与手电筒", style = MaterialTheme.typography.headlineSmall)
        Text(message)
        Text("音量加键：${if (volumeUp) "正常" else "等待按键"}")
        Text("音量减键：${if (volumeDown) "正常" else "等待按键"}")
        Button(
            onClick = {
                vibrate(context)
                message = "已触发震动，请确认是否有明显振感"
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("测试震动") }
        Button(onClick = { setTorch(!torchEnabled) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (torchEnabled) "关闭手电筒" else "测试手电筒")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onResult(DiagnosticStatus.NORMAL) }, modifier = Modifier.weight(1f)) { Text("功能正常") }
            Button(onClick = { onResult(DiagnosticStatus.ABNORMAL) }, modifier = Modifier.weight(1f)) { Text("发现异常") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试") }
    }
}

private fun vibrate(context: android.content.Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    vibrator.vibrate(VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE))
}
