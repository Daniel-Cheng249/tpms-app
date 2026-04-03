package com.tpms.monitor.data

import com.tpms.monitor.ble.BleDevice

/**
 * 轮胎 - 设备映射数据类
 * 用于管理轮胎位置与 BLE 设备的绑定关系
 */
data class TireDeviceMapping(
    val mapping: Map<TirePosition, String> = emptyMap()
) {
    /**
     * 获取指定轮胎的设备地址
     */
    fun getDeviceAddress(position: TirePosition): String? = mapping[position]

    /**
     * 获取指定设备地址对应的轮胎位置
     */
    fun getTirePosition(deviceAddress: String): TirePosition? {
        return mapping.entries.find { it.value == deviceAddress }?.key
    }

    /**
     * 更新映射
     */
    fun updateMapping(newMapping: Map<TirePosition, String>): TireDeviceMapping {
        return TireDeviceMapping(newMapping)
    }
}
