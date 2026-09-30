package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.MusicViewModel
import com.example.ui.theme.AvaMusicTheme

class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            musicViewModel.performStartupScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            android.util.Log.w("MainActivity", "Failed enableEdgeToEdge: ${t.message}")
        }

        checkAndRequestPermissions()

        setContent {
            val settings by musicViewModel.settings.collectAsStateWithLifecycle()
            AvaMusicTheme(
                accentTheme = settings.accentTheme,
                isAmoled = settings.isAmoledDark
            ) {
                MainScreen(viewModel = musicViewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val basePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val permissions = basePermissions.toTypedArray()

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            musicViewModel.performStartupScan()
        } else {
            try {
                requestPermissionLauncher.launch(permissions)
            } catch (t: Throwable) {
                android.util.Log.w("MainActivity", "Failed permission request: ${t.message}")
            }
        }
    }
}
