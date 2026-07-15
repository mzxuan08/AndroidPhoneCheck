package com.androidphonecheck.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioAttributes
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.androidphonecheck.app.domain.DiagnosticStatus
import com.androidphonecheck.app.diagnostic.AlgorithmVerdict
import com.androidphonecheck.app.diagnostic.AudioAlgorithm
import com.androidphonecheck.app.diagnostic.AudioAssessment
import com.androidphonecheck.app.diagnostic.CameraAlgorithm
import com.androidphonecheck.app.diagnostic.CameraAssessment
import com.androidphonecheck.app.diagnostic.CameraSequenceAnalyzer
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun CameraTestScreen(
    onResult: (DiagnosticStatus, String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it) onResult(DiagnosticStatus.PERMISSION_REQUIRED, "相机权限被拒绝")
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
    var assessment by remember { mutableStateOf<CameraAssessment?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CameraPreview(
                lensFacing = lensFacing,
                onError = { cameraError = it },
                onAssessment = { assessment = it },
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
            assessment?.let { result ->
                val verdict = when (result.verdict) {
                    AlgorithmVerdict.NORMAL -> "算法建议：正常"
                    AlgorithmVerdict.SUSPECTED -> "算法建议：疑似异常"
                    AlgorithmVerdict.UNSUITABLE -> "环境不适合检测"
                }
                Text(verdict, color = if (result.verdict == AlgorithmVerdict.NORMAL) Color(0xFF70E0A8) else Color(0xFFFFCA70))
                Text(
                    String.format(Locale.getDefault(), "亮度 %.0f · 清晰度 %.0f · 偏色 %.0f · 噪点 %.1f",
                        result.metrics.meanLuma, result.metrics.sharpness, result.metrics.chromaOffset, result.metrics.noiseEstimate),
                    color = Color.White,
                )
                if (result.reasons.isNotEmpty()) Text(result.reasons.joinToString("；"), color = Color.White)
            } ?: Text("正在分析画面，请对准光线正常且有纹理的静止物体……", color = Color.White)
            OutlinedButton(
                onClick = {
                    assessment = null
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("切换前后摄像头", color = Color.White) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onResult(DiagnosticStatus.NORMAL, assessment.cameraEvidence()) }, modifier = Modifier.weight(1f)) { Text("画面正常") }
                Button(onClick = { onResult(DiagnosticStatus.ABNORMAL, assessment.cameraEvidence()) }, modifier = Modifier.weight(1f)) { Text("发现异常") }
            }
            assessment?.takeIf { it.verdict != AlgorithmVerdict.NORMAL }?.let { result ->
                OutlinedButton(
                    onClick = { onResult(if (result.verdict == AlgorithmVerdict.UNSUITABLE) DiagnosticStatus.ENVIRONMENT_UNSUITABLE else DiagnosticStatus.SUSPECTED, result.cameraEvidence()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("按算法建议记录", color = Color.White) }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试", color = Color.White) }
        }
    }
}

@Composable
private fun CameraPreview(
    lensFacing: Int,
    onError: (String) -> Unit,
    onAssessment: (CameraAssessment) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember(lensFacing) { Executors.newSingleThreadExecutor() }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val disposed = AtomicBoolean(false)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            if (disposed.get()) return@Runnable
            runCatching {
                val provider = providerFuture.get()
                if (disposed.get()) return@runCatching
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                var frameCount = 0
                val sequence = CameraSequenceAnalyzer()
                analysis.setAnalyzer(analysisExecutor) { image ->
                    try {
                        frameCount++
                        if (frameCount % 4 == 0) {
                            val y = image.copyPlane(0)
                            val uMean = image.planeMean(1)
                            val vMean = image.planeMean(2)
                            sequence.add(y, CameraAlgorithm.measure(y, image.width, image.height, uMean, vMean))
                                ?.let { result ->
                                    if (!disposed.get()) ContextCompat.getMainExecutor(context).execute {
                                        if (!disposed.get()) onAssessment(result)
                                    }
                                }
                        }
                    } finally {
                        image.close()
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            }.onFailure { onError("相机启动失败：${it.message ?: "未知错误"}") }
        }
        providerFuture.addListener(listener, executor)
        onDispose {
            disposed.set(true)
            if (providerFuture.isDone) runCatching { providerFuture.get().unbindAll() }
            analysisExecutor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

private fun ImageProxy.copyPlane(index: Int): ByteArray {
    val plane = planes[index]
    val width = if (index == 0) width else (width + 1) / 2
    val height = if (index == 0) height else (height + 1) / 2
    val source = plane.buffer.duplicate()
    val output = ByteArray(width * height)
    var out = 0
    repeat(height) { row ->
        repeat(width) { column ->
            val position = row * plane.rowStride + column * plane.pixelStride
            if (position < source.limit()) output[out] = source.get(position)
            out++
        }
    }
    return output
}

private fun ImageProxy.planeMean(index: Int): Double {
    if (planes.size <= index) return 128.0
    val values = copyPlane(index)
    if (values.isEmpty()) return 128.0
    return values.sumOf { it.toInt() and 0xff }.toDouble() / values.size
}

@Composable
@Suppress("DEPRECATION")
fun AudioTestScreen(
    onResult: (DiagnosticStatus, String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it) onResult(DiagnosticStatus.PERMISSION_REQUIRED, "麦克风权限被拒绝")
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

    val recordingFile = remember { File(context.cacheDir, "diagnostic-audio.wav") }
    var recorder by remember { mutableStateOf<AudioRecord?>(null) }
    var recordThread by remember { mutableStateOf<Thread?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var state by remember { mutableStateOf("等待录音") }
    var audioAssessment by remember { mutableStateOf<AudioAssessment?>(null) }
    var waveform by remember { mutableStateOf(List(64) { 0f }) }
    val speakerPlayback = remember { SpeakerPlayback() }
    val disposed = remember { AtomicBoolean(false) }
    val stopRecording = remember { AtomicBoolean(false) }
    val recorderRef = remember { AtomicReference<AudioRecord?>(null) }
    val threadRef = remember { AtomicReference<Thread?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            disposed.set(true)
            stopRecording.set(true)
            threadRef.get()?.interrupt()
            player?.release()
            if (threadRef.get() == null) recordingFile.delete()
            speakerPlayback.release()
        }
    }

    fun startRecording() {
        player?.release()
        player = null
        audioAssessment = null
        stopRecording.set(false)
        val sampleRate = 16_000
        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT),
            2_048,
        )
        runCatching {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize,
            )
            check(audioRecord.state == AudioRecord.STATE_INITIALIZED) { "麦克风初始化失败" }
            recorder = audioRecord
            recorderRef.set(audioRecord)
            audioRecord.startRecording()
            state = "请先保持安静 2 秒……"
            val main = Handler(Looper.getMainLooper())
            recordThread = Thread {
                val all = ArrayList<Short>(sampleRate * 7)
                val buffer = ShortArray(bufferSize / 2)
                try {
                    val started = System.currentTimeMillis()
                    var lastWaveUpdate = 0L
                    while (!stopRecording.get() && !Thread.currentThread().isInterrupted && System.currentTimeMillis() - started < 7_000) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        if (read > 0) repeat(read) { all.add(buffer[it]) }
                        val elapsed = System.currentTimeMillis() - started
                        if (read > 0 && elapsed - lastWaveUpdate >= 80) {
                            lastWaveUpdate = elapsed
                            val step = maxOf(1, read / 64)
                            val levels = List(64) { slot ->
                                val from = minOf(slot * step, read - 1)
                                val to = minOf(from + step, read)
                                (from until to).maxOfOrNull { kotlin.math.abs(buffer[it].toInt()) }?.div(32768f) ?: 0f
                            }
                            main.post { if (!disposed.get()) waveform = levels }
                        }
                        if (elapsed in 2_000..2_200) main.post { if (!disposed.get()) state = "请正常说话 5 秒……" }
                    }
                    val samples = all.toShortArray()
                    val split = minOf(sampleRate * 2, samples.size)
                    if (!disposed.get() && samples.size > split) {
                        val noise = samples.copyOfRange(0, split)
                        val speech = samples.copyOfRange(split, samples.size)
                        writeWave(recordingFile, samples, sampleRate)
                        val result = AudioAlgorithm.assess(AudioAlgorithm.measure(noise, speech))
                        main.post {
                            if (!disposed.get()) {
                                recorder = null; recordThread = null
                                audioAssessment = result
                                state = "智能检测完成，可回放人工确认"
                            }
                        }
                    } else if (!disposed.get()) main.post {
                        recorder = null; recordThread = null; state = "录音时间过短，请重试"
                    }
                } finally {
                    runCatching { audioRecord.stop() }
                    audioRecord.release()
                    recorderRef.compareAndSet(audioRecord, null)
                    threadRef.set(null)
                    if (disposed.get()) recordingFile.delete()
                }
            }.also { thread -> threadRef.set(thread); thread.start() }
        }.onFailure {
            recorder?.release()
            recorder = null
            state = "录音启动失败：${it.message ?: "未知错误"}"
        }
    }

    fun stopRecording() {
        stopRecording.set(true)
        state = "正在结束录音……"
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("麦克风与扬声器", style = MaterialTheme.typography.headlineSmall)
        Text("录制一段语音后回放，同时检查麦克风是否收音、扬声器是否清晰。")
        Text(state, color = MaterialTheme.colorScheme.primary)
        Canvas(Modifier.fillMaxWidth().height(72.dp).background(Color(0xFF101513))) {
            val middle = size.height / 2
            val step = size.width / waveform.size.coerceAtLeast(1)
            waveform.forEachIndexed { index, value ->
                val amplitude = value.coerceIn(0f, 1f) * middle
                drawLine(Color(0xFF70E0A8), Offset(index * step, middle - amplitude), Offset(index * step, middle + amplitude), 3f)
            }
        }
        audioAssessment?.let { result ->
            Text(
                when (result.verdict) {
                    AlgorithmVerdict.NORMAL -> "算法建议：麦克风收音正常"
                    AlgorithmVerdict.SUSPECTED -> "算法建议：疑似异常"
                    AlgorithmVerdict.UNSUITABLE -> "环境不适合检测，请重测"
                },
                color = MaterialTheme.colorScheme.primary,
            )
            Text(String.format(Locale.getDefault(), "音量 %.0f · 信噪比 %.1f dB · 语音频段 %.0f%% · 主频 %.0f Hz · 削波 %.1f%% · 断音 %.1f%%",
                result.metrics.rms, result.metrics.snrDb, result.metrics.voiceBandRatio * 100, result.metrics.dominantFrequencyHz,
                result.metrics.clippingRatio * 100, result.metrics.dropoutRatio * 100))
            if (result.reasons.isNotEmpty()) Text(result.reasons.joinToString("；"))
        }
        Button(
            onClick = { if (recorder == null) startRecording() else stopRecording() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (recorder == null) "开始录音" else "停止录音") }
        OutlinedButton(onClick = ::playRecording, modifier = Modifier.fillMaxWidth(), enabled = recorder == null) {
            Text("回放录音")
        }
        Text("扬声器辅助测试（请听是否清晰、有无破音）")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ speakerPlayback.play(-1f, false) }, Modifier.weight(1f)) { Text("左声道") }
            OutlinedButton({ speakerPlayback.play(1f, false) }, Modifier.weight(1f)) { Text("右声道") }
            OutlinedButton({ speakerPlayback.play(0f, true) }, Modifier.weight(1f)) { Text("扫频") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onResult(DiagnosticStatus.NORMAL, audioAssessment.audioEvidence()) }, modifier = Modifier.weight(1f)) { Text("声音正常") }
            Button(onClick = { onResult(DiagnosticStatus.ABNORMAL, audioAssessment.audioEvidence()) }, modifier = Modifier.weight(1f)) { Text("发现异常") }
        }
        audioAssessment?.takeIf { it.verdict != AlgorithmVerdict.NORMAL }?.let { result ->
            OutlinedButton(
                onClick = { onResult(if (result.verdict == AlgorithmVerdict.UNSUITABLE) DiagnosticStatus.ENVIRONMENT_UNSUITABLE else DiagnosticStatus.SUSPECTED, result.audioEvidence()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("按算法建议记录") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试") }
    }
}

private fun CameraAssessment?.cameraEvidence(): String = this?.let {
    String.format(Locale.getDefault(), "算法=%s；亮度=%.1f；清晰度=%.1f；偏色=%.1f；噪点=%.1f；依据=%s",
        verdict.name, metrics.meanLuma, metrics.sharpness, metrics.chromaOffset, metrics.noiseEstimate,
        reasons.joinToString("；").ifEmpty { "无异常指标" })
} ?: "人工检查，算法尚未完成"

private fun AudioAssessment?.audioEvidence(): String = this?.let {
    String.format(Locale.getDefault(), "算法=%s；RMS=%.1f；SNR=%.1fdB；语音频段=%.1f%%；主频=%.1fHz；削波=%.2f%%；断音=%.2f%%；依据=%s",
        verdict.name, metrics.rms, metrics.snrDb, metrics.voiceBandRatio * 100, metrics.dominantFrequencyHz,
        metrics.clippingRatio * 100, metrics.dropoutRatio * 100,
        reasons.joinToString("；").ifEmpty { "无异常指标" })
} ?: "人工检查，算法尚未完成"

private fun writeWave(file: File, samples: ShortArray, sampleRate: Int) {
    val dataSize = samples.size * 2
    FileOutputStream(file).use { out ->
        fun intLe(value: Int) = byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte())
        fun shortLe(value: Int) = byteArrayOf(value.toByte(), (value shr 8).toByte())
        out.write("RIFF".toByteArray()); out.write(intLe(36 + dataSize)); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); out.write(intLe(16)); out.write(shortLe(1)); out.write(shortLe(1))
        out.write(intLe(sampleRate)); out.write(intLe(sampleRate * 2)); out.write(shortLe(2)); out.write(shortLe(16))
        out.write("data".toByteArray()); out.write(intLe(dataSize))
        samples.forEach { out.write(shortLe(it.toInt())) }
    }
}

