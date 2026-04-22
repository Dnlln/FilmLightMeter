package com.filmlightmeter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmlightmeter.app.ui.theme.BrassAccent
import com.filmlightmeter.app.ui.theme.CreamDial
import com.filmlightmeter.app.ui.theme.LeatherBrown
import com.filmlightmeter.app.ui.theme.LeatherDark
import androidx.compose.foundation.clickable

/**
 * Горизонтальная «шкала с метками» для выбора значения из списка.
 * Выбранное значение подсвечивается и центрируется.
 */
@Composable
fun ValueStrip(
    label: String,
    values: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = BrassAccent
            )
            if (highlighted) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE0A33A))
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        val scroll = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(LeatherDark, LeatherBrown, LeatherDark)
                    )
                )
                .border(1.dp, BrassAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(scroll),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                values.forEachIndexed { i, v ->
                    val isSel = i == selectedIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSel) BrassAccent.copy(alpha = 0.9f) else Color.Transparent
                            )
                            .clickable { onSelect(i) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = v,
                            color = if (isSel) LeatherDark else CreamDial,
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
