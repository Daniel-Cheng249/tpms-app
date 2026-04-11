package com.tpms.monitor.ble

import android.bluetooth.le.ScanRecord
import android.util.Log
import com.tpms.monitor.data.TirePressureData
import com.tpms.monitor.data.TirePosition

/**
 * TPMS 广播数据解析器
 * 解析 BLE 广播帧中的 Manufacturer Specific Data (type 0xFF)
 */
object TpmsBroadcastParser {

    private const val TAG = "TpmsBroadcastParser"

    // 假设的制造商 ID (需要根据实际传感器修改)
    // 例如：0x004C 是 Apple, 0x0006 是 Microsoft
    // SMP290 的实际制造商 ID 需要通过抓包获取
    private val MANUFACTURER_IDS = listOf(0xFF, 0x00FF) // 占位符，需要替换为实际值

    /**
     * 从 ScanRecord 解析 TPMS 数据
     *
     * @param scanRecord 扫描记录
     * @param deviceAddress 设备 MAC 地址
     * @param rssi 信号强度
     * @param position 轮胎位置
     * @return 解析后的胎压数据，如果解析失败返回 null
     */
    fun parseFromScanRecord(
        scanRecord: ScanRecord?,
        deviceAddress: String,
        rssi: Int,
        position: TirePosition
    ): TirePressureData? {
        if (scanRecord == null) {
            return null
        }

        // 获取 Manufacturer Specific Data (type 0xFF)
        val manufacturerData = scanRecord.manufacturerSpecificData
        if (manufacturerData == null || manufacturerData.size() == 0) {
            return null
        }

        // 遍历所有制造商数据
        for (i in 0 until manufacturerData.size()) {
            val manufacturerId = manufacturerData.keyAt(i)
            val data = manufacturerData.valueAt(i)

            Log.d(TAG, "Manufacturer ID: 0x${manufacturerId.toString(16)}, Data: ${data?.toHexString()}")

            // 尝试解析数据
            val parsedData = parseManufacturerData(data, manufacturerId, deviceAddress, rssi, position)
            if (parsedData != null) {
                return parsedData
            }
        }

        return null
    }

    /**
     * 解析制造商特定数据
     *
     * 数据格式示例（需要根据实际 SMP290 数据格式修改）：
     * Byte 0-1: 制造商 ID (little endian)
     * Byte 2:   协议版本/数据类型
     * Byte 3-4: 压力值 (单位: 0.01 bar, unsigned short, little endian)
     * Byte 5:   温度值 (单位: °C, signed byte, 偏移 -40)
     * Byte 6:   电量百分比 (0-100)
     * Byte 7:   状态标志位
     *
     * @param data 原始数据字节数组
     * @param manufacturerId 制造商 ID
     * @param deviceAddress 设备地址
     * @param rssi 信号强度
     * @param position 轮胎位置
     * @return 解析后的数据
     */
    private fun parseManufacturerData(
        data: ByteArray?,
        manufacturerId: Int,
        deviceAddress: String,
        rssi: Int,
        position: TirePosition
    ): TirePressureData? {
        if (data == null || data.size < 8) {
            Log.w(TAG, "Data too short: ${data?.size ?: 0} bytes")
            return null
        }

        return try {
            // TODO: 这里的数据解析逻辑需要根据 SMP290 的实际广播数据格式调整
            // 以下是一个示例解析逻辑

            // 检查数据格式标识（如果有的话）
            // val protocolVersion = data[0].toInt() and 0xFF

            // 解析压力值 (假设是 16-bit unsigned, little endian, 单位 0.01 bar)
            val pressureRaw = if (data.size >= 3) {
                (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            } else {
                0
            }
            val pressure = pressureRaw / 100f  // 转换为 bar

            // 解析温度 (假设是 8-bit signed, 偏移 -40°C)
            val temperatureRaw = if (data.size >= 4) {
                data[3].toInt()
            } else {
                0
            }
            val temperature = temperatureRaw - 40f  // 应用偏移

            // 解析电量 (假设是 8-bit, 0-100%)
            val batteryLevel = if (data.size >= 5) {
                (data[4].toInt() and 0xFF).coerceIn(0, 100)
            } else {
                0
            }

            // 解析状态标志（可选）
            // val statusFlags = if (data.size >= 6) data[5].toInt() and 0xFF else 0

            Log.i(TAG, "Parsed TPMS data - Pressure: $pressure bar, Temp: $temperature°C, Battery: $batteryLevel%")

            TirePressureData(
                position = position,
                pressure = pressure,
                temperature = temperature,
                batteryLevel = batteryLevel,
                rssi = rssi,
                isValid = pressure in 0.0..10.0  // 简单的有效性检查
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse manufacturer data", e)
            null
        }
    }

    /**
     * 备用解析方法 - 从原始广播数据解析
     * 如果 ScanRecord 解析失败，可以尝试直接解析原始字节
     */
    fun parseFromRawBytes(
        rawData: ByteArray?,
        deviceAddress: String,
        rssi: Int,
        position: TirePosition
    ): TirePressureData? {
        if (rawData == null || rawData.size < 10) {
            return null
        }

        // TODO: 实现原始字节解析逻辑
        // 需要查找 AD Type 0xFF (Manufacturer Specific Data)
        // 格式: [Length][Type 0xFF][Manufacturer ID Low][Manufacturer ID High][Data...]

        var index = 0
        while (index < rawData.size) {
            val length = rawData[index].toInt() and 0xFF
            if (length == 0 || index + length >= rawData.size) break

            val type = rawData[index + 1].toInt() and 0xFF
            if (type == 0xFF) {  // Manufacturer Specific Data
                // 提取制造商数据
                val manufacturerData = rawData.copyOfRange(index + 2, index + 1 + length)
                return parseManufacturerData(
                    manufacturerData,
                    0,
                    deviceAddress,
                    rssi,
                    position
                )
            }

            index += length + 1
        }

        return null
    }

    /**
     * 扩展函数：ByteArray 转十六进制字符串
     */
    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { String.format("%02X", it) }
    }
}
