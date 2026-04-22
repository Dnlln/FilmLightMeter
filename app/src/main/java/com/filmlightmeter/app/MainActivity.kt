package com.filmlightmeter.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.filmlightmeter.app.ui.MeterViewModel
import com.filmlightmeter.app.ui.screens.MeterScreen
import com.filmlightmeter.app.ui.theme.FilmLightMeterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MeterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FilmLightMeterTheme {
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted -> hasPermission = granted }

                Surface(modifier = Modifier.fillMaxSize()) {
                    MeterScreen(
                        vm = viewModel,
                        hasCameraPermission = hasPermission,
                        onRequestPermission = {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun rememberLauncherForActivityResult(
    contract: androidx.activity.result.contract.ActivityResultContract<String, Boolean>,
    onResult: (Boolean) -> Unit
) = androidx.activity.compose.rememberLauncherForActivityResult(contract, onResult)
