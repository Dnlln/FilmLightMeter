package com.filmlightmeter.app.exposure

import java.util.Locale
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Формулы APEX (Additive System of Photographic Exposure).
 *
 * EV = log2(N^2 / t) = Av + Tv
 *   N — диафрагма (f-число)
 *   t — выдержка (секунды)
 *
 * EV при ISO 100, связанный с яркостью сцены:
 *   EV100 = log2(L * S / K)
 *   где L — яркость (cd/m^2), S = 100, K — калибровочная константа (обычно 12.5)
 *
 * Пересчёт EV для другого ISO:
 *   EV_iso = EV100 + log2(S / 100)
 */
object ExposureMath {

    const val K_CONSTANT = 12.5  // Reflected-light calibration (Sekonic/Minolta)

    /** EV для заданной диафрагмы и выдержки (при ISO 100). */
    fun evFromApertureShutter(aperture: Double, shutterSeconds: Double): Double {
        require(aperture > 0) { "aperture must be > 0" }
        require(shutterSeconds > 0) { "shutter must be > 0" }
        return log2(aperture * aperture / shutterSeconds)
    }

    /** Пересчёт EV с одного ISO на другое. */
    fun evAtIso(ev100: Double, iso: Int): Double {
        return ev100 + log2(iso / 100.0)
    }

    /** Обратный пересчёт EV любого ISO к EV100 (для хранения). */
    fun toEv100(evAtIso: Double, iso: Int): Double {
        return evAtIso - log2(iso / 100.0)
    }

    /**
     * Вычислить выдержку по заданным EV, ISO и диафрагме.
     * t = N^2 / 2^EV_iso
     */
    fun shutterFromEv(ev100: Double, iso: Int, aperture: Double): Double {
        val evIso = evAtIso(ev100, iso)
        return aperture * aperture / 2.0.pow(evIso)
    }

    /**
     * Вычислить диафрагму по заданным EV, ISO и выдержке.
     * N = sqrt(t * 2^EV_iso)
     */
    fun apertureFromEv(ev100: Double, iso: Int, shutterSeconds: Double): Double {
        val evIso = evAtIso(ev100, iso)
        return kotlin.math.sqrt(shutterSeconds * 2.0.pow(evIso))
    }

    /**
     * Оценка EV100 из параметров экспозиции камеры телефона.
     * Формула: EV100 = log2( (N^2 / t) * (100 / S) ) - log2(avgY / midGray)
     *
     * Это способ откалиброваться через саму камеру: если телефон снял кадр
     * с параметрами N, t, S и средняя яркость Y близка к среднему серому (≈118/255),
     * то сцена как раз соответствует EV. Отклонение Y от среднего серого даёт поправку.
     *
     * @param aperture f-число, с которым снимал телефон
     * @param shutterSeconds выдержка телефона, сек
     * @param iso ISO телефона
     * @param avgLuminance средняя яркость Y-канала [0..255]
     * @param midGrayTarget целевой «средний серый» (по умолчанию 118 ≈ 18% в гамме sRGB)
     */
    fun estimateEv100FromCamera(
        aperture: Double,
        shutterSeconds: Double,
        iso: Int,
        avgLuminance: Double,
        midGrayTarget: Double = 118.0
    ): Double {
        val evAtCamIso = log2(aperture * aperture / shutterSeconds)
        val ev100 = evAtCamIso - log2(iso / 100.0)
        // Поправка на яркость кадра: если кадр светлее среднего серого — сцена ярче
        val brightnessOffset = log2((avgLuminance.coerceAtLeast(1.0)) / midGrayTarget)
        return ev100 + brightnessOffset
    }

    // -------- Округление к стандартным шкалам --------

    /** Стандартная шкала выдержек в секундах (полные стопы). */
    val STANDARD_SHUTTERS = listOf(
        1.0 / 8000, 1.0 / 4000, 1.0 / 2000, 1.0 / 1000,
        1.0 / 500, 1.0 / 250, 1.0 / 125, 1.0 / 60,
        1.0 / 30, 1.0 / 15, 1.0 / 8, 1.0 / 4, 0.5,
        1.0, 2.0, 4.0, 8.0, 15.0, 30.0, 60.0
    )

    /** Стандартная шкала диафрагм (полные стопы). */
    val STANDARD_APERTURES = listOf(
        1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0
    )

