package com.androidphonecheck.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.telephony.TelephonyManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.androidphonecheck.app.domain.DiagnosticStatus
import com.androidphonecheck.app.diagnostic.SensorCheckRule
import com.androidphonecheck.app.diagnostic.SensorResponse
import com.androidphonecheck.app.diagnostic.SensorResponseEvaluator
import java.util.Locale
import kotlin.math.abs

@Composable
fun SensorTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(SensorManager::class.java) }
    val definitions = remember {
        listOf(
            Sensor.TYPE_ACCELEROMETER to "加速度计",
            Sensor.TYPE_GYROSCOPE to "陀螺仪",
            Sensor.TYPE_PROXIMITY to "距离传感器",
            Sensor.TYPE_LIGHT to "光线传感器",
            Sensor.TYPE_MAGNETIC_FIELD to "磁场传感器",
            Sensor.TYPE_PRESSURE to "气压计",
        )
    }
    var readings by remember { mutableStateOf(emptyMap<Int, String>()) }
    var ranges by remember { mutableStateOf(emptyMap<Int, Pair<Float, Float>>()) }
    var sampleCounts by remember { mutableStateOf(emptyMap<Int, Int>()) }
    var currentStep by remember { mutableIntStateOf(0) }
    var completedSteps by remember { mutableStateOf(emptySet<Int>()) }
    val available = remember { definitions.filter { manager.getDefaultSensor(it.first) != null } }

    LaunchedEffect(currentStep) {
        val type = definitions[currentStep].first
        ranges = ranges - type
        sampleCounts = sampleCounts - type
    }

    BackHandler(onBack = onBack)
    DisposableEffect(manager) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val formatted = event.values.take(3).joinToString(", ") {
                    String.format(Locale.getDefault(), "%.2f", it)
                }
                readings = readings + (event.sensor.type to formatted)
                val magnitude = event.values.take(3).sumOf { abs(it.toDouble()) }.toFloat()
                val previous = ranges[event.sensor.type]
                ranges = ranges + (event.sensor.type to if (previous == null) {
                    magnitude to magnitude
                } else {
                    minOf(previous.first, magnitude) to maxOf(previous.second, magnitude)
                })
                sampleCounts = sampleCounts + (event.sensor.type to (sampleCounts[event.sensor.type] ?: 0) + 1)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        available.forEach { (type, _) ->
            manager.getDefaultSensor(type)?.let {
                manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        onDispose { manager.unregisterListener(listener) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("传感器实时测试", style = MaterialTheme.typography.headlineSmall)
        Text("请逐项按提示操作。不存在的传感器会记为不支持，不会误判损坏。")
        val current = definitions[currentStep]
        val currentSensor = manager.getDefaultSensor(current.first)
        val currentRange = ranges[current.first]
        val rule = sensorRule(current.first)
        val guidedResponse = SensorResponseEvaluator.evaluate(
            supported = currentSensor != null,
            sampleCount = sampleCounts[current.first] ?: 0,
            minimum = currentRange?.first,
            maximum = currentRange?.second,
            rule = rule,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("第 ${currentStep + 1}/${definitions.size} 项：${current.second}", style = MaterialTheme.typography.titleMedium)
                Text(rule.instruction)
                Text("判定：${sensorResponseText(guidedResponse)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            ranges = ranges - current.first
                            sampleCounts = sampleCounts - current.first
                            completedSteps = completedSteps - currentStep
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("重新采样") }
                    Button(
                        onClick = {
                            completedSteps = completedSteps + currentStep
                            if (currentStep < definitions.lastIndex) currentStep++
                        },
                        enabled = guidedResponse != SensorResponse.WAITING,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (currentStep == definitions.lastIndex) "完成此项" else "下一项") }
                }
            }
        }
        Text("全部传感器概览", style = MaterialTheme.typography.titleMedium)
        definitions.forEach { (type, name) ->
            val exists = available.any { it.first == type }
            val range = ranges[type]
            val change = range?.let { it.second - it.first }
            val response = when {
                !exists -> "不支持"
                change == null -> "等待数据"
                change > sensorChangeThreshold(type) -> "已检测到响应"
                else -> "请继续按引导操作"
            }
            Text("$name：$response；${readings[type] ?: "--"}${change?.let { "；变化 ${String.format(Locale.getDefault(), "%.2f", it)}" } ?: ""}")
            if (exists) {
                val sensor = manager.getDefaultSensor(type)
                Text("  ${sensor?.vendor.orEmpty()} · 量程 ${sensor?.maximumRange} · 分辨率 ${sensor?.resolution}")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val allSupportedResponded = definitions.withIndex().all { (index, definition) ->
                val sensor = manager.getDefaultSensor(definition.first)
                index in completedSteps && (sensor == null || SensorResponseEvaluator.evaluate(
                    supported = true,
                    sampleCount = sampleCounts[definition.first] ?: 0,
                    minimum = ranges[definition.first]?.first,
                    maximum = ranges[definition.first]?.second,
                    rule = sensorRule(definition.first),
                ) == SensorResponse.RESPONDED)
            }
            Button(
                onClick = { onResult(DiagnosticStatus.NORMAL) },
                modifier = Modifier.weight(1f),
                enabled = allSupportedResponded,
            ) { Text("数据正常") }
            Button(onClick = { onResult(DiagnosticStatus.ABNORMAL) }, modifier = Modifier.weight(1f)) {
                Text("发现异常")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试") }
    }
}

@Composable
fun ConnectivityTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
        }.toTypedArray()
    }
    var permissionsGranted by remember { mutableStateOf(connectionPermissionsGranted(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionsGranted = connectionPermissionsGranted(context)
    }
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("连接与定位", style = MaterialTheme.typography.headlineSmall)
        if (!permissionsGranted) {
            Text("定位和新版 Android 的蓝牙状态需要授权。拒绝不会影响其他验机项目。")
            Button(onClick = { launcher.launch(requiredPermissions) }, modifier = Modifier.fillMaxWidth()) {
                Text("授予连接测试权限")
            }
        }
        ConnectivityReadings(context = context, permissionsGranted = permissionsGranted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onResult(DiagnosticStatus.NORMAL) }, modifier = Modifier.weight(1f)) { Text("连接正常") }
            Button(onClick = { onResult(DiagnosticStatus.ABNORMAL) }, modifier = Modifier.weight(1f)) { Text("发现异常") }
        }
        if (!permissionsGranted) {
            OutlinedButton(
                onClick = { onResult(DiagnosticStatus.PERMISSION_REQUIRED) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("无权限，跳过此项") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试") }
    }
}

@Composable
@SuppressLint("MissingPermission", "MissingPermission", "DEPRECATION")
private fun ConnectivityReadings(context: Context, permissionsGranted: Boolean) {
    val wifi = remember { context.applicationContext.getSystemService(WifiManager::class.java) }
    val bluetooth = remember { context.getSystemService(BluetoothManager::class.java)?.adapter }
    val nfc = remember { NfcAdapter.getDefaultAdapter(context) }
    val location = remember { context.getSystemService(LocationManager::class.java) }
    val telephony = remember { context.getSystemService(TelephonyManager::class.java) }
    val wifiInfo = runCatching { wifi?.connectionInfo }.getOrNull()
    Text("Wi-Fi：${if (wifi?.isWifiEnabled == true) "已开启" else "未开启或不支持"}" +
        (wifiInfo?.takeIf { it.networkId != -1 }?.let { " · RSSI ${it.rssi} dBm · ${WifiManager.calculateSignalLevel(it.rssi, 5)}/4级" } ?: ""))
    Text(
        "蓝牙：${when {
            bluetooth == null -> "不支持"
            !permissionsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> "权限不足"
            bluetooth.isEnabled -> "已开启"
            else -> "未开启"
        }}",
    )
    Text("NFC：${when { nfc == null -> "不支持"; nfc.isEnabled -> "已开启"; else -> "未开启" }}")
    val locationEnabled = runCatching {
        location.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            location.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }.getOrDefault(false)
    Text("定位服务：${if (locationEnabled) "已开启" else "未开启"}")
    val simText = when (telephony?.simState) {
        TelephonyManager.SIM_STATE_READY -> "SIM 已就绪"
        TelephonyManager.SIM_STATE_ABSENT -> "未插入 SIM"
        else -> "SIM 未就绪或状态受限"
    }
    val networkType = if (permissionGranted(context, Manifest.permission.READ_PHONE_STATE)) {
        mobileNetworkName(runCatching { telephony?.dataNetworkType }.getOrNull())
    } else "权限不足"
    Text("移动网络：$simText · $networkType")
}

