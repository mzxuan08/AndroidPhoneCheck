package com.androidphonecheck.app.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.androidphonecheck.app.domain.DiagnosticCategory
import com.androidphonecheck.app.domain.DiagnosticResult

private val AppColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF176B55),
    secondary = Color(0xFF4E635A),
    surfaceVariant = Color(0xFFE7F0EB),
)

@Composable
fun AndroidPhoneCheckApp(deviceInfo: DiagnosticResult) {
    MaterialTheme(colorScheme = AppColorScheme) {
        HomeScreen(deviceInfo = deviceInfo)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(deviceInfo: DiagnosticResult) {
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
                CategoryRow(category)
            }

            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("开始验机")
            }
        }
    }
}

@Composable
private fun CategoryRow(category: DiagnosticCategory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(category.displayName)
            Text("未测试", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

