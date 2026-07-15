package com.androidphonecheck.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.androidphonecheck.app.domain.DiagnosticStatus
import kotlin.math.floor

@Composable
fun DisplayTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val colors = remember {
        listOf(
            "白色" to Color.White,
            "黑色" to Color.Black,
            "红色" to Color.Red,
            "绿色" to Color.Green,
            "蓝色" to Color.Blue,
            "灰色" to Color(0xFF808080),
        )
    }
    var index by remember { mutableStateOf(0) }
    val (name, color) = colors[index]
    BackHandler(onBack = onBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .clickable { index = (index + 1) % colors.size },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC202020))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "$name（${index + 1}/${colors.size}）· 点击屏幕切换颜色",
                color = Color.White,
            )
            Text("观察坏点、色斑、闪烁和漏光；黑色画面适合在暗处检查。", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onResult(DiagnosticStatus.NORMAL) }, modifier = Modifier.weight(1f)) {
                    Text("显示正常")
                }
                Button(onClick = { onResult(DiagnosticStatus.ABNORMAL) }, modifier = Modifier.weight(1f)) {
                    Text("发现异常")
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("稍后测试", color = Color.White)
            }
        }
    }
}

@Composable
fun TouchTestScreen(
    onResult: (DiagnosticStatus) -> Unit,
    onBack: () -> Unit,
) {
    val columns = 6
    val rows = 12
    val total = columns * rows
    var size by remember { mutableStateOf(IntSize.Zero) }
    var covered by remember { mutableStateOf(emptySet<Int>()) }

    fun mark(offset: Offset) {
        if (size.width <= 0 || size.height <= 0) return
        val column = floor(offset.x / size.width * columns).toInt().coerceIn(0, columns - 1)
        val row = floor(offset.y / size.height * rows).toInt().coerceIn(0, rows - 1)
        covered = covered + (row * columns + column)
    }

    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF101513))) {
        Text(
            "触摸测试：请连续划过所有方格",
            modifier = Modifier.padding(16.dp),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size = it }
                    .pointerInput(size) {
                        detectDragGestures(
                            onDragStart = ::mark,
                            onDrag = { change, _ ->
                                mark(change.position)
                                change.consume()
                            },
                        )
                    },
            ) {
                val cellWidth = this.size.width / columns
                val cellHeight = this.size.height / rows
                for (row in 0 until rows) {
                    for (column in 0 until columns) {
                        val id = row * columns + column
                        drawRect(
                            color = if (id in covered) Color(0xFF36C98F) else Color(0xFF66706C),
                            topLeft = Offset(column * cellWidth, row * cellHeight),
                            size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight),
                            style = Stroke(width = 2f),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("已覆盖 ${covered.size}/$total 个区域", color = Color.White)
            Button(
                onClick = { onResult(DiagnosticStatus.NORMAL) },
                modifier = Modifier.fillMaxWidth(),
                enabled = covered.size == total,
            ) { Text("全部覆盖，触摸正常") }
            Button(
                onClick = { onResult(DiagnosticStatus.ABNORMAL) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("存在无法划过的区域") }
            OutlinedButton(onClick = { covered = emptySet() }, modifier = Modifier.fillMaxWidth()) {
                Text("重新测试")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("稍后测试")
            }
        }
    }
}

