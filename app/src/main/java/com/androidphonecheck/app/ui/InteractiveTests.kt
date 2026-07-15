package com.androidphonecheck.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.androidphonecheck.app.domain.DiagnosticStatus
import kotlin.math.floor

@Composable
private fun ImmersiveMode() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

@Composable
fun DisplayTestScreen(onResult: (DiagnosticStatus) -> Unit, onBack: () -> Unit) {
    ImmersiveMode()
    val colors = remember {
        listOf(
            "白色" to Color.White, "黑色" to Color.Black, "红色" to Color.Red,
            "绿色" to Color.Green, "蓝色" to Color.Blue, "灰色" to Color(0xFF808080),
        )
    }
    var index by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    val (name, color) = colors[index]
    BackHandler(onBack = onBack)
    Box(
        Modifier.fillMaxSize().background(color).pointerInput(Unit) {
            detectTapGestures(
                onTap = { index = (index + 1) % colors.size },
                onDoubleTap = { controlsVisible = !controlsVisible },
                onLongPress = { onBack() },
            )
        },
    ) {
        if (controlsVisible) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xCC202020)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("$name（${index + 1}/${colors.size}）· 单击换色 · 双击隐藏 · 长按退出", color = Color.White)
                Text("观察坏点、色斑、闪烁和漏光。", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button({ onResult(DiagnosticStatus.NORMAL) }, Modifier.weight(1f)) { Text("显示正常") }
                    Button({ onResult(DiagnosticStatus.ABNORMAL) }, Modifier.weight(1f)) { Text("发现异常") }
                }
                OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("稍后测试", color = Color.White) }
            }
        }
    }
}

@Composable
fun TouchTestScreen(onResult: (DiagnosticStatus) -> Unit, onBack: () -> Unit) {
    ImmersiveMode()
    val columns = 8
    val rows = 16
    val total = columns * rows
    var size by remember { mutableStateOf(IntSize.Zero) }
    var covered by remember { mutableStateOf(emptySet<Int>()) }
    val trails = remember { mutableStateMapOf<Long, List<Offset>>() }
    var currentTouches by remember { mutableIntStateOf(0) }
    var maxTouches by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }

    fun mark(id: PointerId, offset: Offset) {
        if (size.width <= 0 || size.height <= 0) return
        val column = floor(offset.x / size.width * columns).toInt().coerceIn(0, columns - 1)
        val row = floor(offset.y / size.height * rows).toInt().coerceIn(0, rows - 1)
        covered = covered + (row * columns + column)
        trails[id.value] = (trails[id.value].orEmpty() + offset).takeLast(200)
    }

    BackHandler(onBack = onBack)
    Box(
        Modifier.fillMaxSize().background(Color(0xFF101513)).onSizeChanged { size = it }
            .pointerInput(size) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        currentTouches = pressed.size
                        maxTouches = maxOf(maxTouches, currentTouches)
                        pressed.forEach { change -> mark(change.id, change.position); change.consume() }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cw = this.size.width / columns
            val ch = this.size.height / rows
            repeat(rows) { row -> repeat(columns) { column ->
                val id = row * columns + column
                drawRect(
                    if (id in covered) Color(0x6636C98F) else Color(0x3366706C),
                    Offset(column * cw, row * ch), androidx.compose.ui.geometry.Size(cw, ch),
                )
                drawRect(Color(0x8866706C), Offset(column * cw, row * ch),
                    androidx.compose.ui.geometry.Size(cw, ch), style = Stroke(1f))
            } }
            trails.forEach { (id, points) ->
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    val palette = listOf(Color.Cyan, Color.Yellow, Color.Magenta, Color.Green, Color.Red)
                    drawPath(path, palette[(id % palette.size).toInt()], style = Stroke(6f))
                }
                points.lastOrNull()?.let { drawCircle(Color.White, 12f, it) }
            }
        }
        Text(
            "覆盖 ${covered.size}/$total · 当前 $currentTouches 点 · 最大 $maxTouches 点",
            Modifier.align(Alignment.TopCenter).background(Color(0xCC202020)).padding(10.dp),
            color = Color.White,
        )
        if (controlsVisible) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xDD202020)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ onResult(DiagnosticStatus.NORMAL) }, Modifier.weight(1f), enabled = covered.size == total) { Text("触控正常") }
                    Button({ onResult(DiagnosticStatus.ABNORMAL) }, Modifier.weight(1f)) { Text("存在死区") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ covered = emptySet(); trails.clear(); maxTouches = 0 }, Modifier.weight(1f)) { Text("重测", color = Color.White) }
                    OutlinedButton({ controlsVisible = false }, Modifier.weight(1f)) { Text("隐藏面板", color = Color.White) }
                    OutlinedButton(onBack, Modifier.weight(1f)) { Text("退出", color = Color.White) }
                }
            }
        } else {
            Button({ controlsVisible = true }, Modifier.align(Alignment.BottomEnd).padding(12.dp)) { Text("显示面板") }
        }
    }
}
