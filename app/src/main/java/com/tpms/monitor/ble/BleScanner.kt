package com.tpms.monitor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.tpms.monitor.data.TireDeviceMapping
import com.tpms.monitor.data.TirePosition
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * BLE 扫描器
 * 负责扫描附近的 BLE 设备，并解析广播帧中的 TPMS 数据
 */
class BleScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private val scanChannel = Channel<BleDevice>(Channel.BUFFERED)

    // 当前轮胎设备映射关系
    private var tireMapping: TireDeviceMapping = TireDeviceMapping()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let { device ->
                val address = device.address
                val rssi = result.rssi
                val scanRecord = result.scanRecord

                // 获取设备对应的轮胎位置
                val position = tireMapping.getTirePosition(address)

                // 尝试从广播帧解析 TPMS 数据
                val parsedData = if (position != null) {
                    TpmsBroadcastParser.parseFromScanRecord(
                        scanRecord = scanRecord,
                        deviceAddress = address,
                        rssi = rssi,
                        position = position
                    )
                } else {
                    null
                }

                val bleDevice = BleDevice(
                    address = address,
                    name = device.name ?: scanRecord?.deviceName,
                    rssi = rssi,
                    device = device,
                    parsedData = parsedData
                )

                scanChannel.trySend(bleDevice)

                if (parsedData != null) {
                    Log.d(TAG, "Scanned device: ${bleDevice.name} ($address), RSSI: $rssi, " +
                            "Pressure: ${parsedData.pressure} bar, Temp: ${parsedData.temperature}°C")
                } else {
                    Log.d(TAG, "Scanned device: ${bleDevice.name} ($address), RSSI: $rssi")
                }
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
     * 设置轮胎设备映射
     * 用于在扫描时将 MAC 地址映射到轮胎位置，从而解析广播数据
     *
     * @param mapping 轮胎设备映射
     */
    fun setTireMapping(mapping: TireDeviceMapping) {
        this.tireMapping = mapping
        Log.d(TAG, "Tire mapping updated: ${mapping.mapping}")
    }

    /**
     * 开始扫描
     *
     * @param filterByManufacturer 是否只扫描包含制造商特定数据的设备（type 0xFF）
     * @return Flow<BleDevice> 扫描到的设备流
     */
    @SuppressLint("MissingPermission")
    fun startScan(filterByManufacturer: Boolean = false): Flow<BleDevice> {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
            ?: throw IllegalStateException("Bluetooth adapter not available")

        val filters = if (filterByManufacturer) {
            // 如果要筛选特定制造商数据，可以在这里添加
            buildScanFilters()
        } else {
            emptyList()
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)  // 支持 BLE 5.2
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0)  // 实时报告结果
            .build()

        scanner.startScan(filters, settings, scanCallback)
        Log.i(TAG, "BLE scan started (filterByManufacturer=$filterByManufacturer)")

        return scanChannel.receiveAsFlow()
    }

    /**
     * 开始扫描并解析已绑定设备的 TPMS 数据
     * 这是用于实时监测的模式
     *
     * @param mapping 轮胎设备映射
     * @return Flow<BleDevice> 扫描到的设备流
     */
    fun startScanForTpmsData(mapping: TireDeviceMapping): Flow<BleDevice> {
        setTireMapping(mapping)
        return startScan(filterByManufacturer = false)
    }

    /**
     * 构建扫描过滤器
     * 可以添加针对 TPMS 设备的特定过滤条件
     */
    private fun buildScanFilters(): List<ScanFilter> {
        val filters = mutableListOf<ScanFilter>()

        // 如果有已知的 TPMS Service UUID，可以添加服务过滤器
        // val serviceUuid = ParcelUuid.fromString("0000xxxx-0000-1000-8000-00805f9b34fb")
        // filters.add(ScanFilter.Builder().setServiceUuid(serviceUuid).build())

        // 如果过滤器为空，则扫描所有设备
        return filters
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
