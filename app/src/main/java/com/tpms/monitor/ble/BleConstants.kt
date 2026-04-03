package com.tpms.monitor.ble

import java.util.UUID

/**
 * BLE 常量定义
 *
 * 注意：以下 UUID 是占位符，需要用 nRF Connect 抓取 SMP290 的实际 UUID 后替换
 */
object BleConstants {

    /**
     * TPMS 服务 UUID
     *
     * 获取方式：
     * 1. 使用 nRF Connect 扫描 SMP290 设备
     * 2. 连接后查看 Services 列表
     * 3. 找到 TPMS 相关的服务 UUID
     */
    val TPMS_SERVICE_UUID: UUID =
        UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")  // TODO: 替换为实际 UUID

    /**
     * 数据特征 UUID（用于接收传感器数据）
     *
     * 获取方式：
     * 1. 在 nRF Connect 中查看 TPMS 服务的 Characteristics
     * 2. 找到用于数据传输的特征 UUID
     */
    val DATA_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")  // TODO: 替换为实际 UUID

    /**
     * 客户端特征配置描述符 UUID (CCCD)
     * 用于启用/禁用通知
     */
    val CCCD_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /**
     * 通知启用值
     */
    val NOTIFICATION_ENABLE_VALUE = byteArrayOf(0x01, 0x00)
}
