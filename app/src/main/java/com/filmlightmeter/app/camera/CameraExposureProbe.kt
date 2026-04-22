package com.filmlightmeter.app.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo

/**
 * Извлекает параметры автоэкспозиции камеры: выдержку, ISO, диафрагму.
 * Эти значения передаются формуле оценки EV100 в ExposureMath.
 */
@OptIn(ExperimentalCamera2Interop::class)
object CameraExposureProbe {

    data class ExposureState(
        val exposureTimeSec: Double,
        val iso: Int,
        val aperture: Double
    )

    /**
     * Читает последний CaptureResult из CameraX Camera2 interop (если доступен).
     * Возвращает null, если данных ещё нет.
     */
    fun readFromCaptureResult(result: CaptureResult?): ExposureState? {
        result ?: return null
        val tNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: return null
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: return null
        val aperture = result.get(CaptureResult.LENS_APERTURE) ?: 1.8f
        return ExposureState(
            exposureTimeSec = tNs / 1_000_000_000.0,
            iso = iso,
            aperture = aperture.toDouble()
        )
    }

    fun defaultAperture(info: CameraInfo): Double {
        val c2Info = Camera2CameraInfo.from(info)
        val apertures = c2Info.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES
        )
        return apertures?.firstOrNull()?.toDouble() ?: 1.8
    }
}
