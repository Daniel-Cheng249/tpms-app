package com.tpms.monitor.data

/**
 * 胎压数据模型
 *
 * @param position 轮胎位置
 * @param pressure 压力值 (单位：bar)
 * @param temperature 温度 (单位：°C)
 * @param batteryLevel 电量百分比 (0-100)
 * @param rssi 信号强度 (单位：dBm, 例如 -65)
 * @param timestamp 数据时间戳
 * @param isValid 数据是否有效
 */
data class TirePressureData(
    val position: TirePosition,
    val pressure: Float,
    val temperature: Float = 0f,
    val batteryLevel: Int = 0,
    val rssi: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isValid: Boolean = true
) {
    /**
     * 压力状态判断
     * - LOW: < 1.8 bar
     * - HIGH: > 3.5 bar
     * - NORMAL: 1.8 - 3.5 bar
     */
    val status: PressureStatus
        get() = when {
            pressure < 1.8f -> PressureStatus.LOW
            pressure > 3.5f -> PressureStatus.HIGH
            else -> PressureStatus.NORMAL
        }

    /**
     * 信号强度状态
     * - STRONG: > -50 dBm (信号强)
     * - MEDIUM: -50 到 -70 dBm (信号中等)
     * - WEAK: < -70 dBm (信号弱)
     * - NONE: rssi == 0 (无信号)
     */
    val signalStatus: SignalStatus
        get() = when {
            rssi == 0 -> SignalStatus.NONE
            rssi > -50 -> SignalStatus.STRONG
            rssi > -70 -> SignalStatus.MEDIUM
            else -> SignalStatus.WEAK
        }
}

/**
 * 信号状态枚举
 */
enum class SignalStatus {
    NONE,     // 无信号 (rssi == 0)
    STRONG,   // 强信号 (> -50 dBm)
    MEDIUM,   // 中等信号 (-50 到 -70 dBm)
    WEAK      // 弱信号 (< -70 dBm)
}

/**
 * 压力状态枚举
 */
enum class PressureStatus {
    LOW,      // 低压 (< 1.8 bar) - 红色
    NORMAL,   // 正常 (1.8-3.5 bar) - 绿色
    HIGH      // 高压 (> 3.5 bar) - 橙色
}
