package com.tpms.monitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.tpms.monitor.ui.components.DashboardScreen
import com.tpms.monitor.ui.components.DeviceMappingScreen
import com.tpms.monitor.ui.theme.TPMSMonitorTheme
import com.tpms.monitor.ui.viewmodel.TirePressureViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // 权限已授予，更新 UI 状态
            viewModel.checkBluetoothState()
        }
    }

    private lateinit var viewModel: TirePressureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启用 Compose 性能优化
        window.setDecorFitsSystemWindows(false)

        viewModel = ViewModelProvider(this)[TirePressureViewModel::class.java]

        // 请求权限
        requestBluetoothPermissions()

        setContent {
            TPMSMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    @Composable
    private fun AppNavigation(viewModel: TirePressureViewModel) {
        var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

        when (currentScreen) {
            Screen.Dashboard -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToMapping = { currentScreen = Screen.DeviceMapping }
                )
            }
            Screen.DeviceMapping -> {
                val uiState by viewModel.uiState.collectAsState()
                val scannedDevices by viewModel.scannedDevices.collectAsState()
                val tireMapping by viewModel.tireMapping.collectAsState()

                DeviceMappingScreen(
                    scannedDevices = scannedDevices,
                    tireMapping = tireMapping,
                    isScanning = uiState is com.tpms.monitor.data.UiState.Scanning,
                    onStartScan = { viewModel.startScan() },
                    onStopScan = { viewModel.stopScan() },
                    onBindDevice = { position, device ->
                        viewModel.bindDeviceToPosition(position, device)
                    },
                    onUnbindDevice = { position ->
                        viewModel.unbindPosition(position)
                    },
                    onBack = { currentScreen = Screen.Dashboard }
                )
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needRequest) {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private enum class Screen {
        Dashboard,
        DeviceMapping
    }
}
