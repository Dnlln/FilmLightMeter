package com.filmlightmeter.app.data

/**
 * Пресет плёночного фотоаппарата — набор физически доступных выдержек.
 *
 * "B" (bulb, от руки) моделируется как большое значение (60 сек) — если расчёт его требует,
 * значит сцена тёмная и снимать надо с B. В этом случае мы помечаем результат как «выдержка B».
 */
data class CameraPreset(
    val id: String,
    val name: String,
    /** Выдержки в секундах, отсортированы по возрастанию. Без B. */
    val shutters: List<Double>,
    /** Поддерживает ли длинную выдержку «от руки» (B / T). */
    val hasBulb: Boolean = true,
    /** Подпись с моделью — для UI. */
    val notes: String = ""
) {
    /** Минимальная (самая короткая) выдержка. */
    val fastest: Double get() = shutters.first()
    /** Максимальная (самая длинная, без B). */
    val slowest: Double get() = shutters.last()
}

object CameraPresets {

    // Шорткаты для читаемости
    private fun s(denom: Int) = 1.0 / denom

    val universal = CameraPreset(
        id = "universal",
        name = "Универсальная шкала",
        shutters = listOf(
            s(4000), s(2000), s(1000), s(500), s(250), s(125), s(60),
            s(30), s(15), s(8), s(4), 0.5, 1.0, 2.0, 4.0, 8.0, 15.0, 30.0
        ),
        hasBulb = true,
        notes = "Полная шкала от 1/4000 до 30 с"
    )

    val zenit12sd = CameraPreset(
        id = "zenit_12sd",
        name = "Зенит-12сд",
        shutters = listOf(s(500), s(250), s(125), s(60), s(30)),
        hasBulb = true,
        notes = "1/30 · 1/60 · 1/125 · 1/250 · 1/500 · В"
    )

    val zenitE = CameraPreset(
        id = "zenit_e",
        name = "Зенит-Е / Зенит-ЕМ",
        shutters = listOf(s(500), s(250), s(125), s(60), s(30)),
        hasBulb = true,
        notes = "1/30..1/500 + B"
    )

    val zenit122 = CameraPreset(
        id = "zenit_122",
        name = "Зенит-122 / 212К",
        shutters = listOf(s(500), s(250), s(125), s(60), s(30), s(15), s(8), s(4), 0.5, 1.0),
        hasBulb = true,
        notes = "1..1/500 + B"
    )

    val smena8m = CameraPreset(
        id = "smena_8m",
        name = "Смена-8М",
        shutters = listOf(s(250), s(125), s(60), s(30), s(15)),
        hasBulb = true,
        notes = "1/15..1/250 + B"
    )

    val fed5 = CameraPreset(
        id = "fed_5",
        name = "ФЭД-5 / Зоркий-4",
        shutters = listOf(s(1000), s(500), s(250), s(125), s(60), s(30), 1.0),
        hasBulb = true,
        notes = "1, 1/30..1/1000 + B"
    )

    val nikonFm2 = CameraPreset(
        id = "nikon_fm2",
        name = "Nikon FM2",
        shutters = listOf(
            s(4000), s(2000), s(1000), s(500), s(250), s(125), s(60),
            s(30), s(15), s(8), s(4), 0.5, 1.0
        ),
        hasBulb = true,
        notes = "1..1/4000 + B"
    )

    val leicaM6 = CameraPreset(
        id = "leica_m6",
        name = "Leica M6 / Pentax K1000",
        shutters = listOf(
            s(1000), s(500), s(250), s(125), s(60), s(30),
            s(15), s(8), s(4), 0.5, 1.0
        ),
        hasBulb = true,
        notes = "1..1/1000 + B"
    )

    val canonAe1 = CameraPreset(
        id = "canon_ae1",
        name = "Canon AE-1 / Olympus OM-1",
        shutters = listOf(
            s(1000), s(500), s(250), s(125), s(60), s(30),
            s(15), s(8), s(4), 0.5, 1.0, 2.0
        ),
        hasBulb = true,
        notes = "2..1/1000 + B"
    )

    val all: List<CameraPreset> = listOf(
        zenit12sd,
        zenitE,
        zenit122,
        smena8m,
        fed5,
        canonAe1,
        nikonFm2,
        leicaM6,
        universal
    )

    /** По умолчанию — Зенит-12сд, по просьбе пользователя. */
    val default: CameraPreset = zenit12sd

    fun byId(id: String): CameraPreset = all.firstOrNull { it.id == id } ?: default
}
