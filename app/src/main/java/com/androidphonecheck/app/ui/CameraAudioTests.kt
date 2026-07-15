package com.androidphonecheck.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.androidphonecheck.app.domain.DiagnosticStatus
import java.io.File

@Composable
fun CameraTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it) onResult(DiagnosticStatus.PERMISSION_REQUIRED)
    }
    BackHandler(onBack = onBack)

    if (!granted) {
        PermissionPrompt(
            title = "需要相机权限",
            description = "相机权限只用于本机实时预览和验机，不上传照片。",
            onGrant = { launcher.launch(Manifest.permission.CAMERA) },
            onBack = onBack,
        )
        return
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CameraPreview(
                lensFacing = lensFacing,
                onError = { cameraError = it },
            )
            Text(
                if (lensFacing == CameraSelector.LENS_FACING_BACK) "后置摄像头" else "前置摄像头",
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color(0x99000000)).padding(8.dp),
                color = Color.White,
            )
            cameraError?.let {
                Text(it, modifier = Modifier.align(Alignment.Center), color = Color.Red)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF202522)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("切换前后摄像头", color = Color.White) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onResult(DiagnosticStatus.NORMAL) }, modifier = Modifier.weight(1f)) { Text("画面正常") }
                Button(onClick = { onResult(DiagnosticStatus.ABNORMAL) }, modifier = Modifier.weight(1f)) { Text("发现异常") }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试", color = Color.White) }
        }
    }
}

@Composable
private fun CameraPreview(lensFacing: Int, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview)
            }.onFailure { onError("相机启动失败：${it.message ?: "未知错误"}") }
        }
        providerFuture.addListener(listener, executor)
        onDispose { runCatching { providerFuture.get().unbindAll() } }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
@Suppress("DEPRECATION")
fun AudioTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it) onResult(DiagnosticStatus.PERMISSION_REQUIRED)
    }
    BackHandler(onBack = onBack)
    if (!granted) {
        PermissionPrompt(
            title = "需要麦克风权限",
            description = "录音只保存在 App 临时目录，退出测试后删除。",
            onGrant = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
            onBack = onBack,
        )
        return
    }

    val recordingFile = remember { File(context.cacheDir, "diagnostic-audio.m4a") }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var state by remember { mutableStateOf("等待录音") }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.stop() }
            recorder?.release()
            player?.release()
            recordingFile.delete()
        }
    }

    fun startRecording() {
        player?.release()
        player = null
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        runCatching {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setOutputFile(recordingFile.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            state = "正在录音，请说话……"
        }.onFailure {
            mediaRecorder.release()
            state = "录音启动失败：${it.message ?: "未知错误"}"
        }
    }

    fun stopRecording() {
        runCatching { recorder?.stop() }
            .onSuccess { state = "录音完成，请回放检查" }
            .onFailure { state = "录音失败或时间过短" }
        recorder?.release()
        recorder = null
    }

    fun playRecording() {
        if (!recordingFile.exists()) {
            state = "请先录音"
            return
        }
        player?.release()
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(recordingFile.absolutePath)
                prepare()
                setOnCompletionListener { state = "回放结束" }
                start()
            }
            state = "正在通过扬声器回放……"
        }.onFailure { state = "回放失败：${it.message ?: "未知错误"}" }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("麦克风与扬声器", style = MaterialTheme.typography.headlineSmall)
        Text("录制一段语音后回放，同时检查麦克风是否收音、扬声器是否清晰。")
        Text(state, color = MaterialTheme.colorScheme.primary)
        Button(
            onClick = { if (recorder == null) startRecording() else stopRecording() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (recorder == null) "开始录音" else "停止录音") }
        OutlinedButton(onClick = ::playRecording, modifier = Modifier.fillMaxWidth(), enabled = recorder == null) {
            Text("回放录音")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onResult(DiagnosticStatus.NORMAL) }, modifier = Modifier.weight(1f)) { Text("声音正常") }
            Button(onClick = { onResult(DiagnosticStatus.ABNORMAL) }, modifier = Modifier.weight(1f)) { Text("发现异常") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试") }
    }
}

@Composable
private fun PermissionPrompt(
    title: String,
    description: String,
    onGrant: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(description, modifier = Modifier.padding(vertical = 16.dp))
        Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) { Text("授权并开始测试") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试") }
    }
}
