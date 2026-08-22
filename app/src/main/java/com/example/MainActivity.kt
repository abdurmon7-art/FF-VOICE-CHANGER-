package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.MainScreen
import com.example.ui.VoiceChangerViewModel
import com.example.ui.theme.VoiceChangerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VoiceChangerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            var hasMicPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                hasMicPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
            }

            fun requestRequiredPermissions() {
                val permissionsToRequest = mutableListOf<String>()
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }
            }

            VoiceChangerTheme(darkTheme = isDarkMode) {
                MainScreen(
                    viewModel = viewModel,
                    onRequestMicPermission = { requestRequiredPermissions() },
                    hasMicPermission = hasMicPermission,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