private fun sensorChangeThreshold(type: Int): Float = when (type) {
    Sensor.TYPE_LIGHT -> 5f
    Sensor.TYPE_PROXIMITY -> .5f
    Sensor.TYPE_ACCELEROMETER -> 3f
    Sensor.TYPE_GYROSCOPE -> .5f
    Sensor.TYPE_MAGNETIC_FIELD -> 5f
    else -> .1f
}

private fun sensorRule(type: Int): SensorCheckRule = when (type) {
    Sensor.TYPE_LIGHT -> SensorCheckRule("先用手遮住听筒附近区域，再移开并对准光源。", 5f)
    Sensor.TYPE_PROXIMITY -> SensorCheckRule("将手掌靠近听筒区域，再完全移开。", .5f)
    Sensor.TYPE_ACCELEROMETER -> SensorCheckRule("上下、左右轻轻摇动手机。", 3f)
    Sensor.TYPE_GYROSCOPE -> SensorCheckRule("绕三个方向缓慢旋转手机。", .5f)
    Sensor.TYPE_MAGNETIC_FIELD -> SensorCheckRule("远离磁性物体，水平转动手机一圈。", 5f)
    Sensor.TYPE_PRESSURE -> SensorCheckRule("静置手机并轻微改变高度；气压变化很小时可稍后人工确认。", .1f)
    else -> SensorCheckRule("移动手机并观察数值变化。", .1f)
}

