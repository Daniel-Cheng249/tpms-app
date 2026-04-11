package com.tpms.monitor.ble

import android.bluetooth.le.ScanRecord
import android.util.Log
import com.tpms.monitor.data.TirePressureData
import com.tpms.monitor.data.TirePosition

/**
 * TPMS 广播数据解析器
 * 解析 BLE 广播帧中的 Manufacturer Specific Data (type 0xFF)
 *
 * 博世 SMP290 广播数据格式：
 * 示例数据: 02010613FFA6021A001B00FFFFFFFF01000000080100A503195905110767337203570218AF3EB8001A9032A602
 *
 * 结构解析:
 * - 02 01 06: Flags (Type 0x01, Length 2, Value 0x06)
 * - 13 FF A6 02 1A 00 1B 00 ...: Manufacturer Specific Data (Type 0xFF, Length 0x13=19)
 *   - A6 02: 博世公司 ID (0x02A6, little endian)
 *   - 1A 00: 压力值 (0x001A = 26, 需要确认单位)
 *   - 1B 00: 温度值 (0x001B = 27, 需要确认单位)
 */
object TpmsBroadcastParser {

    private const val TAG = "TpmsBroadcastParser"

    // 博世 (Bosch) 公司 ID
    private const val BOSCH_MANUFACTURER_ID = 0x02A6

    // 数据字节偏移量 (基于实际广播数据分析)
    // 广播数据: 02010613FFA6021A001B00FFFFFFFF01000000080100A503...
    // Manufacturer Data (从 A6 02 开始):
    // - Byte 0-1: A6 02 - 博世公司 ID (0x02A6)
    // - Byte 2-3: 1A 00 - 压力值 (0x001A = 26, 对应大气压 ~100kPa = 1bar)
    // - Byte 4-5: 1B 00 - 温度值 (0x001B = 27, 对应环境温度)
    // - Byte 6-9: FF FF FF FF - 保留/状态
    // - Byte 10-13: 01 00 00 00 - 保留
    // - Byte 14-15: 08 01 - 电池电压 (0x0108 = 264, 单位 0.01V = 2.64V)
    private const val OFFSET_PRESSURE = 2      // 压力值起始字节
    private const val OFFSET_TEMPERATURE = 4   // 温度值起始字节
    private const val OFFSET_BATTERY = 14      // 电池电压起始字节 (08 01)

    // 单位换算系数
    private const val PRESSURE_SCALE = 0.0385f  // 26 * 0.0385 ≈ 1 bar (大气压)
    private const val TEMPERATURE_OFFSET = 0f   // 直接值即为温度 (需要根据实际校准)
    private const val BATTERY_SCALE = 0.01f     // 单位 0.01V

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

        // 查找博世制造商数据
        val boschData = manufacturerData[BOSCH_MANUFACTURER_ID]
        if (boschData != null) {
            Log.d(TAG, "Found Bosch data (ID=0x${BOSCH_MANUFACTURER_ID.toString(16)}): ${boschData.toHexString()}")
            return parseBoschData(boschData, rssi, position)
        }

        // 如果没找到博世数据，遍历所有制造商数据尝试解析
        for (i in 0 until manufacturerData.size()) {
            val manufacturerId = manufacturerData.keyAt(i)
            val data = manufacturerData.valueAt(i)

            Log.d(TAG, "Manufacturer ID: 0x${manufacturerId.toString(16)}, Data: ${data?.toHexString()}")

            // 尝试作为博世格式解析
            val parsedData = parseBoschData(data, rssi, position)
            if (parsedData != null) {
                return parsedData
            }
        }

