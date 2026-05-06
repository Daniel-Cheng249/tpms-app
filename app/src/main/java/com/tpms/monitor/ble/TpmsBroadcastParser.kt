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
 * 示例数据: 0201060AFFA602010122006645D70709434154504D53
 *
 * 结构解析:
 * - 02 01 06: Flags (Type 0x01, Length 2, Value 0x06)
 * - 0A FF A6 02 01 01 22 00 66 45 D7 07: Manufacturer Specific Data (Type 0xFF, Length 0x0A=10)
 *   - A6 02: 博世公司 ID (0x02A6, little endian)
 *   - 01 01: 保留/状态
 *   - 22: 电量字节 (bit 5 表示电压状态，与 2.2V 比较)
 *   - 00: 保留
 *   - 66: 压力值 (0x66 = 102, 102 * 1.375 = 140.25 kPa ≈ 1.40 bar)
 *   - 45: 温度值 (0x45 = 69, 69 + (-40) = 29°C)
 */
object TpmsBroadcastParser {

    private const val TAG = "TpmsBroadcastParser"

    // 博世 (Bosch) 公司 ID
    private const val BOSCH_MANUFACTURER_ID = 0x02A6

    // 数据字节偏移量 (基于 ScanRecord 解析后的制造商数据，已去掉 ID 字节 A6 02)
    // Manufacturer Data (去掉 A6 02 ID 后): 01 01 22 00 66 45 D7 07
    // - Byte 0-1: 01 01 - 保留/状态
    // - Byte 2: 22 - 电量字节 (bit 5 表示电压状态)
    // - Byte 3: 00 - 保留
    // - Byte 4: 66 - 压力值 (单字节，单位 1.375 kPa)
    // - Byte 5: 45 - 温度值 (单字节，需要 -40°C 偏移)
    // - Byte 6-7: D7 07 - 保留
    private const val OFFSET_STATUS = 0        // 状态字节起始
    private const val OFFSET_BATTERY_FLAGS = 2 // 电量标志字节 (bit 5)
    private const val OFFSET_PRESSURE = 4      // 压力值起始字节 (单字节)
    private const val OFFSET_TEMPERATURE = 5   // 温度值起始字节 (单字节)

    // 单位换算系数
    private const val PRESSURE_SCALE_KPA = 1.375f   // 每个单位 = 1.375 kPa
    private const val PRESSURE_KPA_TO_BAR = 0.01f   // kPa 转 bar
    private const val TEMPERATURE_OFFSET = -40f     // 温度偏移量 -40°C
    private const val BATTERY_VOLTAGE_THRESHOLD = 2.2f // 电压阈值 2.2V
    private const val BATTERY_BIT_MASK = 0x20       // bit 5 = 0b0010_0000

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
     * 新数据格式 (ScanRecord 返回的数据，已去掉制造商ID A6 02):
     * Byte 0-1: 01 01 - 保留/状态
     * Byte 2:   22    - 电量字节 (bit 5 表示电压状态)
     * Byte 3:   00    - 保留
     * Byte 4:   66    - 压力值 (单字节，0x66 = 102, 102 * 1.375 = 140.25 kPa)
     * Byte 5:   45    - 温度值 (单字节，0x45 = 69, 69 - 40 = 29°C)
     * Byte 6-7: D7 07 - 保留
     *
     * @param data 制造商特定数据 (已从 ScanRecord 中分离，不包含ID)
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
            // 解析压力值 (单字节)
            // 示例: 0x66 = 102, 102 * 1.375 kPa = 140.25 kPa ≈ 1.40 bar
            val pressureRaw = data[OFFSET_PRESSURE].toInt() and 0xFF
            val pressureKpa = pressureRaw * PRESSURE_SCALE_KPA
            val pressure = pressureKpa * PRESSURE_KPA_TO_BAR  // 转换为 bar

            // 解析温度值 (单字节，带 -40°C 偏移)
            // 示例: 0x45 = 69, 69 - 40 = 29°C
            val temperatureRaw = data[OFFSET_TEMPERATURE].toInt() and 0xFF
            val temperature = temperatureRaw + TEMPERATURE_OFFSET

            // 解析电量状态 (字节 2 的 bit 5)
            // bit 5 = 1: 电压 >= 2.2V (正常)
            // bit 5 = 0: 电压 < 2.2V (低电量)
            val batteryFlags = data[OFFSET_BATTERY_FLAGS].toInt() and 0xFF
            val isVoltageNormal = (batteryFlags and BATTERY_BIT_MASK) != 0
            val batteryLevel = if (isVoltageNormal) 80 else 20  // 简化：正常80%，低电量20%
            val batteryVoltage = if (isVoltageNormal) 2.5f else 2.0f  // 估算电压

            Log.i(TAG, "Parsed Bosch TPMS - Raw Pressure: 0x${pressureRaw.toString(16)} ($pressureRaw), " +
                    "Raw Temp: 0x${temperatureRaw.toString(16)} ($temperatureRaw), " +
                    "Battery Flags: 0x${batteryFlags.toString(16)}, Bit5: $isVoltageNormal -> " +
                    "Pressure: ${"%.2f".format(pressure)} bar (${"%.1f".format(pressureKpa)} kPa), " +
                    "Temp: ${"%.1f".format(temperature)}°C, " +
                    "Voltage: ${"%.2f".format(batteryVoltage)}V, Battery: $batteryLevel%")

            TirePressureData(
                position = position,
                pressure = pressure,
                temperature = temperature,
                batteryLevel = batteryLevel,
                rssi = rssi,
                isValid = pressure in 0.5f..5.0f && temperature in -40f..100f  // 有效性检查
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Bosch data", e)
            null
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
