package com.tpms.monitor.data

/**
 * UI 状态密封类
 * 表示 App 当前的运行状态
 */
sealed class UiState {
    /** 空闲状态 - 未开始扫描 */
    object Idle : UiState()

    /** 扫描中 - 正在扫描附近的 BLE 设备 */
    object Scanning : UiState()

    /** 连接中 - 已找到设备，正在连接 */
    object Connecting : UiState()

    /** 部分连接 - 4 个传感器中只连接了一部分 */
    data class PartiallyConnected(
        val connected: Int,
        val total: Int = 4
    ) : UiState()

    /** 已连接 - 4 个传感器全部连接成功 */
    object Connected : UiState()

    /** 错误状态 */
    data class Error(val message: String) : UiState()
}

/**
 * 蓝牙连接状态
 */
enum class BleConnectionState {
    DISCONNECTED,   // 未连接
    CONNECTING,     // 连接中
    CONNECTED,      // 已连接
    DISCONNECTING   // 断开中
}
