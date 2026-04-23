package com.filmlightmeter.app.ui

import androidx.lifecycle.ViewModel
import com.filmlightmeter.app.camera.CameraReading
import com.filmlightmeter.app.camera.MeteringMode
import com.filmlightmeter.app.data.CameraPreset
import com.filmlightmeter.app.data.CameraPresets
import com.filmlightmeter.app.data.FilmPreset
import com.filmlightmeter.app.data.FilmPresets
import com.filmlightmeter.app.exposure.ExposureMath
import kotlin.math.pow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PriorityMode { APERTURE, SHUTTER }

data class MeterUiState(
    val film: FilmPreset = FilmPresets.default,
    val camera: CameraPreset = CameraPresets.default,  // плёночный аппарат
    val customIso: Int? = null,          // ручной ISO; null = брать из плёнки
    val priority: PriorityMode = PriorityMode.APERTURE,
    val aperture: Double = 8.0,
    val shutterSeconds: Double = 1.0 / 125,
    val evCompensation: Double = 0.0,   // ±EV, шаг 1/3
    val ndStops: Double = 0.0,          // стопов ND фильтра
    val calibration: Double = 0.0,      // пользовательская калибровка ±EV
    val meteringMode: MeteringMode = MeteringMode.CENTER_WEIGHTED,
    val isLive: Boolean = true,
    val ev100: Double = 12.0,           // текущее оценочное EV100
    val rawLuminance: Double = 0.0,
    val frozenEv100: Double? = null,    // зафиксированный замер
    val reciprocityOn: Boolean = true,
    val snapToCamera: Boolean = true,   // подгонять расчёт под шкалу камеры
) {
    /** Действующее ISO: ручное (если задано) или из пресета плёнки. */
    val effectiveIso: Int
        get() = customIso ?: film.iso

    val effectiveEv100: Double
        get() = (frozenEv100 ?: ev100) + evCompensation - ndStops + calibration

    /** Идеальная (непрерывная) выдержка из формулы APEX. */
    val idealShutter: Double
        get() = ExposureMath.shutterFromEv(effectiveEv100, effectiveIso, aperture).let { t ->
            if (reciprocityOn) ExposureMath.reciprocityCorrection(t, film.reciprocityExponent) else t
        }

    /** Результат подгонки выдержки под шкалу камеры (или null если snap выкл). */
    val shutterSnap: ExposureMath.ShutterSnap?
        get() = if (snapToCamera) ExposureMath.snapShutterToCamera(
            idealShutter = idealShutter,
            availableShutters = camera.shutters,
            hasBulb = camera.hasBulb
        ) else null

    /**
     * Выдержка, которую покажем пользователю в приоритете диафрагмы.
     * Если snap включён — ближайшая из набора камеры, иначе идеал.
     */
    val computedShutter: Double
        get() = shutterSnap?.snappedShutter ?: idealShutter

    /**
     * Диафрагма в приоритете выдержки. Если snap включён — берём диафрагму
     * от уже «снаппнутой» выдержки (чтобы компенсировать сдвиг диафрагмой).
     */
    val computedAperture: Double
        get() = ExposureMath.apertureFromEv(effectiveEv100, effectiveIso, shutterSeconds)

    /**
     * Диафрагма с учётом снаппа выдержки в приоритете диафрагмы.
     * Если выдержка ушла «длиннее» нужного на compStops, диафрагму прикрываем на ту же величину.
     */
    val apertureCompensatedForSnap: Double
        get() {
            val snap = shutterSnap ?: return aperture
            if (snap.useBulb || snap.overexposed) return aperture
            // N_comp = N * sqrt(2^compStops)
            return aperture * 2.0.pow(snap.compensationStops / 2.0)
        }
}

class MeterViewModel : ViewModel() {
    private val _state = MutableStateFlow(MeterUiState())
    val state: StateFlow<MeterUiState> = _state.asStateFlow()

    fun setFilm(f: FilmPreset) = _state.update {
        // При смене плёнки сбрасываем ручной ISO, чтобы синхронизироваться с новой плёнкой
        it.copy(film = f, customIso = null)
    }

    fun setCamera(c: CameraPreset) = _state.update { it.copy(camera = c) }

    fun toggleSnapToCamera() = _state.update { it.copy(snapToCamera = !it.snapToCamera) }

    /** Установить ручной ISO. Значение null вернёт ISO плёнки. */
    fun setIso(iso: Int?) = _state.update { it.copy(customIso = iso) }

    fun setPriority(p: PriorityMode) = _state.update { it.copy(priority = p) }

    fun setAperture(n: Double) = _state.update { it.copy(aperture = n) }

    fun setShutter(t: Double) = _state.update { it.copy(shutterSeconds = t) }

    fun setEvComp(v: Double) = _state.update { it.copy(evCompensation = v) }

    fun setNd(stops: Double) = _state.update { it.copy(ndStops = stops) }

    fun setCalibration(v: Double) = _state.update { it.copy(calibration = v) }

    fun setMeteringMode(m: MeteringMode) = _state.update { it.copy(meteringMode = m) }

    fun toggleLive() = _state.update {
        if (it.isLive) it.copy(isLive = false, frozenEv100 = it.ev100)
        else it.copy(isLive = true, frozenEv100 = null)
    }

    fun toggleReciprocity() = _state.update { it.copy(reciprocityOn = !it.reciprocityOn) }

    /**
     * Пришёл кадр с камеры + известные параметры автоэкспозиции телефона.
     * Оцениваем EV100 по формуле.
     */
    fun onCameraFrame(
        reading: CameraReading,
        camAperture: Double,
        camShutter: Double,
        camIso: Int
    ) {
        if (!_state.value.isLive) return
        val ev100 = ExposureMath.estimateEv100FromCamera(
            aperture = camAperture,
            shutterSeconds = camShutter.coerceAtLeast(1.0 / 100_000),
            iso = camIso.coerceAtLeast(1),
            avgLuminance = reading.avgLuminance
        )
        // Сглаживание (низкочастотный фильтр)
        _state.update {
            val smoothed = it.ev100 * 0.7 + ev100 * 0.3
            it.copy(ev100 = smoothed, rawLuminance = reading.avgLuminance)
        }
    }
}
