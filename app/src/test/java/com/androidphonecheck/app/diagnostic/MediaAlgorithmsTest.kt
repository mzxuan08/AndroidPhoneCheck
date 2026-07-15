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

    @Test fun noiseEstimatorSeparatesFlatAndNoisyFrames() {
        val flat = ByteArray(64 * 64) { 120 }
        val noisy = ByteArray(64 * 64) { (120 + (it * 17 % 9) - 4).toByte() }
        val flatMetrics = CameraAlgorithm.measure(flat, 64, 64)
        val noisyMetrics = CameraAlgorithm.measure(noisy, 64, 64)
        assertTrue(noisyMetrics.noiseEstimate > flatMetrics.noiseEstimate)
    }

    @Test fun cameraSequenceWaitsForMultipleFrames() {
        val frame = ByteArray(64 * 64) { if (it % 2 == 0) 40 else 100 }
        val metrics = CameraAlgorithm.measure(frame, 64, 64)
        val sequence = CameraSequenceAnalyzer(requiredSamples = 3)
        assertEquals(null, sequence.add(frame, metrics))
        assertEquals(null, sequence.add(frame, metrics))
        assertEquals(AlgorithmVerdict.SUSPECTED, sequence.add(frame, metrics)?.verdict)
    }

    @Test fun unstableCameraSequenceIsUnsuitable() {
        val sequence = CameraSequenceAnalyzer(requiredSamples = 3)
        var result: CameraAssessment? = null
        repeat(3) { frameIndex ->
            val frame = ByteArray(64 * 64) { if ((it + frameIndex) % 2 == 0) 20 else 220.toByte() }
            result = sequence.add(frame, CameraAlgorithm.measure(frame, 64, 64))
        }
        assertEquals(AlgorithmVerdict.UNSUITABLE, result?.verdict)
    }

    @Test fun clearSpeechPassesAudioAssessment() {
        val noise = ShortArray(8_000) { (it % 7).toShort() }
        val speech = ShortArray(16_000) { (sin(it * .08) * 4_000).toInt().toShort() }
        assertEquals(AlgorithmVerdict.NORMAL, AudioAlgorithm.assess(AudioAlgorithm.measure(noise, speech)).verdict)
        val metrics = AudioAlgorithm.measure(noise, speech)
        assertTrue(metrics.voiceBandRatio > .8)
        assertTrue(metrics.dominantFrequencyHz in 150.0..300.0)
    }

    @Test fun silentSpeechIsUnsuitable() {
        val metrics = AudioAlgorithm.measure(ShortArray(1_000), ShortArray(2_000))
        assertEquals(AlgorithmVerdict.UNSUITABLE, AudioAlgorithm.assess(metrics).verdict)
    }
}
