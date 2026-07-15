package com.androidphonecheck.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticModelsTest {
    @Test
    fun summaryCountsOnlyActionableAndIncompleteStatuses() {
        val results = listOf(
            result("a", DiagnosticStatus.ABNORMAL),
            result("b", DiagnosticStatus.RISK),
            result("c", DiagnosticStatus.NOT_TESTED),
            result("d", DiagnosticStatus.PERMISSION_REQUIRED),
            result("e", DiagnosticStatus.UNSUPPORTED),
            result("f", DiagnosticStatus.NORMAL),
        )

        assertEquals(DiagnosticSummary(1, 1, 2), DiagnosticSummary.from(results))
    }

    @Test
    fun summarySortsAbnormalBeforeRiskAndHigherSeverityFirst() {
        val results = listOf(
            result("normal", DiagnosticStatus.NORMAL, Severity.INFO),
            result("risk", DiagnosticStatus.RISK, Severity.HIGH),
            result("minor", DiagnosticStatus.ABNORMAL, Severity.LOW),
            result("critical", DiagnosticStatus.ABNORMAL, Severity.CRITICAL),
        )

        assertEquals(
            listOf("critical", "minor", "risk", "normal"),
            results.sortedForSummary().map { it.id },
        )
    }

    private fun result(
        id: String,
        status: DiagnosticStatus,
        severity: Severity = Severity.INFO,
    ) = DiagnosticResult(
        id = id,
        category = DiagnosticCategory.DEVICE_INFO,
        title = id,
        status = status,
        severity = severity,
    )
}
