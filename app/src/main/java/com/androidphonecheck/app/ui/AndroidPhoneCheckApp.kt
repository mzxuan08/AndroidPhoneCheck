package com.androidphonecheck.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.androidphonecheck.app.domain.DiagnosticCategory
import com.androidphonecheck.app.domain.DiagnosticResult
import com.androidphonecheck.app.domain.DiagnosticStatus
import com.androidphonecheck.app.data.DiagnosticSessionStore

private val AppColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF176B55),
    secondary = Color(0xFF4E635A),
    surfaceVariant = Color(0xFFE7F0EB),
)

@Composable
fun AndroidPhoneCheckApp(automaticResults: List<DiagnosticResult>) {
    MaterialTheme(colorScheme = AppColorScheme) {
        DiagnosticFlow(automaticResults = automaticResults)
    }
}

private enum class AppScreen { HOME, CHECKLIST, DISPLAY_TEST, TOUCH_TEST, CAMERA_TEST, AUDIO_TEST, PHYSICAL_TEST, SENSOR_TEST, CONNECTIVITY_TEST, BIOMETRIC_TEST, USB_STORAGE_TEST, SECURITY_TEST, SUMMARY }

@Composable
private fun DiagnosticFlow(automaticResults: List<DiagnosticResult>) {
    val context = LocalContext.current
    val store = remember { DiagnosticSessionStore(context.applicationContext) }
    var statuses by remember { mutableStateOf(store.loadStatuses()) }
    var screen by remember { mutableStateOf(AppScreen.HOME) }

    BackHandler(enabled = screen != AppScreen.HOME) {
        screen = when (screen) {
            AppScreen.SUMMARY -> AppScreen.CHECKLIST
            AppScreen.DISPLAY_TEST, AppScreen.TOUCH_TEST, AppScreen.CAMERA_TEST, AppScreen.AUDIO_TEST, AppScreen.PHYSICAL_TEST, AppScreen.SENSOR_TEST, AppScreen.CONNECTIVITY_TEST, AppScreen.BIOMETRIC_TEST, AppScreen.USB_STORAGE_TEST, AppScreen.SECURITY_TEST -> AppScreen.CHECKLIST
            AppScreen.CHECKLIST -> AppScreen.HOME
            AppScreen.HOME -> AppScreen.HOME
        }
    }

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            automaticResults = automaticResults,
            hasSession = store.hasSession(),
            onStart = {
                statuses = store.start(automaticResults.map { it.category }.toSet())
                screen = AppScreen.CHECKLIST
            },
        )
        AppScreen.CHECKLIST -> ChecklistScreen(
            statuses = statuses,
            onStatusChange = { category, status ->
                statuses = store.update(category, status)
            },
            onSummary = { screen = AppScreen.SUMMARY },
            onInteractiveTest = { category ->
                screen = when (category) {
                    DiagnosticCategory.DISPLAY -> AppScreen.DISPLAY_TEST
                    DiagnosticCategory.TOUCH -> AppScreen.TOUCH_TEST
                    DiagnosticCategory.CAMERA -> AppScreen.CAMERA_TEST
                    DiagnosticCategory.AUDIO -> AppScreen.AUDIO_TEST
                    DiagnosticCategory.PHYSICAL -> AppScreen.PHYSICAL_TEST
                    DiagnosticCategory.SENSOR -> AppScreen.SENSOR_TEST
                    DiagnosticCategory.CONNECTIVITY -> AppScreen.CONNECTIVITY_TEST
                    DiagnosticCategory.BIOMETRIC -> AppScreen.BIOMETRIC_TEST
                    DiagnosticCategory.USB_STORAGE -> AppScreen.USB_STORAGE_TEST
                    DiagnosticCategory.SECURITY -> AppScreen.SECURITY_TEST
                    else -> AppScreen.CHECKLIST
                }
            },
            onBack = { screen = AppScreen.HOME },
        )
        AppScreen.DISPLAY_TEST -> DisplayTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.DISPLAY, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.TOUCH_TEST -> TouchTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.TOUCH, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.CAMERA_TEST -> CameraTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.CAMERA, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.AUDIO_TEST -> AudioTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.AUDIO, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.PHYSICAL_TEST -> PhysicalTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.PHYSICAL, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.SENSOR_TEST -> SensorTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.SENSOR, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.CONNECTIVITY_TEST -> ConnectivityTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.CONNECTIVITY, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.BIOMETRIC_TEST -> BiometricTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.BIOMETRIC, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.USB_STORAGE_TEST -> UsbStorageTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.USB_STORAGE, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.SECURITY_TEST -> SecurityRiskTestScreen(
            onResult = { status ->
                statuses = store.update(DiagnosticCategory.SECURITY, status)
                screen = AppScreen.CHECKLIST
            },
            onBack = { screen = AppScreen.CHECKLIST },
        )
        AppScreen.SUMMARY -> SummaryScreen(
            statuses = statuses,
            automaticResults = automaticResults,
            onBack = { screen = AppScreen.CHECKLIST },
            onClear = {
                statuses = store.clear()
                screen = AppScreen.HOME
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    automaticResults: List<DiagnosticResult>,
    hasSession: Boolean,
    onStart: () -> Unit,
) {
    val deviceInfo = automaticResults.first()
    Scaffold(
        topBar = { TopAppBar(title = { Text("安卓验机") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(deviceInfo.summary, style = MaterialTheme.typography.titleLarge)
                    Text(
                        deviceInfo.details["Android"].orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("验机说明", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("自动检测与人工操作结合。读取不到的项目会明确标记，不会猜测结果。")
                }
            }

            Text("检测项目", style = MaterialTheme.typography.titleMedium)
            DiagnosticCategory.entries.forEach { category ->
                val categoryResults = automaticResults.filter { it.category == category }
                CategoryRow(
                    category = category,
                    status = if (hasSession && categoryResults.isNotEmpty()) {
                        "已自动检测 ${categoryResults.size} 项"
                    } else {
                        "未测试"
                    },
                )
            }

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (hasSession) "继续验机" else "开始验机")
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: DiagnosticCategory,
    status: String,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(category.displayName)
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistScreen(
    statuses: Map<DiagnosticCategory, DiagnosticStatus>,
    onStatusChange: (DiagnosticCategory, DiagnosticStatus) -> Unit,
    onSummary: () -> Unit,
    onInteractiveTest: (DiagnosticCategory) -> Unit,
    onBack: () -> Unit,
) {
    var selected by remember { mutableStateOf<DiagnosticCategory?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("验机项目") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("点击项目记录测试结果。自动检测项目已预先标记，可随时复核。")
            DiagnosticCategory.entries.forEach { category ->
                CategoryRow(
                    category = category,
                    status = statuses.getValue(category).displayName,
                    onClick = {
                        if (category in setOf(
                                DiagnosticCategory.DISPLAY,
                                DiagnosticCategory.TOUCH,
                                DiagnosticCategory.CAMERA,
                                DiagnosticCategory.AUDIO,
                                DiagnosticCategory.PHYSICAL,
                                DiagnosticCategory.SENSOR,
                                DiagnosticCategory.CONNECTIVITY,
                                DiagnosticCategory.BIOMETRIC,
                                DiagnosticCategory.USB_STORAGE,
                                DiagnosticCategory.SECURITY,
                            )
                        ) {
                            onInteractiveTest(category)
                        } else {
                            selected = category
                        }
                    },
                )
            }
            Button(onClick = onSummary, modifier = Modifier.fillMaxWidth()) {
                Text("查看验机结果")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("返回首页")
            }
        }
    }

    selected?.let { category ->
        StatusDialog(
            category = category,
            onSelect = { status ->
                onStatusChange(category, status)
                selected = null
            },
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun StatusDialog(
    category: DiagnosticCategory,
    onSelect: (DiagnosticStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(category.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    DiagnosticStatus.NORMAL,
                    DiagnosticStatus.ABNORMAL,
                    DiagnosticStatus.RISK,
                    DiagnosticStatus.NOT_TESTED,
                    DiagnosticStatus.UNSUPPORTED,
                    DiagnosticStatus.PERMISSION_REQUIRED,
                ).forEach { status ->
                    OutlinedButton(
                        onClick = { onSelect(status) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(status.displayName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryScreen(
    statuses: Map<DiagnosticCategory, DiagnosticStatus>,
    automaticResults: List<DiagnosticResult>,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    val abnormal = statuses.values.count { it == DiagnosticStatus.ABNORMAL }
    val risks = statuses.values.count { it == DiagnosticStatus.RISK }
    val incomplete = statuses.values.count {
        it == DiagnosticStatus.NOT_TESTED || it == DiagnosticStatus.PERMISSION_REQUIRED
    }
    Scaffold(topBar = { TopAppBar(title = { Text("验机结果") }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("异常 $abnormal 项 · 风险 $risks 项 · 未完成 $incomplete 项", fontWeight = FontWeight.Bold)
            statuses.entries
                .sortedByDescending { statusPriority(it.value) }
                .forEach { (category, status) -> CategoryRow(category, status.displayName) }
            Text("自动采集详情", style = MaterialTheme.typography.titleMedium)
            automaticResults.forEach { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(result.title, fontWeight = FontWeight.Bold)
                        Text(result.summary)
                        result.details.forEach { (key, value) -> Text("$key：$value") }
                    }
                }
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回继续检测") }
            OutlinedButton(
                onClick = { confirmClear = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("清除本次验机") }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清除验机结果？") },
            text = { Text("清除后无法恢复。") },
            confirmButton = { Button(onClick = onClear) { Text("确认清除") } },
            dismissButton = { OutlinedButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}

private fun statusPriority(status: DiagnosticStatus): Int = when (status) {
    DiagnosticStatus.ABNORMAL -> 6
    DiagnosticStatus.RISK -> 5
    DiagnosticStatus.PERMISSION_REQUIRED -> 4
    DiagnosticStatus.NOT_TESTED -> 3
    DiagnosticStatus.UNSUPPORTED -> 2
    DiagnosticStatus.NORMAL -> 1
}
