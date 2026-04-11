package com.tpms.monitor.ble

import android.bluetooth.BluetoothDevice
import com.tpms.monitor.data.TirePressureData

/**
 * BLE 设备数据类
 * 封装扫描到的蓝牙设备信息
 */
data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val device: BluetoothDevice,
    /**
     * 从广播帧解析的 TPMS 数据
     * 如果设备已绑定并且广播中包含有效数据，此字段将被填充
     */
    val parsedData: TirePressureData? = null
) {
    /**
     * 信号强度等级
     * - 强：> -50 dBm
     * - 中：-50 到 -70 dBm
     * - 弱：< -70 dBm
     */
    val signalStrength: SignalStrength
        get() = when {
            rssi > -50 -> SignalStrength.STRONG
            rssi > -70 -> SignalStrength.MEDIUM
            else -> SignalStrength.WEAK
        }

    /**
     * 是否有有效的 TPMS 数据
     */
    val hasParsedData: Boolean
        get() = parsedData != null && parsedData.isValid
}

/**
 * 信号强度枚举
 */
enum class SignalStrength {
    STRONG,   // 强 > -50 dBm
    MEDIUM,   // 中 -50 到 -70 dBm
    WEAK      // 弱 < -70 dBm
}
