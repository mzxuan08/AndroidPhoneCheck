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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.androidphonecheck.app.domain.DiagnosticStatus
import java.util.Locale

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
    val available = remember { definitions.filter { manager.getDefaultSensor(it.first) != null } }

    BackHandler(onBack = onBack)
    DisposableEffect(manager) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val formatted = event.values.take(3).joinToString(", ") {
                    String.format(Locale.getDefault(), "%.2f", it)
                }
                readings = readings + (event.sensor.type to formatted)
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
        Text("移动、旋转手机，遮挡听筒附近并改变环境光，观察数值是否变化。")
        definitions.forEach { (type, name) ->
            val exists = available.any { it.first == type }
            Text("$name：${if (!exists) "未发现" else readings[type] ?: "等待数据……"}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onResult(DiagnosticStatus.NORMAL) },
                modifier = Modifier.weight(1f),
                enabled = readings.isNotEmpty(),
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
@SuppressLint("MissingPermission")
private fun ConnectivityReadings(context: Context, permissionsGranted: Boolean) {
    val wifi = remember { context.applicationContext.getSystemService(WifiManager::class.java) }
    val bluetooth = remember { context.getSystemService(BluetoothManager::class.java)?.adapter }
    val nfc = remember { NfcAdapter.getDefaultAdapter(context) }
    val location = remember { context.getSystemService(LocationManager::class.java) }
    Text("Wi-Fi：${if (wifi?.isWifiEnabled == true) "已开启" else "未开启或不支持"}")
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
    val bluetoothGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        permissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)
    return locationGranted && bluetoothGranted
}

private fun biometricCapabilityMessage(code: Int): String = when (code) {
    BiometricManager.BIOMETRIC_SUCCESS -> "设备支持系统生物识别认证"
    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "设备支持，但尚未录入指纹或面部信息"
    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "未发现生物识别硬件"
    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "生物识别硬件当前不可用"
    else -> "当前系统不支持此认证方式"
}
