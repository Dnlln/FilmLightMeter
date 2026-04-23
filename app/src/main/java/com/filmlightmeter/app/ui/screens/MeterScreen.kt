package com.filmlightmeter.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.widget.Toast
import com.filmlightmeter.app.camera.MeteringMode
import com.filmlightmeter.app.data.FilmPresets
import com.filmlightmeter.app.exposure.ExposureMath
import com.filmlightmeter.app.ui.MeterViewModel
import com.filmlightmeter.app.ui.PriorityMode
import com.filmlightmeter.app.ui.components.CameraPreview
import com.filmlightmeter.app.ui.components.ValueStrip
import com.filmlightmeter.app.ui.theme.BrassAccent
import com.filmlightmeter.app.ui.theme.CreamDial
import com.filmlightmeter.app.ui.theme.LeatherBrown
import com.filmlightmeter.app.ui.theme.LeatherDark
import com.filmlightmeter.app.util.ScreenCapture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterScreen(
    vm: MeterViewModel,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val state by vm.state.collectAsState()
    var filmMenuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(LeatherDark, LeatherBrown, LeatherDark))
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // --- Шапка: название + режим ---
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FILM LIGHT METER",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrassAccent,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "Экспонометр для плёнки",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreamDial.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { aboutOpen = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Справка",
                    tint = BrassAccent
                )
            }
        }

        if (aboutOpen) {
            AboutDialog(
                onDismiss = { aboutOpen = false },
                onOpenSettings = {
                    aboutOpen = false
                    settingsOpen = true
                },
                onOpenGithub = {
                    uriHandler.openUri("https://github.com/Dnlln/FilmLightMeter")
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        // --- Камера ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(2.dp, BrassAccent.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .background(LeatherDark)
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    meteringMode = state.meteringMode,
                    onReading = { reading, aperture, shutter, iso ->
                        vm.onCameraFrame(reading, aperture, shutter, iso)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Нужен доступ к камере для замера освещения",
                        color = CreamDial,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrassAccent,
                            contentColor = LeatherDark
                        )
                    ) { Text("Разрешить") }
                }
            }

            // Плашка со значением EV и кнопка Lock
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LeatherDark.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "EV₁₀₀ ",
                    color = CreamDial.copy(alpha = 0.75f),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    String.format(java.util.Locale.US, "%.1f", state.effectiveEv100),
                    color = CreamDial,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            IconButton(
                onClick = vm::toggleLive,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = if (state.isLive) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = "Заморозить замер",
                    tint = if (state.isLive) CreamDial else BrassAccent
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Режимы замера + кнопка «Снять» ---
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ModeChip("Точка", state.meteringMode == MeteringMode.SPOT) {
                vm.setMeteringMode(MeteringMode.SPOT)
            }
            ModeChip("Центр", state.meteringMode == MeteringMode.CENTER_WEIGHTED) {
                vm.setMeteringMode(MeteringMode.CENTER_WEIGHTED)
            }
            ModeChip("Матрица", state.meteringMode == MeteringMode.MATRIX) {
                vm.setMeteringMode(MeteringMode.MATRIX)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val activity = context as? Activity
                    if (activity == null) {
                        Toast.makeText(context, "Не удалось сделать снимок", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    ScreenCapture.capture(activity) { result ->
                        when (result) {
                            is ScreenCapture.Result.Success ->
                                Toast.makeText(
                                    context,
                                    "Сохранено в галерею: ${result.fileName}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            is ScreenCapture.Result.Error ->
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrassAccent,
                    contentColor = LeatherDark
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Снять", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))

        // --- Экспопара: крупным шрифтом ---
        val snap = state.shutterSnap
        val shutterText = when (state.priority) {
            PriorityMode.APERTURE ->
                if (snap?.useBulb == true) "B"
                else ExposureMath.formatShutter(state.computedShutter)
            PriorityMode.SHUTTER -> ExposureMath.formatShutter(state.shutterSeconds)
        }
        val apertureText = ExposureMath.formatAperture(
            when (state.priority) {
                PriorityMode.APERTURE -> state.apertureCompensatedForSnap
                PriorityMode.SHUTTER -> state.computedAperture
            }
        )

        ExposurePairCard(shutterText, apertureText, state.priority, vm::setPriority)

        // Предупреждения о выходе за диапазон
        if (state.snapToCamera && snap != null && (snap.useBulb || snap.overexposed)) {
            Spacer(Modifier.height(6.dp))
            WarningBanner(
                text = when {
                    snap.useBulb ->
                        "Нужна выдержка длиннее ${ExposureMath.formatShutter(state.camera.slowest)} — снимайте на режиме B примерно ${ExposureMath.formatShutter(state.idealShutter)}"
                    else ->
                        "Сцена слишком яркая: даже на ${ExposureMath.formatShutter(state.camera.fastest)} не хватает. Прикройте диафрагму или используйте ND-фильтр"
                }
            )
        }

        // Варианты реальных пар (выдержка × диафрагма)
        if (state.snapToCamera) {
            Spacer(Modifier.height(10.dp))
            ExposurePairsCard(
                pairs = state.bestPairs(4),
                lensName = state.lens.name,
                cameraName = state.camera.name,
                onPick = { pair ->
                    vm.setAperture(pair.aperture)
                    vm.setShutter(pair.shutter)
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        // --- Выбор диафрагмы/выдержки ---
        if (state.priority == PriorityMode.APERTURE) {
            ValueStrip(
                label = "ДИАФРАГМА  f/",
                values = ExposureMath.STANDARD_APERTURES.map {
                    String.format(java.util.Locale.US, "%.1f", it).trimEnd('0').trimEnd('.')
                },
                selectedIndex = ExposureMath.STANDARD_APERTURES.indexOfFirst {
                    kotlin.math.abs(it - state.aperture) < 0.01
                }.coerceAtLeast(0),
                onSelect = { vm.setAperture(ExposureMath.STANDARD_APERTURES[it]) },
                highlighted = true
            )
        } else {
            // В приоритете выдержки показываем только выдержки камеры (если snap вкл.)
            val shutterValues = if (state.snapToCamera)
                state.camera.shutters
            else
                ExposureMath.STANDARD_SHUTTERS
            ValueStrip(
                label = if (state.snapToCamera) "ВЫДЕРЖКА  ·  ${state.camera.name}"
                        else "ВЫДЕРЖКА",
                values = shutterValues.map(ExposureMath::formatShutter),
                selectedIndex = shutterValues.indexOfFirst {
                    kotlin.math.abs(it - state.shutterSeconds) < 1e-5
                }.coerceAtLeast(0),
                onSelect = { vm.setShutter(shutterValues[it]) },
                highlighted = true
            )
        }

        Spacer(Modifier.height(12.dp))

        // --- ISO (ручной выбор) ---
        val isoValues = ExposureMath.STANDARD_ISOS
        val isoLabels = buildList {
            add("AUTO") // = брать ISO из плёнки
            addAll(isoValues.map { it.toString() })
        }
        val isoSelectedIdx = if (state.customIso == null) 0
            else (isoValues.indexOf(state.customIso) + 1).coerceAtLeast(0)
        ValueStrip(
            label = "ISO  ·  ${state.effectiveIso}" +
                if (state.customIso != null) "  (ручной)" else "  (плёнка)",
            values = isoLabels,
            selectedIndex = isoSelectedIdx,
            onSelect = { idx ->
                if (idx == 0) vm.setIso(null)
                else vm.setIso(isoValues[idx - 1])
            },
            highlighted = state.customIso != null
        )

        Spacer(Modifier.height(10.dp))

        // --- Плёнка ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ПЛЁНКА",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrassAccent
                )
                Text(
                    state.film.name,
                    color = CreamDial,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "ISO плёнки ${state.film.iso} · ${state.film.notes}",
                    color = CreamDial.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Box {
                OutlinedButton(
                    onClick = { filmMenuOpen = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrassAccent)
                ) { Text("Выбрать") }
                DropdownMenu(
                    expanded = filmMenuOpen,
                    onDismissRequest = { filmMenuOpen = false },
                    modifier = Modifier.background(LeatherBrown)
                ) {
                    FilmPresets.all.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(preset.name, color = CreamDial)
                                    Text(
                                        "ISO ${preset.iso}",
                                        color = CreamDial.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            onClick = {
                                vm.setFilm(preset)
                                filmMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Камера (плёночный аппарат) ---
        var cameraMenuOpen by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "КАМЕРА",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrassAccent
                )
                Text(
                    state.camera.name,
                    color = CreamDial,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    state.camera.notes,
                    color = CreamDial.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Box {
                OutlinedButton(
                    onClick = { cameraMenuOpen = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrassAccent)
                ) { Text("Выбрать") }
                DropdownMenu(
                    expanded = cameraMenuOpen,
                    onDismissRequest = { cameraMenuOpen = false },
                    modifier = Modifier.background(LeatherBrown)
                ) {
                    com.filmlightmeter.app.data.CameraPresets.all.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(preset.name, color = CreamDial)
                                    Text(
                                        preset.notes,
                                        color = CreamDial.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            onClick = {
                                vm.setCamera(preset)
                                cameraMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Объектив ---
        var lensMenuOpen by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ОБЪЕКТИВ",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrassAccent
                )
                Text(
                    state.lens.name,
                    color = CreamDial,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    state.lens.notes,
                    color = CreamDial.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Box {
                OutlinedButton(
                    onClick = { lensMenuOpen = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrassAccent)
                ) { Text("Выбрать") }
                DropdownMenu(
                    expanded = lensMenuOpen,
                    onDismissRequest = { lensMenuOpen = false },
                    modifier = Modifier.background(LeatherBrown)
                ) {
                    com.filmlightmeter.app.data.LensPresets.all.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(preset.name, color = CreamDial)
                                    Text(
                                        preset.notes,
                                        color = CreamDial.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            onClick = {
                                vm.setLens(preset)
                                lensMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (settingsOpen) {
            SettingsPanel(vm)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun WarningBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LeatherDark)
            .border(1.dp, BrassAccent, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠ ", color = BrassAccent, fontWeight = FontWeight.Bold)
        Text(
            text,
            color = CreamDial.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ExposurePairsCard(
    pairs: List<ExposureMath.ExposurePair>,
    lensName: String,
    cameraName: String,
    onPick: (ExposureMath.ExposurePair) -> Unit
) {
    if (pairs.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LeatherDark)
            .border(1.dp, BrassAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            "ВАРИАНТЫ СЪЕМКИ",
            color = BrassAccent,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            "Реальные пары из шкал $cameraName / $lensName — нажмите чтобы выбрать",
            color = CreamDial.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        pairs.forEach { pair ->
            PairRow(pair, onClick = { onPick(pair) })
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PairRow(
    pair: ExposureMath.ExposurePair,
    onClick: () -> Unit
) {
    val borderColor = when {
        pair.isExact -> BrassAccent
        kotlin.math.abs(pair.errorStops) < 0.5 -> BrassAccent.copy(alpha = 0.5f)
        else -> CreamDial.copy(alpha = 0.3f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(LeatherBrown.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .then(Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Выдержка
        Text(
            ExposureMath.formatShutter(pair.shutter),
            color = CreamDial,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(70.dp)
        )
        Text("×", color = BrassAccent, fontFamily = FontFamily.Serif, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        // Диафрагма
        Text(
            ExposureMath.formatAperture(pair.aperture),
            color = CreamDial,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(70.dp)
        )
        Spacer(Modifier.weight(1f))
        // Ошибка
        val errText = if (pair.isExact) "точно"
            else String.format(java.util.Locale.US, "%+.2f EV", pair.errorStops)
        Text(
            errText,
            color = if (pair.isExact) BrassAccent else CreamDial.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrassAccent)
        ) { Text("✓", fontSize = 14.sp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrassAccent,
            selectedLabelColor = LeatherDark,
            containerColor = LeatherDark,
            labelColor = CreamDial
        )
    )
}

@Composable
private fun ExposurePairCard(
    shutter: String,
    aperture: String,
    priority: PriorityMode,
    onPriorityChange: (PriorityMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(listOf(LeatherDark, LeatherBrown, LeatherDark))
            )
            .border(1.5.dp, BrassAccent, RoundedCornerShape(14.dp))
            .padding(vertical = 18.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ExposureValueBlock(
            label = "ВЫДЕРЖКА",
            value = shutter,
            highlighted = priority == PriorityMode.APERTURE,
            onClick = { onPriorityChange(PriorityMode.SHUTTER) }
        )
        Text(
            "×",
            color = BrassAccent,
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp
        )
        ExposureValueBlock(
            label = "ДИАФРАГМА",
            value = aperture,
            highlighted = priority == PriorityMode.SHUTTER,
            onClick = { onPriorityChange(PriorityMode.APERTURE) }
        )
    }
}

@Composable
private fun ExposureValueBlock(
    label: String,
    value: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) BrassAccent else CreamDial.copy(alpha = 0.6f)
        )
        Text(
            value,
            color = if (highlighted) CreamDial else CreamDial.copy(alpha = 0.9f),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        Text(
            if (highlighted) "рассчитано" else "нажмите чтобы фиксировать",
            color = if (highlighted) BrassAccent.copy(alpha = 0.8f) else CreamDial.copy(alpha = 0.45f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(2.dp))
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrassAccent)
        ) { Text(if (highlighted) "задать" else "приоритет", fontSize = 11.sp) }
    }
}

@Composable
private fun SettingsPanel(vm: MeterViewModel) {
    val state by vm.state.collectAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LeatherDark)
            .border(1.dp, BrassAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("Точная настройка", color = BrassAccent, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))

        StopAdjuster(
            label = "Коррекция EV",
            value = state.evCompensation,
            onChange = vm::setEvComp
        )
        Spacer(Modifier.height(8.dp))
        StopAdjuster(
            label = "ND фильтр (стопов)",
            value = state.ndStops,
            onChange = vm::setNd,
            positiveOnly = true
        )
        Spacer(Modifier.height(8.dp))
        StopAdjuster(
            label = "Калибровка",
            value = state.calibration,
            onChange = vm::setCalibration
        )
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Учитывать закон взаимности плёнки",
                color = CreamDial,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = state.reciprocityOn,
                onClick = vm::toggleReciprocity,
                label = { Text(if (state.reciprocityOn) "ВКЛ" else "ВЫКЛ") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrassAccent,
                    selectedLabelColor = LeatherDark,
                    containerColor = LeatherBrown,
                    labelColor = CreamDial
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Подгонять под шкалу камеры",
                    color = CreamDial
                )
                Text(
                    "Ближайшая доступная выдержка + компенсация диафрагмой",
                    color = CreamDial.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            FilterChip(
                selected = state.snapToCamera,
                onClick = vm::toggleSnapToCamera,
                label = { Text(if (state.snapToCamera) "ВКЛ" else "ВЫКЛ") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrassAccent,
                    selectedLabelColor = LeatherDark,
                    containerColor = LeatherBrown,
                    labelColor = CreamDial
                )
            )
        }
    }
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGithub: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LeatherDark,
        titleContentColor = BrassAccent,
        textContentColor = CreamDial,
        title = { Text("СПРАВКА") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                HelpSection("Что такое EV") {
                    Text(
                        "EV (Exposure Value) — число, которое одновременно описывает яркость сцены и настройки камеры. +1 EV = вдвое больше света, −1 EV = вдвое меньше. Одна единица = один стоп экспозиции.",
                        color = CreamDial
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Формула: EV = log₂(N² / t), где N — диафрагма, t — выдержка (сек). Всегда указывается для ISO 100 (EV₁₀₀).",
                        color = CreamDial.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(14.dp))

                HelpSection("Таблица EV для типичных сцен") {
                    EvTableRow("Звёздное небо, Млечный путь", "−4 … −2")
                    EvTableRow("Свет полной луны", "−2 … 0")
                    EvTableRow("Ночная улица, витрины", "3 … 6")
                    EvTableRow("Комната с лампой", "5 … 7")
                    EvTableRow("Получас до/после заката", "8 … 10")
                    EvTableRow("Яркий закат / облачно вне дома", "10 … 12")
                    EvTableRow("Пасмурный день", "12 … 13")
                    EvTableRow("Лёгкая дымка, солнце в облаках", "13 … 14")
                    EvTableRow("Солнечный день (sunny 16)", "15")
                    EvTableRow("Снег / пляж на солнце", "16")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Поправка ISO: +log₂(ISO/100). ISO 200 → +1, ISO 400 → +2, ISO 800 → +3.",
                        color = CreamDial.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(14.dp))

                HelpSection("Как пользоваться") {
                    HelpStep("1.", "Выберите плёнку, камеру и объектив — приложение ограничит расчёт их реальными выдержками и диафрагмами.")
                    HelpStep("2.", "Наведите телефон на сцену или серую карту. В левом углу превью появится EV₁₀₀.")
                    HelpStep("3.", "Выберите режим замера: Точка (по центру), Центр (с весами) или Матрица (весь кадр).")
                    HelpStep("4.", "Замок рядом с EV фиксирует замер — можно перенаправить телефон, не потеряв значение.")
                    HelpStep("5.", "В блоке «Варианты съемки» показываются 3–4 реальные пары выдержка×диафрагма с одинаковой экспозицией. Выберите по ситуации.")
                    HelpStep("6.", "Метка «точно» — пара попадает в EV без ошибки. Если написано «+0.33 EV» — снимок будет на 1/3 стопа светлее.")
                }

                Spacer(Modifier.height(14.dp))

                HelpSection("Полезные хитрости") {
                    Text(
                        "• Portra и другие негативные плёнки любят +0.5…1 EV передержки.\n" +
                        "• Слайды (Velvia, Ektachrome) — наоборот, −0.3 EV даёт более насыщенные цвета.\n" +
                        "• В тени при ярком солнце — на 2–3 EV темнее, чем на свету.\n" +
                        "• Снег и белые сцены — требуют +1 EV, иначе будут серыми.",
                        color = CreamDial.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(14.dp))

                HelpSection("GitHub и открытый код") {
                    OutlinedButton(
                        onClick = onOpenGithub,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrassAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("github.com/Dnlln/FilmLightMeter") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrassAccent,
                    contentColor = LeatherDark
                )
            ) { Text("Настройки") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CreamDial)
            ) { Text("Закрыть") }
        }
    )
}

@Composable
private fun HelpSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        color = BrassAccent,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(6.dp))
    content()
}

@Composable
private fun EvTableRow(scene: String, ev: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            scene,
            color = CreamDial,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            ev,
            color = BrassAccent,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HelpStep(num: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            num,
            color = BrassAccent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(22.dp)
        )
        Text(
            text,
            color = CreamDial,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StopAdjuster(
    label: String,
    value: Double,
    onChange: (Double) -> Unit,
    positiveOnly: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, color = CreamDial, modifier = Modifier.weight(1f))
        val step = 1.0 / 3.0
        OutlinedButton(
            onClick = {
                val newVal = value - step
                if (!positiveOnly || newVal >= 0.0) onChange(newVal)
            },
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) { Text("−") }
        Spacer(Modifier.width(10.dp))
        Text(
            String.format(java.util.Locale.US, "%+.2f EV", value),
            color = CreamDial,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )
        Spacer(Modifier.width(10.dp))
        OutlinedButton(
            onClick = { onChange(value + step) },
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) { Text("+") }
    }
}
