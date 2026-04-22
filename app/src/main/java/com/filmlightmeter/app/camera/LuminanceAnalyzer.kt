package com.filmlightmeter.app.camera

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Анализатор кадров CameraX: читает Y-плоскость (яркость в YUV_420_888)
 * и вычисляет среднюю яркость в заданной области кадра.
 *
 * Область (meteringRegion) задана в долях [0..1] относительно размера кадра.
 * Это позволяет реализовать точечный / центровзвешенный / матричный замер.
 */
class LuminanceAnalyzer(
    private val onResult: (CameraReading) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile
    var meteringRegion: RegionFractions = RegionFractions(0f, 0f, 1f, 1f)

    @Volatile
    var modeWeight: MeteringMode = MeteringMode.CENTER_WEIGHTED

    override fun analyze(image: ImageProxy) {
        try {
            val yPlane = image.planes[0]
            val buffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride
            val width = image.width
            val height = image.height

            val rect = computeRect(width, height, meteringRegion)
            val avg = when (modeWeight) {
                MeteringMode.SPOT -> averageY(buffer, rowStride, pixelStride, rect)
                MeteringMode.CENTER_WEIGHTED -> centerWeightedAverage(
                    buffer, rowStride, pixelStride, width, height
                )
                MeteringMode.MATRIX -> averageY(
                    buffer, rowStride, pixelStride,
                    Rect(0, 0, width, height)
                )
            }

            // Извлечение EXIF-подобных метаданных кадра
            val exposureTimeNs = runCatching {
                image.imageInfo.let { info ->
                    // CameraX не выдаёт это напрямую — читаем через Camera2 tag если есть
                    info.javaClass.getMethod("getCameraCaptureResult").invoke(info)
                }
            }.getOrNull()

            onResult(
                CameraReading(
                    avgLuminance = avg,
                    width = width,
                    height = height,
                    meteringRect = rect
                )
            )
        } catch (t: Throwable) {
            // безопасно игнорируем единичные сбои анализа
        } finally {
            image.close()
        }
    }

    private fun computeRect(w: Int, h: Int, r: RegionFractions): Rect {
        val left = (r.left * w).toInt().coerceIn(0, w - 1)
        val top = (r.top * h).toInt().coerceIn(0, h - 1)
        val right = (r.right * w).toInt().coerceIn(left + 1, w)
        val bottom = (r.bottom * h).toInt().coerceIn(top + 1, h)
        return Rect(left, top, right, bottom)
    }

    private fun averageY(
        buffer: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        rect: Rect
    ): Double {
        var sum = 0L
        var count = 0L
        // Сэмплируем, чтобы не нагружать CPU при высоком разрешении
        val step = ((rect.width() * rect.height()) / 20000).coerceAtLeast(1)
        var y = rect.top
        while (y < rect.bottom) {
            val rowStart = y * rowStride
            var x = rect.left
            while (x < rect.right) {
                val idx = rowStart + x * pixelStride
                if (idx in 0 until buffer.capacity()) {
                    sum += buffer.get(idx).toInt() and 0xFF
                    count++
                }
                x += step
            }
            y += step
        }
        return if (count == 0L) 0.0 else sum.toDouble() / count
    }

    private fun centerWeightedAverage(
        buffer: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        w: Int, h: Int
    ): Double {
        val cx = w / 2.0
        val cy = h / 2.0
        val maxR = kotlin.math.hypot(cx, cy)
        var sum = 0.0
        var wSum = 0.0
        val step = ((w * h) / 20000).coerceAtLeast(1).let {
            kotlin.math.sqrt(it.toDouble()).toInt().coerceAtLeast(1)
        }
        var y = 0
        while (y < h) {
            val rowStart = y * rowStride
            var x = 0
            while (x < w) {
                val idx = rowStart + x * pixelStride
                if (idx in 0 until buffer.capacity()) {
                    val v = buffer.get(idx).toInt() and 0xFF
                    val d = kotlin.math.hypot(x - cx, y - cy) / maxR
                    val weight = (1.0 - d).coerceAtLeast(0.05)
                    sum += v * weight
                    wSum += weight
                }
                x += step
            }
            y += step
        }
        return if (wSum == 0.0) 0.0 else sum / wSum
    }
}

data class RegionFractions(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class CameraReading(
    val avgLuminance: Double, // Y 0..255
    val width: Int,
    val height: Int,
    val meteringRect: Rect
)

enum class MeteringMode { SPOT, CENTER_WEIGHTED, MATRIX }
