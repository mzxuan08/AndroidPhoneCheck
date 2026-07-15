package com.androidphonecheck.app.diagnostic

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sqrt

enum class AlgorithmVerdict { NORMAL, SUSPECTED, UNSUITABLE }

data class CameraMetrics(
    val meanLuma: Double,
    val darkRatio: Double,
    val brightRatio: Double,
    val sharpness: Double,
    val chromaOffset: Double,
)

data class CameraAssessment(val verdict: AlgorithmVerdict, val reasons: List<String>, val metrics: CameraMetrics)

object CameraAlgorithm {
    fun measure(y: ByteArray, width: Int, height: Int, uMean: Double = 128.0, vMean: Double = 128.0): CameraMetrics {
        require(width > 2 && height > 2 && y.size >= width * height)
        var sum = 0.0
        var dark = 0
        var bright = 0
        for (value in y.take(width * height)) {
            val level = value.toInt() and 0xff
            sum += level
            if (level < 20) dark++
            if (level > 235) bright++
        }
        var laplacianEnergy = 0.0
        var samples = 0
        for (row in 1 until height - 1 step 2) for (column in 1 until width - 1 step 2) {
            val center = y[row * width + column].toInt() and 0xff
            val laplacian = 4 * center -
                (y[(row - 1) * width + column].toInt() and 0xff) -
                (y[(row + 1) * width + column].toInt() and 0xff) -
                (y[row * width + column - 1].toInt() and 0xff) -
                (y[row * width + column + 1].toInt() and 0xff)
            laplacianEnergy += laplacian.toDouble() * laplacian
            samples++
        }
        val count = width * height
        return CameraMetrics(
            meanLuma = sum / count,
            darkRatio = dark.toDouble() / count,
            brightRatio = bright.toDouble() / count,
            sharpness = laplacianEnergy / samples.coerceAtLeast(1),
            chromaOffset = sqrt((uMean - 128) * (uMean - 128) + (vMean - 128) * (vMean - 128)),
        )
    }

    fun assess(metrics: CameraMetrics): CameraAssessment {
        if (metrics.meanLuma < 18 || metrics.darkRatio > .92) {
            return CameraAssessment(AlgorithmVerdict.UNSUITABLE, listOf("环境过暗或镜头被遮挡"), metrics)
        }
        if (metrics.meanLuma > 242 || metrics.brightRatio > .92) {
            return CameraAssessment(AlgorithmVerdict.UNSUITABLE, listOf("画面严重过曝，请更换场景"), metrics)
        }
        val reasons = buildList {
            if (metrics.sharpness < 35) add("画面纹理较少或清晰度偏低")
            if (metrics.chromaOffset > 42) add("画面存在明显色偏")
            if (metrics.darkRatio > .65) add("暗部比例偏高")
            if (metrics.brightRatio > .65) add("高光比例偏高")
        }
        return CameraAssessment(if (reasons.isEmpty()) AlgorithmVerdict.NORMAL else AlgorithmVerdict.SUSPECTED, reasons, metrics)
    }
}

data class AudioMetrics(
    val rms: Double,
    val peak: Int,
    val snrDb: Double,
    val clippingRatio: Double,
    val dropoutRatio: Double,
    val zeroCrossingRate: Double,
)

data class AudioAssessment(val verdict: AlgorithmVerdict, val reasons: List<String>, val metrics: AudioMetrics)

object AudioAlgorithm {
    fun measure(noise: ShortArray, speech: ShortArray): AudioMetrics {
        fun rms(values: ShortArray) = sqrt(values.fold(0.0) { sum, v -> sum + v.toDouble() * v } / values.size.coerceAtLeast(1))
        val noiseRms = rms(noise).coerceAtLeast(1.0)
        val speechRms = rms(speech)
        val peak = speech.maxOfOrNull { abs(it.toInt()) } ?: 0
        val clipping = speech.count { abs(it.toInt()) >= 32_000 }.toDouble() / speech.size.coerceAtLeast(1)
        val window = 256
        val dropout = speech.asSequence().chunked(window).count { block -> block.maxOfOrNull { abs(it.toInt()) } ?: 0 < 20 }
            .toDouble() / ((speech.size + window - 1) / window).coerceAtLeast(1)
        var crossings = 0
        for (i in 1 until speech.size) if ((speech[i] >= 0) != (speech[i - 1] >= 0)) crossings++
        return AudioMetrics(
            rms = speechRms,
            peak = peak,
            snrDb = 20 * log10(speechRms.coerceAtLeast(1.0) / noiseRms),
            clippingRatio = clipping,
            dropoutRatio = dropout,
            zeroCrossingRate = crossings.toDouble() / speech.size.coerceAtLeast(1),
        )
    }

    fun assess(metrics: AudioMetrics): AudioAssessment {
        if (metrics.rms < 120 || metrics.snrDb < 4) {
            return AudioAssessment(AlgorithmVerdict.UNSUITABLE, listOf("未检测到足够清晰的语音，请靠近麦克风重试"), metrics)
        }
        val reasons = buildList {
            if (metrics.clippingRatio > .02) add("录音存在明显削波/爆音")
            if (metrics.dropoutRatio > .08) add("录音存在疑似断音")
            if (metrics.zeroCrossingRate !in .01..0.45) add("语音频率特征异常")
        }
        return AudioAssessment(if (reasons.isEmpty()) AlgorithmVerdict.NORMAL else AlgorithmVerdict.SUSPECTED, reasons, metrics)
    }
}
