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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            IconButton(onClick = { settingsOpen = !settingsOpen }) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "Настройки",
                    tint = BrassAccent
                )
            }
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

        // --- Режимы замера ---
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        }

        Spacer(Modifier.height(14.dp))

        // --- Экспопара: крупным шрифтом ---
        val shutterText = ExposureMath.formatShutter(
            when (state.priority) {
                PriorityMode.APERTURE -> state.computedShutter
                PriorityMode.SHUTTER -> state.shutterSeconds
            }
        )
        val apertureText = ExposureMath.formatAperture(
            when (state.priority) {
                PriorityMode.APERTURE -> state.aperture
                PriorityMode.SHUTTER -> state.computedAperture
            }
        )

        ExposurePairCard(shutterText, apertureText, state.priority, vm::setPriority)

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
            ValueStrip(
                label = "ВЫДЕРЖКА",
                values = ExposureMath.STANDARD_SHUTTERS.map(ExposureMath::formatShutter),
                selectedIndex = ExposureMath.STANDARD_SHUTTERS.indexOfFirst {
                    kotlin.math.abs(it - state.shutterSeconds) < 1e-5
                }.coerceAtLeast(0),
                onSelect = { vm.setShutter(ExposureMath.STANDARD_SHUTTERS[it]) },
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

        Spacer(Modifier.height(12.dp))

        if (settingsOpen) {
            SettingsPanel(vm)
        }

        Spacer(Modifier.height(20.dp))
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
