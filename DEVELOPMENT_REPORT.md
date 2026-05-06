# TPMS UAES Android App 开发工作报告

**最新更新**: 2026-05-06
**项目**: 汽车胎压监测 Android App (TPMS UAES)
**状态**: 核心功能实现完成，构建通过，待真机硬件测试

---

## 一、项目概述

| 项目 | 说明 |
|------|------|
| **目标** | 开发 Android 手机 App，通过 BLE 广播接收胎压传感器数据 |
| **显示内容** | 四个轮胎压力、温度、电量、信号强度 |
| **状态显示** | 扫描状态、连接状态 |
| **硬件** | Bosch SMP290 芯片 (BLE 5.2) |
| **传感器** | 4 个独立传感器 (每胎一个) |
| **通信方式** | BLE 广播 (Manufacturer Specific Data type 0xFF) |

## 二、技术选型

| 组件 | 选择 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose |
| 架构模式 | MVVM |
| 状态管理 | StateFlow |
| 最低版本 | Android 8.0 (API 26) |
| 目标版本 | Android 14 (API 34) |
| 主题 | 深色主题 |
| 状态色 | 绿(正常) / 红(低压) / 橙(高压) |
| 持久化 | DataStore (Preferences) |

## 三、系统架构

```
┌─────────────────────────────────────────────────────────┐
│                     UI 层 (Compose)                      │
│  DashboardScreen, TirePressureCard, VehicleTopView     │
│  DeviceMappingScreen, StatusIndicators                  │
└─────────────────────────────────────────────────────────┘
                          ↕ observe / action
┌─────────────────────────────────────────────────────────┐
│                   ViewModel 层                           │
│  TirePressureViewModel (StateFlow 状态管理)             │
└─────────────────────────────────────────────────────────┘
                          ↕ command / data flow
┌─────────────────────────────────────────────────────────┐
│                    BLE 通信层                            │
│  BleScanner (扫描), TpmsBroadcastParser (数据解析)      │
│  BleManager (连接管理)                                  │
└─────────────────────────────────────────────────────────┘
                          ↕ scan / broadcast
┌─────────────────────────────────────────────────────────┐
│                  硬件层 (Bosch SMP290)                   │
│  4 个独立传感器：左前/右前/左后/右后                      │
└─────────────────────────────────────────────────────────┘
```

## 四、核心设计决策

### 4.1 状态管理

```kotlin
sealed class UiState {
    object Idle : UiState()
    object Scanning : UiState()
    object Connecting : UiState()
    data class PartiallyConnected(val connected: Int, val total: Int) : UiState()
    object Connected : UiState()
    data class Error(val message: String) : UiState()
}
```

### 4.2 压力状态判断

```kotlin
enum class PressureStatus {
    LOW,      // < 1.8 bar - 红色
    NORMAL,   // 1.8-3.5 bar - 绿色
    HIGH      // > 3.5 bar - 橙色
}
```

### 4.3 广播数据解析

SMP290 广播数据格式（Manufacturer Data type 0xFF）：
- 制造商 ID：`0x02A6` (Bosch)
- 压力值：单字节，`值 * 1.375 kPa`
- 温度值：单字节，`值 - 40°C`
- 电量：字节 2 的 bit 5（1 = 正常 >=2.2V，0 = 低电量 <2.2V）

## 五、开发里程碑

| 时间 | 关键节点 |
|------|---------|
| 2026-03-14 | 项目骨架搭建、基础架构设计 |
| 2026-04-07 | UI 基础组件、设备绑定界面实现 |
| 2026-04 | BLE 广播数据解析迭代（基于实际数据多次校准偏移量和系数） |
| 2026-05-06 | 科幻车辆俯视图、文档同步更新 |

GitHub 仓库：https://github.com/Daniel-Cheng249/tpms-app

## 六、待完成事项

参考 `TODO.md`，按优先级：

- **P0**: 真机验证（BLE 扫描、数据解析准确性）
- **P1**: 报警功能（低/高胎压、低电量）、可配置阈值
- **P2**: 启动页、权限引导页、数据动画、历史图表
- **P3**: Room 数据库、后台服务、稳定性测试

## 七、关键注意事项

1. **模拟器不支持蓝牙** - 必须使用真实 Android 设备测试
2. **查看 Logcat 日志** - 在 Android Studio 底部点击 Logcat 标签查看调试信息
3. **权限请求** - 首次运行会弹出蓝牙权限请求，必须点击"允许"
4. **UUID 占位符** - `BleConstants.kt` 中的 Service/Characteristic UUID 仍是占位符，当前功能主要依赖广播数据解析

## 八、常用命令

```bash
# 编译调试 APK
./gradlew :app:assembleDebug

# 安装到真机
./gradlew :app:installDebug

# 查看提交历史
git log --oneline

# 快速推送代码
git add . && git commit -m "描述" && git push
```
