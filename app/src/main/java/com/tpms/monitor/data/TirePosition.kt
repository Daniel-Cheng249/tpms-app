package com.tpms.monitor.data

/**
 * 轮胎位置枚举
 *
 * @param tireName 英文缩写，用于内部标识
 * @param displayName 中文显示名称
 */
enum class TirePosition(
    val tireName: String,
    val displayName: String
) {
    FRONT_LEFT("FL", "左前"),
    FRONT_RIGHT("FR", "右前"),
    REAR_LEFT("RL", "左后"),
    REAR_RIGHT("RR", "右后");

    companion object {
        fun getAll(): List<TirePosition> = values().toList()

        fun fromName(name: String): TirePosition? =
            values().find { it.tireName == name }
    }
}
