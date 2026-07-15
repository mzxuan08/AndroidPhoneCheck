package com.androidphonecheck.app.diagnostic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class MediaAlgorithmsTest {
    @Test fun darkCameraSceneIsUnsuitable() {
        val metrics = CameraAlgorithm.measure(ByteArray(64 * 64) { 5 }, 64, 64)
        assertEquals(AlgorithmVerdict.UNSUITABLE, CameraAlgorithm.assess(metrics).verdict)
    }

    @Test fun texturedCameraSceneHasSharpness() {
        val frame = ByteArray(64 * 64) { if (it % 2 == 0) 40 else 200.toByte() }
        assertTrue(CameraAlgorithm.measure(frame, 64, 64).sharpness > 35)
    }

    @Test fun clearSpeechPassesAudioAssessment() {
        val noise = ShortArray(8_000) { (it % 7).toShort() }
        val speech = ShortArray(16_000) { (sin(it * .08) * 4_000).toInt().toShort() }
        assertEquals(AlgorithmVerdict.NORMAL, AudioAlgorithm.assess(AudioAlgorithm.measure(noise, speech)).verdict)
    }

    @Test fun silentSpeechIsUnsuitable() {
        val metrics = AudioAlgorithm.measure(ShortArray(1_000), ShortArray(2_000))
        assertEquals(AlgorithmVerdict.UNSUITABLE, AudioAlgorithm.assess(metrics).verdict)
    }
}
