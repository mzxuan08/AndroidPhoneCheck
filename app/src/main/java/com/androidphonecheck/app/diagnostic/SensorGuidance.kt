package com.androidphonecheck.app.diagnostic

enum class SensorResponse { WAITING, RESPONDED, NO_RESPONSE, UNSUPPORTED }

data class SensorCheckRule(
    val instruction: String,
    val minimumChange: Float,
)

object SensorResponseEvaluator {
    fun evaluate(
        supported: Boolean,
        sampleCount: Int,
        minimum: Float?,
        maximum: Float?,
        rule: SensorCheckRule,
    ): SensorResponse = when {
        !supported -> SensorResponse.UNSUPPORTED
        sampleCount < 5 || minimum == null || maximum == null -> SensorResponse.WAITING
        maximum - minimum >= rule.minimumChange -> SensorResponse.RESPONDED
        sampleCount >= 50 -> SensorResponse.NO_RESPONSE
        else -> SensorResponse.WAITING
    }
}
