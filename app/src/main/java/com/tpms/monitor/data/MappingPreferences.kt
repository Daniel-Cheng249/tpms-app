package com.tpms.monitor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 轮胎-设备映射数据存储
 * 使用 DataStore 持久化保存轮胎位置与设备 MAC 地址的绑定关系
 */
class MappingPreferences(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tire_mapping")

    /**
     * 获取所有映射关系
     */
    val tireMappingFlow: Flow<TireDeviceMapping> = context.dataStore.data.map { preferences ->
        val mapping = mutableMapOf<TirePosition, String>()
        TirePosition.getAll().forEach { position ->
            preferences[position.toKey()]?.let { address ->
                mapping[position] = address
            }
        }
        TireDeviceMapping(mapping)
    }

    /**
     * 保存单个映射
     */
    suspend fun saveMapping(position: TirePosition, deviceAddress: String) {
        context.dataStore.edit { preferences ->
            preferences[position.toKey()] = deviceAddress
        }
    }

    /**
     * 移除单个映射
     */
    suspend fun removeMapping(position: TirePosition) {
        context.dataStore.edit { preferences ->
            preferences.remove(position.toKey())
        }
    }

    /**
     * 清除所有映射
     */
    suspend fun clearAllMappings() {
        context.dataStore.edit { preferences ->
            TirePosition.getAll().forEach { position ->
                preferences.remove(position.toKey())
            }
        }
    }

    /**
     * 获取已绑定的设备地址列表
     */
    fun getBoundAddressesFlow(): Flow<Set<String>> = tireMappingFlow.map { mapping ->
        mapping.mapping.values.toSet()
    }

    private fun TirePosition.toKey() = when (this) {
        TirePosition.FRONT_LEFT -> PreferencesKeys.FRONT_LEFT
        TirePosition.FRONT_RIGHT -> PreferencesKeys.FRONT_RIGHT
        TirePosition.REAR_LEFT -> PreferencesKeys.REAR_LEFT
        TirePosition.REAR_RIGHT -> PreferencesKeys.REAR_RIGHT
    }

    private object PreferencesKeys {
        val FRONT_LEFT = stringPreferencesKey("tire_fl")
        val FRONT_RIGHT = stringPreferencesKey("tire_fr")
        val REAR_LEFT = stringPreferencesKey("tire_rl")
        val REAR_RIGHT = stringPreferencesKey("tire_rr")
    }
}
