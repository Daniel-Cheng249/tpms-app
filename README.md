# TPMS UAES 胎压监测 App

蓝牙胎压监测系统，支持 Bosch SMP290 传感器。通过 BLE 广播扫描实时显示四轮胎压、温度、电量和信号强度。

## 功能

- BLE 广播扫描和传感器识别
- 4 轮胎压 / 温度 / 电量 / RSSI 信号强度实时显示
- 传感器与轮胎位置绑定管理（含 DataStore 持久化）
- 科幻风格仪表盘 UI（深色主题）
- 支持 Android 8.0+ 真机

## 技术栈

- Kotlin + Jetpack Compose
- MVVM + StateFlow 状态管理
- BLE 5.2 广播数据解析（Manufacturer Specific Data type 0xFF）
- DataStore 数据持久化

## 开发环境

- Android Studio 8.2.0 / AGP 8.2.0 / Gradle 8.x (Version Catalogs)
- JDK 17 (Zulu)
- minSdk 26, compileSdk 34, targetSdk 34
- 国内网络需配置 HTTP 代理（gradle.properties 已配置 Clash 7892 端口）

## 快速开始

```bash
# 克隆项目
git clone https://github.com/Daniel-Cheng249/tpms-app.git

# 编译调试 APK
./gradlew :app:assembleDebug
```

## 运行项目

```bash
# 编译调试 APK
./gradlew :app:assembleDebug

# 安装到连接的真机
./gradlew :app:installDebug
```

**注意事项**：
- 必须使用 Android 真机（模拟器不支持蓝牙 LE）
- 首次运行需授予蓝牙扫描和位置权限
- 通过 Android Studio 的 Logcat 查看调试日志

## 项目结构

```
app/src/main/java/com/tpms/monitor/
├── MainActivity.kt              # 主 Activity，导航管理
├── ble/
│   ├── BleScanner.kt            # BLE 扫描器
│   ├── BleManager.kt            # BLE GATT 连接管理
│   ├── TpmsBroadcastParser.kt   # 广播数据解析（SMP290 协议）
│   ├── BleDevice.kt             # BLE 设备模型
│   ├── BleConnectionState.kt    # 连接状态枚举
│   └── BleConstants.kt          # BLE 常量（UUID 占位符）
├── data/
│   ├── TirePosition.kt          # 轮胎位置枚举
│   ├── TirePressureData.kt      # 胎压数据结构
│   ├── UiState.kt               # UI 状态
│   ├── TireDeviceMapping.kt     # 设备映射模型
│   └── MappingPreferences.kt    # DataStore 持久化
└── ui/
    ├── theme/                   # Material 3 主题
    ├── components/              # Compose UI 组件
    │   ├── DashboardScreen.kt   # 主仪表盘
    │   ├── TirePressureCard.kt  # 轮胎卡片
    │   ├── DeviceMappingScreen.kt  # 设备绑定界面
    │   ├── VehicleTopView.kt    # 科幻车辆俯视图
    │   └── StatusIndicators.kt  # 状态指示器
    └── viewmodel/
        └── TirePressureViewModel.kt
```

## 持续优化

| 优先级 | 方向 | 当前状态 |
|--------|------|---------|
| P0 | BLE 数据解析准确性（真机验证） | 已基于实际广播数据更新，待验证 |
| P1 | 低胎压/高胎压/低电量报警 | 待实现 |
| P2 | 历史数据记录与图表、启动页、权限引导页 | 待实现 |
| P3 | 后台持续监测服务 | 待实现 |
