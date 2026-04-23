package com.filmlightmeter.app.data

/**
 * Пресет плёночного объектива — дискретный набор физических значений диафрагмы.
 * На объективе шкала диафрагм ступенчатая: кольцо щёлкает по рискам.
 * У большинства старых объективов шаг = 1 стоп; у части современных — 1/2 стопа.
 */
data class LensPreset(
    val id: String,
    val name: String,
    /** Диафрагмы (f-числа), отсортированы по возрастанию. */
    val apertures: List<Double>,
    /** Короткая подпись для UI. */
    val notes: String = ""
) {
    val widest: Double get() = apertures.first()
    val narrowest: Double get() = apertures.last()
}

object LensPresets {

    val helios44m = LensPreset(
        id = "helios_44m",
        name = "Гелиос-44М (58mm f/2)",
        apertures = listOf(2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0),
        notes = "f/2 · 2.8 · 4 · 5.6 · 8 · 11 · 16"
    )

    val industar50 = LensPreset(
        id = "industar_50",
        name = "Индустар-50-2 (50mm f/3.5)",
        apertures = listOf(3.5, 4.0, 5.6, 8.0, 11.0, 16.0),
        notes = "f/3.5 · 4 · 5.6 · 8 · 11 · 16"
    )

    val jupiter37a = LensPreset(
        id = "jupiter_37a",
        name = "Юпитер-37А (135mm f/3.5)",
        apertures = listOf(3.5, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0),
        notes = "f/3.5 · 4 · 5.6 · 8 · 11 · 16 · 22"
    )

    val jupiter9 = LensPreset(
        id = "jupiter_9",
        name = "Юпитер-9 (85mm f/2)",
        apertures = listOf(2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0),
        notes = "f/2 · 2.8 · 4 · 5.6 · 8 · 11 · 16"
    )

    val mir1 = LensPreset(
        id = "mir_1",
        name = "Мир-1 (37mm f/2.8)",
        apertures = listOf(2.8, 4.0, 5.6, 8.0, 11.0, 16.0),
        notes = "f/2.8 · 4 · 5.6 · 8 · 11 · 16"
    )

    val tair11a = LensPreset(
        id = "tair_11a",
        name = "Таир-11А (135mm f/2.8)",
        apertures = listOf(2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0),
        notes = "f/2.8 · 4 · 5.6 · 8 · 11 · 16 · 22"
    )

    val triplet78 = LensPreset(
        id = "triplet_78",
        name = "Триплет-78 (Смена-8М, 40mm f/4)",
        apertures = listOf(4.0, 5.6, 8.0, 11.0, 16.0),
        notes = "f/4 · 5.6 · 8 · 11 · 16"
    )

    val nikkor50 = LensPreset(
        id = "nikkor_50",
        name = "Nikkor 50mm f/1.8 (half-stop)",
        apertures = listOf(1.8, 2.0, 2.8, 3.3, 4.0, 4.8, 5.6, 6.7, 8.0, 9.5, 11.0, 13.0, 16.0),
        notes = "Полустопы 1.8..16"
    )

    val universal = LensPreset(
        id = "universal_lens",
        name = "Универсальная шкала",
        apertures = listOf(1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0),
        notes = "Стандартные стопы 1.4..22"
    )

    val all: List<LensPreset> = listOf(
        helios44m,
        industar50,
        jupiter37a,
        jupiter9,
        mir1,
        tair11a,
        triplet78,
        nikkor50,
        universal
    )

    /** По умолчанию Гелиос-44М — штатник Зенита. */
    val default: LensPreset = helios44m

    fun byId(id: String): LensPreset = all.firstOrNull { it.id == id } ?: default
}