        return null
    }

    /**
     * 解析博世 TPMS 数据
     *
     * 数据格式 (基于实际抓包):
     * Byte 0-1: 博世公司 ID (0xA6, 0x02) - 已经从 ScanRecord 中分离
     * Byte 2-3: 压力值 (little endian, 16-bit)
     * Byte 4-5: 温度值 (little endian, 16-bit)
     * Byte 6+:  其他数据 (电量、状态等)
     *
     * @param data 制造商特定数据 (不包含前两个字节的 ID)
     * @param rssi 信号强度
     * @param position 轮胎位置
     * @return 解析后的数据
     */
    private fun parseBoschData(
        data: ByteArray?,
        rssi: Int,
        position: TirePosition
    ): TirePressureData? {
        if (data == null || data.size < 6) {
            Log.w(TAG, "Bosch data too short: ${data?.size ?: 0} bytes")
            return null
        }

        return try {
            // 解析压力值 (16-bit unsigned, little endian)
            // 示例: 1A 00 -> 0x001A = 26
            val pressureRaw = (data[OFFSET_PRESSURE].toInt() and 0xFF) or
                    ((data[OFFSET_PRESSURE + 1].toInt() and 0xFF) shl 8)

            // 解析温度值 (16-bit unsigned, little endian)
            // 示例: 1B 00 -> 0x001B = 27
            val temperatureRaw = (data[OFFSET_TEMPERATURE].toInt() and 0xFF) or
                    ((data[OFFSET_TEMPERATURE + 1].toInt() and 0xFF) shl 8)

            // 压力和温度换算 (基于实际测量数据校准)
            // - 压力：26 对应大气压 ~1 bar，所以比例系数约为 1/26 ≈ 0.0385
            // - 温度：直接值即为摄氏度 (需要根据实际校准)
            val pressure = pressureRaw * PRESSURE_SCALE
            val temperature = temperatureRaw + TEMPERATURE_OFFSET

            // 解析电池电压 (16-bit unsigned, little endian)
            // 示例: 08 01 -> 0x0108 = 264 (单位 0.01V = 2.64V)
            val batteryVoltageRaw = if (data.size >= OFFSET_BATTERY + 2) {
                (data[OFFSET_BATTERY].toInt() and 0xFF) or
                        ((data[OFFSET_BATTERY + 1].toInt() and 0xFF) shl 8)
            } else {
                0
            }
            val batteryVoltage = batteryVoltageRaw * BATTERY_SCALE  // 转换为伏特

            // 将电池电压转换为电量百分比 (假设 2.0V=0%, 3.0V=100%)
            val batteryLevel = voltageToPercentage(batteryVoltageRaw)

            Log.i(TAG, "Parsed Bosch TPMS - Raw Pressure: 0x${pressureRaw.toString(16)} ($pressureRaw), " +
                    "Raw Temp: 0x${temperatureRaw.toString(16)} ($temperatureRaw), " +
                    "Raw Battery: 0x${batteryVoltageRaw.toString(16)} ($batteryVoltageRaw) -> " +
                    "Pressure: ${"%.2f".format(pressure)} bar, Temp: ${"%.1f".format(temperature)}°C, " +
                    "Voltage: ${"%.2f".format(batteryVoltage)}V, Battery: $batteryLevel%")

            TirePressureData(
                position = position,
                pressure = pressure,
                temperature = temperature,
                batteryLevel = batteryLevel,
                rssi = rssi,
                isValid = pressure in 0.1f..10.0f  // 简单的有效性检查
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Bosch data", e)
            null
        }
    }

    /**
     * 将电池电压原始值转换为电量百分比
     * 假设电压原始值范围: 200 (2.0V) = 0%, 300 (3.0V) = 100%
     *
     * @param voltageRaw 电池电压原始值 (单位: 0.01V)
     * @return 电量百分比 (0-100)
     */
    private fun voltageToPercentage(voltageRaw: Int): Int {
        // 电压范围: 200 (2.0V) - 300 (3.0V)
        val minVoltage = 200
        val maxVoltage = 300

        return when {
            voltageRaw <= minVoltage -> 0
            voltageRaw >= maxVoltage -> 100
            else -> ((voltageRaw - minVoltage) * 100 / (maxVoltage - minVoltage)).coerceIn(0, 100)
        }
    }

    /**
     * 备用解析方法 - 从原始广播数据解析
     * 用于直接解析原始字节，不依赖 ScanRecord
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

        // 在原始广播数据中查找 Manufacturer Specific Data (Type 0xFF)
        var index = 0
        while (index < rawData.size) {
            if (index >= rawData.size) break

            val length = rawData[index].toInt() and 0xFF
            if (length == 0 || index + length >= rawData.size) break

            val type = rawData[index + 1].toInt() and 0xFF
            if (type == 0xFF) {  // Manufacturer Specific Data
                // 提取制造商数据 (包含制造商 ID)
                val manufacturerDataWithId = rawData.copyOfRange(index + 2, index + 2 + length - 1)

                // 检查是否是博世 ID
                if (manufacturerDataWithId.size >= 2) {
                    val id = (manufacturerDataWithId[0].toInt() and 0xFF) or
                            ((manufacturerDataWithId[1].toInt() and 0xFF) shl 8)

                    if (id == BOSCH_MANUFACTURER_ID && manufacturerDataWithId.size >= 8) {
                        // 去掉 ID 部分，解析数据
                        val dataOnly = manufacturerDataWithId.copyOfRange(2, manufacturerDataWithId.size)
                        return parseBoschData(dataOnly, rssi, position)
                    }
                }
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
