package com.tpms.monitor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * BLE 扫描器
 * 负责扫描附近的 BLE 设备
 */
class BleScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private val scanChannel = Channel<BleDevice>(Channel.BUFFERED)

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let { device ->
                val bleDevice = BleDevice(
                    address = device.address,
                    name = device.name ?: result.scanRecord?.deviceName,
                    rssi = result.rssi,
                    device = device
                )
                scanChannel.trySend(bleDevice)
                Log.d(TAG, "Scanned device: ${bleDevice.name} (${bleDevice.address}), RSSI: ${bleDevice.rssi}")
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            Log.d(TAG, "Batch scan results: ${results.size}")
            results.forEach { result ->
                onScanResult(0, result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
        }
    }

    /**
     * 开始扫描
     *
     * @return Flow<BleDevice> 扫描到的设备流
     */
    @SuppressLint("MissingPermission")
    fun startScan(): Flow<BleDevice> {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
            ?: throw IllegalStateException("Bluetooth adapter not available")

        val filters = listOf<ScanFilter>(buildScanFilter())
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)  // 支持 BLE 5.2
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        scanner.startScan(filters, settings, scanCallback)
        Log.i(TAG, "BLE scan started")

        return scanChannel.receiveAsFlow()
    }

    /**
     * 构建扫描过滤器
     *
     * 注意：当前配置为扫描所有 BLE 设备。
     * 如果知道 SMP290 的特定广播数据（如 Service UUID、设备名称前缀等），
     * 可以添加更精确的过滤条件以提高扫描效率。
     */
    private fun buildScanFilter(): ScanFilter {
        return ScanFilter.Builder().build()
    }

    /**
     * 停止扫描
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanChannel.close()
        Log.i(TAG, "BLE scan stopped")
    }

    /**
     * 检查蓝牙是否可用
     */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    companion object {
        private const val TAG = "BleScanner"
    }
}
