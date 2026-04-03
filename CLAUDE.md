# TPMS App 开发日志

## 项目概述
蓝牙胎压监测 (TPMS) Android App，使用 Kotlin + Jetpack Compose + MVVM 架构
- 目标传感器：Bosch SMP290
- 使用 BLE 5.2 进行设备扫描和数据读取

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

## 当前进度 (2026-03-15)
- [x] 项目结构搭建
- [x] Gradle 配置完成（依赖下载成功）
- [x] 权限配置完成 (BLE + 位置)
- [x] 基础 UI 组件已创建
- [x] BleScanner 基础实现
- [x] BleManager 基础实现
- [x] UI 完成（2x2 轮胎卡片、轮毂图标、状态指示器）
- [x] 编译通过，APK 已安装到模拟器

## 下一步
1. 使用 nRF Connect 抓取 SMP290 的实际 UUID 和数据格式
2. 更新 BleConstants.kt 中的 UUID
3. 完善 BleManager 数据解析逻辑
4. 连接真机测试 BLE 扫描功能

## 项目结构
```
app/src/main/java/com/tpms/monitor/
├── MainActivity.kt          # 主 Activity，权限请求
├── ble/
│   ├── BleScanner.kt        # BLE 扫描器
│   ├── BleManager.kt        # BLE 连接管理器
│   ├── BleDevice.kt         # 设备模型
│   └── BleConstants.kt      # BLE 常量
├── data/
│   ├── TirePosition.kt      # 轮胎位置枚举
│   ├── TirePressureData.kt  # 胎压数据结构
│   └── UiState.kt           # UI 状态
└── ui/
    ├── theme/               # Material 3 主题
    ├── components/          # UI 组件
    └── viewmodel/           # ViewModel
```

## 重要文件
- `gradle.properties` - 代理和 JDK 配置
- `local.properties` - SDK 和 Java 路径
- `app/src/main/AndroidManifest.xml` - 权限声明
