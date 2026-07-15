package com.androidphonecheck.app.diagnostic

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorGuidanceTest {
    private val rule = SensorCheckRule("移动手机", 3f)

    @Test fun unsupportedIsDistinctFromNoResponse() {
        assertEquals(SensorResponse.UNSUPPORTED, SensorResponseEvaluator.evaluate(false, 0, null, null, rule))
        assertEquals(SensorResponse.NO_RESPONSE, SensorResponseEvaluator.evaluate(true, 50, 1f, 2f, rule))
    }

    @Test fun sufficientChangeResponds() {
        assertEquals(SensorResponse.RESPONDED, SensorResponseEvaluator.evaluate(true, 10, 1f, 5f, rule))
    }

    @Test fun fewSamplesWait() {
        assertEquals(SensorResponse.WAITING, SensorResponseEvaluator.evaluate(true, 3, 1f, 8f, rule))
    }
}
