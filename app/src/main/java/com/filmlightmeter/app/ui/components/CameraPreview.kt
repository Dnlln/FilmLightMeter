package com.filmlightmeter.app.ui.components

import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.filmlightmeter.app.camera.CameraExposureProbe
import com.filmlightmeter.app.camera.CameraReading
import com.filmlightmeter.app.camera.LuminanceAnalyzer
import com.filmlightmeter.app.camera.MeteringMode
import com.filmlightmeter.app.camera.RegionFractions
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraPreview(
    meteringMode: MeteringMode,
    onReading: (reading: CameraReading, aperture: Double, shutter: Double, iso: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    // Храним последние параметры AE из CaptureResult
    val aeState = remember {
        androidx.compose.runtime.mutableStateOf(
            CameraExposureProbe.ExposureState(1.0 / 60.0, 100, 1.8)
        )
    }

    val analyzer = remember {
        LuminanceAnalyzer { reading ->
            val ae = aeState.value
            onReading(reading, ae.aperture, ae.exposureTimeSec, ae.iso)
        }.also { it.modeWeight = meteringMode }
    }

    LaunchedEffect(meteringMode) {
        analyzer.modeWeight = meteringMode
        analyzer.meteringRegion = when (meteringMode) {
            MeteringMode.SPOT -> RegionFractions(0.4f, 0.4f, 0.6f, 0.6f)
            MeteringMode.CENTER_WEIGHTED -> RegionFractions(0f, 0f, 1f, 1f)
            MeteringMode.MATRIX -> RegionFractions(0f, 0f, 1f, 1f)
        }
    }

    DisposableEffect(Unit) {
        val executor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysisBuilder = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

            // Camera2 interop — читаем параметры AE через CaptureCallback
            val c2Ext = Camera2Interop.Extender(analysisBuilder)
            c2Ext.setSessionCaptureCallback(object :
                android.hardware.camera2.CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: android.hardware.camera2.CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    CameraExposureProbe.readFromCaptureResult(result)?.let {
                        aeState.value = it
                    }
                }
            })

            val analysis = analysisBuilder.build()
            analysis.setAnalyzer(executor, analyzer)

            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (_: Exception) { }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            executor.shutdown()
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        // Оверлей — рамка зоны замера
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (meteringMode) {
                MeteringMode.SPOT -> {
                    val cx = w / 2f; val cy = h / 2f
                    drawCircle(
                        color = Color(0xFFE8DFC4),
                        radius = kotlin.math.min(w, h) * 0.06f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }
                MeteringMode.CENTER_WEIGHTED -> {
                    drawCircle(
                        color = Color(0xFFE8DFC4).copy(alpha = 0.8f),
                        radius = kotlin.math.min(w, h) * 0.28f,
                        center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f),
                        style = Stroke(width = 2f)
                    )
                }
                MeteringMode.MATRIX -> {
                    val n = 3
                    val cellW = w / n
                    val cellH = h / n
                    for (i in 0 until n) for (j in 0 until n) {
                        drawRect(
                            color = Color(0xFFE8DFC4).copy(alpha = 0.35f),
                            topLeft = androidx.compose.ui.geometry.Offset(
                                i * cellW + 4f, j * cellH + 4f
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                cellW - 8f, cellH - 8f
                            ),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
            }
        }
    }
}
