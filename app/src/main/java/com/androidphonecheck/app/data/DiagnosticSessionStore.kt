package com.androidphonecheck.app.data

import android.content.Context
import com.androidphonecheck.app.domain.DiagnosticCategory
import com.androidphonecheck.app.domain.DiagnosticStatus

class DiagnosticSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("diagnostic_session", Context.MODE_PRIVATE)

    fun hasSession(): Boolean = preferences.getBoolean(KEY_STARTED, false)

    fun loadStatuses(): Map<DiagnosticCategory, DiagnosticStatus> =
        DiagnosticCategory.entries.associateWith { category ->
            preferences.getString(statusKey(category), null)
                ?.let { stored -> DiagnosticStatus.entries.find { it.name == stored } }
                ?: DiagnosticStatus.NOT_TESTED
        }

    fun start(automaticCategories: Set<DiagnosticCategory>): Map<DiagnosticCategory, DiagnosticStatus> {
        val statuses = loadStatuses().toMutableMap()
        automaticCategories.forEach { category ->
            if (statuses[category] == DiagnosticStatus.NOT_TESTED) {
                statuses[category] = DiagnosticStatus.NORMAL
            }
        }
        saveAll(statuses, started = true)
        return statuses
    }

    fun update(
        category: DiagnosticCategory,
        status: DiagnosticStatus,
    ): Map<DiagnosticCategory, DiagnosticStatus> {
        preferences.edit().putString(statusKey(category), status.name).apply()
        return loadStatuses()
    }

    fun clear(): Map<DiagnosticCategory, DiagnosticStatus> {
        preferences.edit().clear().apply()
        return loadStatuses()
    }

    private fun saveAll(
        statuses: Map<DiagnosticCategory, DiagnosticStatus>,
        started: Boolean,
    ) {
        preferences.edit().apply {
            putBoolean(KEY_STARTED, started)
            statuses.forEach { (category, status) ->
                putString(statusKey(category), status.name)
            }
        }.apply()
    }

    private fun statusKey(category: DiagnosticCategory) = "status_${category.name}"

    private companion object {
        const val KEY_STARTED = "started"
    }
}

