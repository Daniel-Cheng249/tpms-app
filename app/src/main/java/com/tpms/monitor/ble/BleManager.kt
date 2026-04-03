package com.tpms.monitor.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import com.tpms.monitor.data.TirePressureData
import com.tpms.monitor.data.TirePosition
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * BLE 管理器
 * 负责连接设备、订阅数据、解析数据包
 */
class BleManager(private val context: Context) {

    private var bluetoothGatt: BluetoothGatt? = null
    private val connectionStateChannel = Channel<BleConnectionState>(Channel.BUFFERED)
    private val dataChannel = Channel<List<TirePressureData>>(Channel.BUFFERED)

    private var dataCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server, status: $status")
                    connectionStateChannel.trySend(BleConnectionState.CONNECTED)
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server, status: $status")
                    connectionStateChannel.trySend(BleConnectionState.DISCONNECTED)
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered")
                setupCharacteristics(gatt)
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
            }
        }

        // Android 13+ (API 33) 新回调
        @SuppressLint("MissingPermission")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                parseTirePressureData(value)?.let { data ->
                    dataChannel.trySend(data)
                }
            }
        }

        // Android 12 及以下兼容回调 (已废弃)
        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                characteristic.value?.let { value ->
                    parseTirePressureData(value)?.let { data ->
                        dataChannel.trySend(data)
                    }
                }
            }
        }

        // Android 13+ (API 33) 新回调
        @SuppressLint("MissingPermission")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            parseTirePressureData(value)?.let { data ->
                dataChannel.trySend(data)
            }
        }

        // Android 12 及以下兼容回调 (已废弃)
        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            characteristic.value?.let { value ->
                parseTirePressureData(value)?.let { data ->
                    dataChannel.trySend(data)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Descriptor write successful")
            } else {
                Log.e(TAG, "Descriptor write failed with status: $status")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            Log.d(TAG, "Remote RSSI: $rssi, status: $status")
        }
    }

    /**
     * 获取连接状态流
     */
    fun getConnectionStateFlow(): Flow<BleConnectionState> {
        return connectionStateChannel.receiveAsFlow()
    }

    /**
     * 获取数据流
     */
    fun getDataFlow(): Flow<List<TirePressureData>> {
        return dataChannel.receiveAsFlow()
    }

    /**
     * 连接到 BLE 设备
     */
    @SuppressLint("MissingPermission")
    fun connect(device: BleDevice): Boolean {
        if (bluetoothGatt != null) {
            Log.w(TAG, "Already connected to a device")
            return false
        }

        Log.i(TAG, "Connecting to device: ${device.address}")
        connectionStateChannel.trySend(BleConnectionState.CONNECTING)

        bluetoothGatt = device.device.connectGatt(context, false, gattCallback)
        return true
    }

    /**
     * 断开连接
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        Log.i(TAG, "Disconnecting...")
        connectionStateChannel.trySend(BleConnectionState.DISCONNECTING)

        bluetoothGatt?.let {
            it.disconnect()
            it.close()
        }
        bluetoothGatt = null
        dataCharacteristic = null
    }

    /**
     * 设置特征和启用通知
     */
    @SuppressLint("MissingPermission")
    private fun setupCharacteristics(gatt: BluetoothGatt?) {
        val service = gatt?.getService(BleConstants.TPMS_SERVICE_UUID)

        if (service == null) {
            Log.e(TAG, "TPMS service not found!")
            Log.w(TAG, "Available services:")
            gatt?.services?.forEach { s ->
                Log.w(TAG, "  Service: ${s.uuid}")
                s.characteristics.forEach { c ->
                    Log.w(TAG, "    Characteristic: ${c.uuid}")
                }
            }
            return
        }

        dataCharacteristic = service.getCharacteristic(BleConstants.DATA_CHARACTERISTIC_UUID)

        dataCharacteristic?.let { characteristic ->
            // 设置通知
            gatt.setCharacteristicNotification(characteristic, true)

            // 写入 CCCD 启用通知（兼容 Android 13+ 和旧版本）
            characteristic.descriptors.find { it.uuid == BleConstants.CCCD_UUID }?.let { descriptor ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BleConstants.NOTIFICATION_ENABLE_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BleConstants.NOTIFICATION_ENABLE_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            }

            // 或者尝试读取初始值
            gatt.readCharacteristic(characteristic)

            Log.i(TAG, "Data characteristic setup complete")
        } ?: run {
            Log.e(TAG, "Data characteristic not found!")
        }
    }

    /**
     * 解析胎压数据
     *
     * TODO: 这个方法需要根据 SMP290 的实际数据格式修改
     * 在修改前，先用 nRF Connect 抓取实际数据包，分析字节布局
     */
    private fun parseTirePressureData(data: ByteArray): List<TirePressureData>? {
        Log.d(TAG, "Received data: ${data.joinToString(" ") { String.format("%02X", it) }}")

        // 假设数据格式（需要替换为实际格式）:
        // Byte 0-1: 传感器 ID
        // Byte 2: 压力高字节
        // Byte 3: 压力低字节
        // Byte 4: 温度
        // Byte 5: 电量

        if (data.size < 6) {
            Log.w(TAG, "Data too short: ${data.size} bytes")
            return null
        }

        return try {
            val pressures = mutableListOf<TirePressureData>()
            val positions = TirePosition.getAll()

            // 假设每 6 字节一个传感器的数据
            for (i in 0 until 4) {
                val offset = i * 6
                if (offset + 5 >= data.size) break

                val pressureValue = ((data[offset + 2].toInt() and 0xFF) shl 8) or
                                    (data[offset + 3].toInt() and 0xFF)
                val pressure = pressureValue / 100f  // 转换为 bar

                val temperature = data[offset + 4].toInt() and 0xFF
                val battery = data[offset + 5].toInt() and 0xFF

                pressures.add(
                    TirePressureData(
                        position = positions[i],
                        pressure = pressure,
                        temperature = temperature.toFloat(),
                        batteryLevel = battery,
                        isValid = true
                    )
                )
            }

            pressures.ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse data", e)
            null
        }
    }

    companion object {
        private const val TAG = "BleManager"
    }
}
