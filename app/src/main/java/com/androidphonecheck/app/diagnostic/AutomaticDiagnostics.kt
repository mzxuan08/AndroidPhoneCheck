package com.androidphonecheck.app.diagnostic

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import com.androidphonecheck.app.domain.DiagnosticCategory
import com.androidphonecheck.app.domain.DiagnosticResult
import com.androidphonecheck.app.domain.DiagnosticStatus
import com.androidphonecheck.app.domain.Severity
import java.util.Locale

class AutomaticDiagnostics(private val context: Context) {
    fun collect(): List<DiagnosticResult> = buildList {
        add(DeviceInfoCollector(context).collect())
        add(collectBattery())
        add(collectSensors())
        add(collectHardwareCapabilities())
    }

    private fun collectBattery(): DiagnosticResult {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return DiagnosticResult(
                id = "battery",
                category = DiagnosticCategory.BATTERY,
                title = "电池与充电",
                status = DiagnosticStatus.UNSUPPORTED,
                summary = "系统未提供电池状态",
            )

        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else null
        val temperature = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE)
        val plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val health = battery.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

        return DiagnosticResult(
            id = "battery",
            category = DiagnosticCategory.BATTERY,
            title = "电池与充电",
            status = DiagnosticStatus.NORMAL,
            summary = percent?.let { "当前电量 $it%" } ?: "已读取电池状态",
            details = linkedMapOf<String, String>().apply {
                percent?.let { put("电量", "$it%") }
                if (temperature != Int.MIN_VALUE) {
                    put("温度", String.format(Locale.getDefault(), "%.1f ℃", temperature / 10f))
                }
                if (voltage != Int.MIN_VALUE) put("电压", "$voltage mV")
                put("充电状态", status.toChargeStatus())
                put("供电来源", plugged.toPowerSource())
                put("系统健康状态", health.toBatteryHealth())
            },
        )
    }

    private fun collectSensors(): DiagnosticResult {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensors = manager.getSensorList(Sensor.TYPE_ALL)
        val grouped = sensors.groupBy { it.type }
        val important = listOf(
            Sensor.TYPE_ACCELEROMETER to "加速度计",
            Sensor.TYPE_GYROSCOPE to "陀螺仪",
            Sensor.TYPE_PROXIMITY to "距离传感器",
            Sensor.TYPE_LIGHT to "光线传感器",
            Sensor.TYPE_MAGNETIC_FIELD to "磁场传感器",
            Sensor.TYPE_PRESSURE to "气压计",
        )

        return DiagnosticResult(
            id = "sensor_inventory",
            category = DiagnosticCategory.SENSOR,
            title = "传感器清单",
            status = if (sensors.isEmpty()) DiagnosticStatus.UNSUPPORTED else DiagnosticStatus.NORMAL,
            severity = if (sensors.isEmpty()) Severity.LOW else Severity.INFO,
            summary = "发现 ${sensors.size} 个传感器",
            details = important.associate { (type, name) ->
                name to if (grouped[type].isNullOrEmpty()) "未发现" else "支持"
            },
        )
    }

    private fun collectHardwareCapabilities(): DiagnosticResult {
        val pm = context.packageManager
        val capabilities = linkedMapOf(
            "后置摄像头" to pm.hasSystemFeature(PackageManager.FEATURE_CAMERA),
            "前置摄像头" to pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT),
            "闪光灯" to pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH),
            "麦克风" to pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE),
            "蓝牙" to pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            "低功耗蓝牙" to pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            "Wi-Fi" to pm.hasSystemFeature(PackageManager.FEATURE_WIFI),
            "NFC" to pm.hasSystemFeature(PackageManager.FEATURE_NFC),
            "GPS" to pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS),
            "指纹" to pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT),
            "USB Host" to pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
        )

        return DiagnosticResult(
            id = "hardware_capabilities",
            category = DiagnosticCategory.DEVICE_INFO,
            title = "硬件能力",
            status = DiagnosticStatus.NORMAL,
            summary = "支持 ${capabilities.count { it.value }} 项公共硬件能力",
            details = capabilities.mapValues { if (it.value) "支持" else "未发现" },
        )
    }
}

private fun Int.toChargeStatus(): String = when (this) {
    BatteryManager.BATTERY_STATUS_CHARGING -> "正在充电"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> "正在放电"
    BatteryManager.BATTERY_STATUS_FULL -> "已充满"
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "已连接但未充电"
    else -> "未知"
}

private fun Int.toPowerSource(): String = when (this) {
    BatteryManager.BATTERY_PLUGGED_AC -> "充电器"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
    else -> "未连接电源"
}

private fun Int.toBatteryHealth(): String = when (this) {
    BatteryManager.BATTERY_HEALTH_GOOD -> "良好（系统报告）"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
    BatteryManager.BATTERY_HEALTH_DEAD -> "失效"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "电压过高"
    BatteryManager.BATTERY_HEALTH_COLD -> "温度过低"
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "系统报告故障"
    else -> "未知"
}

