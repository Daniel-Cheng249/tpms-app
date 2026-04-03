package com.tpms.monitor.ble

/**
 * BLE 连接状态
 */
enum class BleConnectionState {
    DISCONNECTED,   // 未连接
    CONNECTING,     // 连接中
    CONNECTED,      // 已连接
    DISCONNECTING   // 断开中
}
