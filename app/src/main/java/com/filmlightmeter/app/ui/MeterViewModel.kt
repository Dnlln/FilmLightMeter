package com.filmlightmeter.app.ui

import androidx.lifecycle.ViewModel
import com.filmlightmeter.app.camera.CameraReading
import com.filmlightmeter.app.camera.MeteringMode
import com.filmlightmeter.app.data.FilmPreset
import com.filmlightmeter.app.data.FilmPresets
import com.filmlightmeter.app.exposure.ExposureMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PriorityMode { APERTURE, SHUTTER }

data class MeterUiState(
    val film: FilmPreset = FilmPresets.default,
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
) {
    /** Действующее ISO: ручное (если задано) или из пресета плёнки. */
    val effectiveIso: Int
        get() = customIso ?: film.iso

    val effectiveEv100: Double
        get() = (frozenEv100 ?: ev100) + evCompensation - ndStops + calibration

    val computedShutter: Double
        get() = ExposureMath.shutterFromEv(effectiveEv100, effectiveIso, aperture).let { t ->
            if (reciprocityOn) ExposureMath.reciprocityCorrection(t, film.reciprocityExponent) else t
        }

    val computedAperture: Double
        get() = ExposureMath.apertureFromEv(effectiveEv100, effectiveIso, shutterSeconds)
}

class MeterViewModel : ViewModel() {
    private val _state = MutableStateFlow(MeterUiState())
    val state: StateFlow<MeterUiState> = _state.asStateFlow()

    fun setFilm(f: FilmPreset) = _state.update {
        // При смене плёнки сбрасываем ручной ISO, чтобы синхронизироваться с новой плёнкой
        it.copy(film = f, customIso = null)
    }

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
