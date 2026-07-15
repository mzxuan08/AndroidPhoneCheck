package com.androidphonecheck.app.domain

enum class DiagnosticCategory(val displayName: String) {
    DEVICE_INFO("基本信息"),
    DISPLAY("屏幕显示"),
    TOUCH("触摸"),
    CAMERA("摄像头"),
    AUDIO("音频"),
    PHYSICAL("实体功能"),
    SENSOR("传感器"),
    CONNECTIVITY("连接能力"),
    BATTERY("电池与充电"),
    BIOMETRIC("生物识别"),
    USB_STORAGE("USB 与存储"),
    SECURITY("安全风险"),
}

enum class DiagnosticStatus(val displayName: String) {
    NORMAL("正常"),
    ABNORMAL("异常"),
    RISK("风险提示"),
    NOT_TESTED("未测试"),
    UNSUPPORTED("不支持"),
    PERMISSION_REQUIRED("权限不足"),
}

enum class Severity(val priority: Int) {
    INFO(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4),
}

data class DiagnosticResult(
    val id: String,
    val category: DiagnosticCategory,
    val title: String,
    val status: DiagnosticStatus,
    val severity: Severity = Severity.INFO,
    val summary: String = "",
    val details: Map<String, String> = emptyMap(),
)

data class DiagnosticSummary(
    val abnormalCount: Int,
    val riskCount: Int,
    val incompleteCount: Int,
) {
    companion object {
        fun from(results: List<DiagnosticResult>) = DiagnosticSummary(
            abnormalCount = results.count { it.status == DiagnosticStatus.ABNORMAL },
            riskCount = results.count { it.status == DiagnosticStatus.RISK },
            incompleteCount = results.count {
                it.status == DiagnosticStatus.NOT_TESTED ||
                    it.status == DiagnosticStatus.PERMISSION_REQUIRED
            },
        )
    }
}

fun List<DiagnosticResult>.sortedForSummary(): List<DiagnosticResult> =
    sortedWith(
        compareByDescending<DiagnosticResult> { it.status == DiagnosticStatus.ABNORMAL }
            .thenByDescending { it.status == DiagnosticStatus.RISK }
            .thenByDescending { it.severity.priority }
            .thenBy { it.category.ordinal },
    )

