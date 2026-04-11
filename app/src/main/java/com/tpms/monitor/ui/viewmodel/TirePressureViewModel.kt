package com.tpms.monitor.ui.viewmodel

import android.app.Application
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tpms.monitor.ble.BleConnectionState
import com.tpms.monitor.ble.BleDevice
import com.tpms.monitor.ble.BleManager
import com.tpms.monitor.ble.BleScanner
import com.tpms.monitor.data.MappingPreferences
import com.tpms.monitor.data.TireDeviceMapping
import com.tpms.monitor.data.TirePosition
import com.tpms.monitor.data.TirePressureData
import com.tpms.monitor.data.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 胎压监测 ViewModel
 * 管理 UI 状态和 BLE 业务逻辑
 */
class TirePressureViewModel(application: Application) : AndroidViewModel(application) {

    private val bleScanner = BleScanner(application)
    private val bleManager = BleManager(application)
    private val mappingPreferences = MappingPreferences(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _tirePressures = MutableStateFlow(
        TirePosition.getAll().associate { position ->
            position.tireName to TirePressureData(position, 0f, 0f, 0, isValid = false)
        }.toMap()
    )
    val tirePressures: StateFlow<Map<String, TirePressureData>> = _tirePressures.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    // 从 DataStore 加载映射关系
    val tireMapping: StateFlow<TireDeviceMapping> = mappingPreferences.tireMappingFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, TireDeviceMapping())

    // 已绑定的设备地址集合
    val boundAddresses: StateFlow<Set<String>> = mappingPreferences.getBoundAddressesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        checkBluetoothState()
        observeConnectionState()
        observeDataFlow()
    }

    /**
     * 检查蓝牙状态
     */
    fun checkBluetoothState() {
        val context = getApplication<Application>()
        _isBluetoothEnabled.value = hasBluetoothPermissions(context) &&
                                       bleScanner.isBluetoothEnabled()
    }

    private fun hasBluetoothPermissions(context: android.content.Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val connectPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            scanPermission && connectPermission
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 开始扫描
     */
    fun startScan() {
        if (_uiState.value !is UiState.Idle && _uiState.value !is UiState.Error) {
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Scanning
            _scannedDevices.value = emptyList()

            try {
                // 使用轮胎映射来解析广播数据
                bleScanner.startScanForTpmsData(tireMapping.value).collect { device ->
                    val currentList = _scannedDevices.value
                    val existingIndex = currentList.indexOfFirst { it.address == device.address }

                    // 更新或添加设备
                    _scannedDevices.value = if (existingIndex >= 0) {
                        currentList.toMutableList().apply { set(existingIndex, device) }
                    } else {
                        currentList + device
                    }

                    // 如果解析到了 TPMS 数据，更新轮胎压力显示
                    device.parsedData?.let { parsedData ->
                        updateTirePressure(parsedData)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("扫描失败：${e.message}")
            }
        }
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        bleScanner.stopScan()
        if (_uiState.value is UiState.Scanning) {
            _uiState.value = UiState.Idle
        }
    }

    /**
     * 连接设备
     */
    fun connectToDevice(device: BleDevice) {
        viewModelScope.launch {
            _uiState.value = UiState.Connecting
            bleManager.connect(device)
        }
    }

    /**
     * 连接所有绑定的设备
     */
    fun connectAllBoundDevices() {
        val mapping = tireMapping.value
        val boundDevices = _scannedDevices.value.filter { device ->
            mapping.getTirePosition(device.address) != null
        }

        if (boundDevices.isEmpty()) {
            _uiState.value = UiState.Error("未找到已绑定的设备，请先扫描并绑定传感器")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Connecting
            // 先连接第一个设备
            bleManager.connect(boundDevices.first())
            // TODO: 支持多设备同时连接
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        bleManager.disconnect()
        _uiState.value = UiState.Idle
    }

    /**
     * 绑定设备到轮胎位置
     */
    fun bindDeviceToPosition(position: TirePosition, device: BleDevice) {
        viewModelScope.launch {
            // 如果该位置已有绑定，先解绑
            tireMapping.value.getDeviceAddress(position)?.let { oldAddress ->
                if (oldAddress != device.address) {
                    mappingPreferences.removeMapping(position)
                }
            }
            // 保存新绑定
            mappingPreferences.saveMapping(position, device.address)
        }
    }

    /**
     * 解绑轮胎位置的设备
     */
    fun unbindPosition(position: TirePosition) {
        viewModelScope.launch {
            mappingPreferences.removeMapping(position)
        }
    }

    /**
     * 清除所有绑定
     */
    fun clearAllBindings() {
        viewModelScope.launch {
            mappingPreferences.clearAllMappings()
        }
    }

    /**
     * 获取指定位置的已绑定设备
     */
    fun getBoundDeviceForPosition(position: TirePosition): BleDevice? {
        val address = tireMapping.value.getDeviceAddress(position) ?: return null
        return _scannedDevices.value.find { it.address == address }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bleManager.getConnectionStateFlow().collect { state ->
                when (state) {
                    BleConnectionState.CONNECTED -> {
                        _uiState.update { current ->
                            if (current is UiState.Connecting) {
                                UiState.PartiallyConnected(1, 4)
                            } else {
                                current
                            }
                        }
                    }
                    BleConnectionState.DISCONNECTED -> {
                        _uiState.value = UiState.Idle
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeDataFlow() {
        viewModelScope.launch {
            bleManager.getDataFlow().collect { pressures ->
                updateTirePressures(pressures)
            }
        }
    }

    private fun updateTirePressures(pressures: List<TirePressureData>) {
        val currentMap = _tirePressures.value.toMutableMap()
        pressures.forEach { pressure ->
            currentMap[pressure.position.tireName] = pressure
        }
        _tirePressures.value = currentMap
    }

    /**
     * 更新单个轮胎压力数据
     */
    private fun updateTirePressure(pressure: TirePressureData) {
        val currentMap = _tirePressures.value.toMutableMap()
        currentMap[pressure.position.tireName] = pressure
        _tirePressures.value = currentMap
        Log.d("TirePressureViewModel", "Updated ${pressure.position.displayName}: ${pressure.pressure} bar, ${pressure.temperature}°C")
    }

    /**
     * 请求蓝牙权限
     */
    fun requestBluetoothPermissions() {
        checkBluetoothState()
    }

    override fun onCleared() {
        super.onCleared()
        bleScanner.stopScan()
        bleManager.disconnect()
    }
}
