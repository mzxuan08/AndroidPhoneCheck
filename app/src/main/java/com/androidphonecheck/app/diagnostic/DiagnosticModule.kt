package com.androidphonecheck.app.diagnostic

import com.androidphonecheck.app.domain.DiagnosticCategory
import com.androidphonecheck.app.domain.DiagnosticResult
import kotlinx.coroutines.flow.Flow

sealed interface Capability {
    data object Available : Capability
    data class Unsupported(val reason: String) : Capability
    data class PermissionRequired(val permissions: Set<String>) : Capability
}

interface DiagnosticModule {
    val category: DiagnosticCategory

    suspend fun probeCapability(): Capability

    fun results(): Flow<List<DiagnosticResult>>

    suspend fun start()

    suspend fun stop()
}