    /** Стандартная шкала ISO. */
    val STANDARD_ISOS = listOf(
        25, 50, 64, 100, 125, 160, 200, 400, 800, 1600, 3200, 6400
    )

    /** Ближайшее стандартное значение (логарифмическое расстояние). */
    fun nearestStandard(value: Double, scale: List<Double>): Double {
        return scale.minBy { kotlin.math.abs(ln(it) - ln(value)) }
    }

    /** Форматировать выдержку как "1/125" или "2s". */
    fun formatShutter(seconds: Double): String {
        if (seconds >= 1.0) {
            return if (seconds == seconds.roundToInt().toDouble()) {
                "${seconds.roundToInt()}s"
            } else {
                String.format(Locale.US, "%.1fs", seconds)
            }
        }
        val denom = (1.0 / seconds).roundToInt()
        return "1/$denom"
    }

    fun formatAperture(n: Double): String {
        val formatted = String.format(Locale.US, "%.1f", n).trimEnd('0').trimEnd('.')
        return "f/$formatted"
    }

    /**
     * Компенсация взаимности (reciprocity failure) для плёнки.
     * При выдержках длиннее 1 секунды плёнка теряет чувствительность.
     * Упрощённая формула: t_corrected = t^p, где p зависит от плёнки.
     */
    fun reciprocityCorrection(shutterSeconds: Double, exponent: Double): Double {
        if (shutterSeconds <= 1.0) return shutterSeconds
        return shutterSeconds.pow(exponent)
    }

    // -------- Подгонка под реальную шкалу камеры --------

    /**
     * Результат подгонки идеального значения выдержки к дискретной шкале камеры.
     *
     * @property snappedShutter ближайшая выдержка из набора камеры (сек)
     * @property compensationStops насколько «снаппинг» сдвинул EV: log2(snapped / ideal).
     *   Положительное число означает что камера держит дольше, чем надо, то есть
     *   экспозиция «переработана» на эти стопы — надо **прикрыть** диафрагму.
     * @property useBulb true, если даже самой длинной выдержки мало и нужен режим B
     * @property overexposed true, если даже самой короткой выдержки мало
     *   (нужно либо прикрыть диафрагму больше, чем позволяет объектив, либо ND-фильтр)
     */
    data class ShutterSnap(
        val snappedShutter: Double,
        val compensationStops: Double,
        val useBulb: Boolean,
        val overexposed: Boolean
    )

    /**
     * Подогнать идеальное значение выдержки под дискретный набор камеры.
     *
     * Логика: из списка берём ближайшую по логарифму (в стопах) выдержку.
     * Если идеал длиннее самой длинной и у камеры есть B — предлагаем B
     * (возвращаем идеальное значение как snappedShutter + флаг useBulb).
     * Если идеал короче самой короткой — возвращаем самую короткую + флаг
     * overexposed: в этом случае разницу надо отрабатывать диафрагмой/ND.
     */
    fun snapShutterToCamera(
        idealShutter: Double,
        availableShutters: List<Double>,
        hasBulb: Boolean
    ): ShutterSnap {
        require(availableShutters.isNotEmpty()) { "shutter list empty" }
        val fastest = availableShutters.min()
        val slowest = availableShutters.max()

        // Идеал длиннее самой длинной — пробуем B
        if (idealShutter > slowest * 1.01 && hasBulb) {
            return ShutterSnap(
                snappedShutter = idealShutter,
                compensationStops = 0.0,
                useBulb = true,
                overexposed = false
            )
        }

        // Идеал короче самой короткой — передержка, берём самую короткую
        if (idealShutter < fastest * 0.99) {
            val comp = log2(fastest / idealShutter)
            return ShutterSnap(
                snappedShutter = fastest,
                compensationStops = comp,
                useBulb = false,
                overexposed = true
            )
        }

        val nearest = availableShutters.minBy { kotlin.math.abs(ln(it) - ln(idealShutter)) }
        // Положительная компенсация = snapped держит дольше идеала = диафрагму надо прикрыть
        val compensationStops = log2(nearest / idealShutter)
        return ShutterSnap(
            snappedShutter = nearest,
            compensationStops = compensationStops,
            useBulb = false,
            overexposed = false
        )
    }
}
