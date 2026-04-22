package com.filmlightmeter.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmlightmeter.app.ui.theme.BrassAccent
import com.filmlightmeter.app.ui.theme.CreamDial
import com.filmlightmeter.app.ui.theme.CreamSoft
import com.filmlightmeter.app.ui.theme.LeatherBrown
import com.filmlightmeter.app.ui.theme.NeedleRed
import com.filmlightmeter.app.ui.theme.ShadowBlack
import kotlin.math.cos
import kotlin.math.sin

/**
 * Аналоговый циферблат экспонометра в стиле Sekonic L-398.
 * Шкала EV от -2 до 18 (размах ≈ 240°).
 */
@Composable
fun AnalogEvDial(
    ev: Float,
    modifier: Modifier = Modifier,
    minEv: Float = -2f,
    maxEv: Float = 18f,
    sweepDegrees: Float = 240f
) {
    val animated by animateFloatAsState(
        targetValue = ev.coerceIn(minEv, maxEv),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "evNeedle"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CreamDial, CreamSoft),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
            drawCircle(
                color = BrassAccent,
                radius = r,
                center = center,
                style = Stroke(width = r * 0.04f)
            )
            drawCircle(
                color = ShadowBlack.copy(alpha = 0.4f),
                radius = r * 0.97f,
                center = center,
                style = Stroke(width = r * 0.01f)
            )

            val startAngle = 270f - sweepDegrees / 2f
            val ticks = (maxEv - minEv).toInt()
            for (i in 0..ticks) {
                val isMajor = i % 2 == 0
                val angleDeg = startAngle + (i.toFloat() / ticks) * sweepDegrees
                val rad = Math.toRadians(angleDeg.toDouble())
                val outer = r * 0.90f
                val inner = if (isMajor) r * 0.76f else r * 0.83f
                val p1 = Offset(
                    center.x + (cos(rad) * outer).toFloat(),
                    center.y + (sin(rad) * outer).toFloat()
                )
                val p2 = Offset(
                    center.x + (cos(rad) * inner).toFloat(),
                    center.y + (sin(rad) * inner).toFloat()
                )
                drawLine(
                    color = ShadowBlack,
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) r * 0.014f else r * 0.007f,
                    cap = StrokeCap.Round
                )

                if (isMajor) {
                    val labelR = r * 0.66f
                    val lp = Offset(
                        center.x + (cos(rad) * labelR).toFloat(),
                        center.y + (sin(rad) * labelR).toFloat()
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = r * 0.09f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.SERIF,
                                android.graphics.Typeface.BOLD
                            )
                        }
                        drawText(
                            "${(minEv + i).toInt()}",
                            lp.x,
                            lp.y + paint.textSize / 3f,
                            paint
                        )
                    }
                }
            }

            // Красная дуга «рабочего диапазона» (EV 8..16)
            val arcR = r * 0.88f
            val arcStart = startAngle + ((8f - minEv) / (maxEv - minEv)) * sweepDegrees
            val arcSweep = ((16f - 8f) / (maxEv - minEv)) * sweepDegrees
            drawArc(
                color = NeedleRed.copy(alpha = 0.25f),
                startAngle = arcStart,
                sweepAngle = arcSweep,
                useCenter = false,
                topLeft = Offset(center.x - arcR, center.y - arcR),
                size = androidx.compose.ui.geometry.Size(arcR * 2, arcR * 2),
                style = Stroke(width = r * 0.08f, cap = StrokeCap.Butt)
            )

            // Стрелка
            val needleAngle = startAngle +
                ((animated - minEv) / (maxEv - minEv)) * sweepDegrees
            rotate(degrees = needleAngle - 270f, pivot = center) {
                val pivot = center
                val tip = Offset(pivot.x, pivot.y - r * 0.80f)
                val baseL = Offset(pivot.x - r * 0.025f, pivot.y + r * 0.15f)
                val baseR = Offset(pivot.x + r * 0.025f, pivot.y + r * 0.15f)
                val path = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(baseL.x, baseL.y)
                    lineTo(baseR.x, baseR.y)
                    close()
                }
                drawPath(path, color = NeedleRed)
                drawCircle(
                    color = LeatherBrown,
                    radius = r * 0.06f,
                    center = pivot
                )
                drawCircle(
                    color = BrassAccent,
                    radius = r * 0.06f,
                    center = pivot,
                    style = Stroke(width = r * 0.012f)
                )
            }
        }

        Text(
            text = "EV",
            color = Color(0xFF4A3A22),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 110.dp)
        )
    }
}