private class SpeakerPlayback {
    @Volatile private var track: AudioTrack? = null
    @Volatile private var worker: Thread? = null

    @Synchronized fun play(pan: Float, sweep: Boolean) {
        release()
        worker = Thread {
        val rate = 44_100
        val duration = 2
        val mono = ShortArray(rate * duration) { index ->
            val time = index.toDouble() / rate
            val phase = if (sweep) {
                val slope = 3_800.0 / duration
                2 * PI * (200.0 * time + .5 * slope * time * time)
            } else 2 * PI * 880.0 * time
            (sin(phase) * 7_000).toInt().toShort()
        }
        val stereo = ShortArray(mono.size * 2)
        mono.forEachIndexed { index, value ->
            stereo[index * 2] = if (pan > .5f) 0 else value
            stereo[index * 2 + 1] = if (pan < -.5f) 0 else value
        }
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(stereo.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track = audioTrack
        runCatching {
            audioTrack.write(stereo, 0, stereo.size)
            audioTrack.play()
            Thread.sleep(duration * 1_000L + 100)
            audioTrack.stop()
        }
        audioTrack.release()
        if (track === audioTrack) track = null
        }.also { it.start() }
    }

    @Synchronized fun release() {
        worker?.interrupt()
        worker = null
        track?.let { audioTrack ->
            runCatching { audioTrack.pause() }
            runCatching { audioTrack.flush() }
            runCatching { audioTrack.release() }
        }
        track = null
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
