package com.filmlightmeter.app.data

/**
 * Пресет плёнки: название, номинальное ISO и показатель
 * для компенсации закона взаимности (reciprocity failure).
 *
 * reciprocityExponent — экспонента для t_corrected = t^p при t > 1s.
 * Значения примерные, основанные на техлистах производителей.
 * p = 1.0 означает «нет коррекции» (цифровая камера, reversal).
 */
data class FilmPreset(
    val name: String,
    val iso: Int,
    val type: FilmType,
    val reciprocityExponent: Double = 1.0,
    val notes: String = ""
)

enum class FilmType { COLOR_NEG, BW_NEG, SLIDE }

object FilmPresets {

    val all: List<FilmPreset> = listOf(
        // Цветная негативная
        FilmPreset("Kodak Portra 160", 160, FilmType.COLOR_NEG, 1.33, "Мягкие тона, портрет"),
        FilmPreset("Kodak Portra 400", 400, FilmType.COLOR_NEG, 1.33, "Универсальная, широкая широта"),
        FilmPreset("Kodak Portra 800", 800, FilmType.COLOR_NEG, 1.33, "Для низкого света"),
        FilmPreset("Kodak Ektar 100", 100, FilmType.COLOR_NEG, 1.30, "Насыщенные цвета, пейзаж"),
        FilmPreset("Kodak Gold 200", 200, FilmType.COLOR_NEG, 1.33, "Тёплая, бюджетная"),
        FilmPreset("Kodak ColorPlus 200", 200, FilmType.COLOR_NEG, 1.33, "Бюджетная"),
        FilmPreset("Kodak UltraMax 400", 400, FilmType.COLOR_NEG, 1.33, "Универсальная"),
        FilmPreset("Fuji Superia X-Tra 400", 400, FilmType.COLOR_NEG, 1.30, "Зелёно-магента"),
        FilmPreset("Fuji C200", 200, FilmType.COLOR_NEG, 1.30, "Нейтральная, зелёная"),
        FilmPreset("Cinestill 400D", 400, FilmType.COLOR_NEG, 1.20, "Дейлайт, halation"),
        FilmPreset("Cinestill 800T", 800, FilmType.COLOR_NEG, 1.20, "Tungsten, ночь"),
        FilmPreset("Lomo 400", 400, FilmType.COLOR_NEG, 1.33, "Контрастная"),
        FilmPreset("Harman Phoenix 200", 200, FilmType.COLOR_NEG, 1.30, "Вибрантная, halation"),

        // Чёрно-белая негативная
        FilmPreset("Kodak Tri-X 400", 400, FilmType.BW_NEG, 1.33, "Классика"),
        FilmPreset("Kodak T-Max 100", 100, FilmType.BW_NEG, 1.20, "Мелкозернистая"),
        FilmPreset("Kodak T-Max 400", 400, FilmType.BW_NEG, 1.20, "T-grain"),
        FilmPreset("Kodak T-Max P3200", 3200, FilmType.BW_NEG, 1.20, "Высокочувствительная"),
        FilmPreset("Ilford HP5 Plus 400", 400, FilmType.BW_NEG, 1.31, "Универсальная, push"),
        FilmPreset("Ilford FP4 Plus 125", 125, FilmType.BW_NEG, 1.26, "Средний контраст"),
        FilmPreset("Ilford Delta 100", 100, FilmType.BW_NEG, 1.26, "Мелкое зерно"),
        FilmPreset("Ilford Delta 400", 400, FilmType.BW_NEG, 1.41, "T-grain"),
        FilmPreset("Ilford Delta 3200", 3200, FilmType.BW_NEG, 1.30, "Для слабого света"),
        FilmPreset("Ilford Pan F Plus 50", 50, FilmType.BW_NEG, 1.33, "Сверхмелкое зерно"),
        FilmPreset("Ilford XP2 Super 400", 400, FilmType.BW_NEG, 1.31, "C-41 процесс"),
        FilmPreset("Fomapan 100", 100, FilmType.BW_NEG, 1.30, "Бюджетная классика"),
        FilmPreset("Fomapan 400", 400, FilmType.BW_NEG, 1.30, "Бюджетная"),
        FilmPreset("Kentmere Pan 400", 400, FilmType.BW_NEG, 1.30, "Бюджетная Ilford"),

        // Слайд (reversal) — ниже допуски
        FilmPreset("Fuji Velvia 50", 50, FilmType.SLIDE, 1.00, "Насыщенные пейзажи"),
        FilmPreset("Fuji Velvia 100", 100, FilmType.SLIDE, 1.00, "Контраст и цвет"),
        FilmPreset("Fuji Provia 100F", 100, FilmType.SLIDE, 1.00, "Нейтральная"),
        FilmPreset("Kodak Ektachrome E100", 100, FilmType.SLIDE, 1.00, "Чистые цвета"),
    )

    val default: FilmPreset = all.first { it.name == "Kodak Portra 400" }
}
