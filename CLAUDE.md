# TPMS App 开发日志

## 项目概述
蓝牙胎压监测 (TPMS) Android App，使用 Kotlin + Jetpack Compose + MVVM 架构
- 目标传感器：Bosch SMP290
- 使用 BLE 5.2 广播模式（Manufacturer Specific Data type 0xFF）进行数据解析
- 支持 4 轮胎压/温度/电量/RSSI 实时显示

## 环境配置

### JDK
- 版本：Zulu JDK 17.0.18
- 路径：`C:\Program Files\Zulu\zulu-17`

### Android Studio
- 版本：8.2.0
- AGP: 8.2.0
- Gradle: 8.x (Version Catalogs)
- compileSdk: 34, minSdk: 26

### 网络代理 (中国)
- Clash HTTP 代理端口：**7892**
- gradle.properties 配置：
```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7892
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7892
```

## 当前进度 (2026-05-06)
- [x] 项目结构搭建
- [x] Gradle 配置完成（依赖下载成功）
- [x] 权限配置完成 (BLE + 位置)
- [x] BleScanner 基础实现
- [x] BleManager 基础实现
- [x] BLE 广播数据解析（SMP290 协议，Manufacturer Data type 0xFF）
- [x] 胎压/温度/电量/RSSI 实时显示
- [x] 设备映射绑定界面
- [x] DataStore 持久化存储映射关系
- [x] UI 完成（2x2 轮胎卡片、科幻车辆俯视图、状态指示器）
- [x] 应用图标（科技风格）
- [x] 编译通过，APK 可构建

## 下一步
1. 真机测试 BLE 扫描和广播数据解析
2. 实现低胎压/高胎压/低电量报警
3. 历史数据记录与图表
4. 启动页和权限请求引导页
5. 后台持续监测服务

## 项目结构
```
app/src/main/java/com/tpms/monitor/
├── MainActivity.kt          # 主 Activity，导航管理
├── ble/
│   ├── BleScanner.kt        # BLE 扫描器
│   ├── BleManager.kt        # BLE 连接管理器
│   ├── TpmsBroadcastParser.kt  # 广播数据解析
│   ├── BleDevice.kt         # 设备模型
│   ├── BleConnectionState.kt  # 连接状态
│   └── BleConstants.kt      # BLE 常量
├── data/
│   ├── TirePosition.kt      # 轮胎位置枚举
│   ├── TirePressureData.kt  # 胎压数据结构
│   ├── UiState.kt           # UI 状态
│   ├── TireDeviceMapping.kt  # 设备映射
│   └── MappingPreferences.kt  # DataStore 持久化
└── ui/
    ├── theme/               # Material 3 主题
    ├── components/          # UI 组件
    └── viewmodel/           # ViewModel
```

## 重要文件
- `gradle.properties` - 代理和 JDK 配置
- `local.properties` - SDK 和 Java 路径
- `app/src/main/AndroidManifest.xml` - 权限声明