private fun sensorResponseText(response: SensorResponse): String = when (response) {
    SensorResponse.WAITING -> "采样中，请继续操作"
    SensorResponse.RESPONDED -> "已检测到有效响应"
    SensorResponse.NO_RESPONSE -> "已采集足够数据但变化不足，建议重试或人工复核"
    SensorResponse.UNSUPPORTED -> "本机不支持"
}

private fun mobileNetworkName(type: Int?): String = when (type) {
    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
    TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
    TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
    TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
    TelephonyManager.NETWORK_TYPE_UNKNOWN, null -> "网络类型未知"
    else -> "移动网络（类型 $type）"
}

@Composable
fun BiometricTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val capability = remember {
        BiometricManager.from(context).canAuthenticate(authenticators)
    }
    var message by remember { mutableStateOf(biometricCapabilityMessage(capability)) }
    BackHandler(onBack = onBack)

    fun authenticate() {
        if (activity == null) {
            message = "当前页面无法调起系统认证"
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                message = "系统认证成功"
                onResult(DiagnosticStatus.NORMAL)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                message = "认证未完成：$errString"
            }

            override fun onAuthenticationFailed() {
                message = "未识别，请重试"
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("生物识别验机")
            .setSubtitle("使用系统认证界面确认传感器可用")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("生物识别", style = MaterialTheme.typography.headlineSmall)
        Text(message)
        Text("App 不读取或保存指纹、面部等生物信息，只调用系统认证界面。")
        Button(
            onClick = ::authenticate,
            modifier = Modifier.fillMaxWidth(),
            enabled = capability == BiometricManager.BIOMETRIC_SUCCESS,
        ) { Text("调起系统认证") }
        Button(onClick = { onResult(DiagnosticStatus.ABNORMAL) }, modifier = Modifier.fillMaxWidth()) {
            Text("认证硬件存在异常")
        }
        if (capability != BiometricManager.BIOMETRIC_SUCCESS) {
            OutlinedButton(
                onClick = { onResult(DiagnosticStatus.UNSUPPORTED) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("记录为不支持") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("稍后测试") }
    }
}

private fun permissionGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun connectionPermissionsGranted(context: Context): Boolean {
    val locationGranted = permissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    val phoneGranted = permissionGranted(context, Manifest.permission.READ_PHONE_STATE)
    val bluetoothGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        permissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)
    return locationGranted && bluetoothGranted && phoneGranted
}

private fun biometricCapabilityMessage(code: Int): String = when (code) {
    BiometricManager.BIOMETRIC_SUCCESS -> "设备支持系统生物识别认证"
    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "设备支持，但尚未录入指纹或面部信息"
    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "未发现生物识别硬件"
    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "生物识别硬件当前不可用"
    else -> "当前系统不支持此认证方式"
}
